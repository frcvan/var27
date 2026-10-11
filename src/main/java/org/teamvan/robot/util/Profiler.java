package org.teamvan.robot.util;

import one.profiler.AsyncProfiler;
import org.teamvan.robot.BuildConstants;
import org.wpilib.framework.RobotBase;

public class Profiler {
  static boolean running = false;
  static String folder;

  private static AsyncProfiler getProf() {
    return AsyncProfiler.getInstance();
  }

  private static String genFolder() {
    if (folder != null) return folder;
    String base =
        (RobotBase.isSimulation() ? "profiler" : "/U/profiler")
            + "/build_"
            + (BuildConstants.DIRTY == 1
                    ? BuildConstants.BUILD_UNIX_TIME
                    : BuildConstants.GIT_REVISION)
                % 1000
            + "_run_";
    int runId = 0;
    var f = new java.io.File(base + runId);
    while (f.exists()) {
      runId++;
      f = new java.io.File(base + runId);
    }
    f.mkdirs();
    folder = base + runId;
    return folder;
  }

  public static void start() {
    if (running) return;
    try {
      getProf()
          .execute(
              "start,jfr,event=wall,jfrsync=profile,trace=org.teamvan.robot.**,trace=org.wpilib.command3.Scheduler.run,loop=1m,file="
                  + genFolder()
                  + "/profile-%t.jfr");
    } catch (Exception e) {
      System.err.println("Failed to start profiler: " + e.getMessage());
      e.printStackTrace();
    }
    System.out.println("Profiler started, outputting to " + genFolder() + "/profile.jfr");
    running = true;
  }

  public static void stop() {
    if (!running) return;
    try {
      getProf().stop();
    } catch (Exception e) {
      System.err.println("Failed to stop profiler: " + e.getMessage());
      e.printStackTrace();
    }
    System.out.println("Profiler stopped.");
    running = false;
  }
}
