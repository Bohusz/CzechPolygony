package cz.xlii.xbr.shp.convertor;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

final class OutputFilenameResolver {
    private final Path output;
    private final String stem;
    private final String extension;
    private final Set<Path> targets = new HashSet<>();

    OutputFilenameResolver(Path output) {
        this.output = output;
        String filename = output.getFileName().toString();
        int extensionStart = filename.lastIndexOf('.');
        stem = filename.substring(0, extensionStart);
        extension = filename.substring(extensionStart);
    }

    Path resolve(String name) {
        String safeName = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim().replaceAll("\\s+", " ");
        if (safeName.isBlank()) safeName = "polygon";
        Path target = output.resolveSibling(stem + "-" + safeName + extension).toAbsolutePath().normalize();
        if (!targets.add(target)) {
            throw new ConversionException("Output filename collision: " + target);
        }
        return target;
    }
}
