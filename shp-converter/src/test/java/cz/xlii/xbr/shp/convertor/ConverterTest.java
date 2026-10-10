package cz.xlii.xbr.shp.convertor;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConverterTest {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @TempDir
    Path directory;

    @Test
    void convertsPolygonToAllFormatsAndPreservesCzechName() throws Exception {
        Path shapefile = createShapefile("Bělá pod Bezdězem");

        Path geojson = directory.resolve("result.geojson");
        Main.convert(new Main.Arguments(shapefile, geojson, OutputFormat.GEOJSON, null, "EPSG:4326"));
        String geojsonText = Files.readString(directory.resolve("result-Bělá pod Bezdězem.geojson"));
        assertTrue(geojsonText.contains("Bělá pod Bezdězem"));
        assertTrue(geojsonText.contains("[14.0,50.0]"));

        Path gpx = directory.resolve("result.gpx");
        Main.convert(new Main.Arguments(shapefile, gpx, OutputFormat.GPX, "NAZEV", null));
        String gpxText = Files.readString(directory.resolve("result-Bělá pod Bezdězem.gpx"));
        assertTrue(gpxText.contains("version=\"1.1\""));
        assertTrue(gpxText.contains("lat=\"50.0\" lon=\"14.0\""));

        Path pgon = directory.resolve("result.pgon");
        Main.convert(new Main.Arguments(shapefile, pgon, OutputFormat.PGON, "NAZEV", null));
        String pgonText = Files.readString(directory.resolve("result-Bělá pod Bezdězem.pgon"));
        assertEquals("# GsakName=Bělá pod Bezdězem\n50.0 14.0\n51.0 15.0\n50.0 15.0\n50.0 14.0\n#\n", pgonText);
    }

    @Test
    void rejectsMissingConfiguredNameField() throws Exception {
        Path shapefile = createShapefile("Brno");
        ConversionException exception = assertThrows(ConversionException.class,
                () -> Main.convert(new Main.Arguments(shapefile, directory.resolve("result.gpx"), OutputFormat.GPX, "MISSING", null)));
        assertTrue(exception.getMessage().contains("MISSING"));
    }

    @Test
    void rejectsMissingShapefileComponent() {
        ConversionException exception = assertThrows(ConversionException.class,
                () -> Main.convert(new Main.Arguments(directory.resolve("missing.shp"), directory.resolve("result.geojson"), OutputFormat.GEOJSON, null, null)));
        assertTrue(exception.getMessage().contains("Missing or unreadable"));
    }

    @Test
    void transformsKrovakCoordinatesToLongitudeFirstWgs84() throws Exception {
        Point krovakPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(-743261.88, -1043538.13));
        Point wgs84Point = (Point) GeometryTransformer.between(CRS.decode("EPSG:5514"), "EPSG:4326").transform(krovakPoint);
        assertTrue(wgs84Point.getX() > 13 && wgs84Point.getX() < 15);
        assertTrue(wgs84Point.getY() > 49 && wgs84Point.getY() < 51);
    }

    @Test
    void transformsProvidedKrovakWktWithoutDiscardingItsAxisParameters() throws Exception {
        String wkt = "PROJCS[\"S-JTSK_Krovak_East_North\",GEOGCS[\"GCS_S_JTSK\",DATUM[\"D_S_JTSK\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Krovak\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Pseudo_Standard_Parallel_1\",78.5],PARAMETER[\"Scale_Factor\",0.9999],PARAMETER[\"Azimuth\",30.28813975277778],PARAMETER[\"Longitude_Of_Center\",24.83333333333333],PARAMETER[\"Latitude_Of_Center\",49.5],PARAMETER[\"X_Scale\",-1.0],PARAMETER[\"Y_Scale\",1.0],PARAMETER[\"XY_Plane_Rotation\",90.0],UNIT[\"Meter\",1.0]]";
        Point sourcePoint = GEOMETRY_FACTORY.createPoint(new Coordinate(-590398.43, -1111697.68));

        Point wgs84Point = (Point) GeometryTransformer.between(CRS.parseWKT(wkt), "EPSG:4326").transform(sourcePoint);

        assertTrue(wgs84Point.getX() > 16.5 && wgs84Point.getX() < 16.8, wgs84Point::toString);
        assertTrue(wgs84Point.getY() > 49.5 && wgs84Point.getY() < 49.8, wgs84Point::toString);
    }

    @Test
    void reportsPathsProgressAndCompletion() throws Exception {
        Path shapefile = createShapefileWithDistinctNames("Brno", 250);
        Path output = directory.resolve("result.geojson");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(new Main.Arguments(shapefile, output, OutputFormat.GEOJSON, null, null));
        } finally {
            System.setOut(originalOut);
        }
        String text = captured.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Input file: " + shapefile));
        assertTrue(text.contains("Output directory: " + directory));
        assertTrue(text.contains("Processed: 250, remaining: 0"));
        assertTrue(text.contains("Completed: 250 input features read and processed."));
    }

    @Test
    void quietOptionsSuppressStatusOutput() throws Exception {
        Path shapefile = createShapefile("Brno");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), directory.resolve("first.geojson").toString(), "--quiet"}));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), directory.resolve("second.geojson").toString(), "-q"}));
        } finally {
            System.setOut(originalOut);
        }
        assertEquals("", captured.toString(StandardCharsets.UTF_8));
    }

    @Test
    void forceOptionsCreateMissingOutputDirectoryAndReportIt() throws Exception {
        Path shapefile = createShapefile("Brno");
        Path longOptionDirectory = directory.resolve("long-option");
        Path shortOptionDirectory = directory.resolve("short-option");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), longOptionDirectory.resolve("result.geojson").toString(), "--force"}));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), shortOptionDirectory.resolve("result.geojson").toString(), "-f"}));
        } finally {
            System.setOut(originalOut);
        }
        String text = captured.toString(StandardCharsets.UTF_8);
        assertTrue(Files.isDirectory(longOptionDirectory));
        assertTrue(Files.isDirectory(shortOptionDirectory));
        assertTrue(text.contains("Created output directory: " + longOptionDirectory));
        assertTrue(text.contains("Created output directory: " + shortOptionDirectory));
    }

    @Test
    void quietModeSuppressesForceDirectoryCreationOutput() throws Exception {
        Path shapefile = createShapefile("Brno");
        Path outputDirectory = directory.resolve("quiet-force");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), outputDirectory.resolve("result.geojson").toString(), "--force", "--quiet"}));
        } finally {
            System.setOut(originalOut);
        }
        assertTrue(Files.isDirectory(outputDirectory));
        assertEquals("", captured.toString(StandardCharsets.UTF_8));
    }

    @Test
    void reportsConfiguredParameters() throws Exception {
        Path shapefile = createShapefile("Brno");
        Path output = directory.resolve("result.geojson");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), output.toString(), "--name-field", "NAZEV",
                    "--file-name-field", "IDENTIFIER", "--reproject", "EPSG:4326", "--force"}));
        } finally {
            System.setOut(originalOut);
        }

        String text = captured.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Input file: " + shapefile), text);
        assertTrue(text.contains("Output file: " + output), text);
        assertTrue(text.contains("Name field: NAZEV"), text);
        assertTrue(text.contains("File name field: IDENTIFIER"), text);
        assertTrue(text.contains("Reproject: EPSG:4326"), text);
        assertTrue(text.contains("Force: enabled"), text);
    }

    @Test
    void reportsSuppliedMetadataParameters() throws Exception {
        Path shapefile = createShapefile("Brno");
        Path report = directory.resolve("report.csv");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata", report.toString(),
                    "--csv-separator", ",", "--csv-charset", "UTF-8"}));
        } finally {
            System.setOut(originalOut);
        }

        String text = captured.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Metadata: enabled"), text);
        assertTrue(text.contains("CSV separator: ,"), text);
        assertTrue(text.contains("CSV charset: UTF-8"), text);
    }

    @Test
    void usesCpgEncodingForDbfAttributes() throws Exception {
        Path shapefile = createShapefile("Žďár", 1, Charset.forName("windows-1250"));
        Files.writeString(shapefile.resolveSibling("input.cpg"), "1250", StandardCharsets.UTF_8);

        Main.convert(new Main.Arguments(shapefile, directory.resolve("result.geojson"), OutputFormat.GEOJSON, null, null));

        String geojson = Files.readString(directory.resolve("result-Žďár.geojson"));
        assertTrue(geojson.contains("Žďár"));
    }

    @Test
    void rejectsUnsupportedCpgEncoding() throws Exception {
        Path shapefile = createShapefile("Brno");
        Files.writeString(shapefile.resolveSibling("input.cpg"), "unsupported-encoding", StandardCharsets.UTF_8);

        ConversionException exception = assertThrows(ConversionException.class,
                () -> Main.convert(new Main.Arguments(shapefile, directory.resolve("result.geojson"), OutputFormat.GEOJSON, null, null)));
        assertTrue(exception.getMessage().contains("Unsupported CPG encoding"));
    }

    @Test
    void rejectsOutputFilenameCollisions() throws Exception {
        Path shapefile = createShapefile("Brno", 2);

        ConversionException exception = assertThrows(ConversionException.class,
                () -> Main.convert(new Main.Arguments(shapefile, directory.resolve("result.geojson"), OutputFormat.GEOJSON, null, null)));
        assertTrue(exception.getMessage().contains("Output filename collision"));
    }

    @Test
    void reportsFeatureMetadataWithGeocachingCoordinates() throws Exception {
        Path shapefile = createShapefile("Bělá");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata"}));
        } finally {
            System.setOut(originalOut);
        }

        String report = captured.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("NAZEV:      Bělá"), report);
        assertTrue(report.contains("Polygon 1 north: N51°00.000 E015°00.000"), report);
        assertTrue(report.contains("Polygon 1 east:  N51°00.000 E015°00.000"), report);
        assertTrue(report.contains("Polygon 1 south: N50°00.000 E014°00.000"), report);
        assertTrue(report.contains("Polygon 1 west:  N50°00.000 E014°00.000"), report);
    }

    @Test
    void writesFeatureMetadataCsv() throws Exception {
        Path shapefile = createShapefile("Bělá");
        Path report = directory.resolve("report.csv");

        Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata", report.toString(), "--quiet"}));

        String csv = Files.readString(report, Charset.forName("windows-1250"));
        assertTrue(csv.startsWith("\"NAZEV\";\"IDENTIFIER\";\"pgon1_lat_n\";\"pgon1_lon_n\";"
                + "\"pgon1_lat_e\";\"pgon1_lon_e\";\"pgon1_lat_s\";\"pgon1_lon_s\";"
                + "\"pgon1_lat_w\";\"pgon1_lon_w\""), csv);
        assertTrue(csv.contains("\"Bělá\";\"1\";\"51.0\";\"15.0\";\"51.0\";\"15.0\";\"50.0\";\"14.0\";\"50.0\";\"14.0\""), csv);
    }

    @Test
    void metadataCsvOptionsConfigureSeparatorAndCharset() throws Exception {
        Path shapefile = createShapefile("Bělá");
        Path report = directory.resolve("report.csv");

        Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata", report.toString(),
                "--csv-separator", ",", "--csv-charset", "UTF-8", "--quiet"}));

        String csv = Files.readString(report, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\"NAZEV\",\"IDENTIFIER\","), csv);
        assertTrue(csv.contains("\"Bělá\",\"1\","), csv);
    }

    @Test
    void fileNameFieldUsesItsAttributeWithoutChangingTheFeatureName() throws Exception {
        Path shapefile = createShapefile("Bělá");
        Path output = directory.resolve("obec.gpx");

        Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), output.toString(), "--name-field", "NAZEV",
                "--file-name-field", "IDENTIFIER", "--quiet"}));

        Path namedOutput = directory.resolve("obec-1.gpx");
        assertTrue(Files.isRegularFile(namedOutput));
        assertTrue(Files.readString(namedOutput).contains("<name>Bělá</name>"));
    }

    @Test
    void rejectsMissingFileNameField() throws Exception {
        Path shapefile = createShapefile("Brno");

        ConversionException exception = assertThrows(ConversionException.class,
                () -> Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), directory.resolve("obec.gpx").toString(),
                        "--file-name-field", "MISSING", "--quiet"})));

        assertTrue(exception.getMessage().contains("file name field"));
    }

    @Test
    void rejectsInvalidMetadataCsvOptions() throws Exception {
        Path shapefile = createShapefile("Brno");

        ConversionException charsetException = assertThrows(ConversionException.class,
                () -> Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata", "report.csv", "--csv-charset", "invalid-charset"}));
        ConversionException separatorException = assertThrows(ConversionException.class,
                () -> Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata", "report.csv", "--csv-separator", "::"}));
        assertTrue(charsetException.getMessage().contains("CSV charset"));
        assertTrue(separatorException.getMessage().contains("separator"));
    }

    @Test
    void separatesAndIndependentlyAlignsStandardOutputMetadataReports() throws Exception {
        Path shapefile = createShapefile("Bělá", 2);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.convert(Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata"}));
        } finally {
            System.setOut(originalOut);
        }

        String report = captured.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("NAZEV:      Bělá"), report);
        assertTrue(report.contains("IDENTIFIER: 1"), report);
        assertTrue(report.contains("Polygon 1 east:  N51°00.000 E015°00.000"), report);
        assertTrue(report.matches("(?s).*\\R\\RNAZEV:.*"), report);
    }

    @Test
    void rejectsNonCsvMetadataReport() throws Exception {
        Path shapefile = createShapefile("Brno");

        ConversionException exception = assertThrows(ConversionException.class,
                () -> Main.Arguments.parse(new String[]{shapefile.toString(), "--metadata", "report.txt"}));

        assertTrue(exception.getMessage().contains(".csv"));
    }

    private Path createShapefile(String name) throws Exception {
        return createShapefile(name, 1);
    }

    private Path createShapefile(String name, int featureCount) throws Exception {
        return createShapefile(name, featureCount, StandardCharsets.UTF_8, false);
    }

    private Path createShapefile(String name, int featureCount, Charset charset) throws Exception {
        return createShapefile(name, featureCount, charset, false);
    }

    private Path createShapefileWithDistinctNames(String name, int featureCount) throws Exception {
        return createShapefile(name, featureCount, StandardCharsets.UTF_8, true);
    }

    private Path createShapefile(String name, int featureCount, Charset charset, boolean distinctNames) throws Exception {
        Path path = directory.resolve("input.shp");
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("areas");
        typeBuilder.setCRS(CRS.decode("EPSG:4326", true));
        typeBuilder.add("the_geom", Polygon.class);
        typeBuilder.add("NAZEV", String.class);
        typeBuilder.add("IDENTIFIER", Integer.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("url", path.toUri().toURL());
        parameters.put("create spatial index", true);
        DataStore dataStore = factory.createNewDataStore(parameters);
        try {
            ((ShapefileDataStore) dataStore).setCharset(charset);
            dataStore.createSchema(type);
            SimpleFeatureStore store = (SimpleFeatureStore) dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
            Transaction transaction = new DefaultTransaction("write");
            store.setTransaction(transaction);
            try {
                org.geotools.feature.DefaultFeatureCollection features = new org.geotools.feature.DefaultFeatureCollection(null, type);
                for (int i = 0; i < featureCount; i++) {
                    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                    builder.add(polygon());
                    builder.add(distinctNames ? name + i : name);
                    builder.add(i + 1);
                    features.add(builder.buildFeature(null));
                }
                store.addFeatures(features);
                transaction.commit();
            } finally {
                transaction.close();
            }
        } finally {
            dataStore.dispose();
        }
        return path;
    }

    private Polygon polygon() {
        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(new Coordinate[]{
                new Coordinate(14, 50), new Coordinate(15, 50), new Coordinate(15, 51), new Coordinate(14, 50)
        });
        return GEOMETRY_FACTORY.createPolygon(shell);
    }
}
