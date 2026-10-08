package org.teamvan.robot;

import org.wpilib.command3.Scheduler;
import org.wpilib.framework.OpModeRobot;

public class Robot extends OpModeRobot {

  public Robot() {
    // FIXME: should this be a selector through NT?
    System.out.println("init: " + System.getProperty("debugMode"));
    if (Boolean.getBoolean("debugMode")) {
      var profiler = one.profiler.AsyncProfiler.getInstance();
      var folder = isSimulation() ? "profiler" : "/U/profiler";
      try {
        var profilerFolder = new java.io.File(folder);
        if (!profilerFolder.exists()) {
          profilerFolder.mkdirs();
        }
        profiler.execute("start,event=cpu,o=flamegraph,file=" + folder + "/profile.html");
      } catch (Exception e) {
        System.err.println("Failed to start profiler");
        e.printStackTrace();
      }

      System.out.println("Profiler started, outputting to " + folder + "/profile.jfr");
    }
  }

  @Override
  public void robotPeriodic() {
    Scheduler.getDefault().run();
  }
}
