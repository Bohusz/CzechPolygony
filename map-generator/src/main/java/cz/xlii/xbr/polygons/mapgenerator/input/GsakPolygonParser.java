package cz.xlii.xbr.polygons.mapgenerator.input;

import cz.xlii.xbr.polygons.mapgenerator.model.PolygonData;
import cz.xlii.xbr.polygons.mapgenerator.TextFileReader;
import org.locationtech.jts.geom.Coordinate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class GsakPolygonParser {
    public PolygonData parse(Path path) throws IOException {
        return parse(path, Charset.forName("windows-1250"));
    }

    public PolygonData parse(Path path, Charset charset) throws IOException {
        var lines = TextFileReader.readLines(path, charset);
        if (lines.isEmpty() || !lines.getFirst().trim().startsWith("# GsakName=")) throw error(path, 1, "missing GsakName header");
        var name = lines.getFirst().trim().substring("# GsakName=".length()).trim();
        if (name.isEmpty()) throw error(path, 1, "empty GsakName");
        var points = new ArrayList<Coordinate>();
        for (var i = 1; i < lines.size(); i++) {
            var text = lines.get(i).trim();
            if (text.isEmpty() || text.equals("#")) continue;
            var parts = text.split("\\s+");
            if (parts.length != 2) throw error(path, i + 1, "expected latitude and longitude");
            try {
                var lat = Double.parseDouble(parts[0]); var lon = Double.parseDouble(parts[1]);
                if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
                points.add(new Coordinate(lon, lat));
            } catch (NumberFormatException e) { throw error(path, i + 1, "invalid coordinate"); }
        }
        if (points.size() < 4 || !points.getFirst().equals2D(points.getLast())) throw error(path, 1, "polygon must have at least three vertices and be explicitly closed");
        var base = path.getFileName().toString().replaceFirst("(?i)\\.pgon$", "");
        var polygonId = base.split("_", 2)[0];
        var territoryId = polygonId.split("[- \\t]", 2)[0];
        return new PolygonData(path, polygonId, territoryId, name, points);
    }
    private IllegalArgumentException error(Path path, int line, String message) { return new IllegalArgumentException(path + ":" + line + ": " + message); }
}
