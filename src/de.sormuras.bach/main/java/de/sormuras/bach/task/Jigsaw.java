package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.util.Paths;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

class Jigsaw {

  private final Bach bach;
  private final Realm realm;
  private final Folder folder;
  private final Path classes;

  Jigsaw(Bach bach, Realm realm) {
    this.bach = bach;
    this.realm = realm;
    this.folder = bach.getProject().folder();
    this.classes = folder.realm(realm.name()).resolve("classes/jigsaw");
  }

  void compile(List<Unit> units) {
    var moduleNames = units.stream().map(Unit::name).collect(Collectors.toList());
    var modulePaths = Paths.filterExisting(realm.modulePaths());
    bach.execute(
        new Call("javac")
            .add("-d", classes)
            .add("--module", String.join(",", moduleNames))
            // .addEach(realm.toolArguments.javac)
            // .iff(realm.preview(), c -> c.add("--enable-preview"))
            // .iff(realm.release() != 0, c -> c.add("--release", realm.release()))
            .add("--module-source-path", realm.moduleSourcePath())
            .forEach(units, this::patchModule)
            .add("--module-path", modulePaths)
            .add("--module-version", bach.getProject().version()));
    Paths.createDirectories(folder.modules(realm.name()));
    for (var unit : units) {
      jarModule(unit);
      // if (realm.modifiers.contains(Project.Realm.Modifier.DOCUMENT)) {
      jarSources(unit);
      //   TODO javadoc(unit);
      // }
    }
  }

  private void patchModule(Call call, Unit unit) {
    if (unit.patches().isEmpty()) return;
    call.add("--patch-module", unit.name() + '=' + Paths.join(unit.patches()));
  }

  private void jarModule(Unit unit) {
    var file = bach.getProject().modularJar(unit); // "../{REALM}/modules/{MODULE}-{VERSION}.jar"
    var resources = Paths.filterExisting(unit.resources()); // TODO include patched resources
    bach.execute(
        new Call("jar")
            .add("--create")
            .add("--file", file)
            .iff(bach.isVerbose(), c -> c.add("--verbose"))
            .iff(unit.descriptor().version(), (c, v) -> c.add("--module-version", v.toString()))
            .iff(unit.descriptor().mainClass(), (c, m) -> c.add("--main-class", m))
            .add("-C", classes.resolve(unit.name()))
            .add(".")
            .forEach(resources, (cmd, path) -> cmd.add("-C", path).add(".")));
    if (bach.isVerbose()) {
      bach.execute(new Call("jar").add("--describe-module").add("--file", file));
    }
  }

  private void jarSources(Unit unit) {
    var jar = unit.name() + '-' + bach.getProject().version(unit) + "-sources.jar";
    var file = folder.realm(realm.name(), jar); // "../{REALM}/{MODULE}-{VERSION}-sources.jar"
    var sources = Paths.filterExisting(unit.sources());
    var resources = Paths.filterExisting(unit.resources());
    bach.execute(
        new Call("jar")
            .add("--create")
            .add("--file", file)
            .iff(bach.isVerbose(), c -> c.add("--verbose"))
            .add("--no-manifest")
            .forEach(sources, (cmd, path) -> cmd.add("-C", path).add("."))
            .forEach(resources, (cmd, path) -> cmd.add("-C", path).add(".")));
  }
}
