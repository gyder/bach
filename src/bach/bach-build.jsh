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

/*
 * Zero-installation Bach.java build script.
 */

// The "/open" directive below this comment requires a constant String literal as its argument.
// Due to this restriction, the URL points
//   a) to the "master" tag/branch and
//   b) to "src/bach/Bach.java", which requires JDK 11 or later.

System.out.println("| /open https://github.com/sormuras/bach/raw/master/src/bach/Bach.java")
/open https://github.com/sormuras/bach/raw/master/src/bach/Bach.java

System.out.println("| /open https://github.com/sormuras/bach/raw/master/src/bach/Build.default")
/open https://github.com/sormuras/bach/raw/master/src/bach/Build.default

System.out.println("| /open src/bach/Build.java")
/open src/bach/Build.java

System.out.println("| Build.main()")
var code = 0
try {
  Build.main();
} catch (Throwable throwable) {
  throwable.printStackTrace();
  code = 1;
}

System.out.println("| /exit " + code)
/exit code
