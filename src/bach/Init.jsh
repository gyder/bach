//usr/bin/env jshell --execution local --show-version "$0" "$@"; exit $?

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

/*
 * Declare constants and helper methods.
 */
String VERSION = "master"
var version = System.getProperty("Bach.java/version", VERSION)
var source = new URL("https://github.com/sormuras/bach/raw/" + version + "/src/bach/")
var target = Path.of(".bach/src")
var bach = target.resolve("Bach.java")

/open PRINTING

/*
 * Banner!
 */
println("    ___      ___      ___      ___   ")
println("   /\\  \\    /\\  \\    /\\  \\    /\\__\\")
println("  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_")
println(" /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\")
println(" \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /")
println("  \\::/  /   /:/  /  \\:\\__\\    /:/  /")
println("   \\/__/    \\/__/    \\/__/    \\/__/.java")
println()
println("     Java Shell Builder - " + version)
println("     https://github.com/sormuras/bach")
println()

/*
 * Download "Bach.java" and other assets from GitHub to local directory.
 */
println()
println("Downloading assets to " + target.toAbsolutePath() + "...")
println()
Files.createDirectories(target)
for (var asset : Set.of(bach)) {
  var remote = new URL(source, asset.getFileName().toString());
  println("Loading " + remote + "...");
  try (var stream = remote.openStream()) {
    Files.copy(stream, asset, StandardCopyOption.REPLACE_EXISTING);
  }
  println("     -> " + asset);
}

/*
 * Generate local launchers.
 */
var java = "java --show-version " + bach
println()
println("Generating local launchers and initial configuration...")
println("     -> bach")
Files.write(Path.of("bach"), List.of("//usr/bin/env " + java + " \"$@\"")).toFile().setExecutable(true)
println("     -> bach.bat")
Files.write(Path.of("bach.bat"), List.of("@ECHO OFF", java + " %*"))

/*
 * Scaffold
 */
var src = Path.of("src")
if (!Files.isDirectory(src)) {
  var scanner = new Scanner(System.in);
  println();
  println("No src/ directory exits. Scaffold a project?");
  println("  0 -> No.");
  println("  1 -> Create `module-info.java`-only project");
  print("Your choice: ");
  switch(scanner.nextInt()) {
    case 0: break;
    case 1: {
      var demo = Files.createDirectories(src.resolve("demo"));
      Files.write(demo.resolve("module-info.java"), List.of("module demo {}", ""));
      break;
    }
  }
}

/*
 * Print some help and wave goodbye.
 */
println()
println("Bootstrap finished. Use the following command to launch Bach:")
println("")
println("   Any OS: java .bach/" + version + "/Bach.java <actions>")
println("  Windows: bach[.bat] <actions>")
println("    Linux: ./bach <actions>")
println()
println()
println("Have fun using Bach -- https://github.com/sormuras/bach")
println()

/exit
