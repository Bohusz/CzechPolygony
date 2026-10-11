package cz.xlii.xbr.polygons.mapgenerator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColorMappingParserTest {
    @Test
    void parsesCommentsAndMappings() throws Exception {
        var file = Files.createTempFile("territory-colors", ".txt");
        Files.writeString(file, "# colors\n\nCZ = #ff0000\nSK=#00ff0080\n");

        assertEquals("#ff0000", ColorMappingParser.parse(file).get("CZ"));
        assertEquals("#00ff0080", ColorMappingParser.parse(file).get("SK"));
    }

    @Test
    void rejectsMalformedMapping() throws Exception {
        var file = Files.createTempFile("territory-colors", ".txt");
        Files.writeString(file, "CZ red\n");

        assertThrows(IllegalArgumentException.class, () -> ColorMappingParser.parse(file));
    }
}
