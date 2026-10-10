# GSAK Polygon File Format

- The file name extension is `.pgon`.
- The file is a simple text based format.

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
It's not really mandatory, so we won't require it when reading,
but we output it when writing,
to mitigate a possible problem with tools
that ignore the last line of a text file not terminated with a line separator.

For example:

```
# GsakName=Brno
49.20 16.60
49.21 16.61
49.21 16.62
49.20 16.60
#
```
