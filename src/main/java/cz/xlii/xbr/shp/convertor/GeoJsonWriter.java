package cz.xlii.xbr.shp.convertor;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class GeoJsonWriter implements AutoCloseable {
    private final BufferedWriter writer;
    private boolean first = true;

    GeoJsonWriter(Path output) throws IOException {
        writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
        writer.write("{\"type\":\"FeatureCollection\",\"features\":[");
    }

    void write(SimpleFeature feature, Geometry geometry) throws IOException {
        if (!first) writer.write(',');
        first = false;
        writer.write("{\"type\":\"Feature\",\"properties\":{");
        boolean firstProperty = true;
        for (AttributeDescriptor descriptor : feature.getFeatureType().getAttributeDescriptors()) {
            if (Geometry.class.isAssignableFrom(descriptor.getType().getBinding())) continue;
            Object value = feature.getAttribute(descriptor.getName());
            if (value == null) continue;
            if (!firstProperty) writer.write(',');
            firstProperty = false;
            string(descriptor.getLocalName());
            writer.write(':');
            value(value);
        }
        writer.write("},\"geometry\":");
        geometry(geometry);
        writer.write('}');
    }

    private void value(Object value) throws IOException {
        if (value instanceof Number || value instanceof Boolean) writer.write(value.toString()); else string(value.toString());
    }

    private void geometry(Geometry geometry) throws IOException {
        if (geometry instanceof Polygon polygon) {
            writer.write("{\"type\":\"Polygon\",\"coordinates\":");
            polygon(polygon);
        } else if (geometry instanceof MultiPolygon multiPolygon) {
            writer.write("{\"type\":\"MultiPolygon\",\"coordinates\":[");
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                if (i > 0) writer.write(',');
                polygon((Polygon) multiPolygon.getGeometryN(i));
            }
            writer.write(']');
        } else throw new ConversionException("Unsupported geometry type: " + geometry.getGeometryType());
        writer.write('}');
    }

    private void polygon(Polygon polygon) throws IOException {
        writer.write('[');
        ring(polygon.getExteriorRing().getCoordinates());
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            writer.write(',');
            ring(polygon.getInteriorRingN(i).getCoordinates());
        }
        writer.write(']');
    }

    private void ring(Coordinate[] coordinates) throws IOException {
        writer.write('[');
        for (int i = 0; i < coordinates.length; i++) {
            if (i > 0) writer.write(',');
            writer.write('[' + Double.toString(coordinates[i].x) + ',' + Double.toString(coordinates[i].y) + ']');
        }
        writer.write(']');
    }

    private void string(String value) throws IOException {
        writer.write('"');
        writer.write(value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
        writer.write('"');
    }

    @Override
    public void close() throws IOException {
        writer.write("]}");
        writer.close();
    }
}
