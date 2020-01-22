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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

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

  /** Supported operations by the default build program. */
  private enum Operation {
    /** Build the project in the current working directory. */
    BUILD,
    /** Generate, validate, and print project information. */
    DRY_RUN;

    /** Return the operation for the specified argument. */
    static Operation of(String argument, Operation defaultOperation) {
      if (argument == null) return defaultOperation;
      return valueOf(argument.toUpperCase().replace('-', '_'));
    }
  }

  /** Default build program. */
  public static void main(String... args) {
    var arguments = new ArrayDeque<>(List.of(args));
    var bach = new Bach11();
    var project = bach.newProject();
    var plan = bach.newPlan(project);
    plan.walk((indent, call) -> System.out.println(indent + "- " + call.toMarkdown()));
    switch (Operation.of(arguments.pollFirst(), Operation.DRY_RUN)) {
      case BUILD:
        throw new UnsupportedOperationException("Build is being implemented, soon.");
      case DRY_RUN:
        break;
    }
    System.out.println();
    System.out.println("Thanks for using Bach.java · https://github.com/sponsors/sormuras (-:");
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

  /** Create project for the current working directory. */
  public Project newProject() {
    return newProjectBuilder(Path.of("")).build();
  }

  /** Create call plan for the given project. */
  public Plan newPlan(Project project) {
    return new Planner(project).call();
  }

  /** Return {@code "Bach11 " + }{@link #VERSION}. */
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
      return "Project {name=\"" + name() + "\", version=" + version() + "}";
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

    /** Lookup a property value by its key name. */
    public Optional<String> getProperty(String name) {
      var key = "project." + name;
      var property = Optional.ofNullable(System.getProperty(key));
      property.ifPresent(v -> logger.log(LEVEL, "System.getProperty(\"{0}\") -> \"{1}\"", key, v));
      return property;
    }

    /** Scan for name property. */
    @SuppressWarnings("RedundantThrows")
    public Optional<String> scanName() throws Exception {
      var name = getProperty("name");
      if (name.isPresent()) return name;
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
      return getProperty("version").map(Version::parse);
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

  /** A task representation, usually calling a tool by its name and passing arguments. */
  public interface Call {

    String name();

    List<String> args();

    default String toMarkdown() {
      return '`' + toString() + '`';
    }

    static Call of(String name, String... args) {
      class Tool implements Call {
        @Override
        public String name() {
          return name;
        }

        @Override
        public List<String> args() {
          return List.of(args);
        }

        @Override
        public String toString() {
          return name + (args().isEmpty() ? "" : " " + String.join(" ", args()));
        }
      }
      return new Tool();
    }

    static Call of(String name, Callable<?> callable, String... code) {
      class Lambda implements Call, Callable<Object> {
        @Override
        public String name() {
          return name;
        }

        @Override
        public List<String> args() {
          return List.of(code);
        }

        @Override
        public Object call() throws Exception {
          return callable.call();
        }

        @Override
        public String toString() {
          return String.join(" ", args());
        }
      }
      return new Lambda();
    }
  }

  /** A composite task that is composed of a name and a list of nested {@link Call} instances. */
  public interface Plan extends Call {

    List<Call> calls();

    Level level();

    boolean parallel();

    default int walk(BiConsumer<String, Call> consumer) {
      return walk(this, consumer);
    }

    static Plan of(String name, Level level, boolean parallel, Call... calls) {
      class Record implements Plan {
        @Override
        public String name() {
          return name;
        }

        @Override
        public List<String> args() {
          return List.of();
        }

        @Override
        public Level level() {
          return level;
        }

        @Override
        public boolean parallel() {
          return parallel;
        }

        @Override
        public List<Call> calls() {
          return List.of(calls);
        }

        @Override
        public String toMarkdown() {
          return String.format(
              "%s _(size=%d, level=%s, parallel=%s)_", name(), calls().size(), level(), parallel());
        }
      }
      return new Record();
    }

    static int walk(Call call, BiConsumer<String, Call> consumer) {
      return walk(call, "", "  ", consumer);
    }

    static int walk(Call call, String indent, String inc, BiConsumer<String, Call> consumer) {
      consumer.accept(indent, call);
      if (call instanceof Plan) {
        var plan = ((Plan) call);
        var count = 0;
        for (var child : plan.calls()) count += walk(child, indent + inc, inc, consumer);
        return count;
      }
      return 1;
    }
  }

  /** Build plan builder. */
  public class Planner implements Callable<Plan> {

    private final Project project;

    public Planner(Project project) {
      this.project = project;
    }

    @Override
    public Plan call() {
      logger.log(LEVEL, "Computing build plan for {0}", project);
      return Plan.of(
          "Build " + project.name() + " " + project.version(),
          Level.ALL,
          false,
          showSystemInformation(),
          createOutputDirectory());
    }

    public Call showSystemInformation() {
      return Plan.of(
          "Show System Information",
          Level.DEBUG,
          true,
          Call.of("javac", "--version"),
          Call.of("javadoc", "--version"),
          Call.of("jar", "--version"),
          Call.of("jdeps", "--version"));
    }

    public Call createOutputDirectory() {
      var path = Path.of(".123");
      return Call.of(
          "Create output directory",
          () -> Files.createDirectories(path),
          "Files.createDirectories(" + Code.pathOf(path) + ")");
    }
  }

  /** Java source code related helpers. */
  public static class Code {
    private Code() {}

    /** Convert the string representation of the given object into a Java source snippet. */
    public static String $(Object object) {
      return "\"" + object.toString() + "\"";
    }

    /** Create {@code Path.of("some/path/...")} snippet. */
    public static String pathOf(Path path) {
      return "Path.of(" + $(path.toString().replace('\\', '/')) + ")";
    }
  }
}
