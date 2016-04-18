/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.semantizators;

import java.util.Map;

/**
 *
 * from
 * https://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
 */
public class ValueComparator implements java.util.Comparator<String> {

    Map<String, Number> base;

    public ValueComparator(Map base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    @Override
    public int compare(String a, String b) {
        if (base.get(a).doubleValue() >= base.get(b).doubleValue()) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
