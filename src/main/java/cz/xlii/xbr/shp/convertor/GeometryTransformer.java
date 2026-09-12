package cz.xlii.xbr.shp.convertor;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

public final class GeometryTransformer {
    private final MathTransform transform;

    private GeometryTransformer(MathTransform transform) {
        this.transform = transform;
    }

    public static GeometryTransformer between(CoordinateReferenceSystem source, String targetCode) {
        if (source == null) {
            throw new ConversionException("Input Shapefile has no readable CRS (.prj); reprojection is required for this output.");
        }
        try {
            CoordinateReferenceSystem longitudeFirstSource = normalizedSource(source);
            CoordinateReferenceSystem target = CRS.decode(targetCode, true);
            return new GeometryTransformer(CRS.findMathTransform(longitudeFirstSource, target, true));
        } catch (Exception e) {
            throw new ConversionException("Cannot transform input CRS to " + targetCode + ".", e);
        }
    }

    private static CoordinateReferenceSystem normalizedSource(CoordinateReferenceSystem source) throws Exception {
        if ("S-JTSK_Krovak_East_North".equalsIgnoreCase(source.getName().getCode())) {
            return CRS.decode("EPSG:5514", true);
        }
        Integer sourceCode = CRS.lookupEpsgCode(source, true);
        return sourceCode == null ? source : CRS.decode("EPSG:" + sourceCode, true);
    }

    public Geometry transform(Geometry geometry) {
        try {
            return JTS.transform(geometry, transform);
        } catch (Exception e) {
            throw new ConversionException("Cannot transform geometry.", e);
        }
    }
}
