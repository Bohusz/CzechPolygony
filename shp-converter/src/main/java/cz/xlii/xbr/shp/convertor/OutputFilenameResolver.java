package cz.xlii.xbr.shp.convertor;

import org.geotools.api.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OutputFilenameResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^{}]+)}}");

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

    Path resolvePgon(String name, SimpleFeature feature, Integer componentNumber) {
        if (!PLACEHOLDER.matcher(output.getFileName().toString()).find()) {
            return resolve(name);
        }

        String filename = output.getFileName().toString();
        Matcher matcher = PLACEHOLDER.matcher(filename);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            if (feature.getFeatureType().getDescriptor(fieldName) == null) {
                throw new ConversionException("Output filename field does not exist: " + fieldName);
            }
            Object value = feature.getAttribute(fieldName);
            String replacement = value == null ? "" : sanitize(value.toString());
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        if (componentNumber != null) {
            int extensionStart = resolved.lastIndexOf(".");
            resolved.insert(extensionStart, " - " + componentNumber);
        }

        Path target = output.resolveSibling(resolved.toString()).toAbsolutePath().normalize();
        if (!targets.add(target)) {
            throw new ConversionException("Output filename collision: " + target);
        }
        return target;
    }

    private String sanitize(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim().replaceAll("\\s+", " ");
    }
}
