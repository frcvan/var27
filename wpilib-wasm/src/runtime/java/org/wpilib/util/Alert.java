package org.wpilib.util;

/** Wasm stand-in. Alert state stays in Java; nothing is published through JNI. */
public class Alert implements AutoCloseable {
  /** Alert severity. Values match the WPILib enum so existing callsites compile. */
  public enum Level {
    /** Critical. */
    HIGH,
    /** Warning. */
    MEDIUM,
    /** Informational. */
    LOW
  }

  private final String group;
  private final String id;
  private String text;
  private final Level level;
  private boolean active;

  /**
   * @param id alert id
   * @param text alert text
   * @param level alert level
   */
  public Alert(String id, String text, Level level) {
    this("Alerts", id, text, level);
  }

  /**
   * @param group alert group
   * @param id alert id
   * @param text alert text
   * @param level alert level
   */
  public Alert(String group, String id, String text, Level level) {
    this.group = group;
    this.id = id;
    this.text = text;
    this.level = level;
  }

  /**
   * @param active whether the condition is currently active
   */
  public void set(boolean active) {
    this.active = active;
  }

  /**
   * @return whether the alert is active
   */
  public boolean get() {
    return active;
  }

  /**
   * @param text replacement text
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * @return the alert text
   */
  public String getText() {
    return text;
  }

  /**
   * @return the alert group
   */
  public String getGroup() {
    return group;
  }

  /**
   * @return the alert id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the alert level
   */
  public Level getLevel() {
    return level;
  }

  @Override
  public void close() {}
}
