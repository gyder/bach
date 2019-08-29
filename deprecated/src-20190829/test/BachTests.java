import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  @SwallowSystem
  void mainWithIllegalArgument() {
    var e = assertThrows(Error.class, () -> Bach.main("illegal argument"));
    assertEquals("Bach.main(\"illegal argument\") failed with error code: 42", e.getMessage());
  }

  @Test
  void defaultValuesAreAssigned() {
    var bach = Bach.of();
    assertNotNull(bach.out);
    assertNotNull(bach.err);
    assertNotNull(bach.configuration);
    assertNotNull(bach.project);
  }

  @Test
  void mainWithListOfCustomTools() {
    var probe = new Probe();
    assertEquals(0, probe.bach.main(List.of("version", "noop")));
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            ">> version(<empty>)",
            "Running API tool named \\QBach$Tool$$Lambda\\E.+",
            "Bach::version",
            Bach.VERSION,
            ">> noop(<empty>)",
            "Running configured tool named 'Probe$NoopTool'..."),
        probe.lines());
  }
}
