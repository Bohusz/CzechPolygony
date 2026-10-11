# Map generator implementation plan

## 1. Scope and agreed decisions

Create a new Java Maven module named `map-generator` with main package
`cz.xlii.xbr.polygons.mapgenerator`.

The first release will:

- recursively discover all `.pgon` files below a user-specified input folder;
- parse the GSAK polygon format, including `GsakName`, latitude/longitude order,
  polygon closure, and polygon-file termination rules;
- derive polygon and territory identifiers from the file name;
- fail fast for an empty input folder, malformed files, invalid coordinates, and
  invalid polygon geometry, with file-specific diagnostics;
- normalize nearby border vertices using a configurable precision tolerance;
- select Křovák (`EPSG:5514`) automatically when every input coordinate lies
  within the assignment's Czechia bounds, otherwise use Lambert azimuthal
  equal-area;
- support explicit Křovák, Lambert azimuthal equal-area, and Web Mercator
  (`EPSG:3857`) projection selection;
- render filled polygons and thin borders to PNG or basic SVG;
- use a 1600×1000 canvas by default, with width and height overrides;
- use transparent outside/background by default, with configurable CSS-hex
  colors for fills, borders, and background;
- support a color mapping file for territory-specific fills; and
- select PNG or SVG from the output filename extension.

Input providers and output renderers must be interfaces so that GPX and other
formats can be added without changing the map model or rendering pipeline.

## 2. Recommended technology

- Java 21, matching the repository parent POM.
- GeoTools 35.1 for CRS definitions, coordinate-reference-system decoding,
  transformations, and JTS integration. The root POM already imports its BOM
  and the OSGeo repository.
- JTS, supplied through GeoTools, for geometry validation, envelopes,
  precision reduction, snapping, and coordinate operations that are not tied
  to file parsing.
- Picocli for typed command-line options, generated help, validation, and
  testable command execution.
- Java2D and `ImageIO` for PNG rendering, avoiding a second raster graphics
  dependency.
- A small dedicated SVG writer using XML escaping and numeric formatting for
  basic `<path>`, fill, stroke, and opacity output. This keeps SVG output
  deterministic and avoids coupling the domain model to a general-purpose
  drawing library.
- JUnit Jupiter for unit and integration tests, following the existing module's
  test setup.

## 3. Proposed module structure

```text
map-generator/
  pom.xml
  README.md
  src/main/java/cz/xlii/xbr/polygons/mapgenerator/
    Main.java
    cli/MapGeneratorCommand.java
    cli/ColorMappingParser.java
    cli/CliConfiguration.java
    input/PolygonInputProvider.java
    input/GsakPolygonInputProvider.java
    input/GsakPolygonParser.java
    model/PolygonData.java
    model/Territory.java
    model/GeoMap.java
    geometry/PolygonValidator.java
    geometry/BorderNormalizer.java
    projection/ProjectionSelector.java
    projection/MapProjection.java
    projection/GeoToolsMapProjection.java
    layout/MapLayout.java
    layout/MapProjector.java
    render/MapRenderer.java
    render/PngMapRenderer.java
    render/SvgMapRenderer.java
    render/RenderStyle.java
  src/test/java/.../
```

The exact class split may be simplified during implementation, but these
boundaries should remain observable in the design:

1. `input` converts external files into a format-neutral model.
2. `model` contains IDs, names, coordinates, and territories without CLI or
   rendering dependencies.
3. `geometry` validates and applies the explicitly limited border
   normalization policy.
4. `projection` transforms geographic coordinates into a planar coordinate
   system and exposes projection metadata.
5. `layout` calculates bounds, scale, and translation for the canvas.
6. `render` consumes projected paths and style data and knows nothing about
   input file naming or territory derivation.
7. `cli` maps user options to the pipeline and translates failures into useful
   exit codes/messages.

## 4. Domain and identification rules

Represent each input polygon with:

- source path;
- polygon ID;
- territory ID;
- display name;
- ordered geographic coordinates; and
- validated geometry.

Derive IDs as follows:

- remove the `.pgon` extension;
- polygon ID is the part before the first underscore, or the complete base name
  when no underscore exists;
- territory ID is the part of the polygon ID before the first dash or space;
- the display name comes from `# GsakName=...`.

If the name differs among polygons with the same territory ID, derive the
territory display name from the shortest common prefix after trimming a clear
separator; retain the original polygon names for diagnostics and future label
support. This rule should be covered by tests because the assignment permits
inconsistent source names.

## 5. CLI contract

Implement a command with options equivalent to:

```text
map-generator <input-folder> <output.png|output.svg>
  [--recursive|--no-recursive]
  [--width <pixels>] [--height <pixels>]
  [--projection auto|5514|laea|3857]
  [--fill <css-hex>] [--border <css-hex>] [--background <css-hex|transparent>]
  [--territory-colors <mapping-file>]
  [--snap-tolerance <degrees-or-defined-unit>]
```

Recursive discovery is the default; retaining a non-recursive switch is useful
for predictable operation on large trees. The output extension must be `.png`
or `.svg`. Reject invalid combinations before reading files. Document the
precise color mapping file syntax in the module README, using one
`territory-id=color` entry per line, blank lines and comments ignored.

The initial defaults are:

- width `1600`, height `1000`;
- projection `auto`;
- polygon fill white;
- border black, one logical pixel;
- transparent background; and
- a documented small snap tolerance, exposed as an option rather than hidden
  in geometry code.

