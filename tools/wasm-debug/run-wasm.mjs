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
import { fileURLToPath, pathToFileURL } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const dir = path.resolve(process.argv[2] ?? path.join(repoRoot, "build/native/nativeCompile"));

globalThis.document = {
  currentScript: { src: pathToFileURL(path.join(dir, "var27.js")).href },
};

let nowNanos = 0;
const pose = {};
globalThis.wasmNowNanos = () => nowNanos;
globalThis.wasmDs = () => ({ enabled: true, alliance: "blue", leftX: 0, leftY: 0, rightX: 0 });
globalThis.wasmNtGet = () => undefined;
globalThis.wasmNtPut = (p, v) => {
  pose[p] = v;
};

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
  globalThis.wasmStart();
  for (let i = 0; i < 5; i++) {
    nowNanos += 0.02 * 1e9;
    globalThis.wasmTick();
  }
  console.log("STARTED_OK", JSON.stringify(pose));
} catch (error) {
  console.log("START_FAILED: " + errorText(error));
  try {
    if (typeof error.getStackTrace === "function") {
      console.log("JAVA_STACK:\n" + error.getStackTrace());
    }
  } catch (e) {
    console.log("STACK_ERR", e);
  }
}
