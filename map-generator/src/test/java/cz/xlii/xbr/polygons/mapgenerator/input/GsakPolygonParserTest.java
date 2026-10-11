package cz.xlii.xbr.polygons.mapgenerator.input;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class GsakPolygonParserTest {
 @Test void parsesLatitudeLongitudeAndIds() throws Exception {
  var path=Files.createTempFile("CZ-1_area", ".pgon");
  Files.writeString(path,"# GsakName=Test\n50 14\n50 15\n51 15\n50 14\n#\n");
  var p=new GsakPolygonParser().parse(path);
  assertEquals("CZ-1",p.polygonId()); assertEquals("CZ",p.territoryId()); assertEquals(14,p.coordinates().getFirst().x); assertEquals(50,p.coordinates().getFirst().y);
  Files.deleteIfExists(path);
 }
 @Test void rejectsUnclosedPolygon() throws Exception {
  var path=Files.createTempFile("bad", ".pgon"); Files.writeString(path,"# GsakName=X\n50 14\n50 15\n51 15\n");
  assertThrows(IllegalArgumentException.class,()->new GsakPolygonParser().parse(path)); Files.deleteIfExists(path);
 }
}
