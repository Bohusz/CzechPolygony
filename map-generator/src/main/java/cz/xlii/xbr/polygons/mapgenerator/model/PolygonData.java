package cz.xlii.xbr.polygons.mapgenerator.model;

import org.locationtech.jts.geom.Coordinate;
import java.nio.file.Path;
import java.util.List;

public record PolygonData(Path source, String polygonId, String territoryId, String name, List<Coordinate> coordinates) {
    public PolygonData { coordinates = List.copyOf(coordinates); }
}
