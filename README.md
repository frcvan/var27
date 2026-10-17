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

WASM support is provided by the independent [`wpilib-wasm` library](wpilib-wasm/README.md), consumed through `pluginManagement.includeBuild('wpilib-wasm')` and the `org.teamvan.wpilib-wasm` Gradle plugin. The library owns the runtime/shadows, JSPI engine, native build configuration, browser assets, and smoke suite.

This robot keeps only [`src/wasm/java/org/teamvan/robot/WasmMain.java`](src/wasm/java/org/teamvan/robot/WasmMain.java): its simulation IO, manager/default-command wiring, and existing numeric `/Sim/Pose/X`, `/Sim/Pose/Y`, and `/Sim/Pose/Rotation` publication. Struct publishing and the real Commands V3 Scheduler behavior are unchanged.

Use the Web Image GraalVM JDK from Downloads as `JAVA_HOME`/`GRAALVM_HOME`. On Windows, build from the **VS Code x64 developer environment** with Binaryen on `PATH`. Run:

```sh
./gradlew wasmRun wasmSmoke wasmSite
python -m http.server 8099 --directory build/wasm/site
```

Open `http://localhost:8099/sim/`. WASD drives, Q/E rotate, and Shift selects red alliance. A JSPI-capable browser is required. The staged frontend and native files come entirely from the library. `nativeCompile` and `nativeSmokeCompile` remain available; the image basename is `wasm`.

The [library integration guide](wpilib-wasm/README.md) covers adding support to arbitrary Commands V3 robots, published-plugin consumption, lifecycle adapters, supported APIs, host telemetry, and the independent non-drive example. Pushing `main` publishes the staged simulator and Javadoc through `.github/workflows/pages.yml`.

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
