# Implementation Plan: Shapefile Converter

## 1. Maven project configuration

1. Update `pom.xml` to use the application package `cz.xlii.xbr.shp.convertor`.
2. Add the current stable GeoTools BOM from the stable `35.x` release line (not GeoTools `36-SNAPSHOT`) and configure the OSGeo Maven repository.
3. Add GeoTools modules for Shapefile input, core feature support, and CRS/referencing; add compatible JTS and JUnit 5 dependencies.
4. Configure Java 21, UTF-8 compilation/output, and a Maven plugin that produces a self-contained executable JAR.

## 2. Command-line application

1. Create the entry point in `src/main/java/cz/xlii/xbr/shp/convertor`.
2. Support positional input and output paths plus `--help`, `--version`, `--name-field FIELD`, `--file-name-field FIELD`, `--reproject EPSG:CODE`, the `--force` / `-f` output-directory option, `--metadata [report.csv]`, `--csv-separator SEPARATOR`, and `--csv-charset CHARSET`.
3. Infer `geojson`, `gpx`, or `pgon` from the output extension and reject unsupported extensions and invalid arguments with a non-zero exit code.
4. Keep the application conceptually format-neutral; do not retain a GeoJSON-specific JAR or command name.
5. Treat `--metadata` as a metadata-only mode with `input.shp` as its sole required positional argument; accept an optional CSV path as its value and reject combinations with normal conversion output arguments.

## 3. Processing progress reporting (PRG-1 to PRG-6)

1. Extend argument parsing with equivalent `--quiet` / `-q` and `--force` / `-f` flags, and carry both states through the conversion workflow.
2. Add a small progress-reporting component that writes the input file and output directory on separate lines before feature iteration (PRG-1).
3. Obtain the feature count from the GeoTools collection and, after every 250 processed input features, report both the processed count and remaining count (PRG-2).
4. Emit one completion line after a successful conversion that reports the number of input features read and processed (PRG-3).
5. Ensure quiet mode suppresses all normal path, progress, and completion output while leaving errors on standard error unchanged (PRG-4).
6. Before output creation, create a missing output directory only when `--force` or `-f` is supplied; report the directory created through the progress-reporting component, subject to quiet mode (PRG-5).
7. Before feature iteration, report the supplied output file and values of every additional supplied parameter on separate lines; suppress these messages in quiet mode (PRG-6).

## 4. Shapefile reading and validation

1. Validate the input `.shp` and required `.shx` and `.dbf` sidecar files before reading.
2. Resolve the optional sibling `.cpg` file before feature access. Parse its code-page declaration into a Java `Charset`, configure `ShapefileDataStore` through its GeoTools charset API, and otherwise use UTF-8 as the default input encoding (ENC-1).
3. Reject an unreadable, malformed, or unsupported `.cpg` declaration with a clear error instead of decoding DBF attributes using an incorrect fallback (ENC-1).
4. Read features iteratively through GeoTools, without loading the full dataset into memory.
5. Discover the source CRS from the `.prj` file and report a clear error when a required CRS is absent, unreadable, or unsupported.
6. Accept Polygon and MultiPolygon geometries; fail explicitly for any unsupported geometry or conversion failure rather than dropping a feature.

## 5. CRS transformation

1. Implement a geometry transformation component using GeoTools CRS facilities rather than manual coordinate math.
2. Handle the expected S-JTSK / Krovak East North source CRS (`EPSG:5514`) and transform to longitude-first WGS 84 (`EPSG:4326`) with deliberate axis-order handling.
3. Detect ESRI WKT named `S-JTSK_Krovak_East_North` and normalize it through GeoTools as `EPSG:5514` with east/north axes before transformation, avoiding incorrect interpretation of its scale and plane-rotation WKT parameters (CRS-1).
4. Apply an explicitly requested `--reproject EPSG:4326` transformation to GeoJSON output.
5. Automatically transform non-WGS84 input to `EPSG:4326` for GPX and PGON, because those formats require geographic latitude/longitude coordinates.

