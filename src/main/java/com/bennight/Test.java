package com.bennight;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;
import org.geotools.geometry.jts.JTSFactoryFinder;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;


public class Test {

    public static final String COASTLINE_SHAPE_FILE = "./target/coastline/ne_10m_coastline.shp";
    private static final String COASTLINE_DIR = "./target/coastline/";
    private static final int NUM_STEPS = 2000;
    private static final Random RND = new Random(8675309);

    public static void main( String[] args ) throws Exception {
        try {
            extractTestData();
        }
        catch (ZipException e) {
            System.out.println("Error extracting test data, error was: " + e.getLocalizedMessage());
            System.exit(1);
        }


        Map<Color, List<Polygon>> styledFeats = new HashMap<>();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        List<Polygon> gPolys = new ArrayList<>();

        Polygon p = geometryFactory.createPolygon(new Coordinate[] {new Coordinate(-50,40), new Coordinate(-50,45), new Coordinate(75,50), new Coordinate(75,40), new Coordinate(-50,40)});


        gPolys.add(createPolygon(6, -90, -35));

        gPolys.add(createPolygon(4, 90, -45));
        gPolys.add(p);




        styledFeats.put(Color.RED, gPolys);
        MapRenderer.drawMap("cartesian_poly", styledFeats, 2000, "cartesian_poly.png");
        styledFeats.clear();

        List<Polygon> poly2 = geodesifyPolygon(gPolys);

        styledFeats.put(Color.BLUE, poly2);
        MapRenderer.drawMap("geodesic_poly", styledFeats, 2000, "geodesic_poly.png");
        styledFeats.clear();

        styledFeats.put(Color.RED, gPolys);
        styledFeats.put(Color.BLUE, poly2);
        MapRenderer.drawMap("both", styledFeats, 2000, "both.png");

    }

    private static List<Polygon> geodesifyPolygon(List<Polygon> polygons){
        List<Polygon> geodesifiedPolygons = new ArrayList<>();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        for (Polygon p : polygons) {
            List<Coordinate> coords = new ArrayList<>();
            Coordinate[] oldCords = p.getCoordinates();

            for (int i = 0; i < oldCords.length - 1; i++) {
                GeodesicData gd = Geodesic.WGS84.Inverse(oldCords[i].y, oldCords[i].x, oldCords[i + 1].y, oldCords[i + 1].x);
                GeodesicLine gl = Geodesic.WGS84.Line(gd.lat1, gd.lon1, gd.azi1);
                for (int j = 0; j < NUM_STEPS + 1; j++) {
                    GeodesicData gdp = gl.Position(j * gd.s12 / NUM_STEPS);
                    coords.add(new Coordinate(gdp.lon2, gdp.lat2));
                }
            }
            coords.set(coords.size() - 1, coords.get(0));
            geodesifiedPolygons.add(geometryFactory.createPolygon(coords.toArray(new Coordinate[coords.size()])));
        }
        return geodesifiedPolygons;
    }


    private static void extractTestData()
            throws ZipException {
        File f2 = new File(COASTLINE_SHAPE_FILE);
        if (!f2.exists()){
            f2.mkdirs();
            ZipFile zf = new ZipFile(Test.class.getClassLoader().getResource("ne_10m_coastline.zip").getFile());
            zf.extractAll(COASTLINE_DIR);
        }
    }


    private static Polygon createPolygon(
            final int numPoints , double centerX, double centerY) {

        final int maxRadius = 60;
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

        final List<Coordinate> coords = new ArrayList<Coordinate>();

        final double increment = (double) 360 / numPoints;

        for (double theta = 0; theta <= 360; theta += increment) {
            final double radius = (RND.nextDouble() * maxRadius) + 0.1;
            final double rad = (theta * Math.PI) / 180.0;
            final double x = centerX + (radius * Math.sin(rad));
            final double y = centerY + (radius * Math.cos(rad));
            coords.add(new Coordinate(
                    x,
                    y));
        }
        coords.add(coords.get(0));
        return geometryFactory.createPolygon(coords.toArray(new Coordinate[coords.size()]));
    }



}
