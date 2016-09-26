/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.disambiguation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import nl.uva.sne.commons.Term;
import org._3pq.jgrapht.edge.DirectedWeightedEdge;
import org.json.simple.parser.ParseException;
import org.semanticweb.skos.AddAssertion;
import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSAssertion;
import org.semanticweb.skos.SKOSChange;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataFactory;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSEntity;
import org.semanticweb.skos.SKOSEntityAssertion;
import org.semanticweb.skos.SKOSLiteral;
import org.semanticweb.skos.SKOSObjectRelationAssertion;
import org.semanticweb.skos.SKOSUntypedLiteral;
import org.semanticweb.skosapibinding.SKOSManager;

/**
 *
 * @author S. Koulouzis
 */
public class SkosUtils {

    public static final String SKOS_URI = "http://www.edison-project.eu/ontologies/2016/DS-JOB-ADS";
    private static SKOSDataFactory skosDF;
    private static SKOSDataset dataset;
    private static SKOSManager manager;
//    private static String schme;

    public static SKOSDataFactory getSKOSDataFactory() throws SKOSCreationException {
        if (skosDF == null) {
            skosDF = getSKOSManager().getSKOSDataFactory();
        }
        return skosDF;
    }

    public static SKOSManager getSKOSManager() throws SKOSCreationException {
        if (manager == null) {
            manager = new SKOSManager();
        }
        return manager;
    }

    public static SKOSDataset getSKOSDataset() throws SKOSCreationException, IOException {
        if (dataset == null) {
            dataset = getSKOSManager().createSKOSDataset(URI.create(SKOS_URI));
        }
        return dataset;
    }

    static Collection<? extends SKOSChange> create(Term tv, String lang, boolean isTpConcept, String version) throws SKOSCreationException, IOException {
        List<SKOSChange> addAssertions = new ArrayList<>();
        URI uri = URI.create(SKOS_URI + "v" + version);
        SKOSConceptScheme scheme = getSKOSDataFactory().getSKOSConceptScheme(uri);

        SKOSEntityAssertion schemaAss = getSKOSDataFactory().getSKOSEntityAssertion(scheme);
        addAssertions.add(new AddAssertion(getSKOSDataset(), schemaAss));

        SKOSConcept concept = getSKOSDataFactory().getSKOSConcept(URI.create(uri + "#" + tv.getUID()));

        if (isTpConcept) {
            SKOSObjectRelationAssertion topConcept = getSKOSDataFactory().getSKOSObjectRelationAssertion(scheme, getSKOSDataFactory().getSKOSHasTopConceptProperty(), concept);
            addAssertions.add(new AddAssertion(getSKOSDataset(), topConcept));
        }

        addAssertions.add(new AddAssertion(getSKOSDataset(), getPrefAssertion(concept, tv.getLemma(), lang)));
        addAssertions.add(new AddAssertion(getSKOSDataset(), getInSchemeAssertion(concept, scheme)));

        List<String> alt = tv.getAlternativeLables();
        if (alt != null) {
            for (String s : alt) {
                if (!s.equals(tv.getLemma())) {
                    addAssertions.add(new AddAssertion(getSKOSDataset(), getAltAssertion(concept, s, lang)));
                }
            }
        }

        List<String> cats = tv.getCategories();
        if (cats != null) {
            for (String cat : cats) {
                addAssertions.add(new AddAssertion(getSKOSDataset(), getCategoryAssertion(concept, cat, lang)));
            }
        }
        List<String> glosses = tv.getGlosses();
        if (glosses != null) {
            for (String gloss : glosses) {
                addAssertions.add(new AddAssertion(getSKOSDataset(), getDefinitionAassertion(concept, gloss, lang)));
            }
        }

        List<Term> broader = tv.getBroader();
        if (broader != null) {
            for (Term b : broader) {
                SKOSConcept broaderConcept = getSKOSDataFactory().getSKOSConcept(URI.create(uri + "#" + b.getUID()));
                SKOSObjectRelationAssertion broaderPropertyRelationAssertion = getSKOSDataFactory().
                        getSKOSObjectRelationAssertion(concept, getSKOSDataFactory().getSKOSBroaderProperty(), broaderConcept);
                addAssertions.add(new AddAssertion(getSKOSDataset(), broaderPropertyRelationAssertion));

                SKOSObjectRelationAssertion narrowerPropertyRelationAssertion = getSKOSDataFactory().
                        getSKOSObjectRelationAssertion(broaderConcept, getSKOSDataFactory().getSKOSNarrowerProperty(), concept);
                addAssertions.add(new AddAssertion(getSKOSDataset(), narrowerPropertyRelationAssertion));

                if (tv.getBroader() == null || tv.getBroader().isEmpty()) {
                    addAssertions.addAll(create(b, "EN", true, version));
                } else {
                    addAssertions.addAll(create(b, "EN", false, version));
                }
            }
        }

        List<Term> related = tv.getSynonyms();
        if (related != null) {
            for (Term r : related) {
                SKOSConcept relatedConcept = getSKOSDataFactory().getSKOSConcept(URI.create(uri + "#" + r.getUID()));

                SKOSObjectRelationAssertion relatedPropertyRelationAssertion = getSKOSDataFactory().
                        getSKOSObjectRelationAssertion(concept, getSKOSDataFactory().getSKOSRelatedProperty(), relatedConcept);
                
                
                addAssertions.add(new AddAssertion(getSKOSDataset(), relatedPropertyRelationAssertion));

                relatedPropertyRelationAssertion = getSKOSDataFactory().
                        getSKOSObjectRelationAssertion(relatedConcept, getSKOSDataFactory().getSKOSRelatedProperty(), concept);

                addAssertions.add(new AddAssertion(getSKOSDataset(), relatedPropertyRelationAssertion));
            }
        }

        return addAssertions;
    }

