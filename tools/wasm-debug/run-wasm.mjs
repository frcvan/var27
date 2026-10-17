// Node harness for the built GraalVM Web Image WASM image.
//
// Usage:
//   node tools/wasm-debug/run-wasm.mjs [buildDir]
//
// buildDir defaults to build/native/nativeCompile and must contain var27.js /
// var27.js.wasm produced by `./gradlew nativeCompile`.
//
// It shims the handful of globals the browser page (web/sim/sim.js) provides,
// imports the GraalVM glue, runs the image, then calls wasmStart()/wasmTick().
// Any Java throwable is printed with its message chain and, when the image
// exposes one, its Java backtrace.
import path from "node:path";
import assert from "node:assert/strict";
import { fileURLToPath, pathToFileURL } from "node:url";
import { createWasmContinuations } from "../../web/sim/continuations.js";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const dir = path.resolve(process.argv[2] ?? path.join(repoRoot, "build/native/nativeCompile"));

globalThis.document = {
  currentScript: { src: pathToFileURL(path.join(dir, "var27.js")).href },
};

let nowNanos = 0;
const pose = {};
const ds = { enabled: false, alliance: "blue", leftX: 0, leftY: -1, rightX: 0 };
globalThis.wasmNowNanos = () => nowNanos;
globalThis.wasmDs = () => ds;
globalThis.wasmNtGet = () => undefined;
globalThis.wasmNtPut = (p, v) => {
  assert.equal(typeof p, "string", "telemetry paths must be JS strings");
  pose[p] = v;
};

globalThis.wasmContinuations = createWasmContinuations();
await import(pathToFileURL(path.join(dir, "var27.js")).href);

const config = new globalThis.GraalVM.Config();
config.wasm_path = path.join(dir, "var27.js.wasm");
await globalThis.GraalVM.run({}, config);

function errorText(error) {
  const parts = [];
  let current = error;
  for (let depth = 0; current && depth < 8; depth++) {
    const message =
      typeof current.getMessage === "function" ? current.getMessage()?.toString() : null;
    const text =
      typeof message === "string"
        ? message
        : (current.stack ?? current.message ?? String(current));
    if (text && !text.startsWith("[Java Proxy")) parts.push(text);
    current = typeof current.getCause === "function" ? current.getCause() : null;
  }
  return parts.join("\ncaused by: ") || String(error);
}

try {
  const tick = async (count) => {
    for (let i = 0; i < count; i++) {
      nowNanos += 20_000_000;
      await globalThis.wasmTick();
      for (const path of ["/Sim/Pose/X", "/Sim/Pose/Y", "/Sim/Pose/Rotation"]) {
        assert.equal(typeof pose[path], "number", path);
        assert.ok(Number.isFinite(pose[path]), `${path} must be finite`);
      }
    }
  };
  globalThis.wasmStart();
  globalThis.wasmStart(); // Repeated startup must not rebuild or reset the robot.
  await tick(25);
  assert.equal(pose["/Sim/Pose/X"], 0, "disabled input must not drive");
  ds.enabled = true;
  await tick(100);
  const forwardX = pose["/Sim/Pose/X"];
  assert.ok(forwardX > 1, "forward input must drive in positive X");
  assert.ok(Math.abs(pose["/Sim/Pose/Y"]) < 1e-6, "forward drive must not drift sideways");
  ds.alliance = "red";
  await tick(100);
  assert.ok(pose["/Sim/Pose/X"] < forwardX - 1, "red alliance must reverse field direction");
  ds.leftY = 0;
  ds.leftX = 1;
  const beforeY = pose["/Sim/Pose/Y"];
  await tick(100);
  assert.ok(pose["/Sim/Pose/Y"] > beforeY + 1, "red alliance lateral drive must move positive Y");
  ds.leftX = 0;
  ds.rightX = -0.5;
  const beforeRotation = pose["/Sim/Pose/Rotation"];
  await tick(25);
  assert.ok(pose["/Sim/Pose/Rotation"] > beforeRotation + 0.1, "rotation input must rotate");
  ds.enabled = false;
  ds.leftY = -1;
  await tick(100); // Allow physical spin-down after disabling with nonzero axes.
  const stoppedX = pose["/Sim/Pose/X"];
  const stoppedY = pose["/Sim/Pose/Y"];
  await tick(50);
  assert.ok(Math.hypot(pose["/Sim/Pose/X"] - stoppedX, pose["/Sim/Pose/Y"] - stoppedY) < 1e-5,
    "disabled robot must settle despite held axes");
  ds.enabled = true;
  ds.rightX = 0;
  ds.leftY = 0;
  await tick(25);
  for (const [path, value] of Object.entries(pose)) {
    if (Array.isArray(value)) {
      assert.ok(Object.keys(value).every((key) => /^\d+$/.test(key)), `${path}: malformed array`);
    }
  }
  const samples = [];
  for (let i = 0; i < 1000; i++) {
    const started = performance.now();
    await tick(1);
    samples.push(performance.now() - started);
  }
  samples.sort((a, b) => a - b);
  console.log("WASM_SMOKE_OK", JSON.stringify({
    forwardX,
    pose: [pose["/Sim/Pose/X"], pose["/Sim/Pose/Y"], pose["/Sim/Pose/Rotation"]],
    tickMedianMs: samples[500],
    tickP95Ms: samples[950],
    tickMaxMs: samples[999],
  }));
} catch (error) {
  console.log("START_FAILED: " + errorText(error));
  try {
    if (typeof error.getStackTrace === "function") {
      console.log("JAVA_STACK:\n" + error.getStackTrace());
    }
  } catch (e) {
    console.log("STACK_ERR", e);
  }
  process.exitCode = 1;
}
