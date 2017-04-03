package de.uni_hildesheim.sse.kernel_haven.kconfigreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_hildesheim.sse.kernel_haven.util.FormatException;
import de.uni_hildesheim.sse.kernel_haven.variability_model.VariabilityModel;
import de.uni_hildesheim.sse.kernel_haven.variability_model.VariabilityVariable;

/**
 * Converter for converting the DIMACS output of KconfigReader to a {@link VariabilityModel}.
 * 
 * @author Adam
 * @author Moritz
 */
public class Converter {
    
    private File dimacsFile;
    
    /**
     * A cache the intermediate results for collecting the variables found in the comments.
     * The types of these variables may be changed in the conversion process.
     * This only not <code>null</code>, if readVariables() is currently running.
     */
    private Map<String, VariabilityVariable> variableCache;
    
    /**
     * Creates a new converter for the given DIMACS file.
     * 
     * @param dimacsFile The DIMACS file created by KconfigReader. Must not be null.
     */
    public Converter(File dimacsFile) {
        this.dimacsFile = dimacsFile;
    }
    
    /**
     * Starts the conversion process.
     * 
     * @return The {@link VariabilityModel} representing the given DIMACS file. Never null.
     * 
     * @throws IOException If reading the DIMACS file fails.
     * @throws FormatException If the DIMACS file has the wrong format.
     */
    public VariabilityModel convert() throws IOException, FormatException {
        Set<VariabilityVariable> variables = readVariables();
        VariabilityModel result = new VariabilityModel(dimacsFile, variables);
        
        return result;
    }

    /**
     * Reads all variables from a DIMACS file created by KconfigReader. This reads the comment
     * section at the top of the file and also considers the DIMACS number mapping.
     *
     * @return The variable read from the file. Never null.
     * 
     * @throws IOException If reading the file fails.
     * @throws FormatException If the file has not the correct format.
     */
    private Set<VariabilityVariable> readVariables() throws IOException, FormatException {
        BufferedReader in = null;
        variableCache = new HashMap<>();
        
        try {
            in = new BufferedReader(new FileReader(dimacsFile));
    
            String line;
            while ((line = in.readLine()) != null) {
                String[] elements = line.split(" ");
                
                // check if we found the "p cnf" line; this is at the bottom of the comment section
                // so we are done
                if (elements.length == 4 && elements[0].equals("p")) {
                    break;
                }
                
                if (!elements[0].equals("c")) {
                    throw new FormatException();
                }
                readVariable(elements);
                
            }
        } finally {
            if (in != null) {
                in.close();
            }
            
        }

        Set<VariabilityVariable> result = new HashSet<>(variableCache.values());
        variableCache = null;
        return result;
    }
    
    /**
     * Reads the given line and adds the variable to the cache.
     *  
     * @param elements The parts read from the line. Must not be null.
     * 
     * @throws FormatException If the number is not parseable.
     */
    private void readVariable(String[] elements) throws FormatException {
        int number = -1;
        try {
            number = Integer.parseInt(elements[1]);
        } catch (NumberFormatException exc) {
            throw new FormatException();
        }

        String name = elements[2];
        name = "CONFIG_" + name;
        
        // apparently, names can contain spaces
        for (int i = 3; i < elements.length; i++) {
            name += " " + elements[i];
        }
        
        if (name.endsWith("_MODULE")) {
            // we found the module part of a tristate variable
            name = name.substring(0, name.length() - "_MODULE".length());
            int existingNumber = 0;
            if (variableCache.containsKey(name)) {
                // the boolean part was already found earlier, so we reuuse its number
                existingNumber = variableCache.get(name).getDimacsNumber();
            }
            variableCache.put(name, new TristateVariable(name, existingNumber, number));
            
        } else {
            // we found the boolean part of a variable
            
            if (!variableCache.containsKey(name)) {
                // we haven't found a module part yet, so we assume it is boolean for now
                variableCache.put(name, new VariabilityVariable(name, "bool", number));
            } else {
                // we already found the module part, so we just set the number for the boolean part
                variableCache.get(name).setDimacsNumber(number);
            }
            
        }
    }

}
