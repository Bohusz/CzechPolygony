package cz.xlii.xbr.shp.convertor;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class PgonWriter {
    private final Path output;

    PgonWriter(Path output) {
        this.output = output;
    }

    void write(String name, Polygon polygon) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("# GsakName=" + name + "\n");
            Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
            for (Coordinate coordinate : coordinates) writer.write(coordinate.y + " " + coordinate.x + "\n");
            if (!coordinates[0].equals2D(coordinates[coordinates.length - 1])) writer.write(coordinates[0].y + " " + coordinates[0].x + "\n");
            writer.write("#\n");
        }
    }
}
