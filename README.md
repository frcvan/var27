# VAR27: Very Awesome Robot '27

This is currently a WPILib 2027 + Commands V3 testing repository with simulation support.

## Requirements

- WPILib 2027 Alpha 7
- Java 25
- Git

## Clone

```sh
git clone https://github.com/frcvan/var27
cd var27
```

## Run

Run the desktop simulator, including the simulation GUI:

```sh
./gradlew simulateJava
```

Build the project and run its tests:

```sh
./gradlew build
./gradlew test
```

## Wasm

`org.teamvan.robot.WasmMain` is the Web Image entry. It does not call `RobotBase.startRobot` or construct `Robot`, because both load HAL. Each tick reads inputs, runs the real Commands V3 `Scheduler` while enabled, publishes outputs, and writes `/Sim/Pose/X`, `/Sim/Pose/Y`, and `/Sim/Pose/Rotation`. Disabled ticks cancel commands.

`Scheduler` and `Coroutine` are unchanged. Wasm-only `Continuation` and `ContinuationScope` shadows use JavaScript Promise Integration (JSPI) to suspend and resume their Java stacks. The browser must provide `WebAssembly.Suspending` and `WebAssembly.promising`; unsupported browsers report a startup error. This does not require `SharedArrayBuffer` or cross-origin isolation.

`wasmTick()` returns a Promise. Advance the host clock by 20 ms, then **await the tick before starting another one**. `web/sim/continuations.js` exports `createWasmContinuations()`; install its result as `globalThis.wasmContinuations` before `GraalVM.run()`. The native build hooks the generated module loader into this engine and fails if GraalVM changes the expected loader hook. The bridge also uses GraalVM's `conversion.unproxy()` helper; check both integration points when upgrading GraalVM.

The host installs these functions on `globalThis` before calling `wasmStart()`:

- `wasmNowNanos()` returns monotonic time in nanoseconds.
- `wasmDs()` returns `{enabled, alliance, leftX, leftY, rightX}`. `alliance` is `"red"` or `"blue"`.
- `wasmNtGet(path)` returns the current NetworkTables value, or `undefined`.
- `wasmNtPut(path, value)` receives telemetry and the pose numbers.

Telemetry scalars and arrays are ordinary JavaScript values. Raw, little-endian struct, and protobuf wire payloads are lowercase hex strings. Longs outside the JavaScript safe-integer range are exact decimal strings, including inside arrays. The host API does not carry timestamps or topic metadata; metadata writes do not replace topic values.

`web/sim/` is the keyboard driver station. WASD drives, Q and E rotate, and Shift selects the red alliance. Pushing `main` publishes that page and the Javadoc from `.github/workflows/pages.yml`.

`./gradlew nativeCompile` builds `build/native/nativeCompile/var27.js` and `var27.js.wasm`. Use a GraalVM JDK containing Web Image and put Binaryen on `PATH`. On Windows, run this from a **VS Code x64 developer terminal**, not the default x86 developer environment, with `JAVA_HOME` pointing to that GraalVM JDK.

Run the built robot and measure tick latency with a JSPI-capable Node.js or Bun:

```sh
bun tools/wasm-debug/run-wasm.mjs
```

Build and run the native behavioral smoke image for matrix/motor dynamics, telemetry wire values, and coroutine yield/resume, completion, cancellation, child awaiting, and Java exceptions:

```sh
./gradlew nativeSmokeCompile
bun tools/wasm-debug/run-smoke.mjs
```

Desktop `./gradlew simulateJava` is unchanged.

## Debug mode

Debug mode is intended for a deployed RoboRIO application. It enables JMX profiling on port `1099`; it does not enable a JDWP source-level debugger.

Deploy with debug mode enabled:

```sh
./gradlew deploy -PdebugMode
```

The RoboRIO must be reachable and the team number must be configured in WPILib preferences. The JMX endpoint is:

```text
service:jmx:rmi:///jndi/rmi://roborio-<TEAM>-frc.local:1099/jmxrmi
```

You can also use the RoboRIO address `10.<TEAM/100>.2` in place of the hostname. For example, team `2600` uses `10.26.2`.

### VS Code

Install a JMX-capable Java extension or profiling tool, then create a JMX connection using the endpoint above. Use the RoboRIO hostname or IP address and port `1099`.

VS Code's standard Java remote-debug attach command expects JDWP, so it will not connect to this JMX endpoint as a normal source debugger.

### IntelliJ IDEA

Use IntelliJ's JMX support or a JMX-capable plugin to create a connection with the endpoint above. Select the RoboRIO hostname or IP address and port `1099`.

IntelliJ's **Remote JVM Debug** configuration expects JDWP, so it will not connect to this JMX endpoint as a normal source debugger.

JMX remote authentication and SSL are disabled by this repository's debug configuration. Only use this mode on a trusted robot network.
