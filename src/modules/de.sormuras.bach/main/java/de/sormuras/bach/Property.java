package de.sormuras.bach;

import java.net.URI;
import java.nio.file.Path;

/*BODY*/
enum Property {
  /** Be verbose. */
  DEBUG("ebug", "false"),
  /** */
  CONFIGURATION_CLASS_NAME("configurationClassName", "Configuration"),

  /** Base directory of the project. */
  PROJECT_BASE("base", ""),
  /** Name of the project. */
  PROJECT_NAME("name", "Project"),
  /** Version of the project (used for every module). */
  PROJECT_VERSION("version", "0"),

  LIBRARY_DIRECTORY("library", "lib"),
  SOURCE_DIRECTORY("source", "src"),
  TARGET_DIRECTORY("target", "bin"),

  MAVEN_REPOSITORY("maven.repository", "https://repo1.maven.org/maven2");

  private final String key;
  private final String defaultValue;

  Property(String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public String getKey() {
    return key;
  }

  String getDefaultValue() {
    return defaultValue;
  }

  String get() {
    return get(defaultValue);
  }

  String get(String defaultValue) {
    return System.getProperty(key, defaultValue);
  }
}
