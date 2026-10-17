// JSPI-capable Node/Bun harness for a consumer's GraalVM Web Image WASM image.
//
// Usage:
//   node <library>/tools/wasm-debug/run-wasm.mjs [buildDir] [imageName]
//
// buildDir defaults to the consumer cwd's build/native/nativeCompile; imageName
// defaults to wasm. The directory must contain <imageName>.js and <imageName>.js.wasm.
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
if (args.length > 2 || args.some((arg) => arg.startsWith("--"))) {
  throw new Error("Usage: run-wasm.mjs [buildDir] [imageName]");
}
const [buildDir, imageName = "wasm"] = args;
const dir = path.resolve(buildDir ?? path.join(process.cwd(), "build/native/nativeCompile"));
const imagePath = path.join(dir, `${imageName}.js`);

globalThis.document = {
  currentScript: { src: pathToFileURL(imagePath).href },
};

let nowNanos = 0;
const telemetry = new Map();
const ds = { enabled: false, alliance: "blue", leftX: 0, leftY: 0, rightX: 0 };
globalThis.wasmNowNanos = () => nowNanos;
globalThis.wasmDs = () => ds;
globalThis.wasmNtGet = () => undefined;
globalThis.wasmNtPut = (p, v) => {
  assert.equal(typeof p, "string", "telemetry paths must be JS strings");
  telemetry.set(p, v);
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
    }
  };
  globalThis.wasmStart();
  globalThis.wasmStart(); // Repeated startup must not rebuild or reset the robot.
  await tick(25);
  ds.enabled = true;
  await tick(100);
  for (const [path, value] of telemetry) {
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
    ticks,
    telemetryEntries: telemetry.size,
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
