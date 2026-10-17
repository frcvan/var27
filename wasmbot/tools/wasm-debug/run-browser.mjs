// Drive headless Chrome over the DevTools Protocol to load the sim page and
// report what the status element says after boot. This exercises the real
// browser interface the user hits.
//
// Usage:
//   1. Serve the consumer's build/wasm/site directory on port 8099 after wasmSite.
//   2. Start headless Chrome with a DevTools port:
//        google-chrome-stable --headless=new --no-sandbox --disable-gpu \
//          --remote-debugging-port=9222 --user-data-dir=/tmp/chrome-sim about:blank
//   3. node <library>/tools/wasm-debug/run-browser.mjs [pageUrl] [debugPort]
const url = process.argv[2] ?? "http://127.0.0.1:8099/sim/index.html";
const debugPort = process.argv[3] ?? "9222";

const res = await fetch(`http://127.0.0.1:${debugPort}/json/new?${encodeURIComponent(url)}`, {
  method: "PUT",
});
const target = await res.json();

const ws = new WebSocket(target.webSocketDebuggerUrl);
let id = 0;
const pending = new Map();
const logs = [];

function send(method, params = {}) {
  return new Promise((resolve) => {
    const msgId = ++id;
    pending.set(msgId, resolve);
    ws.send(JSON.stringify({ id: msgId, method, params }));
  });
}

await new Promise((resolve) => (ws.onopen = resolve));
ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  if (msg.id && pending.has(msg.id)) {
    pending.get(msg.id)(msg.result);
    pending.delete(msg.id);
  } else if (msg.method === "Runtime.consoleAPICalled") {
    logs.push(
      `[${msg.params.type}] ` +
        msg.params.args.map((a) => a.value ?? a.description ?? "").join(" "),
    );
  } else if (msg.method === "Runtime.exceptionThrown") {
    logs.push("EXCEPTION " + (msg.params.exceptionDetails?.text ?? ""));
  }
};

await send("Runtime.enable");
await send("Page.enable");
await send("Page.navigate", { url });

const deadline = Date.now() + 90000;
let status = "";
while (Date.now() < deadline) {
  const r = await send("Runtime.evaluate", {
    expression: "document.querySelector('#status')?.textContent ?? ''",
    returnByValue: true,
  });
  status = r?.result?.value ?? "";
  if (status && status !== "Loading robot…") break;
  await new Promise((r) => setTimeout(r, 500));
}

console.log("STATUS:", status);
if (logs.length) console.log("CONSOLE:\n" + logs.join("\n"));
process.exit(0);