## 6. Output writers

### GeoJSON

1. Stream an RFC 7946 `FeatureCollection` in UTF-8.
2. Emit each input feature as a GeoJSON Feature, preserve all usable non-geometric DBF attributes, and preserve Polygon interior rings.
3. Write WGS 84 coordinates in `[longitude, latitude]` order.

### GPX

1. Write a valid UTF-8 GPX 1.1 document.
2. Emit one track and exterior-ring track segment for each Polygon component, including an explicit closing coordinate.
3. Write coordinate attributes in `lat`, `lon` order and derive component track names from the resolved feature name.
4. Do not export polygon holes because GPX has no standard polygon-interior representation.

### PGON

1. Write UTF-8 GSAK PGON files with exactly `# GsakName=<name>`, vertex lines, a repeated first vertex, and a terminal line containing exactly `#`.
2. Write each vertex as `latitude longitude`, retaining normal Java double precision.
3. Generate a separate, safely sanitized `.pgon` filename for every Polygon component by default.

## 7. Feature-name output filenames (NAM-1)

1. Add a centralized output-filename resolver that combines the supplied output stem, the resolved feature name, and the original extension as `<stem>-<sanitized-name>.<extension>`.
2. Use that resolver for every generated output item, while retaining the original resolved name without filename sanitization in GeoJSON properties, GPX track names, and PGON `GsakName` content.
3. Adapt format writing so that formats with multiple output items create a separate named file for each item, including every Polygon component.
4. Reject filename collisions deterministically with a clear error rather than silently overwriting an earlier output item.

## 8. Separate output filename field (NAM-2)

1. Extend argument parsing with `--file-name-field FIELD` and validate the configured field against the Shapefile schema.
2. Resolve the feature-content name and filename value independently: preserve `--name-field` for output content, use the configured filename value only when `--file-name-field` is supplied, and otherwise reuse the resolved feature name.
3. Apply filename-field values to all generated GeoJSON, GPX, and PGON paths, including MultiPolygon component suffixes, while retaining feature names in GPX and PGON content.
4. Add regression tests for a distinct filename attribute, unchanged output content, and a missing filename field; document the option and its fallback behavior.

## 9. Name handling and errors

1. Centralize name resolution for GPX and PGON.
2. Require that a configured `--name-field` exists; otherwise terminate with a clear error.
3. If no field is configured, use the first suitable string attribute, then a generated fallback such as `polygon-1`.
4. Preserve Czech characters, including names such as `Bělá pod Bezdězem`, in output content while sanitizing only generated filenames.
5. Provide clear non-zero-exit failures for missing files/components, invalid Shapefiles, CRS issues, invalid arguments, missing name fields, unsupported geometries, and output write failures.

## 10. Feature metadata report (MET-1 to MET-5)

1. Add a metadata-reporting path that iterates the same GeoTools features without invoking normal output writers and reports every non-geometric attribute name and value.
2. Transform each Polygon and every MultiPolygon component to WGS 84 before deriving its northernmost, easternmost, southernmost, and westernmost coordinates.
3. Format standard-output coordinates as geocaching latitude/longitude strings with hemisphere prefixes, fixed degree widths, three decimal minute places, and exactly one separator space; emit every attribute and extrema datum on a separate line.
4. When `--metadata` receives a file path, validate the `.csv` extension and write one `windows-1250` CSV file with a header and exactly one row per feature by default (MET-3).
5. Build CSV columns from all attribute names, then append separate signed-decimal-degree latitude and longitude columns for the north, east, south, and west extrema of every Polygon component in deterministic component order.
6. Define CSV escaping for attribute values, use semicolons as the default field separator, and allow an alternative separator through `--csv-separator` (MET-2).
7. Parse `--csv-charset` with `Charset.forName`, use `windows-1250` when it is absent, and return a clear error for an invalid or unsupported charset identifier (MET-3).
8. Compute attribute-label and coordinate-label widths independently for standard-output metadata, then pad after each colon so values form two separate aligned columns (MET-4).
9. Emit exactly one blank line between standard-output feature reports, without changing CSV output (MET-5).
10. Create no GeoJSON/GPX/PGON output in metadata mode, and return clear errors for invalid metadata arguments or report-file write failures.

