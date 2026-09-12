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
java -jar target/shp-converter-1.0-SNAPSHOT.jar input.shp output.geojson [--name-field FIELD] [--reproject EPSG:CODE] [--quiet|-q] [--force|-f]
```

Supported output extensions are `.geojson`, `.gpx`, and `.pgon`.

```powershell
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp obce.geojson --reproject EPSG:4326
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp obce.gpx --name-field NAZEV
java -jar target/shp-converter-1.0-SNAPSHOT.jar obce.shp obce.pgon --name-field NAZEV
```

Use `--help` for syntax and `--version` for the version.

By default, the converter prints the input file and output directory before processing, progress after each 250 input features, and a completion line with the total processed count. Use `--quiet` or `-q` to suppress these status messages; errors remain on standard error.

If the output directory does not exist, use `--force` or `-f` to create it. The converter reports the created directory by default; `--quiet` suppresses that message too.

## CRS and coordinates

The converter reads the Shapefile CRS from its `.prj` file. `--reproject EPSG:4326` requests GeoJSON reprojection. GPX and PGON always transform input geometries to `EPSG:4326`, as they require geographic coordinates. A missing or unreadable CRS is an error whenever transformation is needed.

Coordinate order differs by format:

- GeoJSON: `[longitude, latitude]`
- GPX: `lat="latitude" lon="longitude"`
- PGON: `latitude longitude`

## Geometry and names

Only `Polygon` and `MultiPolygon` input is supported. GeoJSON preserves every polygon component and interior ring. GPX writes one exterior-boundary track per polygon component. PGON writes a separate UTF-8 file per component.

`--name-field NAZEV` selects the name attribute. Without it, the first non-empty string attribute is used; otherwise names such as `polygon-1` are generated. Every output item receives a separate name-based file: `obec.geojson` and the name `Bělá pod Bezdězem` produce `obec-Bělá pod Bezdězem.geojson`. The filename component is sanitized only as required by the operating system; the original name remains unchanged in output content. Duplicate generated filenames are rejected rather than overwritten.

## Input encoding

All generated output is UTF-8. When a sibling `.cpg` file is present, its code-page declaration controls GeoTools DBF attribute decoding (for example, `1250` selects Windows-1250). Without a `.cpg` file, DBF input defaults to UTF-8. Invalid or unsupported `.cpg` declarations cause a conversion error.

The input `.shp`, `.shx`, and `.dbf` files must all be present and readable.