The implementation must settle and document the tolerance's unit. The
recommended choice is to transform first to a suitable metric CRS and apply a
meter-based tolerance there, because “several meters” is the requirement and
degree tolerances vary with latitude.

## 6. Processing pipeline

1. Parse and validate CLI options.
2. Walk the input folder and sort paths deterministically by normalized path.
3. Parse each `.pgon` file as UTF-8, accepting the optional final `#` line but
   requiring a valid `GsakName` header and at least three distinct vertices.
4. Validate latitude/longitude ranges, explicit closure, finite numeric values,
   and polygon validity. Report the source path and line number on failure.
5. Build polygon and territory identity objects.
6. Determine the automatic projection from the complete input coordinate
   extent, using the exact bounds in the assignment; explicit projection wins.
7. Transform coordinates to a metric working CRS where necessary.
8. Apply precision snapping/nearby-vertex normalization with a documented
   tolerance, without attempting broad topology repair or silently changing
   polygon ownership.
9. Compute a combined projected envelope, preserve aspect ratio, and fit it to
   the canvas with a small configurable/default margin. Define y-axis handling
   once so PNG and SVG have identical geographic orientation.
10. Resolve territory styles, draw fills, and draw borders in a deterministic
    order. Prefer one shared border pass after fills to reduce doubled borders.
11. Write the selected output atomically where practical and return a non-zero
    exit status for any failure.

## 7. Projection design

Create a projection abstraction that accepts longitude/latitude coordinates and
returns planar coordinates plus a stable projection identifier.

- `EPSG:5514` is used for the automatic Křovák case and explicit Křovák mode.
- Lambert azimuthal equal-area needs a defined CRS centered on the input
  envelope. The plan should use a dynamically constructed LAEA CRS centered on
  the envelope centroid, with a documented datum/ellipsoid, rather than a
  silently arbitrary fixed center.
- `EPSG:3857` is available explicitly for OpenStreetMap-compatible display.

Use GeoTools CRS APIs for decoding and transformation and add tests for the
automatic threshold boundary, explicit override, coordinate order, and a
representative Czechia extent. Avoid assuming that EPSG:3857 is suitable for
area measurements; it is only an output projection option.

## 8. Rendering design

Convert each projected polygon into a renderer-neutral path model. The model
must preserve polygon holes if the parser or future providers supply them.

PNG rendering will create an ARGB `BufferedImage`, clear it to transparent (or
the selected background), draw fills and borders with anti-aliasing, and write
through `ImageIO`.

SVG rendering will write a UTF-8 XML document with explicit `width`, `height`,
`viewBox`, background handling, and escaped path/style attributes. Numeric
formatting must be locale-independent and deterministic. Basic SVG does not
include labels, metadata, external assets, or JavaScript.

## 9. Verification strategy

### Unit tests

- GSAK header, coordinate order, closure, terminator, whitespace, and EOF
  handling.
- File-name ID derivation for underscores, dashes, spaces, and edge cases.
- Territory-name common-prefix behavior.
- Invalid coordinate, malformed number, missing header, too-small polygon, and
  self-intersecting polygon diagnostics.
- Recursive discovery, deterministic ordering, empty-folder failure, and
  extension filtering.
- CLI defaults, option validation, color mapping parsing, and output extension
  selection.
- Automatic projection boundary and explicit projection override.
- Border snapping with points differing by meters, including a case that must
  remain distinct because it exceeds tolerance.
- Layout aspect-ratio preservation and y-axis orientation.

### Integration tests

- Build a small temporary folder of `.pgon` files and produce a PNG; verify
  dimensions, alpha behavior, and representative pixel colors.
- Produce SVG and verify valid XML, viewBox, path count, fill/stroke values,
  and transparent-background behavior.
- Run the shaded JAR with `--help`, a successful sample, an empty folder, and a
  malformed file; verify exit status and diagnostics.

Use fixed sample fixtures under `src/test/resources` and avoid brittle full
image comparisons. If visual regression coverage becomes necessary later, add
small golden images after the geometry and renderer contracts stabilize.

## 10. Implementation sequence

1. Add `map-generator` to the root reactor and create its POM, package name,
   Java 21 compiler settings, GeoTools/JTS, Picocli, JUnit, and shaded-JAR
   entry point.
2. Implement the format-neutral model and GSAK parser with parser tests.
3. Implement folder discovery, ID/name derivation, and fail-fast diagnostics.
4. Implement projection selection and GeoTools transformations.
5. Implement metric border normalization and geometry validation.
6. Implement layout and renderer-neutral projected paths.
7. Implement PNG and SVG renderers.
8. Implement Picocli wiring, color mapping, output selection, help, and exit
   behavior.
9. Add integration fixtures and end-to-end tests.
10. Add `map-generator/README.md`, update the top-level README module list,
    update shared run configurations only if requested, and verify the reactor
    build and shaded-JAR invocation.

## 11. Open implementation constraints to confirm during coding

- The assignment does not specify the exact default margin, snap tolerance,
  border width unit, LAEA datum/center, or whether an explicitly closed input
  must be rejected when closure is missing. These values should be chosen,
  tested, and documented before implementation is considered complete.
- The parser should initially require the documented GSAK header and at least
  three distinct vertices; compatibility relaxations should be deliberate and
  covered by tests rather than inferred from malformed input.
- No polygon labels are rendered in this version, although names and territory
  IDs remain part of the model for styling and future features.