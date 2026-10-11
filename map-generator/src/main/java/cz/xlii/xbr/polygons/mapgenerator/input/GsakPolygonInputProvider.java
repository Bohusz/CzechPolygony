package cz.xlii.xbr.polygons.mapgenerator.input;

import cz.xlii.xbr.polygons.mapgenerator.model.PolygonData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class GsakPolygonInputProvider implements PolygonInputProvider {
    private final GsakPolygonParser parser = new GsakPolygonParser();
    private final Charset charset;

    public GsakPolygonInputProvider() {
        this(Charset.forName("windows-1250"));
    }

    public GsakPolygonInputProvider(Charset charset) {
        this.charset = charset;
    }
    @Override public List<PolygonData> read(Path folder, boolean recursive) throws IOException {
        if (!Files.isDirectory(folder)) throw new IOException("Input folder does not exist: " + folder);
        var stream = recursive ? Files.walk(folder) : Files.list(folder);
        try (stream) {
            var paths = stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pgon")).sorted(Comparator.comparing(p -> p.toString().toLowerCase())).toList();
            if (paths.isEmpty()) throw new IOException("No .pgon files found in: " + folder);
            return paths.stream().map(p -> { try { return parser.parse(p, charset); } catch (IOException e) { throw new RuntimeException(e); } }).toList();
        } catch (RuntimeException e) { if (e.getCause() instanceof IOException io) throw io; throw e; }
    }
}
