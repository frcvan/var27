# Wasmbot

`wasmbot` builds ordinary FRC WPILib Commands V3 robot programs with GraalVM Web Image. The runtime package is **`org.teamvan.wasmbot`**. The same `Robot` class and its `robotPeriodic()` method run on desktop/hardware and under WASM; there is no separate simulation lifecycle adapter.

The plugin owns the runtime/shadows, JSPI continuation engine, native build configuration, generic browser telemetry viewer, and native smoke suite. It contains no project-specific drivetrain acceptance tests or Pose-channel assumptions. Struct/protobuf publishing retains its existing hex wire format.

## Requirements

- Gradle 9.4.1, Java 25, and WPILib 2027 alpha-7 with its Commands V3 vendordep are the supported baseline.
- A GraalVM JDK containing Web Image (`native-image --tool:svm-wasm` and `org.graalvm.webimage.api`). A normal Java 25 JDK alone cannot compile the WASM runtime.
- Binaryen on `PATH` and a GraalVM-supported native compiler.
- A JSPI-capable JavaScript runtime for CLI smokes, and a JSPI-capable browser for the viewer: both `WebAssembly.Suspending` and `WebAssembly.promising` must exist. Bun is the default CLI runtime.

On Windows, use a **VS Code x64 developer terminal**. For the Downloads JDK used here:

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE 'Downloads\graalvm-25.4.4.1.1+1.1'
$env:GRAALVM_HOME = $env:JAVA_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
$env:VSCMD_ARG_HOST_ARCH
$env:VSCMD_ARG_TGT_ARCH
```

Both architecture values must be `x64`. Adjust the JDK path for another installation, and ensure Gradle's JVM/toolchain configuration does not select a different compiler. Keep the consumer's ordinary WPILib and vendor Java dependencies, including Commands V3; Commands V2 and V3 cannot coexist.

## Install the plugin

### Included source build

Put this standalone build at `wasmbot/` inside the robot project, or use a corresponding relative path. Add the following **inside the consumer's existing `pluginManagement` block** in `settings.gradle`:

```groovy
includeBuild('wasmbot')
```

Keep the normal generated WPILib repository configuration. Add this entry to the consumer's existing `plugins` block:

```groovy
id 'wasmbot'
```

The plugin JAR bundles runtime Java sources, metadata, browser assets, tools, and smoke sources. Each consumer compiles the runtime against its own WPILib dependency classpath. The included build tracks source and resource changes automatically.

### Published artifact

From this repository's root:

```sh
./gradlew -p wasmbot check assemble publishToMavenLocal
```

With an installed compatible Gradle, run `gradle check assemble publishToMavenLocal` from this standalone build instead. Add `mavenLocal()` to the consumer's existing `pluginManagement.repositories`, then apply:

```groovy
id 'wasmbot' version '0.1.0'
```

The implementation coordinate is `org.teamvan.wasmbot:wasmbot:0.1.0`, resolved through the `wasmbot` Gradle plugin marker. The [independent example](examples/commands-v3) consumes that published plugin, not an included source build. A team Maven server can host the same publications.

Do not add the core runtime as a desktop `implementation` dependency. The plugin adds it only to the WASM source sets.

## Start your normal Robot

Use the same entry-point pattern as ordinary WPILib:

```java
package example;

import org.wpilib.framework.RobotBase;

public final class Main {
  private Main() {}

  public static void main(String... args) {
    RobotBase.startRobot(Robot::new);
  }
}
```

The [example Main](examples/commands-v3/src/main/java/example/Main.java) is used directly for both targets. You can instead keep a tiny `WasmMain` in `src/wasm/java/` with the same `RobotBase.startRobot(Robot::new)` call, if your project wants separate entry-point names. It must not duplicate mechanism construction, input/output loops, or scheduler execution.

Configure that actual class in the consumer:

```groovy
wasmbot {
    mainClass.set('example.Main')
    imageName.set('counter')
}
```

| Property | Default | Meaning |
|---|---|---|
| `mainClass` | Required | Ordinary entry point calling `RobotBase.startRobot(Robot::new)`. |
| `imageName` | `wasm` | Native artifact basename, without extensions. |
| `javascriptRuntime` | `bun` | JSPI-capable executable on `PATH`, or an absolute executable path. |

The plugin applies the native-build plugin itself. Remove obsolete local WASM adapters, copied runtime sources, native binary definitions, and loader-patching scripts when migrating. The public extension is `wasmbot`.

## Robot lifecycle and the command scheduler

The host installs the runtime and invokes `wasmStart()`. Wasmbot installs its telemetry backend, refreshes driver-station data, constructs the real `Robot` once, and starts a nonblocking framework lifecycle. For the 2027 SDK, initialization belongs in the constructor; `simulationInit()` follows exactly once.

```mermaid
flowchart LR
    Host[Await wasmTick] --> DS[Refresh driver station]
    DS --> Due[Run due framework callbacks]
    Due --> RP[Robot.robotPeriodic]
    RP --> App[Application-owned scheduler and IO]
    App --> Sim[simulationPeriodic]
    Sim --> Done[Resolve tick Promise]
