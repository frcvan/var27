package org.wpilib.util.runtime;

import java.io.IOException;

/**
 * Wasm stand-in for WPILib's native library loader. Loading a JNI library is a hard failure on this
 * target, so the call is reported and ignored. A later native method still fails as an unsatisfied
 * link, which is the signal that a callsite was not redirected.
 */
public final class RuntimeLoader {
  /**
   * Reports that a native library would have been loaded and returns without loading it.
   *
   * @param libraryName the name of the library that was requested
   * @throws IOException never; declared to match the WPILib signature
   */
  public static void loadLibrary(String libraryName) throws IOException {
    System.err.println(
        "WARNING: JNI library '" + libraryName + "' was not loaded. Wasm has no JNI runtime.");
  }

  private RuntimeLoader() {}
}
