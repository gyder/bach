// THIS FILE WAS GENERATED ON 2019-08-30T10:26:28.488075600Z
/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Bach {

  public static String VERSION = "2-ea";

  /**
   * Create new Bach instance with default configuration.
   *
   * @return new default Bach instance
   */
  public static Bach of() {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(out, err, Configuration.of());
  }

  /**
   * Main entry-point.
   *
   * @param args List of API method or tool names.
   */
  public static void main(String... args) {
    var bach = Bach.of();
    bach.main(args.length == 0 ? List.of("build") : List.of(args));
  }

  /** Text-output writer. */
  final PrintWriter out, err;

  /** Configuration. */
  final Configuration configuration;

  public Bach(PrintWriter out, PrintWriter err, Configuration configuration) {
    this.out = Objects.requireNonNull(out, "out must not be null");
    this.err = Objects.requireNonNull(err, "err must not be null");
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
  }

  void main(List<String> args) {
    var arguments = new ArrayDeque<>(args);
    while (!arguments.isEmpty()) {
      var argument = arguments.pop();
      try {
        switch (argument) {
          case "build":
            build();
            continue;
          case "validate":
            validate();
            continue;
        }
        // Try Bach API method w/o parameter -- single argument is consumed
        var method = Util.findApiMethod(getClass(), argument);
        if (method.isPresent()) {
          method.get().invoke(this);
          continue;
        }
        // Try provided tool -- all remaining arguments are consumed
        var tool = ToolProvider.findFirst(argument);
        if (tool.isPresent()) {
          var code = tool.get().run(out, err, arguments.toArray(String[]::new));
          if (code != 0) {
            throw new RuntimeException("Tool " + argument + " returned: " + code);
          }
          return;
        }
      } catch (ReflectiveOperationException e) {
        throw new Error("Reflective operation failed for: " + argument, e);
      }
      throw new IllegalArgumentException("Unsupported argument: " + argument);
    }
  }

  String getBanner() {
    var module = getClass().getModule();
    try (var stream = module.getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream == null) {
        return String.format("Bach.java %s (member of %s)", VERSION, module);
      }
      var lines = new BufferedReader(new InputStreamReader(stream)).lines();
      var banner = lines.collect(Collectors.joining(System.lineSeparator()));
      return banner + " " + VERSION;
    } catch (IOException e) {
      throw new UncheckedIOException("loading banner resource failed", e);
    }
  }

  public void help() {
    out.println("F1! F1! F1!");
    out.println("Method API");
    Arrays.stream(getClass().getMethods())
        .filter(Util::isApiMethod)
        .map(m -> "  " + m.getName() + " (" + m.getDeclaringClass().getSimpleName() + ")")
        .sorted()
        .forEach(out::println);
    out.println("Provided tools");
    ServiceLoader.load(ToolProvider.class).stream()
        .map(provider -> "  " + provider.get().name())
        .sorted()
        .forEach(out::println);
  }

  public void build() {
    info();
    validate();
  }

  public void info() {
    out.printf("Bach (%s)%n", VERSION);
    configuration.toStrings().forEach(line -> out.println("  " + line));
  }

  public void validate() {
    configuration.validate();
  }

  public void version() {
    out.println(getBanner());
  }

  public static final class Configuration {

    private static class ValidationError extends AssertionError {
      private ValidationError(String expected, Object hint) {
        super(String.format("expected that %s: %s", expected, hint));
      }
    }

    public static Configuration of() {
      var home = Path.of("");
      var work = Path.of("bin");
      return of(home, work);
    }

    public static Configuration of(Path home, Path work) {
      var lib = List.of(Path.of("lib"));
      var src = List.of(Path.of("src"));
      return new Configuration(home, work, lib, src);
    }

    private final Path home;
    private final Path work;
    private final List<Path> libraries;
    private final List<Path> sources;

    private Configuration(Path home, Path work, List<Path> libraries, List<Path> sources) {
      this.home = Objects.requireNonNull(home, "home must not be null");
      this.work = home(Objects.requireNonNull(work, "work must not be null"));
      this.libraries = home(requireNonEmpty(libraries, "libraries"));
      this.sources = home(requireNonEmpty(sources, "sources"));
    }

    private Path home(Path path) {
      return path.isAbsolute() ? path : home.resolve(path);
    }

    private List<Path> home(List<Path> paths) {
      return List.of(paths.stream().map(this::home).toArray(Path[]::new));
    }

    private static <C extends Collection<?>> C requireNonEmpty(C collection, String name) {
      if (Objects.requireNonNull(collection, name + " must not be null").isEmpty()) {
        throw new IllegalArgumentException(name + " must not be empty");
      }
      return collection;
    }

    final void validate() {
      requireDirectory(home);
      if (Util.list(home, Files::isDirectory).size() == 0)
        throw new ValidationError("home contains a directory", home.toUri());
      if (Files.exists(work)) {
        requireDirectory(work);
        if (!work.toFile().canWrite()) throw new ValidationError("bin is writable: %s", work.toUri());
      } else {
        var parentOfBin = work.toAbsolutePath().getParent();
        if (parentOfBin != null && !parentOfBin.toFile().canWrite())
          throw new ValidationError("parent of work is writable", parentOfBin.toUri());
      }
      requireDirectoryIfExists(getLibraryDirectory());
      getSourceDirectories().forEach(this::requireDirectory);
    }

    private void requireDirectoryIfExists(Path path) {
      if (Files.exists(path)) requireDirectory(path);
    }

    private void requireDirectory(Path path) {
      if (!Files.isDirectory(path))
        throw new ValidationError("path is a directory: %s", path.toUri());
    }

    public Path getHomeDirectory() {
      return home;
    }

    public Path getWorkspaceDirectory() {
      return work;
    }

    public Path getLibraryDirectory() {
      return libraries.get(0);
    }

    public List<Path> getLibraryPaths() {
      return libraries;
    }

    public List<Path> getSourceDirectories() {
      return sources;
    }

    @Override
    public String toString() {
      return "Configuration [" + String.join(", ", toStrings()) + "]";
    }

    public List<String> toStrings() {
      return List.of(
          String.format("home = '%s' -> %s", home, home.toUri()),
          String.format("workspace = '%s'", getWorkspaceDirectory()),
          String.format("library paths = %s", getLibraryPaths()),
          String.format("source directories = %s", getSourceDirectories()));
    }
  }

  /** Load required modules. */
  static class Library {

    public static void main(String... args) {
      System.out.println("Library.main(" + List.of(args) + ")");
      System.out.println("  requires -> " + RequiresMap.of(args));
      var modulePath = new ModulePath(Path.of("demo/lib"));
      System.out.println("--module-path=" + List.of(modulePath.paths));
      System.out.println("  modules  -> " + modulePath.modules());
      System.out.println("  requires -> " + modulePath.requires());
      var sourcePath = new SourcePath(Path.of("demo/src"));
      System.out.println("--module-source-path=" + List.of(sourcePath.paths));
      System.out.println("  modules  -> " + sourcePath.modules());
      System.out.println("  requires -> " + sourcePath.requires());
    }

    static class RequiresMap extends TreeMap<String, Set<Version>> {

      static RequiresMap of(String... strings) {
        var map = new RequiresMap();
        for (var string : strings) {
          var versionMarkerIndex = string.indexOf('@');
          var any = versionMarkerIndex == -1;
          var module = any ? string : string.substring(0, versionMarkerIndex);
          var version = any ? null : Version.parse(string.substring(versionMarkerIndex + 1));
          map.merge(module, any ? Set.of() : Set.of(version));
        }
        return map;
      }

      static <E> Stream<E> merge(Set<E> set1, Set<E> set2) {
        return Stream.concat(set1.stream(), set2.stream()).distinct();
      }

      void merge(ModuleDescriptor.Requires requires) {
        merge(requires.name(), requires.compiledVersion().map(Set::of).orElse(Set.of()));
      }

      void merge(String key, String version) {
        merge(key, version.isEmpty() ? Set.of() : Set.of(Version.parse(version)));
      }

      void merge(String key, Set<Version> value) {
        merge(key, value, (oldSet, newSet) -> Set.of(merge(oldSet, newSet).toArray(Version[]::new)));
      }

      RequiresMap validate() {
        var invalids = entrySet().stream().filter(e -> e.getValue().size() > 1).collect(toList());
        if (invalids.isEmpty()) {
          return this;
        }
        throw new IllegalStateException("Multiple versions mapped: " + invalids);
      }
    }

    static class ModulePath {

      final Path[] paths;

      ModulePath(Path... paths) {
        this.paths = paths;
      }

      Stream<ModuleDescriptor> findAll() {
        return ModuleFinder.of(paths).findAll().stream().map(ModuleReference::descriptor);
      }

      public Set<String> modules() {
        return findAll().map(ModuleDescriptor::name).collect(Collectors.toCollection(TreeSet::new));
      }

      public RequiresMap requires() {
        var map = new RequiresMap();
        findAll().map(ModuleDescriptor::requires).flatMap(Set::stream).forEach(map::merge);
        return map.validate();
      }
    }

    static class SourcePath {

      final Path[] paths;

      SourcePath(Path... paths) {
        this.paths = paths;
      }

      public Set<String> modules() {
        return Set.of();
      }

      public RequiresMap requires() {
        return new RequiresMap();
      }
    }
  }

  /** File transfer. */
  static class Transfer {

    final PrintWriter out, err;
    final HttpClient client;

    Transfer(PrintWriter out, PrintWriter err) {
      this(out, err, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    Transfer(PrintWriter out, PrintWriter err, HttpClient client) {
      this.out = out;
      this.err = err;
      this.client = client;
    }

    Path getFile(Path path, URI uri) {
      var request = HttpRequest.newBuilder(uri).GET();
      if (Files.exists(path)) {
        try {
          var etagBytes = (byte[]) Files.getAttribute(path, "user:etag");
          var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
          request.setHeader("If-None-Match", etag);
        } catch (Exception e) {
          err.println("Couldn't get 'user:etag' file attribute: " + e);
        }
      }
      try {
        var handler = HttpResponse.BodyHandlers.ofFile(path);
        var response = client.send(request.build(), handler);
        if (response.statusCode() == 200) {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent()) {
            try {
              var etag = etagHeader.get();
              Files.setAttribute(path, "user:etag", StandardCharsets.UTF_8.encode(etag));
            } catch (Exception e) {
              err.println("Couldn't set 'user:etag' file attribute: " + e);
            }
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            try {
              var format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
              var millis = format.parse(lastModifiedHeader.get()).getTime(); // 0 means "unknown"
              var fileTime = FileTime.fromMillis(millis == 0 ? System.currentTimeMillis() : millis);
              Files.setLastModifiedTime(path, fileTime);
            } catch (Exception e) {
              err.println("Couldn't set last modified file attribute: " + e);
            }
          }
          out.println(path + " <- " + uri);
        }
      } catch (IOException | InterruptedException e) {
        err.println("Failed to load: " + uri + " -> " + e);
      }
      return path;
    }
  }

  /** Static helpers. */
  static class Util {

    static Optional<Method> findApiMethod(Class<?> container, String name) {
      try {
        var method = container.getMethod(name);
        return isApiMethod(method) ? Optional.of(method) : Optional.empty();
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    }

    static boolean isApiMethod(Method method) {
      if (method.getDeclaringClass().equals(Object.class)) return false;
      if (Modifier.isStatic(method.getModifiers())) return false;
      return method.getParameterCount() == 0;
    }

    static List<Path> list(Path directory) {
      return list(directory, __ -> true);
    }

    static List<Path> list(Path directory, Predicate<Path> filter) {
      try {
        return Files.list(directory).filter(filter).sorted().collect(Collectors.toList());
      } catch (IOException e) {
        throw new UncheckedIOException("list directory failed: " + directory, e);
      }
    }
  }
}
