// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package van.robot;

import org.wpilib.command3.Scheduler;
import org.wpilib.framework.OpModeRobot;

public class Robot extends OpModeRobot {

  public Robot() {}

  @Override
  public void robotPeriodic() {
    Scheduler.getDefault().run();
  }
}
