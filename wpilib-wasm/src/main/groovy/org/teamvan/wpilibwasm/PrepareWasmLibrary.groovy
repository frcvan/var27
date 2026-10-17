package org.teamvan.wpilibwasm

import java.security.MessageDigest
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** Materializes the exact source-and-asset bundle shipped inside the plugin JAR. */
@CacheableTask
abstract class PrepareWasmLibrary extends DefaultTask {
    private static final String RESOURCE = '/org/teamvan/wpilibwasm/library.zip'

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations()

    @Inject
    protected abstract ArchiveOperations getArchiveOperations()

    // A content input, rather than the plugin version, also tracks local included-build edits.
    @Input
    String getBundleDigest() {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        openBundle().withCloseable { stream ->
            byte[] buffer = new byte[8192]
            int count
            while ((count = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, count)
            }
        }
        return HexFormat.of().formatHex(digest.digest())
    }

    @TaskAction
    void prepare() {
        File archive = new File(temporaryDir, 'library.zip')
        openBundle().withCloseable { input ->
            archive.withOutputStream { output -> input.transferTo(output) }
        }
        def contents = archiveOperations.zipTree(archive)
        def destination = outputDirectory.get()
        fileSystemOperations.sync {
            from(contents)
            into(destination)
        }
    }

    private static InputStream openBundle() {
        InputStream stream = PrepareWasmLibrary.class.getResourceAsStream(RESOURCE)
        if (stream == null) {
            throw new GradleException("WPILib WASM plugin is missing its bundled resources: ${RESOURCE}")
        }
        return stream
    }
}
