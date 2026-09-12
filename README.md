# Shapefile converter

A small Java command-line converter for ESRI Shapefile polygon data, including Czech S-JTSK (`EPSG:5514`) datasets.

## Build

Install JDK 21, then run:

```powershell
mvn package
```

The self-contained executable is `target/shp-converter-1.0-SNAPSHOT.jar`.

## Usage

```text
java -jar target/shp-converter-1.0-SNAPSHOT.jar input.shp output.geojson [--name-field FIELD] [--file-name-field FIELD] [--reproject EPSG:CODE] [--quiet|-q] [--force|-f]
```

Supported output extensions are `.geojson`, `.gpx`, and `.pgon`.

```powershell
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp obce.geojson --reproject EPSG:4326
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp obce.gpx --name-field NAZEV
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp obce.pgon --name-field NAZEV
```

Use `--help` for syntax and `--version` for the version.

### Metadata report

Use `--metadata` instead of a conversion output path to report all DBF attributes and the north, east, south, and west vertex of every polygon component:

```powershell
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp --metadata
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp --metadata obce.csv
```

Without a report file, every attribute and extremum is printed on its own line. Attribute values and coordinate values are independently aligned, and reports for individual features are separated by one blank line. Coordinates use the geocaching form `N49°23.676 E016°44.608`.

A supplied report must have the `.csv` extension; it creates one CSV file with one row per feature, DBF attribute columns, and separate signed decimal-degree latitude/longitude columns for each component's north, east, south, and west extrema. CSV defaults to the semicolon separator and `windows-1250` encoding. Use `--csv-separator SEPARATOR` to choose a one-character delimiter or `--csv-charset CHARSET` to choose any supported Java charset, for example:

```powershell
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp --metadata obce.csv --csv-separator , --csv-charset UTF-8
```

By default, the converter prints the input file, output file, output directory, and values of supplied options before processing, progress after each 250 input features, and a completion line with the total processed count. Use `--quiet` or `-q` to suppress these status messages; errors remain on standard error.

If the output directory does not exist, use `--force` or `-f` to create it. The converter reports the created directory by default; `--quiet` suppresses that message too.

## CRS and coordinates

The converter reads the Shapefile CRS from its `.prj` file. `--reproject EPSG:4326` requests GeoJSON reprojection. GPX and PGON always transform input geometries to `EPSG:4326`, as they require geographic coordinates. A missing or unreadable CRS is an error whenever transformation is needed.

Coordinate order differs by format:

- GeoJSON: `[longitude, latitude]`
- GPX: `lat="latitude" lon="longitude"`
- PGON: `latitude longitude`

## Geometry and names

Only `Polygon` and `MultiPolygon` input is supported. GeoJSON preserves every polygon component and interior ring. GPX writes one exterior-boundary track per polygon component. PGON writes a separate UTF-8 file per component.

`--name-field NAZEV` selects the name attribute. Without it, the first non-empty string attribute is used; otherwise names such as `polygon-1` are generated. Every output item receives a separate name-based file: `obec.geojson` and the name `Bělá pod Bezdězem` produce `obec-Bělá pod Bezdězem.geojson`. Use `--file-name-field FIELD` to select a different attribute exclusively for filenames; without it, the feature name is used as before. The filename component is sanitized only as required by the operating system; the original name remains unchanged in output content. Duplicate generated filenames are rejected rather than overwritten.

## Input encoding

All generated output is UTF-8 except metadata CSV, which defaults to `windows-1250` and can be changed with `--csv-charset`. When a sibling `.cpg` file is present, its code-page declaration controls GeoTools DBF attribute decoding (for example, `1250` selects Windows-1250). Without a `.cpg` file, DBF input defaults to UTF-8. Invalid or unsupported `.cpg` declarations cause a conversion error.

The input `.shp`, `.shx`, and `.dbf` files must all be present and readable.
