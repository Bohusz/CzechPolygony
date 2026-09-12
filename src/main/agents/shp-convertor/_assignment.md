Create a small standalone Java command-line application that converts ESRI Shapefile data to several commonly used geographic formats.

## Goal

The application should read an ESRI Shapefile dataset (`.shp`, together with its accompanying `.shx`, `.dbf` and optionally `.prj` files) and convert its geographic features to:

* GeoJSON
* GPX
* GSAK polygon (`.pgon`)

The primary use case is converting Czech geographic open data from ČÚZK and ČSÚ, including data using the Czech S-JTSK coordinate reference system (EPSG:5514).

## Technology

* Java
* Maven
* GeoTools for reading Shapefiles and handling GIS data
* JTS for geometry handling
* GeoJSON for standard geographic output
* GPX for GPS-compatible output
* No native GDAL/OGR dependencies unless there is a compelling technical reason.

Use current stable versions of GeoTools and compatible JTS dependencies.

## Input

The basic command syntax should be:

```
java -jar shp2geojson.jar input.shp output.geojson
```

However, the application should be renamed/restructured so that it is not tied conceptually to GeoJSON, since it supports multiple output formats.

Prefer a general syntax such as:

```
java -jar shp-converter.jar input.shp output.geojson
java -jar shp-converter.jar input.shp output.gpx
java -jar shp-converter.jar input.shp output.pgon
```

The output format may be inferred from the output filename extension.

Also support an explicit format option if this makes the implementation cleaner, for example:

```
--format geojson
--format gpx
--format pgon
```

## Output formats

### 1. GeoJSON

Create a valid RFC 7946 GeoJSON FeatureCollection.

Each input feature should become a GeoJSON Feature.

Preserve the non-geometric attributes from the Shapefile DBF table as GeoJSON properties.

The geometry should be written as a GeoJSON geometry object.

When `--reproject EPSG:4326` is specified, transform the geometry from the source CRS to WGS 84 before writing GeoJSON.

For GeoJSON, coordinates in EPSG:4326 must be written in the conventional:

```
[longitude, latitude]
```

order.

Do not silently discard attributes or geometries.

### 2. GPX

Support writing polygon geometry to GPX.

Because standard GPX does not have a dedicated polygon geometry type, represent each polygon boundary as a GPX track/track segment.

For each input polygon:

* create a corresponding GPX track;
* write the polygon's exterior ring as the track segment;
* repeat the first coordinate at the end so that the track is explicitly closed;
* preserve the feature name where a suitable name attribute is available.

For MultiPolygon geometries:

* write each polygon component as a separate GPX track;
* use a sensible name derived from the input feature and component number.

Only polygon boundaries need to be exported. Do not attempt to represent polygon interiors/holes in GPX unless there is a standard-compatible and clearly documented way to do so.

The resulting GPX should be valid GPX 1.1.

Use latitude/longitude in the GPX `lat` and `lon` attributes.

### 3. GSAK `.pgon`

Support the simple polygon format used by GSAK.

A `.pgon` file has the following structure:

```
# GsakName=Bělá pod Bezdězem
50.53834948224382 14.82675439480499
50.5384396501585 14.826903467105987
50.539172432112814 14.825492195096162
...
50.53834948224382 14.82675439480499
#
```

The first line contains:

```
# GsakName=<name>
```

This is followed by polygon vertices, one vertex per line.

Coordinates are written as:

```
latitude longitude
```

This is important: `.pgon` uses latitude first and longitude second, unlike GeoJSON.

The polygon MUST be explicitly closed by repeating the first coordinate as the final coordinate.

After the repeated first coordinate, write a final line containing exactly:

```
#
```

The `#` line terminates the polygon.

For example:

```
# GsakName=Brno
49.20 16.60
49.21 16.61
49.21 16.62
49.20 16.60
#
```

Use the feature's name attribute for `GsakName`.

Provide a command-line option to specify which input attribute contains the name, for example:

```
--name-field NAZEV
```

If no name field is specified, use a reasonable fallback such as the first suitable string attribute. If no suitable attribute exists, use a generated name such as `polygon-1`.

For MultiPolygon geometries, write each polygon component as a separate `.pgon` file by default.

Generated filenames should be based on the feature name and/or feature identifier, sanitized for use as a filename.

The output file must use UTF-8 encoding.

Do not add any additional text, metadata or delimiters to the PGON file beyond the format described above.

```
--name-field NAZEV
```

If no name field is specified, use a reasonable fallback such as the first suitable string attribute. If no suitable attribute exists, use a generated name such as `polygon-1`.

For MultiPolygon geometries:

* write each polygon component as a separate `.pgon` file, or
* provide a clearly documented option for writing multiple polygons to separate files.

Prefer separate files by default because the shown `.pgon` format has no obvious standard mechanism for identifying multiple independent polygons.

Generated filenames should be based on the feature name, sanitized for use as a filename.

## CRS and reprojection

The application must correctly handle coordinate reference systems defined in the Shapefile `.prj` file.

The main expected source CRS is:

```
EPSG:5514 — S-JTSK / Krovak East North
```

The application must use GeoTools/JTS CRS transformation facilities rather than implementing coordinate transformations manually.

Support:

```
--reproject EPSG:4326
```

or an equivalent target CRS option.

For GPX and GSAK `.pgon`, the output must normally be WGS 84 (EPSG:4326), because both formats use geographic latitude/longitude coordinates.

