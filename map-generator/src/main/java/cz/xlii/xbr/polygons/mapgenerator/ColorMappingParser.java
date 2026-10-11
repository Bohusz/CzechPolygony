package cz.xlii.xbr.polygons.mapgenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

final class ColorMappingParser {
    private ColorMappingParser() {
    }

    static Map<String, String> parse(Path path) throws IOException {
        return parse(path, Charset.forName("windows-1250"));
    }

    static Map<String, String> parse(Path path, Charset charset) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Territory color mapping file does not exist: " + path);
        }
        var mappings = new LinkedHashMap<String, String>();
        var lineNumber = 0;
        for (var line : TextFileReader.readLines(path, charset)) {
            lineNumber++;
            var value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) {
                continue;
            }
            var separator = value.indexOf('=');
            if (separator <= 0 || separator == value.length() - 1) {
                throw new IllegalArgumentException("Invalid territory color mapping at line " + lineNumber + " in " + path);
            }
            var territoryId = value.substring(0, separator).trim();
            var color = value.substring(separator + 1).trim();
            if (territoryId.isEmpty() || color.isEmpty() || mappings.putIfAbsent(territoryId, color) != null) {
                throw new IllegalArgumentException("Invalid or duplicate territory color mapping at line " + lineNumber + " in " + path);
            }
            validateColor(color, lineNumber, path);
        }
        return Map.copyOf(mappings);
    }

    private static void validateColor(String color, int lineNumber, Path path) {
        try {
            if (!color.matches("#(?i:[0-9a-f]{6}|[0-9a-f]{8})")) {
                throw new IllegalArgumentException();
            }
            java.awt.Color.decode(color.substring(0, 7));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid color at line " + lineNumber + " in " + path + ": " + color);
        }
    }
}
