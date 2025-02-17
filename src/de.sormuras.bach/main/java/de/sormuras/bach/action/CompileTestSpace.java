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

package de.sormuras.bach.action;

import de.sormuras.bach.Bach;
import de.sormuras.bach.project.TestSpace;
import de.sormuras.bach.project.CodeUnit;
import java.nio.file.Path;

/** An action that compiles test sources to modules. */
public class CompileTestSpace extends BuildTestCodeSpace<TestSpace> {

  public CompileTestSpace(Bach bach) {
    super(bach, bach.project().spaces().test());
  }

  @Override
  public Path[] computeModulePathsForCompileTime() {
    return new Path[] {
      base().modules(""), // main modules
      base().libraries() // external modules
    };
  }

  @Override
  public Path[] computeModulePathsForRuntime(CodeUnit unit) {
    return new Path[] {
      project().toModuleArchive(space().name(), unit.name()), // module under test
      base().modules(""), // main modules
      base().modules(space().name()), // other modules from same realm
      base().libraries() // external modules
    };
  }
}
