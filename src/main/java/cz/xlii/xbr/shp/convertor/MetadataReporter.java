package cz.xlii.xbr.shp.convertor;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MetadataReporter {
    private static final String[] DIRECTIONS = {"north", "east", "south", "west"};
    private final List<AttributeDescriptor> attributes;
    private final GeometryTransformer transformer;
    private final char csvSeparator;
    private final Charset csvCharset;

    MetadataReporter(SimpleFeatureType schema, GeometryTransformer transformer, char csvSeparator, Charset csvCharset) {
        attributes = schema.getAttributeDescriptors().stream()
                .filter(descriptor -> !descriptor.equals(schema.getGeometryDescriptor()))
                .toList();
        this.transformer = transformer;
        this.csvSeparator = csvSeparator;
        this.csvCharset = csvCharset;
    }

    void writeStandardOutput(SimpleFeatureSource source) throws IOException {
        try (SimpleFeatureIterator features = source.getFeatures().features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                List<Polygon> polygons = Main.polygons(transformer.transform(Main.polygonal(feature)));
                List<String> attributeLabels = attributes.stream().map(AttributeDescriptor::getLocalName).toList();
                List<String> coordinateLabels = new ArrayList<>();
                for (int component = 0; component < polygons.size(); component++) {
                    for (String direction : DIRECTIONS) coordinateLabels.add("Polygon " + (component + 1) + " " + direction);
                }
                int attributeWidth = width(attributeLabels);
                int coordinateWidth = width(coordinateLabels);
                for (AttributeDescriptor attribute : attributes) {
                    printAligned(attribute.getLocalName(), String.valueOf(feature.getAttribute(attribute.getName())), attributeWidth);
                }
                for (int component = 0; component < polygons.size(); component++) {
                    Extrema extrema = extrema(polygons.get(component));
                    for (int direction = 0; direction < DIRECTIONS.length; direction++) {
                        printAligned("Polygon " + (component + 1) + " " + DIRECTIONS[direction],
                                geocaching(extrema.coordinates().get(direction)), coordinateWidth);
                    }
                }
                if (features.hasNext()) System.out.println();
            }
        }
    }

    void writeCsv(SimpleFeatureSource source, Path reportFile, Main.ProgressReporter progress) throws IOException {
        List<Row> rows = new ArrayList<>();
        int maximumComponents = 0;
        try (SimpleFeatureIterator features = source.getFeatures().features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                List<String> values = new ArrayList<>();
                for (AttributeDescriptor attribute : attributes) values.add(String.valueOf(feature.getAttribute(attribute.getName())));
                List<Polygon> polygons = Main.polygons(transformer.transform(Main.polygonal(feature)));
                List<Extrema> extrema = polygons.stream().map(MetadataReporter::extrema).toList();
                rows.add(new Row(values, extrema));
                maximumComponents = Math.max(maximumComponents, extrema.size());
                progress.processedFeature();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(reportFile, csvCharset)) {
            writeCsvLine(writer, header(maximumComponents));
            for (Row row : rows) {
                List<String> values = new ArrayList<>(row.attributes());
                for (int component = 0; component < maximumComponents; component++) {
                    Extrema extrema = component < row.extrema().size() ? row.extrema().get(component) : null;
                    for (int direction = 0; direction < DIRECTIONS.length; direction++) {
                        Coordinate coordinate = extrema == null ? null : extrema.coordinates().get(direction);
                        values.add(coordinate == null ? "" : Double.toString(coordinate.y));
                        values.add(coordinate == null ? "" : Double.toString(coordinate.x));
                    }
                }
                writeCsvLine(writer, values);
            }
        }
    }

    private List<String> header(int components) {
        List<String> result = attributes.stream().map(AttributeDescriptor::getLocalName).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (int component = 1; component <= components; component++) {
            for (String direction : DIRECTIONS) {
                result.add("pgon" + component + "_lat_" + direction.charAt(0));
                result.add("pgon" + component + "_lon_" + direction.charAt(0));
            }
        }
        return result;
    }

    private static Extrema extrema(Polygon polygon) {
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        return new Extrema(List.of(
                select(coordinates, Comparator.comparingDouble((Coordinate coordinate) -> coordinate.y).reversed()),
                select(coordinates, Comparator.comparingDouble((Coordinate coordinate) -> coordinate.x).reversed()),
                select(coordinates, Comparator.comparingDouble(coordinate -> coordinate.y)),
                select(coordinates, Comparator.comparingDouble(coordinate -> coordinate.x))
        ));
    }

    private static Coordinate select(Coordinate[] coordinates, Comparator<Coordinate> comparator) {
        return java.util.Arrays.stream(coordinates).min(comparator).orElseThrow();
    }

    private static String geocaching(Coordinate coordinate) {
        return coordinatePart(coordinate.y, 'N', 'S', 2) + " " + coordinatePart(coordinate.x, 'E', 'W', 3);
    }

    private static String coordinatePart(double value, char positive, char negative, int degreeWidth) {
        double absolute = Math.abs(value);
        int degrees = (int) absolute;
        double minutes = (absolute - degrees) * 60;
        if (minutes >= 59.9995) {
            degrees++;
            minutes = 0;
        }
        return String.format(java.util.Locale.ROOT, "%c%0" + degreeWidth + "d°%06.3f", value >= 0 ? positive : negative, degrees, minutes);
    }

    private static int width(List<String> labels) {
        return labels.stream().mapToInt(String::length).max().orElse(0);
    }

    private static void printAligned(String label, String value, int width) {
        System.out.println(label + ":" + " ".repeat(width - label.length() + 1) + value);
    }

    private void writeCsvLine(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) writer.write(csvSeparator);
            writer.write('"');
            writer.write(values.get(index).replace("\"", "\"\""));
            writer.write('"');
        }
        writer.newLine();
    }

    private record Extrema(List<Coordinate> coordinates) {
    }

    private record Row(List<String> attributes, List<Extrema> extrema) {
    }
}