```

`robotPeriodic()` runs in both enabled and disabled cycles. **Wasmbot does not call `Scheduler.run()` for you.** Keep the existing scheduler call inside your normal robot method; it must run only once per intended robot cycle. For example, the [shared example Robot](examples/commands-v3/src/main/java/example/Robot.java) uses:

```java
@Override
public void robotPeriodic() {
  if (RobotState.isEnabled()) {
    Scheduler.getDefault().run();
  } else {
    Scheduler.getDefault().cancelAll();
  }
  Telemetry.log("Example/Counter", counter.ticks());
  Telemetry.log("Example/Enabled", RobotState.isEnabled());
  Telemetry.log("Example/Wasmbot", Boolean.getBoolean("wasmbot"));
}
```

Input sampling and output publication should likewise stay in this shared robot lifecycle, in the order appropriate for the project. Commands V3's scheduler does not implicitly filter its entire run loop by enable state; the enabled/disabled policy above is ordinary application code, not a WASM-specific branch.

`Scheduler` and `Coroutine` remain the real WPILib classes. JSPI-backed continuation shadows preserve suspended Java stacks, command-local variables, child awaiting, cancellation, and exception identity.

### Supported framework behavior

- `OpModeRobot` and `TimedRobot`/`IterativeRobotBase` use HAL-free, host-driven lifecycle implementations. No HAL notifier, native scheduler thread, or blocking competition loop is started.
- Construction and `simulationInit()` happen once. Driver-station connection callbacks, disabled transitions, periodic hooks, and `simulationPeriodic()` follow the SDK ordering. Exceptions propagate; failed expirations are not replayed at the same host time.
- Configured main-loop periods and `addPeriodic(...)` callbacks use the host's monotonic clock. Only due callbacks execute. A period shorter than the host interval is quantized to that interval; missed expirations coalesce instead of replaying a burst.
- `endCompetition()`/`close()` stop subsequent callbacks. `getLoopStartTime()` uses the host clock.
- The current driver station has **no selected opmode**. `OpModeRobot.nonePeriodic()` runs, while automatic classpath/JAR discovery and explicit opmode registration/selection are not available. They are not replaced with fake selected modes. With the SDK's UNKNOWN mode, iterative frameworks use disabled-mode callbacks even if the enable bit is set; `robotPeriodic()` still runs.
- Native watchdog diagnostics and physical HID outputs are unavailable and fail explicitly if invoked. A custom blocking `RobotBase` subclass needs a host-driven WASM implementation rather than a native loop.

Choose simulated IO using your existing `RobotBase.isSimulation()`/`isReal()` selection. These shadows do not emulate every vendor or HAL API. Avoid hardware/JNI initialization in static fields that run before your simulation selection.

## Detect WASM without a dependency

Use **standard Java**, anywhere in shared robot code:

```java
boolean runningUnderWasm = Boolean.getBoolean("wasmbot");
```

Wasmbot sets the `wasmbot` system property to `true` **before invoking the Robot constructor**. It is unset—and `Boolean.getBoolean` returns `false`—in ordinary desktop/hardware launches unless explicitly overridden by the user.

A project can expose a normal Java helper without importing any Wasmbot class:

```java
public static boolean isWasm() {
  return Boolean.getBoolean("wasmbot");
}
```

The consuming robot's `Constants.isWasm()` follows this pattern. Prefer reading through a method rather than caching a build-time-initialized flag. Do not import `org.teamvan.wasmbot.*` into shared `src/main/java/` just to detect the environment: a conditional around such references would still require those classes at Java compile time. Neither the entry point nor the detection method needs that dependency.

## Build, smoke, and serve

From the consuming robot project:

```sh
./gradlew nativeCompile
./gradlew wasmRun
./gradlew wasmSmoke
./gradlew wasmSite
python -m http.server 8099 --directory build/wasm/site
```

Open `http://localhost:8099/sim/`. On Windows, `.\gradlew.bat` is equivalent to `./gradlew`.