## 11. Tests

1. Create small Shapefile fixtures programmatically under tests, avoiding external-data dependencies.
2. Cover Shapefile reading, valid GeoJSON, DBF attribute preservation, UTF-8 Czech text, Polygon and MultiPolygon behavior, valid GPX 1.1, and exact PGON text structure.
3. Verify `EPSG:5514` to `EPSG:4326` reprojection and all coordinate-order conventions: GeoJSON longitude/latitude; GPX and PGON latitude/longitude.
4. Cover invalid input and a missing configured name field.
5. Verify PRG-1 path output, PRG-2 reporting at 250-feature intervals with a correct remaining count, PRG-3 completion output, and PRG-4 suppression through both quiet-option spellings.
6. Verify PRG-5 creates a missing output directory through both force-option spellings, reports the created directory by default, and suppresses that report in quiet mode.
7. Verify PRG-6 reports the output file and configured option values, while quiet mode suppresses them.
7. Verify ENC-1 with a DBF fixture encoded in a non-UTF-8 Czech code page and a matching `.cpg` file, plus UTF-8 fallback when `.cpg` is absent and clear failures for invalid or unsupported declarations.
8. Verify NAM-1 for each output format: generated names use the supplied output stem plus the sanitized resolved feature name, preserve Czech text in file content, create distinct files for multiple items, and report filename collisions clearly.
9. Verify CRS-1 using the exact ESRI `S-JTSK_Krovak_East_North` WKT and a representative source coordinate, asserting that transformed longitude and latitude are in the expected Czech geographic area.
10. Verify MET-1 standard output includes all attribute names and values plus correctly formatted north/east/south/west geocaching coordinates; verify CSV extension validation, one row per feature, attribute escaping, signed latitude/longitude columns, and repeated extrema column groups for MultiPolygon components.
11. Verify MET-2 uses semicolons by default and the configured separator when `--csv-separator` is supplied.
12. Verify MET-3 produces Windows-1250 CSV by default, accepts a valid `--csv-charset` identifier, and rejects an unsupported charset.
13. Verify MET-4 aligns ordinary attributes and coordinate entries as independent standard-output columns, and MET-5 inserts one blank line between feature reports only.
14. Verify NAM-2 uses the configured filename-field value for generated paths without changing the feature name written to output content, and fails for a missing configured field.

## 12. Documentation and verification

1. Add `README.md` with build/run steps, supported formats and options, CRS rules, coordinate-order differences, and Polygon/MultiPolygon representation.
2. Include EPSG:5514-to-4326 and Czech-name examples.
3. Run `mvn test` and `mvn package`.
4. Smoke-test the generated executable JAR for GeoJSON, GPX, and PGON output, then fix build, test, and runtime failures.
5. Document the default processing-status output, `--quiet` / `-q`, and `--force` / `-f` behavior in `README.md`.
6. Document ENC-1: `.cpg` controls DBF input decoding when supplied, while generated output remains UTF-8.
7. Document NAM-1 with the `obec.EXT` to `obec-NAZEV.EXT` naming example and the behavior for multiple output items.
8. Document MET-1 invocation, standard-output geocaching coordinate format, and CSV column behavior.
9. Document the MET-2 default semicolon separator and `--csv-separator`, the MET-3 default `windows-1250` encoding and `--csv-charset`, and the MET-4/MET-5 standard-output formatting rules.
10. Document NAM-2 and its distinction between output-content names and filename values.
