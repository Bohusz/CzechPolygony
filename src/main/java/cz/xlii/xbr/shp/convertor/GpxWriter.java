package cz.xlii.xbr.shp.convertor;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class GpxWriter implements AutoCloseable {
    private final BufferedWriter writer;

    GpxWriter(Path output) throws IOException {
        writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<gpx version=\"1.1\" creator=\"shp-converter\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
    }

    void write(String name, Polygon polygon) throws IOException {
        writer.write("  <trk><name>" + xml(name) + "</name><trkseg>\n");
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        for (Coordinate coordinate : coordinates) {
            writer.write("    <trkpt lat=\"" + coordinate.y + "\" lon=\"" + coordinate.x + "\"/>\n");
        }
        if (!coordinates[0].equals2D(coordinates[coordinates.length - 1])) {
            Coordinate coordinate = coordinates[0];
            writer.write("    <trkpt lat=\"" + coordinate.y + "\" lon=\"" + coordinate.x + "\"/>\n");
        }
        writer.write("  </trkseg></trk>\n");
    }

    private String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @Override
    public void close() throws IOException {
        writer.write("</gpx>\n");
        writer.close();
    }
}
