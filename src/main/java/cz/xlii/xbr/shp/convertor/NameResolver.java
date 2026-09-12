package cz.xlii.xbr.shp.convertor;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.locationtech.jts.geom.Geometry;

public final class NameResolver {
    private final String configuredField;

    public NameResolver(String configuredField, SimpleFeatureType schema) {
        this.configuredField = configuredField;
        if (configuredField != null && schema.getDescriptor(configuredField) == null) {
            throw new ConversionException("Configured name field does not exist: " + configuredField);
        }
    }

    public String resolve(SimpleFeature feature, int fallbackNumber) {
        if (configuredField != null) {
            return valueOrFallback(feature.getAttribute(configuredField), fallbackNumber);
        }
        for (AttributeDescriptor descriptor : feature.getFeatureType().getAttributeDescriptors()) {
            if (Geometry.class.isAssignableFrom(descriptor.getType().getBinding())) continue;
            if (String.class.isAssignableFrom(descriptor.getType().getBinding())) {
                Object value = feature.getAttribute(descriptor.getName());
                if (value != null && !value.toString().isBlank()) return value.toString();
            }
        }
        return "polygon-" + fallbackNumber;
    }

    private String valueOrFallback(Object value, int fallbackNumber) {
        return value == null || value.toString().isBlank() ? "polygon-" + fallbackNumber : value.toString();
    }
}