If the input CRS is not EPSG:4326, reproject it automatically for GPX and PGON output.

Be especially careful about axis order.

Output conventions:

* GeoJSON EPSG:4326: longitude, latitude
* GPX: latitude, longitude
* GSAK PGON: latitude, longitude

Do not confuse the coordinate order between these formats.

## Geometry handling

Support at least:

* Polygon
* MultiPolygon

For Polygon:

* export the exterior ring;
* for GeoJSON, preserve interior rings/holes;
* for GPX and PGON, export the exterior boundary by default.

For MultiPolygon:

* process every polygon component independently;
* document clearly how multiple components are represented in each output format.

Do not silently convert or merge geometries in a way that changes their topology.

## Feature attributes

For GeoJSON, preserve all usable DBF attributes.

For GPX and PGON, attributes are not generally supported in the same way, so use the configured name field for identifying the feature.

Provide:

```
--name-field FIELD
```

For example:

```
--name-field NAZEV
```

If the specified field does not exist, terminate with a clear error.

## Command-line interface

Support at least:

```
--help

--version

--name-field FIELD

--reproject EPSG:CODE
```

The output format should preferably be inferred from the output file extension:

```
.geojson
.gpx
.pgon
```

Provide clear usage information.

Examples:

```
java -jar shp-converter.jar obce.shp obce.geojson --reproject EPSG:4326

java -jar shp-converter.jar obce.shp obce.gpx --name-field NAZEV

java -jar shp-converter.jar obce.shp obce.pgon --name-field NAZEV
```

## Error handling

Provide clear command-line error messages for:

* missing input file
* missing or unreadable Shapefile components
* invalid Shapefile
* missing CRS information when required
* unsupported CRS
* invalid command-line arguments
* unsupported geometry type
* missing name field
* failure to write the output file

Return a non-zero exit code on failure.

Do not silently ignore features that cannot be converted. Report them clearly.

## Encoding

All text output must be UTF-8.

This is particularly important for Czech characters in feature names and attributes.

Do not unnecessarily escape Czech characters.

The `.pgon` file should also be written as UTF-8.

## Precision

Do not unnecessarily round coordinates.

For `.pgon`, preserve sufficient decimal precision for the source geometry, preferably using Java's normal double-to-string representation or an explicitly configurable precision.

The example `.pgon` files use approximately 14–15 significant decimal digits, so the implementation should not reduce coordinates to a small number of decimal places by default.

## Large files

Do not assume that the entire Shapefile must be loaded into memory at once.

Use streaming/iterative processing where practical.

For formats that require a complete document structure, such as GeoJSON FeatureCollection, use a streaming writer if supported by the chosen library.

## Architecture

Keep the application small and understandable.

A possible structure:

* `Main` – command-line interface
* `ShapefileReader` – reading the Shapefile
* `GeometryTransformer` – CRS transformation
* `GeoJsonWriter` – GeoJSON output
* `GpxWriter` – GPX output
* `PgonWriter` – GSAK PGON output
* `OutputFormat` – output format selection

Do not introduce unnecessary frameworks.

Use interfaces where they make the output writers easy to test and extend, but avoid overengineering.

## Testing

Create unit tests for at least:

1. Reading a small Shapefile.
2. Preserving feature attributes in GeoJSON.
3. Writing valid GeoJSON.
4. Reprojecting EPSG:5514 to EPSG:4326.
5. Correct handling of Czech characters.
6. Writing valid GPX 1.1.
7. Exporting a Polygon to GPX.
8. Exporting a Polygon to PGON.
9. Correct latitude/longitude order in PGON.
10. Correct longitude/latitude order in GeoJSON.
11. Correct handling of MultiPolygon.
12. Handling invalid input.
13. Handling a missing name field.

Create small test fixtures rather than depending on external data.

For the PGON tests, verify the exact textual structure, including:

```
# GsakName=<name>
```

and the latitude/longitude coordinate order.

## Maven

Configure the Maven project so that:

* `mvn test` runs all tests.
* `mvn package` creates an executable JAR.
* all required runtime dependencies are included in the resulting JAR;
* the resulting application can be run without manually assembling a classpath.

Prefer a self-contained executable/fat JAR.

## Documentation

Create a concise `README.md` containing:

* prerequisites
* how to build the application
* command-line syntax
* supported output formats
* examples
* CRS/reprojection explanation
* coordinate-order differences between GeoJSON, GPX and PGON
* explanation of how Polygon and MultiPolygon are handled
* example using EPSG:5514 input and EPSG:4326 output
* example using a Czech feature name such as `Bělá pod Bezdězem`

## Development process

Create Java code in hierarchy starting with package `cz.xlii.xbr.shp.convertor`.

First inspect the project and determine the appropriate current GeoTools/JTS dependencies and Maven configuration.
- Use the most advanced (newest) stable versions.
- Maven modules `org.geotools:gt-shapefile` and `org.geotools:gt-main` are expected but not required.

Then implement the application.

Run:

```
mvn test
```

and:

```
mvn package
```

Fix compilation errors, test failures and runtime problems rather than merely reporting them.

If practical, create a small real-world integration test using a sample Czech Shapefile.

Pay particular attention to:

* EPSG:5514 → EPSG:4326 transformation;
* axis order;
* Polygon vs. MultiPolygon;
* GeoJSON longitude/latitude order;
* GPX latitude/longitude attributes;
* PGON latitude/longitude order;
* preservation of Czech UTF-8 text.

Do not add functionality that is not required above.
