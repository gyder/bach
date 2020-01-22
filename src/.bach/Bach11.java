/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Java Shell Builder.
 *
 * <p>Requires JDK 11 or later.
 */
public class Bach11 {

  /** Version of the Java Shell Builder. */
  static final Version VERSION = Version.parse("1-ea");

  /** Debug flag. */
  static final boolean DEBUG = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));

  /** Default log level. */
  static final Level LEVEL = DEBUG ? Level.INFO : Level.DEBUG;

  /** Default build program. */
  public static void main(String... args) {
    System.out.println(new Bach11().newProjectBuilder(Path.of("")).build());
  }

  /** Logger instance. */
  private final Logger logger;

  /** Initialize Java Shell Builder instance with default values. */
  public Bach11() {
    this(System.getLogger("Bach"));
  }

  /** Initialize Java Shell Builder instance canonically. */
  public Bach11(Logger logger) {
    this.logger = logger;
    logger.log(LEVEL, "Initialized {0}", this);
  }

  /** Create project builder instance using {@link ProjectScanner}. */
  public Project.Builder newProjectBuilder(Path base) {
    try {
      return new ProjectScanner(base).call();
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  @Override
  public String toString() {
    return "Bach11 " + VERSION;
  }

  /** Project model API. */
  /*record*/ public static final class Project {

    private final String name;
    private final Version version;

    /** Initialize this project instance. */
    public Project(String name, Version version) {
      this.name = name;
      this.version = version;
    }

    /** Get name of this project. */
    public String name() {
      return name;
    }

    /** Get version of this project. */
    public Version version() {
      return version;
    }

    @Override
    public String toString() {
      return "Project {name=" + name() + ", version=" + version() + "}";
    }

    /** Project model builder. */
    public static final class Builder {
      private String name = "project";
      private Version version = Version.parse("0");

      /** Create project instance using property values from this builder. */
      public Project build() {
        return new Project(name, version);
      }

      /** Set project's name. */
      public Builder setName(String name) {
        this.name = name;
        return this;
      }

      /** Set project's version. */
      public Builder setVersion(Version version) {
        this.version = version;
        return this;
      }
    }
  }

  /** Directory-based project model scanner. */
  public class ProjectScanner implements Callable<Project.Builder> {

    private final Path base;

    /** Initialize this scanner instance with a directory to scan. */
    public ProjectScanner(Path base) {
      this.base = base;
      logger.log(LEVEL, "Initialized {0}", this);
    }

    /** Get base directory to be scanned for project properties. */
    public final Path base() {
      return base;
    }

    /** Get Bach's logger instance. */
    public final Logger logger() {
      return logger;
    }

    /** Scan for name property. */
    @SuppressWarnings("RedundantThrows")
    public Optional<String> scanName() throws Exception {
      return Optional.ofNullable(base().toAbsolutePath().getFileName()).map(Path::toString);
    }

    /**
     * Scan for version property.
     *
     * <p>Example implementation reading and parsing a version from a {@code .version} file:
     *
     * <pre><code>
     *    public Optional&lt;Version&gt; scanVersion() throws Exception {
     *      var version = base().resolve(".version");
     *      if (Files.notExists(version)) return Optional.empty();
     *      return Optional.of(Version.parse(Files.readString(version)));
     *    }
     * </code></pre>
     */
    @SuppressWarnings("RedundantThrows")
    public Optional<Version> scanVersion() throws Exception {
      return Optional.empty();
    }

    @Override
    public Project.Builder call() throws Exception {
      logger.log(LEVEL, "Build project for directory: {0}", base().toAbsolutePath());
      var builder = new Project.Builder();
      scanName().ifPresent(builder::setName);
      scanVersion().ifPresent(builder::setVersion);
      return builder;
    }
  }
}
