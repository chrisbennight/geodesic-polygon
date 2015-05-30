package com.bennight;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.*;
import org.geotools.styling.Stroke;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;

import javax.imageio.ImageIO;

/**
 * Created by bennight on 5/29/2015.
 */
public class MapRenderer {



    public static void drawMap(String title, Map<Color, List<Polygon>> polygons, int imageWidth, String outputFile) throws Exception {

        File f = new File(Test.COASTLINE_SHAPE_FILE);
        ShapefileDataStore shapefile = new ShapefileDataStore(f.toURI().toURL());
        MapContent map = new MapContent();
        map.setTitle(title);
        StyleBuilder styleBuilder = new StyleBuilder();

        LineSymbolizer lsymbol = styleBuilder.createLineSymbolizer(Color.BLACK, 1);
        Style lineStyle = styleBuilder.createStyle(lsymbol);
        FeatureLayer layer = new FeatureLayer(shapefile.getFeatureSource(), lineStyle);
        map.addLayer(layer);


        StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
        FilterFactory ff = CommonFactoryFinder.getFilterFactory2();
        SimpleFeatureSource fs = shapefile.getFeatureSource();
        Map<Color, SimpleFeatureType> types = new HashMap<Color, SimpleFeatureType>();
        Map<Color, ListFeatureCollection> cols = new HashMap<>();
        for (Map.Entry<Color, List<Polygon>> kvp : polygons.entrySet()){
            types.put(kvp.getKey(), DataUtilities.createType("Polygon-" + kvp.getKey().toString(), "the_geom:Polygon,name:String"));
            ListFeatureCollection col = new ListFeatureCollection(types.get(kvp.getKey()));
            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(types.get(kvp.getKey()));
            int i = 0;
            for (Polygon p : kvp.getValue()){
                sfb.set("the_geom", p);
                sfb.set("name", " ");
                col.add(sfb.buildFeature(String.valueOf(i)));
                i++;
            }
            cols.put(kvp.getKey(), col);
        }
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);





        Map<ListFeatureCollection, Style> styledSets = new HashMap<>();

        for (Map.Entry<Color, ListFeatureCollection> kvp : cols.entrySet()){
            Fill fill2 = sf.createFill(null);

            Stroke stroke2 = sf.createStroke(
                    ff.literal(kvp.getKey()),
                    ff.literal(2)
            );

            Mark mark = sf.getSquareMark();
            mark.setFill(fill2);
            mark.setStroke(stroke2);

            Graphic graphic = sf.createDefaultGraphic();
            graphic.graphicalSymbols().clear();
            graphic.graphicalSymbols().add(mark);

            graphic.setSize(ff.literal(2));

            GeometryDescriptor geomDesc = fs.getSchema().getGeometryDescriptor();
            String geometryAttributeName = geomDesc.getLocalName();
            //PointSymbolizer sym2 = sf.createPointSymbolizer(graphic, geometryAttributeName);

            PolygonSymbolizer sym2 = sf.createPolygonSymbolizer(stroke2, null, geometryAttributeName);

            Rule rule2 = sf.createRule();
            rule2.symbolizers().add(sym2);
            Rule rules2[] = {rule2};
            FeatureTypeStyle fts2 = sf.createFeatureTypeStyle(rules2);
            Style style2 = sf.createStyle();
            style2.featureTypeStyles().add(fts2);

            map.addLayer(new FeatureLayer(kvp.getValue(), style2));
        }


        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);
        Rectangle imageBounds = null;
        ReferencedEnvelope mapBounds = null;
        try {
            mapBounds = map.getMaxBounds();
            double heightToWidth = mapBounds.getSpan(1) / mapBounds.getSpan(0);
            imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * heightToWidth));

        } catch (Exception e) {
            // failed to access map layers
            throw new RuntimeException(e);
        }

        BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

        Graphics2D gr = image.createGraphics();
        gr.setPaint(Color.WHITE);
        gr.fill(imageBounds);

        try {
            renderer.paint(gr, imageBounds, mapBounds);
            File fileToSave = new File(outputFile);
            if (fileToSave.exists()){
                fileToSave.delete();
            }
            ImageIO.write(image, "png", fileToSave);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            map.dispose();
        }


    }
}
