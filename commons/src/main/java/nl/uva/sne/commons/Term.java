/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author S. Koulouzis
 */
public class Term {

    private final String lemma;
    private List<Term> broader;
    private String uid;
    private List<Term> synonyms;
    private List<String> categories;
    private boolean fromDictionary;
    private List<String> altLables;
    private List<String> glosses;
    private List<String> buids;
    private Set<String> nuids;
    private String fKey;
    private List<Term> narrower;
    private double confidence;
    private String originalTerm;

    public Term(String lemma) {
        this.lemma = lemma;
    }

    public void setBroader(List<Term> broader) {
        broader.equals(this);
        this.broader = broader;
    }

    public List<Term> getBroader() {
        return this.broader;
    }

    public void setUID(String uid) {
        this.uid = uid;
    }

    void setSynonyms(List<Term> synonyms) {
        this.synonyms = synonyms;
    }

    public List<Term> getSynonyms() {
        return this.synonyms;
    }

    public String getLemma() {
        return lemma;
    }

    public void setIsFromDictionary(boolean fromDict) {
        this.fromDictionary = fromDict;
    }

    public boolean getIsFromDictionary() {
        return this.fromDictionary;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Term) {
            Term v = (Term) obj;
            return (v.getUID().equals(this.uid));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.uid);
    }

    public String getUID() {
        return this.uid;
    }

    public List<String> getCategories() {
        return this.categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    void setAlternativeLables(List<String> altLables) {
        this.altLables = altLables;
    }

    public List<String> getAlternativeLables() {
        return this.altLables;
    }

    public void setGlosses(List<String> glosses) {
        this.glosses = glosses;
    }

    public List<String> getGlosses() {
        return this.glosses;
    }

    public void setBroaderUIDS(List<String> buids) {
        this.buids = buids;
    }

    public Set<String> getNarrowerUIDS() {
        return this.nuids;
    }

    public void setNarrowerUIDS(Set<String> nuids) {
        this.nuids = nuids;
    }

    public List<String> getBroaderUIDS() {
        return this.buids;
    }

    public void setForeignKey(String fKey) {
        this.fKey = fKey;
    }

    public String getForeignKey() {
        return this.fKey;
    }

    @Override
    public String toString() {
        return this.lemma + "-" + uid;
    }

    public void addNarrowerUID(String uid) {
        if (this.nuids == null) {
            nuids = new HashSet<>();
        }
        nuids.add(uid);
    }

    public List<Term> getNarrower() {
        return this.narrower;
    }

    public void addNarrower(Term tv) {
        if (narrower == null) {
            this.narrower = new ArrayList<>();
        }
        narrower.add(tv);
    }

    public void setNarrower(ArrayList<Term> narrower) {
        this.narrower = narrower;
    }

    public void addBroaderUID(String buid) {
        if (this.buids == null) {
            this.buids = new ArrayList<>();
        }
        this.buids.add(buid);
    }

    public void addBroader(Term tv) {
        if (this.broader == null) {
            this.broader = new ArrayList<>();
        }
        this.broader.add(tv);
    }

    /**
     * @return the confidence
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * @param confidence the confidence to set
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /**
     * @return the originalTerm
     */
    public String getOriginalTerm() {
        return originalTerm;
    }

    /**
     * @param originalTerm the originalTerm to set
     */
    public void setOriginalTerm(String originalTerm) {
        this.originalTerm = originalTerm;
    }

   
}
