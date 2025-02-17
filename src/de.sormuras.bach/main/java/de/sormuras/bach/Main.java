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

package de.sormuras.bach;

import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.Base;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;

/** Bach's main program. */
public final class Main {

  private static final Path WORKSPACE = Base.of().workspace();

  public static Main ofSystem() {
    return new Main(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
  }

  public static void main(String... args) {
    var main = ofSystem();
    var code = main.run(args);
    if (code != 0) throw new Error("Non-zero exit code: " + code);
  }

  private final PrintWriter out, err;

  private Main(PrintWriter out, PrintWriter err) {
    this.out = out;
    this.err = err;
  }

  public int run(String... args) {
    if (args.length == 0) {
      build();
      return 0;
    }
    var actions = new ArrayDeque<>(List.of(args));
    while (actions.size() > 0) {
      var action = actions.removeFirst();
      switch (action) {
        case "build":
          build();
          break;
        case "clean":
          Paths.deleteDirectories(WORKSPACE);
          break;
        case "help":
          help();
          break;
        case "info":
          Project.ofCurrentDirectory().toStrings().forEach(out::println);
          break;
        case "version":
          out.println("bach " + Bach.VERSION);
          break;
        default:
          throw new IllegalArgumentException("Unknown action name: " + action);
      }
    }
    return 0;
  }

  public void build() {
    var logbook = Logbook.ofSystem().printer(out::println);
    var configuration = Configuration.ofSystem().logbook(logbook);
    build(configuration);
  }

  public void build(Configuration configuration) {
    var program = Path.of(".bach/src/build/build/Build.java");
    if (Files.exists(program)) {
      var classes = Path.of(".bach/workspace/bootstrap/classes");
      var javac =
          Call.javac()
              .withModule("build")
              .withModuleSourcePath(".bach/src")
              .withModulePath(System.getProperty("jdk.module.path"))
              .with("-d", classes);
      try {
        int code = javac.run();
        if (code != 0) throw new AssertionError("Non-zero exit code: " + code);
        var roots = Set.of("build");
        var finder = ModuleFinder.of(classes);
        var parent = ClassLoader.getPlatformClassLoader();
        var boot = ModuleLayer.boot();
        var config = boot.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
        var controller = ModuleLayer.defineModulesWithOneLoader(config, List.of(boot), parent);
        var layer = controller.layer();
        var buildModule = layer.findModule("build").orElseThrow();
        controller.addExports(buildModule, "build", Bach.class.getModule());
        var loader = layer.findLoader("build");
        var buildClass = loader.loadClass("build.Build");
        var buildMainMethod = buildClass.getMethod("main", String[].class);
        buildMainMethod.invoke(null, new Object[] {new String[] {}});
      } catch (Exception e) {
        throw new RuntimeException("Running custom build program failed: " + e, e);
      }
      return;
    }
    var project = Project.ofCurrentDirectory();
    new Bach(configuration, project).build();
  }

  public void help() {
    out.println("Usage: bach [actions...]");

    out.println();
    out.println("Supported actions");
    out.format("\t%-9s Build modular Java project%n", "build");
    out.format("\t%-9s Delete workspace directory (%s) recursively%n", "clean", WORKSPACE);
    out.format("\t%-9s Print this help screen%n", "help");
    out.format("\t%-9s Scan current working directory and print project information%n", "info");
    out.format("\t%-9s Print version to the output stream%n", "version");

    out.println();
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "\t" + provider.get().name())
        .sorted()
        .forEach(out::println);

    var descriptor = Optional.ofNullable(getClass().getModule().getDescriptor());
    var nameAndVersion = descriptor.map(ModuleDescriptor::toNameAndVersion).orElse("<unnamed>");
    var homepage = "https://github.com/sormuras/bach";
    out.println();
    out.printf("Find more documentation about module %s at: %s%n", nameAndVersion, homepage);
  }

  /**
   * An implementation of the {@link ToolProvider} SPI, providing access to Java Shell Builder tool.
   */
  public static final class BachToolProvider implements ToolProvider {

    @Override
    public String name() {
      return "bach";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      return new Main(out, err).run(args);
    }
  }
}
