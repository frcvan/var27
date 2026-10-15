package org.wpilib.util.runtime;

import java.io.IOException;

/** Desktop stand-in. Never loads a native library. */
public final class RuntimeLoader {
  public static String getPlatformName() {
    return "java-sim";
  }

  public static void loadLibrary(String libraryName) throws IOException {}

  private RuntimeLoader() {}
}
