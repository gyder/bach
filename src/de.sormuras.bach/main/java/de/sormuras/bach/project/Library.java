package de.sormuras.bach.project;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Manage external 3rd-party modules. */
public /*record*/ class Library {

  public enum Modifier {
    RESOLVE_RECURSIVELY,
    ADD_MISSING_JUNIT_TEST_ENGINES,
    ADD_MISSING_JUNIT_PLATFORM_CONSOLE
  }

  /** Link a module name to resource and a default version. */
  public static /*record*/ class Link {

    public static Link central(String group, String artifact, String version) {
      return central(group, artifact, version, "");
    }

    public static Link central(String group, String artifact, String version, String classifier) {
      return of("https://repo1.maven.org/maven2", group, artifact, version, classifier);
    }

    public static Link of(
        String repository, String group, String artifact, String version, String classifier) {
      var VERSION = Template.Placeholder.VERSION.getTarget();
      var versionAndClassifier = classifier.isBlank() ? VERSION : VERSION + '-' + classifier;
      var type = "jar";
      var file = artifact + '-' + versionAndClassifier + '.' + type;
      var ref = String.join("/", repository, group.replace('.', '/'), artifact, VERSION, file);
      return new Link(ref, Version.parse(version));
    }

    private final String reference;
    private final Version version;

    public Link(String reference, Version version) {
      this.reference = reference;
      this.version = version;
    }

    public String reference() {
      return reference;
    }

    public Version version() {
      return version;
    }

    @Override
    public String toString() {
      return "Link{" + reference + '@' + version + '}';
    }
  }

  /** Requires description. */
  public static /*record*/ class Requires {

    public static Requires of(String requires) {
      int indexOfAt = requires.indexOf('@');
      if (indexOfAt < 0) return new Requires(requires, null);
      var module = requires.substring(0, indexOfAt);
      var version = indexOfAt > 0 ? Version.parse(requires.substring(indexOfAt + 1)) : null;
      return new Requires(module, version);
    }

    private final String name;
    private final Version version;

    public Requires(String name, Version version) {
      this.name = Objects.requireNonNull(name);
      this.version = version;
    }

    public String name() {
      return name;
    }

    public Version version() {
      return version;
    }
  }

  @SuppressWarnings("serial")
  private static class DefaultLinks extends TreeMap<String, Link> {
    private DefaultLinks() {
      put("org.apiguardian.api", Link.central("org.apiguardian", "apiguardian-api", "1.1.0"));
      put("org.opentest4j", Link.central("org.opentest4j", "opentest4j", "1.2.0"));
      putJavaFX("13.0.1", "base", "controls", "fxml", "graphics", "media", "swing", "web");
      putJLWGL(
          "3.2.3",
          "",
          "assimp",
          "bgfx",
          "cuda",
          "egl",
          "glfw",
          "jawt",
          "jemalloc",
          "libdivide",
          "llvm",
          "lmdb",
          "lz4",
          "meow",
          "nanovg",
          "nfd",
          "nuklear",
          "odbc",
          "openal",
          "opencl",
          "opengl",
          "opengles",
          "openvr",
          "opus",
          "ovr",
          "par",
          "remotery",
          "rpmalloc",
          "shaderc",
          "sse",
          "stb",
          "tinyexr",
          "tinyfd",
          "tootle",
          "vma",
          "vulkan",
          "xxhash",
          "yoga",
          "zstd");
      putJUnitJupiter("5.6.0-M1", "", "api", "engine", "params");
      putJUnitPlatform("1.6.0-M1", "commons", "console", "engine", "launcher", "reporting");
    }

    private void putJavaFX(String version, String... names) {
      for (var name : names) {
        var classifier = Template.Placeholder.JAVAFX_PLATFORM.getTarget();
        var link = Link.central("org.openjfx", "javafx-" + name, version, classifier);
        put("javafx." + name, link);
      }
    }

    private void putJLWGL(String version, String... names) {
      for (var name : names) {
        var artifact = "lwjgl" + (name.isEmpty() ? "" : '-' + name);
        var classifier = Template.Placeholder.LWJGL_NATIVES.getTarget();
        var module = "org.lwjgl" + (name.isEmpty() ? "" : '.' + name);
        put(module, Link.central("org.lwjgl", artifact, version));
        put(module + ".natives", Link.central("org.lwjgl", artifact, version, classifier));
      }
    }

    private void putJUnitJupiter(String version, String... names) {
      for (var name : names) {
        var artifact = "junit-jupiter" + (name.isEmpty() ? "" : '-' + name);
        var link = Link.central("org.junit.jupiter", artifact, version);
        var module = "org.junit.jupiter" + (name.isEmpty() ? "" : '.' + name);
        put(module, link);
      }
    }

    private void putJUnitPlatform(String version, String... names) {
      for (var name : names) {
        var link = Link.central("org.junit.platform", "junit-platform-" + name, version);
        put("org.junit.platform." + name, link);
      }
    }
  }

  public static Library of() {
    return new Library(EnumSet.allOf(Modifier.class), defaultLinks(), Set.of());
  }

  public static Map<String, Link> defaultLinks() {
    return new DefaultLinks();
  }

  public static void addJUnitTestEngines(Map<String, Set<Version>> map) {
    if (map.containsKey("org.junit.jupiter") || map.containsKey("org.junit.jupiter.api")) {
      map.putIfAbsent("org.junit.jupiter.engine", Set.of());
    }
    if (map.containsKey("junit")) {
      map.putIfAbsent("org.junit.vintage.engine", Set.of());
    }
  }

  public static void addJUnitPlatformConsole(Map<String, Set<Version>> map) {
    if (map.containsKey("org.junit.jupiter.engine") || map.containsKey("org.junit.vintage")) {
      map.putIfAbsent("org.junit.platform.console", Set.of());
    }
  }

  private final Set<Modifier> modifiers;
  private final Map<String, Link> links;
  private final Collection<Requires> requires;

  public Library(Set<Modifier> modifiers, Map<String, Link> links, Collection<Requires> requires) {
    this.modifiers = modifiers.isEmpty() ? Set.of() : EnumSet.copyOf(modifiers);
    this.links = Map.copyOf(links);
    this.requires = requires;
  }

  public Set<Modifier> modifiers() {
    return modifiers;
  }

  public Map<String, Link> links() {
    return links;
  }

  public Collection<Requires> requires() {
    return requires;
  }

  @Override
  public String toString() {
    return "Library{" + modifiers + ", links=" + links + ", requires=" + requires + '}';
  }
}
