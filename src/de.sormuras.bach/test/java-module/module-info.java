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

open /*test*/ module de.sormuras.bach {
  exports de.sormuras.bach;
  exports de.sormuras.bach.action;
  // hide de.sormuras.bach.internal;
  exports de.sormuras.bach.project;
  exports de.sormuras.bach.tool;

  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires org.junit.jupiter;
  requires test.base;

  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      de.sormuras.bach.TestToolProvider,
      de.sormuras.bach.Main.BachToolProvider;
}