    public static List<SKOSChange> create(DirectedWeightedEdge edge, String lang, boolean isTpConcept) throws ParseException, SKOSCreationException, IOException {

        Term target = (Term) edge.getTarget();
        Term source = (Term) edge.getSource();

        List<SKOSChange> addAssertions = new ArrayList<>();
        SKOSConceptScheme scheme = getSKOSDataFactory().getSKOSConceptScheme(URI.create(SKOS_URI));
        SKOSEntityAssertion schemaAss = getSKOSDataFactory().getSKOSEntityAssertion(scheme);
        addAssertions.add(new AddAssertion(getSKOSDataset(), schemaAss));

        SKOSConcept targetConcept = getSKOSDataFactory().getSKOSConcept(URI.create(SKOS_URI + "#" + target.getUID()));

        addAssertions.add(new AddAssertion(getSKOSDataset(), getPrefAssertion(targetConcept, target.getLemma(), lang)));
        addAssertions.add(new AddAssertion(getSKOSDataset(), getInSchemeAssertion(targetConcept, scheme)));

        List<String> alt = target.getAlternativeLables();
        if (alt != null) {
            for (String s : alt) {
                if (!s.equals(target.getLemma())) {
                    addAssertions.add(new AddAssertion(getSKOSDataset(), getAltAssertion(targetConcept, s, lang)));
                }
            }
        }

        List<String> cats = target.getCategories();
        if (cats != null) {
            for (String cat : cats) {
                addAssertions.add(new AddAssertion(getSKOSDataset(), getCategoryAssertion(targetConcept, cat, lang)));
            }
        }
        List<String> glosses = target.getGlosses();
        if (glosses != null) {
            for (String gloss : glosses) {
                addAssertions.add(new AddAssertion(getSKOSDataset(), getDefinitionAassertion(targetConcept, gloss, lang)));
            }
        }

        SKOSConcept sourceConcept = getSKOSDataFactory().getSKOSConcept(URI.create(SKOS_URI + "#" + source.getUID()));
//        SKOSConcept sourceConcept = getSKOSDataFactory().getSKOSConcept(URI.create(SKOS_URI + "#" + source.getLemma()));
        addAssertions.add(new AddAssertion(getSKOSDataset(), getPrefAssertion(sourceConcept, source.getLemma(), lang)));
        addAssertions.add(new AddAssertion(getSKOSDataset(), getInSchemeAssertion(sourceConcept, scheme)));
        if (isTpConcept) {
            SKOSObjectRelationAssertion topConcept = getSKOSDataFactory().getSKOSObjectRelationAssertion(scheme, getSKOSDataFactory().getSKOSHasTopConceptProperty(), sourceConcept);
            addAssertions.add(new AddAssertion(getSKOSDataset(), topConcept));
        }

        alt = source.getAlternativeLables();
        if (alt != null) {
            for (String s : alt) {
                if (!s.equals(source.getLemma())) {
                    addAssertions.add(new AddAssertion(getSKOSDataset(), getAltAssertion(sourceConcept, s, lang)));
                }
            }
        }
        cats = source.getCategories();
        if (cats != null) {
            for (String cat : cats) {
                addAssertions.add(new AddAssertion(getSKOSDataset(), getCategoryAssertion(sourceConcept, cat, lang)));
            }
        }
        glosses = source.getGlosses();
        if (glosses != null) {
            for (String gloss : glosses) {
                addAssertions.add(new AddAssertion(getSKOSDataset(), getDefinitionAassertion(sourceConcept, gloss, lang)));
            }
        }

        SKOSObjectRelationAssertion broaderPropertyRelationAssertion = getSKOSDataFactory().
                getSKOSObjectRelationAssertion(sourceConcept, getSKOSDataFactory().getSKOSBroaderProperty(), targetConcept);
        addAssertions.add(new AddAssertion(getSKOSDataset(), broaderPropertyRelationAssertion));

        SKOSObjectRelationAssertion narrowerPropertyRelationAssertion = getSKOSDataFactory().
                getSKOSObjectRelationAssertion(targetConcept, getSKOSDataFactory().getSKOSNarrowerProperty(), sourceConcept);
        addAssertions.add(new AddAssertion(getSKOSDataset(), narrowerPropertyRelationAssertion));

        return addAssertions;
    }

