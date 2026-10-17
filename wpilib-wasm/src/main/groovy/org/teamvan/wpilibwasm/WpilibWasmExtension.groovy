package org.teamvan.wpilibwasm

import org.gradle.api.provider.Property

/** Consumer configuration for the WASM application and its native harness. */
abstract class WpilibWasmExtension {
    /** Java entry point that calls WasmApplication.launch with the robot adapter. */
    abstract Property<String> getMainClass()

    /** Native image basename, without the generated .js or .js.wasm suffix. */
    abstract Property<String> getImageName()

    /** JSPI-capable executable on PATH, or an absolute executable path. */
    abstract Property<String> getJavascriptRuntime()

    /** Enables the production harness's optional robot-specific drive assertions. */
    abstract Property<Boolean> getVerifyDrive()

    WpilibWasmExtension() {
        imageName.convention('wasm')
        javascriptRuntime.convention('bun')
        verifyDrive.convention(false)
    }
}
