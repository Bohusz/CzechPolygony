# map-generator

`map-generator` creates PNG or basic SVG maps from GSAK `.pgon` polygon files.

```text
java -jar map-generator-1.1-SNAPSHOT.jar INPUT_FOLDER OUTPUT.png [options]
```

All text input files use Windows code page 1250 by default, matching the GSAK exports. Use `--encoding UTF-8` (or another Java charset name) to override the default; UTF-8/UTF-16 BOMs take precedence automatically. Format-specific declarations, such as an XML encoding declaration, should likewise take precedence when such formats are added. Output dimensions default to `1600x1000`; use `--width`, `--height`, `--projection auto|5514|laea|3857`, `--fill`, `--border`, `--background transparent|#rrggbb`, and `--territory-colors` to customize output. The output format is selected by the `.png` or `.svg` extension. Invalid input, an empty folder, unsupported options, and malformed polygons return status `1` with a diagnostic.

The `--territory-colors` file contains one `territory-id=color` entry per line. Colors use CSS hex notation with six or eight hexadecimal digits, for example `CZ=#ff0000` or `SK=#00ff0080`. Blank lines and lines beginning with `#` are ignored. Territories not listed in the file use the global `--fill` color.

The GSAK format uses latitude followed by longitude, requires a `# GsakName=...` header, and requires explicit closure by repeating the first coordinate.

Use `--verbose` to print all resolved parameters, including input and output paths, and the processing completion time. `--debug` includes verbose output and additionally reports every text file read with its line count, plus each polygon's vertex count, identifiers, name, and territory-color mapping result.
