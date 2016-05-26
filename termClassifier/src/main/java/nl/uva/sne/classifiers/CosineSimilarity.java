/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.classifiers;

import java.util.Enumeration;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.neighboursearch.PerformanceStats;

/**
 *
 * @author S. Koulouzis
 */
class CosineSimilarity extends NormalizableDistance implements DistanceFunction {

    public CosineSimilarity(Instances data) {
        super(data);
    }

    public CosineSimilarity() {
        super();
    }

    @Override
    public void setInstances(Instances i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Instances getInstances() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAttributeIndices(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getAttributeIndices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setInvertSelection(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getInvertSelection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double distance(Instance instnc, Instance instnc1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double distance(Instance instnc, Instance instnc1, double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double distance(Instance instnc, Instance instnc1, double d, PerformanceStats ps) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void postProcessDistances(double[] doubles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Instance instnc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clean() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<Option> listOptions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setOptions(String[] strings) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] getOptions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String globalInfo() {
        return "Cosine similarity is a measure of similarity between two vectors "
                + "of an inner product space that measures the cosine of the angle "
                + "between them. The cosine of 0° is 1, and it is less than 1 for "
                + "any other angle. It is thus a judgment of orientation and not "
                + "magnitude: two vectors with the same orientation have a cosine "
                + "similarity of 1, two vectors at 90° have a similarity of 0, "
                + "and two vectors diametrically opposed have a similarity of -1, "
                + "independent of their magnitude. Cosine similarity is "
                + "particularly used in positive space, where the outcome is "
                + "neatly bounded in [0,1]. (https://en.wikipedia.org/wiki/Cosine_similarity)";
    }

    @Override
    protected double updateDistance(double d, double d1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getRevision() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
