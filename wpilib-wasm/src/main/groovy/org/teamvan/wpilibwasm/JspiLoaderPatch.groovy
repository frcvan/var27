package org.teamvan.wpilibwasm

import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task

/** Installs JSPI before instantiation, wrapping only the continuation suspension imports. */
final class JspiLoaderPatch implements Action<Task>, Serializable {
    static final String ORIGINAL = 'await WebAssembly.instantiate(file, wasmImports)'
    static final String REPLACEMENT = 'await globalThis.wasmContinuations.instantiate(file, wasmImports)'

    @Override
    void execute(Task task) {
        BuildNativeImageTask nativeTask = (BuildNativeImageTask) task
        String imageName = nativeTask.options.get().imageName.get()
        File script = new File(nativeTask.outputDirectory.get().asFile, "${imageName}.js")
        String source = script.getText('UTF-8')
        int first = source.indexOf(ORIGINAL)
        if (first < 0 || first != source.lastIndexOf(ORIGINAL)) {
            throw new GradleException('GraalVM Web Image instantiate hook changed; update JSPI integration.')
        }
        script.setText(source.replace(ORIGINAL, REPLACEMENT), 'UTF-8')
    }
}
