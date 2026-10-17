import { createWasmContinuations } from "./continuations.js";

const PERIOD_SECONDS = 0.02;
const keys = new Set();
const pose = { x: 0, y: 0, rotation: 0 };
const driverStation = {
  enabled: true,
  alliance: "blue",
  leftX: 0,
  leftY: 0,
  rightX: 0,
};
let nowNanos = 0;

const status = document.querySelector("#status");
const x = document.querySelector("#x");
const y = document.querySelector("#y");
const rotation = document.querySelector("#rotation");

window.wasmNowNanos = () => nowNanos;
window.wasmDs = () => {
  driverStation.alliance = keys.has("shift") ? "red" : "blue";
  driverStation.leftX = axis("a", "d");
  driverStation.leftY = axis("w", "s");
  driverStation.rightX = axis("q", "e");
  return driverStation;
};
window.wasmNtGet = () => undefined;
window.wasmNtPut = (path, value) => {
  if (path === "/Sim/Pose/X") pose.x = value;
  else if (path === "/Sim/Pose/Y") pose.y = value;
  else if (path === "/Sim/Pose/Rotation") pose.rotation = value;
};

addEventListener("keydown", (event) => {
  keys.add(event.key.toLowerCase());
  paintKeys();
});
addEventListener("keyup", (event) => {
  keys.delete(event.key.toLowerCase());
  paintKeys();
});
addEventListener("blur", () => {
  keys.clear();
  paintKeys();
});

function axis(negative, positive) {
  return (keys.has(positive) ? 1 : 0) - (keys.has(negative) ? 1 : 0);
}

function paintKeys() {
  for (const key of ["w", "a", "s", "d", "q", "e"]) {
    document
      .querySelector(`#key-${key}`)
      .classList.toggle("active", keys.has(key));
  }
}

function paintPose() {
  x.textContent = `${pose.x.toFixed(3)} m`;
  y.textContent = `${pose.y.toFixed(3)} m`;
  rotation.textContent = `${((pose.rotation * 180) / Math.PI).toFixed(2)}°`;
}

try {
  globalThis.wasmContinuations = createWasmContinuations();
  const BUILT_FILE = "wasm.js";
  await import(`./${BUILT_FILE}`);

  const config = new GraalVM.Config();
  config.wasm_path = `./${BUILT_FILE}.wasm`;
  await GraalVM.run({}, config);

  window.wasmStart();
  status.textContent = "Running";
  let ticking = false;
  const timer = setInterval(async () => {
    if (ticking) return;
    ticking = true;
    try {
      nowNanos += PERIOD_SECONDS * 1e9;
      await window.wasmTick();
      paintPose();
    } catch (error) {
      clearInterval(timer);
      status.textContent = `Robot stopped: ${errorText(error)}`;
      console.error(errorText(error));
    } finally {
      ticking = false;
    }
  }, PERIOD_SECONDS * 1000);
} catch (error) {
  status.textContent = `Robot failed to start: ${errorText(error)}`;
  console.error(errorText(error));
}

function errorText(error) {
  const parts = [];
  let current = error;
  for (let depth = 0; current && depth < 8; depth++) {
    const message =
      typeof current.getMessage === "function"
        ? current.getMessage()?.toString()
        : null;
    const text =
      typeof message === "string"
        ? message
        : (current.stack ?? current.message ?? String(current));
    if (text && !text.startsWith("[Java Proxy")) parts.push(text);
    current =
      typeof current.getCause === "function" ? current.getCause() : null;
  }
  return parts.join("\ncaused by: ") || String(error);
}
