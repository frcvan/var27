// JSPI-capable Node/Bun harness for a consumer's GraalVM Web Image WASM image.
//
// Usage:
//   node <library>/tools/wasm-debug/run-wasm.mjs [buildDir] [imageName] [--drive]
//
// buildDir defaults to the consumer cwd's build/native/nativeCompile; imageName
// defaults to wasm. The directory must contain <imageName>.js and <imageName>.js.wasm.
// --drive enables the numeric /Sim/Pose drive acceptance scenarios.
//
// It shims the handful of globals the browser page (web/sim/sim.js) provides,
// imports the GraalVM glue, runs the image, then calls wasmStart()/wasmTick().
// Any Java throwable is printed with its message chain and, when the image
// exposes one, its Java backtrace.
import path from "node:path";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { createWasmContinuations } from "../../web/sim/continuations.js";

const args = process.argv.slice(2);
const verifyDrive = args.includes("--drive");
const [buildDir, imageName = "wasm"] = args.filter((arg) => arg !== "--drive");
const dir = path.resolve(buildDir ?? path.join(process.cwd(), "build/native/nativeCompile"));
const imagePath = path.join(dir, `${imageName}.js`);

globalThis.document = {
  currentScript: { src: pathToFileURL(imagePath).href },
};

let nowNanos = 0;
const telemetry = {};
const ds = { enabled: false, alliance: "blue", leftX: 0, leftY: -1, rightX: 0 };
globalThis.wasmNowNanos = () => nowNanos;
globalThis.wasmDs = () => ds;
globalThis.wasmNtGet = () => undefined;
globalThis.wasmNtPut = (p, v) => {
  assert.equal(typeof p, "string", "telemetry paths must be JS strings");
  telemetry[p] = v;
};

globalThis.wasmContinuations = createWasmContinuations();
await import(pathToFileURL(imagePath).href);

const config = new globalThis.GraalVM.Config();
config.wasm_path = `${imagePath}.wasm`;
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
  let ticks = 0;
  const tick = async (count) => {
    for (let i = 0; i < count; i++) {
      nowNanos += 20_000_000;
      await globalThis.wasmTick();
      ticks++;
      if (verifyDrive) {
        for (const path of ["/Sim/Pose/X", "/Sim/Pose/Y", "/Sim/Pose/Rotation"]) {
          assert.equal(typeof telemetry[path], "number", path);
          assert.ok(Number.isFinite(telemetry[path]), `${path} must be finite`);
        }
      }
    }
  };
  globalThis.wasmStart();
  globalThis.wasmStart(); // Repeated startup must not rebuild or reset the robot.
  await tick(25);
  if (verifyDrive) {
    assert.equal(telemetry["/Sim/Pose/X"], 0, "disabled input must not drive");
  }
  ds.enabled = true;
  await tick(100);
  let driveResult = {};
  if (verifyDrive) {
    const forwardX = telemetry["/Sim/Pose/X"];
    assert.ok(forwardX > 1, "forward input must drive in positive X");
    assert.ok(Math.abs(telemetry["/Sim/Pose/Y"]) < 1e-6, "forward drive must not drift sideways");
    ds.alliance = "red";
    await tick(100);
    assert.ok(telemetry["/Sim/Pose/X"] < forwardX - 1, "red alliance must reverse field direction");
    ds.leftY = 0;
    ds.leftX = 1;
    const beforeY = telemetry["/Sim/Pose/Y"];
    await tick(100);
    assert.ok(telemetry["/Sim/Pose/Y"] > beforeY + 1, "red alliance lateral drive must move positive Y");
    ds.leftX = 0;
    ds.rightX = -0.5;
    const beforeRotation = telemetry["/Sim/Pose/Rotation"];
    await tick(25);
    assert.ok(telemetry["/Sim/Pose/Rotation"] > beforeRotation + 0.1, "rotation input must rotate");
    ds.enabled = false;
    ds.leftY = -1;
    await tick(100); // Allow physical spin-down after disabling with nonzero axes.
    const stoppedX = telemetry["/Sim/Pose/X"];
    const stoppedY = telemetry["/Sim/Pose/Y"];
    await tick(50);
    assert.ok(Math.hypot(telemetry["/Sim/Pose/X"] - stoppedX, telemetry["/Sim/Pose/Y"] - stoppedY) < 1e-5,
      "disabled robot must settle despite held axes");
    ds.enabled = true;
    ds.rightX = 0;
    ds.leftY = 0;
    await tick(25);
    driveResult = { forwardX };
  }
  for (const [path, value] of Object.entries(telemetry)) {
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
  if (verifyDrive) {
    driveResult.pose = [telemetry["/Sim/Pose/X"], telemetry["/Sim/Pose/Y"], telemetry["/Sim/Pose/Rotation"]];
  }
  console.log("WASM_SMOKE_OK", JSON.stringify({
    ticks,
    telemetryEntries: Object.keys(telemetry).length,
    ...driveResult,
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
