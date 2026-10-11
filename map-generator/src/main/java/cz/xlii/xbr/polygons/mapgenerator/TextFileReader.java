package cz.xlii.xbr.polygons.mapgenerator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public final class TextFileReader {
    private TextFileReader() {
    }

    public static List<String> readLines(Path path, Charset defaultCharset) throws IOException {
        return readLines(path, defaultCharset, null);
    }

    public static List<String> readLines(Path path, Charset defaultCharset, BiConsumer<Path, Integer> readListener) throws IOException {
        var bytes = Files.readAllBytes(path);
        var offset = 0;
        var charset = defaultCharset;
        if (startsWith(bytes, new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF})) {
            charset = StandardCharsets.UTF_8;
            offset = 3;
        } else if (startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xFE})) {
            charset = StandardCharsets.UTF_16LE;
            offset = 2;
        } else if (startsWith(bytes, new byte[]{(byte) 0xFE, (byte) 0xFF})) {
            charset = StandardCharsets.UTF_16BE;
            offset = 2;
        }
        var content = offset == 0 ? bytes : Arrays.copyOfRange(bytes, offset, bytes.length);
        var lines = new String(content, charset).lines().toList();
        if (readListener != null) readListener.accept(path, lines.size());
        return lines;
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && ByteBuffer.wrap(value, 0, prefix.length).equals(ByteBuffer.wrap(prefix));
    }
}