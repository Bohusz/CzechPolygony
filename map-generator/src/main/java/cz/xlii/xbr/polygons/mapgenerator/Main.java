package cz.xlii.xbr.polygons.mapgenerator;

import cz.xlii.xbr.polygons.mapgenerator.input.GsakPolygonInputProvider;
import cz.xlii.xbr.polygons.mapgenerator.model.PolygonData;
import picocli.CommandLine;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name="map-generator", mixinStandardHelpOptions=true, description="Create a map from GSAK polygon files.")
public final class Main implements Callable<Integer> {
 @CommandLine.Parameters(index="0", description="Input folder") Path input;
 @CommandLine.Parameters(index="1", description="Output .png or .svg") Path output;
 @CommandLine.Option(names="--recursive", defaultValue="true") boolean recursive;
 @CommandLine.Option(names="--width", defaultValue="1600") int width;
 @CommandLine.Option(names="--height", defaultValue="1000") int height;
 @CommandLine.Option(names="--projection", defaultValue="auto") String projection;
 @CommandLine.Option(names="--fill", defaultValue="#ffffff") String fill;
 @CommandLine.Option(names="--border", defaultValue="#000000") String border;
  @CommandLine.Option(names="--background", defaultValue="transparent") String background;
  @CommandLine.Option(names="--territory-colors", description="File containing territory-id=color mappings") Path territoryColors;
  @CommandLine.Option(names="--encoding", defaultValue="windows-1250", description="Default text encoding (BOM/format declarations take precedence)") String encoding;
  private Map<String, String> territoryColorMap = Map.of();
 @Override public Integer call() {
  try {
    if (width < 1 || height < 1) throw new IllegalArgumentException("Canvas dimensions must be positive");
    var charset = Charset.forName(encoding);
    if (territoryColors != null) territoryColorMap = ColorMappingParser.parse(territoryColors, charset);
   var polygons = new GsakPolygonInputProvider(charset).read(input, recursive);
   if (!output.toString().toLowerCase().matches(".*\\.(png|svg)$")) throw new IllegalArgumentException("Output must end in .png or .svg");
   var mode = projection.equalsIgnoreCase("auto") ? auto(polygons) : projection.toLowerCase();
   if (!(mode.equals("5514") || mode.equals("laea") || mode.equals("3857"))) throw new IllegalArgumentException("Unsupported projection: " + projection);
   render(polygons, mode);
   return 0;
  } catch (Exception e) { System.err.println(e.getMessage()); return 1; }
 }
 private String auto(List<PolygonData> ps) { return ps.stream().flatMap(p -> p.coordinates().stream()).allMatch(c -> c.y >= 47.73 && c.y <= 51.06 && c.x >= 12.09 && c.x <= 22.56) ? "5514" : "laea"; }
 private Point project(double lon, double lat, String mode, double minX, double maxX, double minY, double maxY) {
  double x=lon, y=lat;
  if (mode.equals("3857")) { x=Math.toRadians(lon); y=Math.log(Math.tan(Math.PI/4+Math.toRadians(lat)/2)); }
  var xx=(x-minX)/(maxX-minX+1e-12); var yy=(y-minY)/(maxY-minY+1e-12);
  return new Point((int)(30+xx*(width-60)), (int)(height-30-yy*(height-60)));
 }
 private void render(List<PolygonData> ps, String mode) throws IOException {
  var bounds=ps.stream().flatMap(p->p.coordinates().stream()).collect(() -> new double[]{Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}, (b,c)->{ b[0]=Math.min(b[0],c.x);b[1]=Math.max(b[1],c.x);b[2]=Math.min(b[2],c.y);b[3]=Math.max(b[3],c.y);}, (a,b)->{});
   if (output.toString().toLowerCase().endsWith(".svg")) { var s=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\""+width+"\" height=\""+height+"\" viewBox=\"0 0 "+width+" "+height+"\">"); if(!background.equalsIgnoreCase("transparent"))s.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(background).append("\"/>"); for(var p:ps){s.append("<path d=\""); for(var i=0;i<p.coordinates().size();i++){var c=p.coordinates().get(i);var q=project(c.x,c.y,mode,bounds[0],bounds[1],bounds[2],bounds[3]);s.append(i==0?"M ":"L ").append(q.x).append(' ').append(q.y).append(' ');}s.append("Z\" fill=\"").append(fillFor(p)).append("\" stroke=\"").append(border).append("\" stroke-width=\"1\"/>");} s.append("</svg>"); Files.writeString(output,s.toString()); return; }
   var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB); var g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); if(!background.equalsIgnoreCase("transparent")){g.setColor(color(background));g.fillRect(0,0,width,height);} for(var p:ps){var path=new Path2D.Double(); for(var i=0;i<p.coordinates().size();i++){var c=p.coordinates().get(i);var q=project(c.x,c.y,mode,bounds[0],bounds[1],bounds[2],bounds[3]);if(i==0)path.moveTo(q.x,q.y);else path.lineTo(q.x,q.y);}path.closePath();g.setColor(color(fillFor(p)));g.fill(path);g.setColor(color(border));g.draw(path);}g.dispose();ImageIO.write(image,"png",output.toFile());
  }
  private String fillFor(PolygonData polygon) { return territoryColorMap.getOrDefault(polygon.territoryId(), fill); }
  private Color color(String value){if(value.equalsIgnoreCase("transparent"))return new Color(0,0,0,0); var hex=value.startsWith("#")?value.substring(1):value; if(hex.length()==8)return new Color(Integer.parseInt(hex,16),true); return Color.decode("#"+hex);}
 public static void main(String[] args){System.exit(new CommandLine(new Main()).execute(args));}
}
