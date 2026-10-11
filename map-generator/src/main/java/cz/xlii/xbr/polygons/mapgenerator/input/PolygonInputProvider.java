package cz.xlii.xbr.polygons.mapgenerator.input;

import cz.xlii.xbr.polygons.mapgenerator.model.PolygonData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PolygonInputProvider { List<PolygonData> read(Path folder, boolean recursive) throws IOException; }
