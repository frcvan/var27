// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.wpilib.command3;

import java.util.Objects;

/** A continuation scope, identified by object identity rather than its display name. */
final class ContinuationScope {
  private final String m_name;

  ContinuationScope(String name) {
    m_name = Objects.requireNonNull(name);
  }

  @Override
  public String toString() {
    return m_name;
  }
}
