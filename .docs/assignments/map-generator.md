# The task
Create a CLI tool which creates a map from a set of geographic polygons.

# Input data
Polygons can be specified in various formats,
but we start with a [simple text-file format used by GSAK](./references/GSAK-polygon-file-format.md).
- However, other formats will be added later, such as GPX track files.
  Design the tool in the way so that no major refactoring will be necessary.
- Each polygon can be identified and named.

## Polygons' identification

|                          | description                                                                                                                                                                                                                                                                                    |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **polygon id**           | A unique identification of the polygon.                                                                                                                                                                                                                                                        |
| **territory id**         | Territory is formed by one or more polygons.                                                                                                                                                                                                                                                   |
| (**territory**) **name** | Name of the territory. Names just a display attribute and are not unique, more territories can share the same name. They should be the same for all polygons in a territory but for different reasons they can differ - in such case the shortest prefix is used as the name od the territory. |

### Specification of ids and names

|                   | GSAK polygon file                                                                                        |
|-------------------|----------------------------------------------------------------------------------------------------------|
| **polygon id:**   | The first part of base file name separated from the rest by the first underscore (if there is any).      |
| **territory id:** | The first part of the _polygon id_ separated from the rest by the first dash or space (if there is any). |
| **name:**         | GsakName "comment".                                                                                      |

## About the accuracy of the input data and their rendering
The input polygons will usually share the most segments of its borders with other input polygons.
But it's possible that they won't match perfectly.
- The points can be specified in different orders in different polygons.
- The positions of the "same" points may differ by several meters.
- The segment or its part may be specified by different points.

It's not necessary to completely normalize the borders, but to make the borders visually as thin as possible,
i.e., not display two or more pixels just because of such tiny mismatches.

No outside knowledge is required. All data including identification of the polygons will be provided as input,
either in the polygon files or as the tool parameters.

# Output
The output will be an image file in PNG or SVG format.
- It should depict the polygons given as input.
- The polygons will usually cover a territory of a single country or its subdivision, most likely the Czechia.
  So the covered area will usually be a spheric rectangle about 600 by 400 km.
- The inside of the polygons will be filled with a uniform color, white by default,
  and the borders will be drawn with a thin line, typically one pixel wide and black.
  The remaining parts of the image - the "outside" - will be either transparent or white.
  - The tool parameters can override the default colors/transparency of the polygons, border and the "outside".
  - You can specify a list of territory identifiers to be filled with other colors.
- Currently, no texts or names of the polygons will be displayed on the map.

## Map Projection

- If all polygons lie inside from N47.73° to N51.06° and from E012.09° to E022.56°, use the Křovák projection, EPSG:5514,
- otherwise use the Lambert azimuthal equal-area projection.
- Support also the projection used by OpenStreetMap, EPSG:3857.

User can specify any of these projections explicitly using a tool parameter. 

# Implementation
- Create a new maven module for the tool, `map-generator`.
  - Keep separate versioning for the module (tool).
- Implement the tool using Java.
- The main java package will be `cz.xlii.xbr.polygons.mapgenerator`.