export function createWasmContinuations() {
  if (
    typeof WebAssembly === "undefined" ||
    typeof WebAssembly.Suspending !== "function" ||
    typeof WebAssembly.promising !== "function"
  ) {
    throw new Error(
      "WASM continuations require JSPI: WebAssembly.Suspending and WebAssembly.promising must be available.",
    );
  }

  const states = new WeakMap();
  let instantiationStarted = false;
  let continuationBody = null;
  let tickBody = null;
  let tickReceiver = null;
  let ticking = false;

  async function instantiate(bytes, wasmImports) {
    if (instantiationStarted) {
      throw new Error("The continuation engine can instantiate only one WASM module.");
    }
    instantiationStarted = true;

    const imports = { ...wasmImports };
    const found = { wasmContinuationResume: null, wasmContinuationYield: null };
    for (const [moduleName, namespace] of Object.entries(wasmImports)) {
      let wrappedNamespace = null;
      for (const [name, imported] of Object.entries(namespace)) {
        const matches = Object.keys(found).filter((method) => name.includes(method));
        if (matches.length === 0) continue;
        if (matches.length !== 1) {
          throw new Error(`Ambiguous continuation import: ${moduleName}.${name}`);
        }
        const method = matches[0];
        if (found[method] !== null) {
          throw new Error(
            `Expected one ${method} import; found ${found[method]} and ${moduleName}.${name}.`,
          );
        }
        if (typeof imported !== "function") {
          throw new TypeError(`Continuation import ${moduleName}.${name} is not a function.`);
        }
        found[method] = `${moduleName}.${name}`;
        if (wrappedNamespace === null) {
          wrappedNamespace = { ...namespace };
          imports[moduleName] = wrappedNamespace;
        }
        wrappedNamespace[name] = new WebAssembly.Suspending(imported);
      }
    }
    for (const [method, name] of Object.entries(found)) {
      if (name === null) throw new Error(`Missing ${method} WASM import.`);
    }

    const result = await WebAssembly.instantiate(bytes, imports);
    const instance = result instanceof WebAssembly.Instance ? result : result.instance;
    const bodyExport = findExport(instance.exports, "wasmContinuationBody", 1);
    const tickExport = findExport(instance.exports, "tick", 1);
    const promisingBody = WebAssembly.promising(bodyExport);
    const promisingTick = WebAssembly.promising(tickExport);
    continuationBody = promisingBody;
    tickBody = promisingTick;
    return result;
  }

  function findExport(exports, methodName, parameterCount) {
    let matchedName = null;
    let matchedMethod = null;
    let matchedMetadata = null;
    for (const [name, method] of Object.entries(exports)) {
      if (!name.startsWith("META.")) continue;
      const metadata = name.split(" ");
      if (metadata[1] !== methodName) continue;
      if (matchedName !== null) {
        throw new Error(
          `Expected one ${methodName} metadata export; found ${matchedName} and ${name}.`,
        );
      }
      matchedName = name;
      matchedMethod = method;
      matchedMetadata = metadata;
    }
    if (matchedName === null) {
      throw new Error(`Missing ${methodName} WASM metadata export.`);
    }
    if (
      matchedMetadata.length !== 3 ||
      matchedMetadata[0].length === "META.".length ||
      matchedMetadata[2].length === 0 ||
      typeof matchedMethod !== "function" ||
      matchedMethod.length !== parameterCount
    ) {
      throw new Error(
        `Invalid ${methodName} metadata export ${matchedName}; expected no declared arguments and ${parameterCount} raw WASM parameters.`,
      );
    }
    return matchedMethod;
  }

  function create(continuation) {
    const handle = {};
    states.set(handle, {
      continuation,
      phase: "new",
      waiter: null,
      wake: null,
      failed: false,
      error: undefined,
    });
    return handle;
  }

  function finish(state, failed, error) {
    state.phase = "done";
    state.failed = failed;
    state.error = error;
    state.continuation = null;
    state.wake = null;
    const waiter = state.waiter;
    state.waiter = null;
    if (waiter !== null) {
      if (failed) waiter.reject(error);
      else waiter.resolve();
    }
  }

  function resume(handle) {
    const state = states.get(handle);
    if (state === undefined) {
      return Promise.reject(new TypeError("Unknown WASM continuation state."));
    }
    if (continuationBody === null) {
      return Promise.reject(new Error("The WASM continuation engine is not instantiated."));
    }
    if (state.phase === "running") {
      return Promise.reject(new Error("Cannot resume a WASM continuation concurrently."));
    }
    if (state.phase === "done") {
      return state.failed ? Promise.reject(state.error) : Promise.resolve();
    }

    const starting = state.phase === "new";
    const wake = state.wake;
    state.phase = "running";
    state.wake = null;
    const resumed = new Promise((resolve, reject) => {
      state.waiter = { resolve, reject };
    });
    if (starting) {
      try {
        continuationBody(state.continuation).then(
          () => finish(state, false),
          (error) => finish(state, true, error),
        );
      } catch (error) {
        finish(state, true, error);
      }
    } else {
      wake();
    }
    return resumed;
  }

  function yieldContinuation(handle) {
    const state = states.get(handle);
    if (state === undefined) throw new TypeError("Unknown WASM continuation state.");
    if (state.phase !== "running" || state.waiter === null) {
      throw new Error("Only a running WASM continuation can yield.");
    }

    // The Java body waits here while its caller resumes on a separate JSPI stack.
    const suspended = new Promise((resolve) => {
      state.wake = resolve;
    });
    const waiter = state.waiter;
    state.waiter = null;
    state.phase = "suspended";
    waiter.resolve();
    return suspended;
  }

  function bindTick(receiver) {
    if (tickReceiver !== null) throw new Error("The WASM tick receiver is already bound.");
    tickReceiver = receiver;
  }

  async function tick() {
    if (tickBody === null || tickReceiver === null) {
      throw new Error("The WASM continuation engine is not instantiated.");
    }
    if (ticking) throw new Error("Cannot run WASM ticks concurrently.");
    ticking = true;
    try {
      return await tickBody(tickReceiver);
    } finally {
      ticking = false;
    }
  }

  return { create, resume, yield: yieldContinuation, instantiate, bindTick, tick };
}
