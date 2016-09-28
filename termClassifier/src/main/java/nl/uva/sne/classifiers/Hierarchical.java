/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.awt.Container;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import nl.uva.sne.commons.ClusterUtils;
import org.json.simple.parser.ParseException;

import weka.clusterers.HierarchicalClusterer;
import weka.core.ChebyshevDistance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.MinkowskiDistance;
import weka.core.converters.ArffSaver;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.hierarchyvisualizer.HierarchyVisualizer;

/**
 *
 * @author S. Koulouzis
 */
public class Hierarchical implements Clusterer {

    private int numOfClusters;
    private String distanceFunction;

    @Override
    public void configure(Properties properties) {
        numOfClusters = Integer.valueOf(properties.getProperty("kmeans.num.of.clusters", "6"));
        distanceFunction = properties.getProperty("distance.function", "Euclidean");
        Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "kmeans.num.of.clusters: {0}", numOfClusters);
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        try {

            Instances data = ClusterUtils.terms2Instances(inDir, false);

//            ArffSaver s = new ArffSaver();
//            s.setInstances(data);
//            s.setFile(new File(inDir+"/dataset.arff"));
//            s.writeBatch();

            DistanceFunction df;
//            SimpleKMeans currently only supports the Euclidean and Manhattan distances.
            switch (distanceFunction) {
                case "Minkowski":
                    df = new MinkowskiDistance(data);
                    break;
                case "Euclidean":
                    df = new EuclideanDistance(data);
                    break;
                case "Chebyshev":
                    df = new ChebyshevDistance(data);
                    break;
                case "Manhattan":
                    df = new ManhattanDistance(data);
                    break;
                default:
                    df = new EuclideanDistance(data);
                    break;
            }

            Logger.getLogger(Hierarchical.class.getName()).log(Level.INFO, "Start clusteing");

            weka.clusterers.HierarchicalClusterer clusterer = new HierarchicalClusterer();
            clusterer.setOptions(new String[]{"-L", "COMPLETE"});
            clusterer.setDebug(true);
            clusterer.setNumClusters(numOfClusters);
            clusterer.setDistanceFunction(df);
            clusterer.setDistanceIsBranchLength(true);
            clusterer.setPrintNewick(false);

            weka.clusterers.FilteredClusterer fc = new weka.clusterers.FilteredClusterer();
            String[] options = new String[2];
            options[0] = "-R"; // "range"
            options[1] = "1"; // we want to ignore the attribute that is in the position '1'
            Remove remove = new Remove(); // new instance of filter
            remove.setOptions(options); // set options

            fc.setFilter(remove); //add filter to remove attributes
            fc.setClusterer(clusterer); //bind FilteredClusterer to original clusterer
            fc.buildClusterer(data);

//             // Print normal
//        clusterer.setPrintNewick(false);
//        System.out.println(clusterer.graph());
//        // Print Newick
//        clusterer.setPrintNewick(true);
//        System.out.println(clusterer.graph());
//
//        // Let's try to show this clustered data!
//        JFrame mainFrame = new JFrame("Weka Test");
//        mainFrame.setSize(600, 400);
//        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        Container content = mainFrame.getContentPane();
//        content.setLayout(new GridLayout(1, 1));
//
//        HierarchyVisualizer visualizer = new HierarchyVisualizer(clusterer.graph());
//        content.add(visualizer);
//
//        mainFrame.setVisible(true);
            return ClusterUtils.bulidClusters(clusterer, data, inDir);

        } catch (Exception ex) {
            Logger.getLogger(Hierarchical.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
