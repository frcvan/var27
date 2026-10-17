// Build with ./gradlew nativeSmokeCompile, then run with a JSPI-capable Node or Bun.
import assert from "node:assert/strict";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { createWasmContinuations } from "../../web/sim/continuations.js";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const dir = path.resolve(process.argv[2] ?? path.join(root, "build/native/nativeSmokeCompile"));
globalThis.document = { currentScript: { src: pathToFileURL(path.join(dir, "var27.js")).href } };
const values = {};
let now = 0;
globalThis.wasmNowNanos = () => now;
globalThis.wasmDs = () => ({ enabled: true, alliance: "blue", leftX: 0, leftY: 0, rightX: 0 });
globalThis.wasmNtGet = () => undefined;
globalThis.wasmNtPut = (path, value) => {
  assert.equal(typeof path, "string");
  values[path] = value;
};
globalThis.wasmContinuations = createWasmContinuations();
await import(pathToFileURL(path.join(dir, "var27.js")).href);
const config = new GraalVM.Config();
config.wasm_path = path.join(dir, "var27.js.wasm");
await GraalVM.run({}, config);

const expected = {
  Boolean: true,
  Number: 1.25,
  String: "hello",
  Booleans: [true, false],
  Shorts: [-3, 7],
  Ints: [-100000, 200000],
  Floats: [1.25, -2.5],
  Doubles: [1.25, -2.5],
  Strings: ["a", "b"],
  Empty: [],
  SafeLong: 9007199254740991,
  UnsafeLong: "9007199254740992",
  Longs: [0, 9007199254740991, "9007199254740992", "-9223372036854775808", "9223372036854775807"],
  Raw: "00ff10",
  EmptyStructs: [],
};
for (const [key, value] of Object.entries(expected)) {
  assert.deepEqual(values[`/Review/${key}`], value, key);
}
function struct(x, y, rotation) {
  const bytes = Buffer.alloc(24);
  bytes.writeDoubleLE(x, 0);
  bytes.writeDoubleLE(y, 8);
  bytes.writeDoubleLE(rotation, 16);
  return bytes.toString("hex");
}
assert.equal(values["/Review/Struct"], struct(3, 4, -1));
assert.deepEqual(values["/Review/Structs"], [struct(1.25, -2.5, 0.5), struct(3, 4, -1)]);
function field(tag, value) {
  const bytes = Buffer.alloc(9);
  bytes[0] = tag;
  bytes.writeDoubleLE(value, 1);
  return bytes;
}
const translation = Buffer.concat([field(9, 3), field(17, 4)]);
const rotation = field(9, -1);
const protobuf = Buffer.concat([
  Buffer.from([10, translation.length]), translation,
  Buffer.from([18, rotation.length]), rotation,
]);
assert.equal(values["/Review/Proto"], protobuf.toString("hex"));
assert.equal(typeof globalThis.wasmProbe?.beginYield, "function", "native smoke entry did not finish");
globalThis.wasmStart();
async function tick(count = 1) {
  for (let i = 0; i < count; i++) {
    now += 20_000_000;
    await globalThis.wasmTick();
  }
}
const state = () => globalThis.wasmProbe.state().toString();
globalThis.wasmProbe.beginYield();
await tick();
assert.equal(state(), "[first]");
await tick();
assert.equal(state(), "[first, local=42]");
await tick(2);
assert.equal(state(), "[first, local=42, last=43]", "completed command must not restart");
globalThis.wasmProbe.reschedule();
await tick(3);
assert.equal(state(), "[first, local=42, last=43, first, local=42, last=43]", "rescheduling needs a fresh stack");
globalThis.wasmProbe.beginPark();
await tick(2);
assert.equal(state(), "[parked]");
globalThis.wasmProbe.cancel();
await tick(2);
assert.equal(state(), "[parked, canceled]");
globalThis.wasmProbe.beginAwait();
await tick();
assert.equal(state(), "[parent-start, child-start]");
await tick(5);
assert.equal(state(), "[parent-start, child-start, child-end, parent-end]", "await must wait for child completion");
globalThis.wasmProbe.beginAwait();
await tick();
globalThis.wasmProbe.cancel();
await tick(5);
assert.equal(state(), "[parent-start, child-start]", "canceling a parent must cancel its child");
globalThis.wasmProbe.beginFailure();
await assert.rejects(tick(), (error) => {
  assert.equal(error.getMessage().toString(), "probe-target-error");
  return true;
});
await tick();
assert.equal(state(), "[throws, original-error]", "the original exception must be emitted once and the failed command removed");
const firstTick = globalThis.wasmTick();
await assert.rejects(globalThis.wasmTick(), /concurrently/);
await firstTick;
console.log("NATIVE_SMOKE_OK: dynamics, serialization, yielded locals, completion, rescheduling, park/cancel, await, child cancellation, Java exceptions, overlapping-tick rejection");