    private static SKOSAssertion getCategoryAssertion(SKOSConcept concept, String category, String lang) throws SKOSCreationException {
        SKOSAnnotation annotation = getSKOSDataFactory().getSKOSAnnotation(getSKOSDataFactory().
                getSKOSNotationProperty().getURI(), category, lang);
        return getSKOSDataFactory().getSKOSAnnotationAssertion(concept, annotation);
    }

    private static SKOSAssertion getDefinitionAassertion(SKOSConcept concept, String gloss, String lang) throws SKOSCreationException {
        SKOSAnnotation annotation = getSKOSDataFactory().getSKOSAnnotation(getSKOSDataFactory().getSKOSDefinitionObjectProperty().getURI(), gloss, lang);
        return getSKOSDataFactory().getSKOSAnnotationAssertion(concept, annotation);
    }

    private static SKOSAssertion getAltAssertion(SKOSConcept concept, String s, String lang) throws SKOSCreationException {
        SKOSAnnotation altLabel = getSKOSDataFactory().getSKOSAnnotation(getSKOSDataFactory().
                getSKOSAltLabelProperty().getURI(), s, lang);
        return getSKOSDataFactory().getSKOSAnnotationAssertion(concept, altLabel);
    }

    public static SKOSAssertion getInSchemeAssertion(SKOSConcept concept, SKOSConceptScheme scheme) throws SKOSCreationException {
        return getSKOSDataFactory().getSKOSObjectRelationAssertion(concept, getSKOSDataFactory().getSKOSInSchemeProperty(), scheme);
    }

    public static SKOSAssertion getPrefAssertion(SKOSConcept concept, String lemma, String lang) throws SKOSCreationException {
        SKOSAnnotation prefLabel = getSKOSDataFactory().getSKOSAnnotation(getSKOSDataFactory().getSKOSPrefLabelProperty().getURI(), lemma, lang);
        return getSKOSDataFactory().getSKOSAnnotationAssertion(concept, prefLabel);
    }

    static SKOSAssertion addExactMatchMapping(SKOSConcept sourceConcept, SKOSConcept targetConcept) throws SKOSCreationException {
        return getSKOSDataFactory().
                getSKOSObjectRelationAssertion(sourceConcept, getSKOSDataFactory().getSKOSExactMatchProperty(), targetConcept);
    }

    static SKOSAssertion addCloseMatchMapping(SKOSConcept sourceConcept, SKOSConcept targetConcept) throws SKOSCreationException {
        return getSKOSDataFactory().
                getSKOSObjectRelationAssertion(sourceConcept, getSKOSDataFactory().getSKOSCloseMatchProperty(), targetConcept);
    }

