package org.teamvan.wpilibwasm

import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile

/** Source-bundled WASM support that never adds runtime shadows to desktop source sets. */
class WpilibWasmPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply('java')
        project.pluginManager.apply('org.graalvm.buildtools.native')

        def extension = project.extensions.create('wpilibWasm', WpilibWasmExtension)
        def prepare = project.tasks.register('prepareWasmLibrary', PrepareWasmLibrary) {
            group = 'WASM'
            description = 'Extracts the plugin-bundled runtime, smoke sources, frontend, and tools.'
            outputDirectory.convention(project.layout.buildDirectory.dir('wasm/library'))
        }

        SourceSetContainer sourceSets = project.extensions.getByType(SourceSetContainer)
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        SourceSet runtime = sourceSets.create('wasmRuntime') {
            java.setSrcDirs([prepare.flatMap { it.outputDirectory.dir('runtime/java') }])
            resources.setSrcDirs([prepare.flatMap { it.outputDirectory.dir('runtime/resources') }])
        }
        // Compile bundled shadows independently: robot classes cannot mask the library's own types.
        runtime.compileClasspath = main.compileClasspath
        runtime.runtimeClasspath = runtime.output + main.runtimeClasspath

        SourceSet wasm = sourceSets.create('wasm') {
            java.setSrcDirs(['src/wasm/java'])
        }
        wasm.compileClasspath = runtime.output + main.output + main.compileClasspath + wasm.compileClasspath
        // Consumer WASM overrides win over bundled shadows, which win over desktop/vendor types.
        wasm.runtimeClasspath = wasm.output + runtime.output + main.output + main.runtimeClasspath +
                project.configurations.getByName(wasm.runtimeClasspathConfigurationName)

        SourceSet smoke = sourceSets.create('wasmTest') {
            java.setSrcDirs([prepare.flatMap { it.outputDirectory.dir('tests/java') }])
        }
        smoke.compileClasspath = wasm.output + runtime.output + main.output + main.compileClasspath +
                smoke.compileClasspath
        smoke.runtimeClasspath = smoke.output + wasm.runtimeClasspath +
                project.configurations.getByName(smoke.runtimeClasspathConfigurationName)

        project.tasks.named(runtime.compileJavaTaskName) { dependsOn(prepare) }
        project.tasks.named(runtime.processResourcesTaskName) { dependsOn(prepare) }
        project.tasks.named(smoke.compileJavaTaskName) { dependsOn(prepare) }
        project.tasks.withType(JavaCompile).configureEach {
            options.compilerArgs.addAll(['-parameters', '-XDstringConcat=inline'])
        }

        GraalVMExtension nativeExtension = project.extensions.getByType(GraalVMExtension)
        nativeExtension.binaries.named('main') {
            imageName.set(extension.imageName)
            mainClass.set(extension.mainClass)
            classpath.setFrom(wasm.runtimeClasspath)
            buildArgs.addAll('--tool:svm-wasm', '-H:+UnlockExperimentalVMOptions', '-H:-AutoRunVM')
        }
        nativeExtension.binaries.create('smoke') {
            imageName.set(extension.imageName)
            mainClass.set('org.teamvan.wasm.testing.WasmSmoke')
            classpath.setFrom(smoke.runtimeClasspath)
            buildArgs.addAll('--tool:svm-wasm', '-H:+UnlockExperimentalVMOptions', '-H:-AutoRunVM')
        }

        def nativeCompile = project.tasks.named('nativeCompile', BuildNativeImageTask)
        def nativeSmokeCompile = project.tasks.named('nativeSmokeCompile', BuildNativeImageTask)
        nativeCompile.configure { dependsOn(wasm.classesTaskName) }
        nativeSmokeCompile.configure { dependsOn(smoke.classesTaskName) }
        [nativeCompile, nativeSmokeCompile].each { compile ->
            compile.configure {
                inputs.property('wpilibWasmJspiOriginal', JspiLoaderPatch.ORIGINAL)
                inputs.property('wpilibWasmJspiReplacement', JspiLoaderPatch.REPLACEMENT)
                doLast(new JspiLoaderPatch())
            }
        }

        def mainImageName = nativeCompile.flatMap { it.options.flatMap { it.imageName } }
        def smokeImageName = nativeSmokeCompile.flatMap { it.options.flatMap { it.imageName } }
        def mainDirectory = nativeCompile.flatMap { it.outputDirectory }
        def smokeDirectory = nativeSmokeCompile.flatMap { it.outputDirectory }
        def javascript = mainDirectory.zip(mainImageName) { directory, name -> directory.file("${name}.js") }
        def webAssembly = mainDirectory.zip(mainImageName) { directory, name -> directory.file("${name}.js.wasm") }

        project.tasks.register('wasmSite', Sync) {
            group = 'WASM'
            description = 'Stages the WASM browser frontend and native image under build/wasm/site/sim.'
            dependsOn(prepare, nativeCompile)
            into(project.layout.buildDirectory.dir('wasm/site/sim'))
            from(prepare.flatMap { it.outputDirectory.dir('web/sim') })
            from(javascript) { rename { 'wasm.js' } }
            from(webAssembly) { rename { 'wasm.js.wasm' } }
        }

        project.tasks.register('wasmRun', WasmHarnessTask) {
            group = 'WASM'
            description = 'Builds and runs the production image with the bundled native harness.'
            dependsOn(prepare, nativeCompile)
            javascriptRuntime.set(extension.javascriptRuntime)
            imageName.set(mainImageName)
            verifyDrive.set(extension.verifyDrive)
            harness.set(prepare.flatMap { it.outputDirectory.file('tools/wasm-debug/run-wasm.mjs') })
            nativeDirectory.set(mainDirectory)
            workingDirectory.set(project.layout.projectDirectory)
        }
        project.tasks.register('wasmSmoke', WasmHarnessTask) {
            group = 'WASM'
            description = 'Builds and runs the bundled generic native behavioral smoke suite.'
            dependsOn(prepare, nativeSmokeCompile)
            javascriptRuntime.set(extension.javascriptRuntime)
            imageName.set(smokeImageName)
            harness.set(prepare.flatMap { it.outputDirectory.file('tools/wasm-debug/run-smoke.mjs') })
            nativeDirectory.set(smokeDirectory)
            workingDirectory.set(project.layout.projectDirectory)
        }
    }
}
