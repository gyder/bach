// default package

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void useCanonicalConstructorWithCustomLogger() {
    var log = new Log();
    var bach = new Bach(log, log, Bach.Folder.ofSystem());
    assertDoesNotThrow(bach::hashCode);
    assertLinesMatch(List.of("Initialized Bach.java .+"), log.lines());
  }

  @Nested
  class Build {

    @Test
    void empty(@TempDir Path temp) {
      var log = new Log();
      var project = Bach.newProject("empty").build();
      var summary = new Bach(log, log, Bach.Folder.of(temp)).build(project);
      assertLinesMatch(
          List.of(
              "Initialized Bach.java .+",
              "Build project Project{descriptor=module { name: empty, [mandated java.base] }}",
              "Summary written to " + temp.resolve(".bach/summary.md").toUri()),
          log.lines());
      assertLinesMatch(
          List.of(
              "# Summary",
              "",
              "## Project",
              ">> PROJECT >>",
              "## System Properties",
              ">> SYSTEM PROPERTIES LISTING >>"),
          summary.toMarkdown());
    }
  }
}
