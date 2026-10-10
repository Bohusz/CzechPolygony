package cz.xlii.xbr.shp.convertor;

import java.nio.file.Path;

public enum OutputFormat {
    GEOJSON, GPX, PGON;

    public static OutputFormat fromOutput(Path output) {
        String name = output.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".geojson")) return GEOJSON;
        if (name.endsWith(".gpx")) return GPX;
        if (name.endsWith(".pgon")) return PGON;
        throw new ConversionException("Unsupported output extension. Use .geojson, .gpx, or .pgon.");
    }
}
