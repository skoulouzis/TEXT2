/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import nl.uva.sne.commons.ClusterUtils;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.synthetic.ByUrlClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class Carrot implements Clusterer {

    @Override
    public void configure(Properties properties) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, String> cluster(String inDir) throws IOException, ParseException {
        List<org.carrot2.core.Document> documents = ClusterUtils.terms2Documets(inDir);

        final Controller controller = ControllerFactory.createSimple();
        /*
             * Perform clustering by topic using the Lingo algorithm. Lingo can 
             * take advantage of the original query, so we provide it along with the documents.
         */
        final ProcessingResult byTopicClusters = controller.process(documents, "skill",
                LingoClusteringAlgorithm.class);
       
        final List<Cluster> clustersByTopic = byTopicClusters.getClusters();

        /* Perform clustering by domain. In this case query is not useful, hence it is null. */
//        final ProcessingResult byDomainClusters = controller.process(documents, null,
//                ByUrlClusteringAlgorithm.class);
//        final List<Cluster> clustersByDomain = byDomainClusters.getClusters();
        Map<String, String> clusters = new HashMap<>();
        for (Cluster c : clustersByTopic) {
            String theClass = c.getLabel();
            List<Document> cDocs = c.getAllDocuments();
            for(Document doc : cDocs){
                clusters.put(inDir + File.separator + doc.getStringId(), theClass);
            }
//            System.err.println(c.getLabel());
//             
        }

        return clusters;
    }

}
