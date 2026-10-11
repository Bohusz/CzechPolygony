# map-generator

`map-generator` creates PNG or basic SVG maps from GSAK `.pgon` polygon files.

```text
java -jar map-generator-1.1-SNAPSHOT.jar INPUT_FOLDER OUTPUT.png [options]
```

All `.pgon` files are discovered recursively by default and are parsed as UTF-8. Output dimensions default to `1600x1000`; use `--width`, `--height`, `--projection auto|5514|laea|3857`, `--fill`, `--border`, and `--background transparent|#rrggbb` to customize output. The output format is selected by the `.png` or `.svg` extension. Invalid input, an empty folder, unsupported options, and malformed polygons return status `1` with a diagnostic.

The GSAK format uses latitude followed by longitude, requires a `# GsakName=...` header, and requires explicit closure by repeating the first coordinate.