    static String getPrefLabelValue(SKOSDataset dataset, SKOSConcept concept, String language) throws SKOSCreationException {
        Set<SKOSAnnotation> prefLabel = dataset.getSKOSAnnotationsByURI(concept, getSKOSDataFactory().getSKOSPrefLabelProperty().getURI());
        SKOSAnnotation ann = prefLabel.iterator().next();
        // if the annotation is a literal annotation?

        String value, lang = null;

        if (ann.isAnnotationByConstant()) {
            SKOSLiteral literal = ann.getAnnotationValueAsConstant();
            value = literal.getLiteral();
            if (!literal.isTyped()) {
                // if it has  language
                SKOSUntypedLiteral untypedLiteral = literal.getAsSKOSUntypedLiteral();
                if (untypedLiteral.hasLang()) {
                    lang = untypedLiteral.getLang();
                }
            }
        } else {
            // annotation is some resource
            SKOSEntity entity = ann.getAnnotationValue();
            value = entity.getURI().getFragment();
        }
//        if (lang != null) {
        return value;
//        }
//        return null;
    }

    static String getUID(SKOSConcept concept, File taxonomyFile) {
        return concept.getURI().toString().substring(concept.getURI().toString().indexOf("#"));
    }

    static List<String> getAltLabelValues(SKOSDataset dataset, SKOSConcept concept, String language) throws SKOSCreationException {
        List<String> altLabels = new ArrayList<>();
        Set<SKOSAnnotation> altLabel = dataset.getSKOSAnnotationsByURI(concept, getSKOSDataFactory().getSKOSAltLabelProperty().getURI());
        Iterator<SKOSAnnotation> iter = altLabel.iterator();

        String value, lang = null;
        while (iter.hasNext()) {
            SKOSAnnotation ann = iter.next();
            if (ann.isAnnotationByConstant()) {
                SKOSLiteral literal = ann.getAnnotationValueAsConstant();
                value = literal.getLiteral();
                if (!literal.isTyped()) {
                    // if it has  language
                    SKOSUntypedLiteral untypedLiteral = literal.getAsSKOSUntypedLiteral();
                    if (untypedLiteral.hasLang()) {
                        lang = untypedLiteral.getLang();
                    }
                }
            } else {
                // annotation is some resource
                SKOSEntity entity = ann.getAnnotationValue();
                value = entity.getURI().getFragment();
            }
            if (lang != null && lang.equals(language)) {
                altLabels.add(value);
            }
        }
        return altLabels;
    }

    static List<String> getBroaderUIDs(SKOSDataset dataset, SKOSConcept concept) throws SKOSCreationException {
        List<String> broaderUIDs = new ArrayList<>();
        Set<SKOSAnnotation> broaderLabel = dataset.getSKOSAnnotationsByURI(concept, getSKOSDataFactory().getSKOSBroaderProperty().getURI());
        Iterator<SKOSAnnotation> iter = broaderLabel.iterator();

        String value = null;
        while (iter.hasNext()) {
            SKOSAnnotation ann = iter.next();
            if (ann.isAnnotationByConstant()) {
                SKOSLiteral literal = ann.getAnnotationValueAsConstant();
                value = literal.getLiteral();
            } else {
                // annotation is some resource
                SKOSEntity entity = ann.getAnnotationValue();
                value = entity.getURI().getFragment();
            }
//            if (lang != null && lang.equals(language)) {
            broaderUIDs.add(value);
//            }
        }
        return broaderUIDs;
    }

    static Set<String> getNarrowerUIDs(SKOSDataset dataset, SKOSConcept concept) throws SKOSCreationException {
        Set<String> narrowerUIDs = new HashSet<>();
        Set<SKOSAnnotation> narrowerLabel = dataset.getSKOSAnnotationsByURI(concept, getSKOSDataFactory().getSKOSNarrowerProperty().getURI());
        Iterator<SKOSAnnotation> iter = narrowerLabel.iterator();

        String value = null;
        while (iter.hasNext()) {
            SKOSAnnotation ann = iter.next();
            if (ann.isAnnotationByConstant()) {
                SKOSLiteral literal = ann.getAnnotationValueAsConstant();
                value = literal.getLiteral();
            } else {
                // annotation is some resource
                SKOSEntity entity = ann.getAnnotationValue();
                value = entity.getURI().getFragment();
            }
            narrowerUIDs.add(value);

        }
        return narrowerUIDs;
    }
}
