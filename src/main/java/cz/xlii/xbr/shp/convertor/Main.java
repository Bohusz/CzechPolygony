package cz.xlii.xbr.shp.convertor;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private static final String VERSION = "shp-converter 1.0.0";

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = Arguments.parse(args);
            convert(arguments);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    static void convert(Arguments arguments) throws IOException {
        validateInput(arguments.input());
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("url", arguments.input().toUri().toURL());
            DataStore dataStore = DataStoreFinder.getDataStore(parameters);
            if (dataStore == null) throw new ConversionException("Cannot read Shapefile: " + arguments.input());
            try {
                if (dataStore instanceof ShapefileDataStore shapefileDataStore) {
                    shapefileDataStore.setCharset(inputCharset(arguments.input()));
                }
                String typeName = dataStore.getTypeNames()[0];
                SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
                SimpleFeatureType schema = source.getSchema();
                NameResolver names = new NameResolver(arguments.nameField(), schema);
                GeometryTransformer transformer = transformer(arguments, schema);
                ProgressReporter progress = new ProgressReporter(arguments, source.getFeatures().size());
                progress.prepareOutputDirectory();
                progress.printPaths();
                writeFeatures(arguments, source, names, transformer, progress);
                progress.printCompletion();
            } finally {
                dataStore.dispose();
            }
        } catch (MalformedURLException e) {
            throw new ConversionException("Invalid input path: " + arguments.input(), e);
        }
    }

    private static GeometryTransformer transformer(Arguments arguments, SimpleFeatureType schema) {
        String target = arguments.format() == OutputFormat.GEOJSON ? arguments.reproject() : "EPSG:4326";
        return target == null ? null : GeometryTransformer.between(schema.getCoordinateReferenceSystem(), target);
    }

    private static void writeFeatures(Arguments arguments, SimpleFeatureSource source, NameResolver names,
                                      GeometryTransformer transformer, ProgressReporter progress) throws IOException {
        OutputFilenameResolver outputNames = new OutputFilenameResolver(arguments.output());
        switch (arguments.format()) {
            case GEOJSON -> writeGeoJson(source, names, transformer, progress, outputNames);
            case GPX -> writeGpx(source, names, transformer, progress, outputNames);
            case PGON -> writePgon(source, names, transformer, progress, outputNames);
        }
    }

    private static void writeGeoJson(SimpleFeatureSource source, NameResolver names, GeometryTransformer transformer,
                                     ProgressReporter progress, OutputFilenameResolver outputNames) throws IOException {
        try (SimpleFeatureIterator features = source.getFeatures().features()) {
            int featureNumber = 0;
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                featureNumber++;
                Geometry geometry = polygonal(feature);
                try (GeoJsonWriter writer = new GeoJsonWriter(outputNames.resolve(names.resolve(feature, featureNumber)))) {
                    writer.write(feature, transformer == null ? geometry : transformer.transform(geometry));
                }
                progress.processedFeature();
            }
        }
    }

    private static void writeGpx(SimpleFeatureSource source, NameResolver names, GeometryTransformer transformer,
                                 ProgressReporter progress, OutputFilenameResolver outputNames) throws IOException {
        try (SimpleFeatureIterator features = source.getFeatures().features()) {
            int featureNumber = 0;
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                featureNumber++;
                List<Polygon> polygons = polygons(transformer.transform(polygonal(feature)));
                String name = names.resolve(feature, featureNumber);
                for (int i = 0; i < polygons.size(); i++) {
                    String componentName = polygons.size() == 1 ? name : name + " - " + (i + 1);
                    try (GpxWriter writer = new GpxWriter(outputNames.resolve(componentName))) {
                        writer.write(componentName, polygons.get(i));
                    }
                }
                progress.processedFeature();
            }
        }
    }

    private static void writePgon(SimpleFeatureSource source, NameResolver names, GeometryTransformer transformer,
                                  ProgressReporter progress, OutputFilenameResolver outputNames) throws IOException {
        try (SimpleFeatureIterator features = source.getFeatures().features()) {
            int featureNumber = 0;
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                featureNumber++;
                String name = names.resolve(feature, featureNumber);
                List<Polygon> polygons = polygons(transformer.transform(polygonal(feature)));
                for (int i = 0; i < polygons.size(); i++) {
                    String componentName = polygons.size() == 1 ? name : name + " - " + (i + 1);
                    new PgonWriter(outputNames.resolve(componentName)).write(componentName, polygons.get(i));
                }
                progress.processedFeature();
            }
        }
    }

    private static Geometry polygonal(SimpleFeature feature) {
        Object geometry = feature.getDefaultGeometry();
        if (!(geometry instanceof Geometry value))
            throw new ConversionException("Feature " + feature.getID() + " has no geometry.");
        if (!(value instanceof Polygon) && !(value instanceof MultiPolygon)) {
            throw new ConversionException("Unsupported geometry type in feature " + feature.getID() + ": " + value.getGeometryType());
        }
        return value;
    }

    private static List<Polygon> polygons(Geometry geometry) {
        List<Polygon> result = new ArrayList<>();
        if (geometry instanceof Polygon polygon) result.add(polygon);
        else if (geometry instanceof MultiPolygon multiPolygon) {
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++)
                result.add((Polygon) multiPolygon.getGeometryN(i));
        } else
            throw new ConversionException("Transformation produced unsupported geometry: " + geometry.getGeometryType());
        return result;
    }

    private static void validateInput(Path input) {
        if (!input.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".shp")) {
            throw new ConversionException("Input must be a .shp file.");
        }
        for (String extension : List.of(".shp", ".shx", ".dbf")) {
            Path component = sibling(input, extension);
            if (!Files.isRegularFile(component) || !Files.isReadable(component)) {
                throw new ConversionException("Missing or unreadable Shapefile component: " + component);
            }
        }
    }

    private static Path sibling(Path input, String extension) {
        String name = input.getFileName().toString();
        return input.resolveSibling(name.substring(0, name.length() - 4) + extension);
    }

    private static Charset inputCharset(Path input) throws IOException {
        Path cpg = sibling(input, ".cpg");
        if (!Files.exists(cpg)) return StandardCharsets.UTF_8;
        if (!Files.isRegularFile(cpg) || !Files.isReadable(cpg)) {
            throw new ConversionException("Unreadable CPG encoding declaration: " + cpg);
        }
        String declaration = Files.readString(cpg, StandardCharsets.UTF_8).strip();
        if (declaration.isEmpty() || declaration.lines().count() != 1) {
            throw new ConversionException("Invalid CPG encoding declaration in " + cpg);
        }
        try {
            return Charset.forName(declaration);
        } catch (IllegalArgumentException ignored) {
            if (declaration.matches("[0-9]+")) {
                try {
                    return Charset.forName("windows-" + declaration);
                } catch (IllegalArgumentException ignoredAgain) {
                    // The clear error below applies to both unsupported declaration forms.
                }
            }
            throw new ConversionException("Unsupported CPG encoding declaration '" + declaration + "' in " + cpg);
        }
    }

    record Arguments(Path input, Path output, OutputFormat format, String nameField, String reproject, boolean quiet,
                     boolean force) {
        Arguments(Path input, Path output, OutputFormat format, String nameField, String reproject) {
            this(input, output, format, nameField, reproject, false, false);
        }

        static Arguments parse(String[] args) {
            if (args.length == 1 && "--help".equals(args[0])) {
                usage(0);
            }
            if (args.length == 1 && "--version".equals(args[0])) {
                System.out.println(VERSION);
                System.exit(0);
            }
            List<String> positional = new ArrayList<>();
            String nameField = null;
            String reproject = null;
            boolean quiet = false;
            boolean force = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--name-field" -> nameField = optionValue(args, ++i, "--name-field");
                    case "--reproject" -> reproject = optionValue(args, ++i, "--reproject");
                    case "--quiet", "-q" -> quiet = true;
                    case "--force", "-f" -> force = true;
                    case "--help", "--version" -> usage(0);
                    default -> {
                        if (args[i].startsWith("--")) throw new ConversionException("Unknown option: " + args[i]);
                        positional.add(args[i]);
                    }
                }
            }
            if (positional.size() != 2) usage(1);
            if (reproject != null && !reproject.matches("EPSG:[0-9]+"))
                throw new ConversionException("Invalid CRS code: " + reproject);
            Path output = Path.of(positional.get(1));
            return new Arguments(Path.of(positional.get(0)), output, OutputFormat.fromOutput(output), nameField, reproject, quiet,
                    force);
        }

        private static String optionValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--"))
                throw new ConversionException("Missing value for " + option);
            return args[index];
        }

        private static void usage(int exitCode) {
            System.out.println("Usage: java -jar shp-converter.jar input.shp output.(geojson|gpx|pgon) [--name-field FIELD] [--reproject EPSG:CODE] [--quiet|-q] [--force|-f]");
            System.exit(exitCode);
        }
    }

    private static final class ProgressReporter {
        private final Arguments arguments;
        private final int totalFeatures;
        private int processedFeatures;

        private ProgressReporter(Arguments arguments, int totalFeatures) {
            this.arguments = arguments;
            this.totalFeatures = totalFeatures;
        }

        private void prepareOutputDirectory() throws IOException {
            Path outputDirectory = outputDirectory();
            if (Files.isDirectory(outputDirectory)) return;
            if (Files.exists(outputDirectory)) {
                throw new ConversionException("Output directory is not a directory: " + outputDirectory);
            }
            if (!arguments.force()) {
                throw new ConversionException("Output directory does not exist: " + outputDirectory + " (use --force to create it)");
            }
            Files.createDirectories(outputDirectory);
            if (!arguments.quiet()) {
                System.out.println("Created output directory: " + outputDirectory);
            }
        }

        private void printPaths() {
            if (!arguments.quiet()) {
                System.out.println("Input file: " + arguments.input());
                System.out.println("Output directory: " + outputDirectory());
            }
        }

        private void processedFeature() {
            processedFeatures++;
            if (!arguments.quiet() && processedFeatures % 250 == 0) {
                System.out.println("Processed: " + processedFeatures + ", remaining: " + (totalFeatures - processedFeatures));
            }
        }

        private void printCompletion() {
            if (!arguments.quiet()) {
                System.out.println("Completed: " + processedFeatures + " input features read and processed.");
            }
        }

        private Path outputDirectory() {
            Path parent = arguments.output().toAbsolutePath().getParent();
            return parent == null ? Path.of(".").toAbsolutePath() : parent;
        }
    }
}