| Task | Result |
|---|---|
| `nativeCompile` | `build/native/nativeCompile/<imageName>.js` and `<imageName>.js.wasm`. |
| `wasmRun` | Starts the consumer's actual Robot, advances disabled/enabled ticks, and reports telemetry count and tick latency. |
| `nativeSmokeCompile` | Builds the library's native behavioral smoke image. |
| `wasmSmoke` | Exercises RobotBase startup, constructor detection, lifecycle order/cadence/stop, scheduler execution counts, dynamics, serialization, continuations, cancellation, child awaiting, exceptions, and overlapping-tick rejection. |
| `wasmSite` | Stages the complete generic browser frontend at `build/wasm/site/sim/`. |

The staged browser assets always use canonical `wasm.js`/`wasm.js.wasm` names, even for a native image named `counter`. Serve the staged directory over HTTP, not `file://`.

The browser begins disabled. Enable it with the checkbox. It displays all telemetry topics without assuming a drivetrain or a Pose format. W/S change left Y, A/D left X, Q/E right X, and Shift chooses red alliance. Text is rendered as text, not executable HTML.

The low-level tools in `build/wasm/library/tools/wasm-debug/` accept:

```text
run-wasm.mjs [buildDir] [imageName]
run-smoke.mjs [buildDir] [imageName]
run-browser.mjs [pageUrl] [debugPort]
```

Run them with the configured JSPI-capable Node/Bun executable. Paths default from the consumer's current working directory. Prefer the Gradle tasks to keep image names and artifact paths synchronized.

### Independent published-plugin example

From this repository's root:

```sh
./gradlew -p wasmbot check assemble publishToMavenLocal
./gradlew -p wasmbot/examples/commands-v3 wasmRun wasmSmoke wasmSite
python -m http.server 8099 --directory wasmbot/examples/commands-v3/build/wasm/site
```

That example uses a normal `Main` and `Robot`, a real scheduler-driven counter, a different image name, and no core imports in shared Java. The viewer shows `/Example/Counter`, `/Example/Enabled`, and `/Example/Wasmbot`.

## JavaScript host and telemetry

For a custom frontend, install `createWasmContinuations()` from `sim/continuations.js` as `globalThis.wasmContinuations` **before** `GraalVM.run()`. Install these functions before `wasmStart()`:

| Function | Contract |
|---|---|
| `wasmNowNanos()` | Monotonic simulated time, in nanoseconds. |
| `wasmDs()` | `{enabled, alliance, leftX, leftY, rightX}`; alliance is `"red"` or `"blue"`, axes are in `[-1, 1]`. |
| `wasmNtGet(path)` | Current host value, or `undefined`. The supplied viewer returns `undefined`. |
| `wasmNtPut(path, value)` | Receives an ordinary JavaScript telemetry value. |

Advance the host clock by 20 ms per default tick and **await the `wasmTick()` Promise before starting another tick**. The supplied viewer serializes ticks and reports startup/runtime failures. JSPI requires neither `SharedArrayBuffer` nor cross-origin isolation.

The wire format is unchanged: primitive scalars and arrays are native JS values; raw bytes, little-endian structs, and protobuf wire bytes are lowercase hex strings. Longs outside JS's safe-integer range are exact decimal strings, including in arrays. Timestamps and topic metadata are not transported.

The virtual HID on port zero exposes the three host axes through raw axes 0/1/2 and the standard Gamepad mapping/deadbands. Other controls are neutral and absent from availability masks; ports one through five are disconnected. The supplied host does not publish right-Y, triggers, buttons, POVs, or touchpads. Rumble and LED outputs are not emulated.

## Source isolation and extensions

The plugin creates:

- `wasmRuntime`: bundled runtime sources/resources, compiled against the consumer's ordinary WPILib dependency classpath.
- `wasm`: optional consumer-only sources from `src/wasm/java/`.
- `wasmTest`: bundled native behavioral fixtures.

Consumer WASM classes win over bundled shadows, which win over desktop/vendor classes in the native image classpath. None of the shadows/core classes are added to desktop `main` or its robot JAR's compiled packages. The plugin supplies `-parameters` and `-XDstringConcat=inline` for Web Image compatibility, including reachable ordinary robot classes.

For additional native APIs, use simulated IO or matching consumer-specific replacements in `src/wasm/java/<original/package>/`. Implement the APIs the program actually uses; do not silently substitute hardware no-ops. Additional reflection/resource metadata belongs in `src/wasm/resources/META-INF/native-image/`.

After changing GraalVM, WPILib, framework shadows, or browser interop, run both smoke tasks and exercise the staged page. The guarded loader patch, private Graal reference/throwable conversion helpers, and raw application/continuation metadata exports must remain compatible. Missing JSPI or incompatible hooks produce errors rather than fake continuations.

Retained WPILib-derived code is covered by [WPILib-License.md](WPILib-License.md), also included under `licenses/` in the distributed source bundle.
