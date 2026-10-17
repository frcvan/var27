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

## Wasmbot

WASM support comes from the independent [`wasmbot` plugin](wasmbot/README.md). The plugin ID and extension are both `wasmbot`, and the runtime package is `org.teamvan.wasmbot`.

[`WasmMain`](src/wasm/java/org/teamvan/robot/WasmMain.java) only calls `RobotBase.startRobot(Robot::new)`, just like the normal WPILib entry point. Both targets use the same [`Robot`](src/main/java/org/teamvan/robot/Robot.java), its constructor, and `robotPeriodic()`. The shared robot method owns its real scheduler run/cancel policy; the runtime does not run a second scheduler or duplicate the robot's IO wiring.

Check `Constants.isWasm()` in shared robot code. Its implementation is the standard Java `Boolean.getBoolean("wasmbot")`, so desktop and hardware builds need no Wasmbot classes on their classpath. Wasmbot sets the property before constructing the robot; it is false by default outside WASM.

Use the Web Image GraalVM JDK from Downloads as `JAVA_HOME`/`GRAALVM_HOME`. On Windows, build from the **VS Code x64 developer environment** with Binaryen on `PATH`:

```sh
./gradlew wasmRun wasmSmoke wasmSite
python -m http.server 8099 --directory build/wasm/site
```

Open `http://localhost:8099/sim/` in a JSPI-capable browser. The generic telemetry viewer starts disabled; use its Enabled checkbox. W/S, A/D, and Q/E provide the three virtual gamepad axes, and Shift selects red alliance. It displays all published telemetry rather than depending on special Pose channels. The existing struct/protobuf wire format is unchanged.

The [Wasmbot guide](wasmbot/README.md) documents plugin installation, ordinary Robot startup, lifecycle behavior, dependency-free detection, supported APIs, hosting, native smokes, and the independent published-plugin example. Pushing `main` publishes the staged viewer and Javadoc through `.github/workflows/pages.yml`.

Desktop `./gradlew simulateJava` keeps the normal WPILib entry point and classpath.

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
