/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.sne.commons.tools;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.sne.commons.CacheUtil;

/**
 *
 * @author S. Koulouzis
 */
public class Main {

    public static void main(String args[]) {
        String command = null;
        if (args != null) {
            command = args[0];
            if (command != null) {
                try {
                    switch (command) {
                        case "rm":
                            if (args.length == 3) {
                                rm(args);
                            }
                            break;
                        default:
                            StringBuilder builder = new StringBuilder();
                            for (String s : args) {
                                builder.append(s).append(" ");
                            }
                            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Command not found. {0}", builder);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

    }

    private static void rm(String[] args) throws IOException {
        String keysStr = args[1];
        String cachePath = args[2];
        String[] keys = keysStr.split(",");
        for (String k : keys) {
            CacheUtil.removeElement(k, cachePath);
        }

    }
}
