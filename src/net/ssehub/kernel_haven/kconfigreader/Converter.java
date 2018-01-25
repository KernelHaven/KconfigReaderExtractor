package net.ssehub.kernel_haven.kconfigreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Converter for converting the DIMACS output of KconfigReader to a
 * {@link VariabilityModel}.
 * 
 * @author Adam
 * @author Moritz
 */
public class Converter {

    private File dimacsFile;

    private File rsfFile;

    private int choiceId;

    /**
     * A cache the intermediate results for collecting the variables found in
     * the comments. The types of these variables may be changed in the
     * conversion process. This only not <code>null</code>, if readVariables()
     * is currently running.
     */
    private Map<String, VariabilityVariable> variableCache;

    /**
     * Creates a new converter for the given DIMACS file.
     * 
     * @param outputBase
     *            The base path to the output files of KconfigReader. Append
     *            ".features", ".dimacs", etc. for the different output files.
     *            Must not be <code>null</code>.
     */
    public Converter(File outputBase) {
        this.dimacsFile = new File(outputBase.getAbsolutePath() + ".dimacs");
        this.rsfFile = new File(outputBase.getAbsolutePath() + ".rsf");
    }

    /**
     * Starts the conversion process.
     * 
     * @return The {@link VariabilityModel} representing the given DIMACS file.
     *         Never null.
     * 
     * @throws IOException
     *             If reading the DIMACS file fails.
     * @throws FormatException
     *             If the DIMACS file has the wrong format.
     */
    public VariabilityModel convert() throws IOException, FormatException {
        Set<VariabilityVariable> dimacsVars = readVariables();
        Map<String, VariabilityVariable> variables = readRsfVariables();

        // for every variable we found in the DIMACS file, search the variable found in RSF and set
        // the DIMACS numbers
        for (VariabilityVariable dimacsVar : dimacsVars) {

            String name = dimacsVar.getName();
            int equalIndex = name.indexOf('=');
            if (equalIndex != -1) {
                name = name.substring(0, equalIndex);
            }

            VariabilityVariable var = variables.get(name);

            if (var == null) {
                if (name.equals("CONFIG_MODULES")) {
                    // special case: KconfigReader always adds CONFIG_MODULES to the DIMACS model, even if it
                    // is not defined in the Kconfig files (I personally consider this a bug)
                    // to work around the edge case, that Kconfig does not contain CONFIG_MODULES, we create this
                    // variable here
                    var = new VariabilityVariable("CONFIG_MODULES", "bool");
                    variables.put(name, var);
                    
                } else {
                    throw new FormatException("Found variable " + name + " in DIMACS but not in RSF");
                }
            }

            if (var.getType().equals("bool")) {
                var.setDimacsNumber(dimacsVar.getDimacsNumber());
            } else if (var.getType().equals("tristate")) {

                var.setDimacsNumber(dimacsVar.getDimacsNumber());
                ((TristateVariable) var).setModuleNumber(((TristateVariable) dimacsVar).getModuleNumber());
            }
            // we can't get DIMACS numbers for anything but boolean and tristate
            // this is because they are in the form of "CONFIG_INT_VAR=<value>" and occur multiple times
        }

        VariabilityModel result = new VariabilityModel(dimacsFile, variables);
        return result;
    }

    /**
     * Returns an iterable object for the children of the given parent node.
     * 
     * @param parent
     *            The parent node.
     * @return An iterable object that returns iterators over the parent's
     *         children.
     */
    private static Iterable<Node> nodeIterator(Node parent) {
        return () -> {
            return new Iterator<Node>() {

                private int i = 0;

                @Override
                public boolean hasNext() {
                    return parent.getChildNodes().getLength() > i;
                }

                @Override
                public Node next() {
                    return parent.getChildNodes().item(i++);
                }
            };
        };
    }

    /**
     * Checks if the given symbol has the flag set in the "flags" attribute.
     * 
     * @param symbol
     *            The symbol to check.
     * @param flag
     *            The flag to check.
     * @return Whether the flag is set or not.
     * 
     * @throws FormatException
     *             If the symbol doesn't have a flags attribute, or it isn't an
     *             integer.
     */
    private boolean hasFlag(Node symbol, int flag) throws FormatException {
        try {
            String flagsStr = symbol.getAttributes().getNamedItem("flags").getTextContent();

            int flags = Integer.parseInt(flagsStr);

            return (flags & flag) != 0;

        } catch (NumberFormatException e) {
            throw new FormatException("Invalid flags attribute in <symbol>");
        } catch (NullPointerException e) {
            throw new FormatException("No flags attribute in <symbol>");
        }
    }
    
    /**
     * Reads a menu element and finds the symbol element in it.
     * 
     * @param menu The menu to read.
     * @return The symbol element inside of it; <code>null</code> if not present.
     * 
     * @throws FormatException If the menu element is invalid.
     */
    private Node getSymbol(Node menu) throws FormatException {
        Node symbol = null;
        
        for (Node menuChild : nodeIterator(menu)) {

            switch (menuChild.getNodeName()) {

            case "symbol":
                if (symbol == null) {
                    symbol = menuChild;
                } else {
                    throw new FormatException("More than one symbol in <menu>");
                }
                break;

            case "dep":
                // TODO
                break;

            case "#text":
                // ignore
                break;

            default:
                throw new FormatException("Unexpected tag in <menu>: " + menuChild.getNodeName());
            }
        }
        return symbol;
    }
    
    /**
     * Reads the given menu node.
     * 
     * @param menu
     *            The menu node.
     * @param result
     *            The result to add found variables to.
     * @throws FormatException
     *             If the format is invalid.
     */
    private void readMenu(Node menu, Map<String, VariabilityVariable> result) throws FormatException {
        Node symbol = getSymbol(menu);
        if (symbol == null) {
            return;
        }

        // attributes
        String type = symbol.getAttributes().getNamedItem("type").getTextContent();
//        int id = Integer.parseInt(symbol.getAttributes().getNamedItem("id").getTextContent());
        boolean choice = hasFlag(symbol, 0x0010);
        
        if (type.equals("boolean")) {
            type = "bool";
        }

        // children
        String name = null;

        for (Node symbolChild : nodeIterator(symbol)) {
            switch (symbolChild.getNodeName()) {

            case "name":
                if (name == null) {
                    name = symbolChild.getTextContent();
                } else {
                    throw new FormatException("More than one name for symbol " + name);
                }
                break;

            case "property":
                // TODO
                break;

            case "#text":
                // ignore
                break;

            default:
                throw new FormatException("Unexpected tag in <symbol>: " + symbolChild.getNodeName());
            }
        }

        if (choice) {
            if (name == null) {
                name = "CHOICE_" + (choiceId++);
            }
        } else {
            if (name == null) {
                throw new FormatException("No name found in symbol");
            }
        }

        name = "CONFIG_" + name;

        // System.out.println("ID: " + id);
        // System.out.println("Symbol: " + name);
        // System.out.println("Type: " + type);
        // System.out.println("Choice: " + choice);
        // System.out.println("-----------------");

        VariabilityVariable var;
        if (type.equals("tristate")) {
            var = new TristateVariable(name);
        } else {
            var = new VariabilityVariable(name, type);
        }
        result.put(name, var);
    }

    /**
     * Reads the given submenu node.
     * 
     * @param submenu
     *            The submenu node.
     * @param result
     *            The result to add found variables to.
     * @throws FormatException
     *             If the format is invalid.
     */
    private void readSubMenu(Node submenu, Map<String, VariabilityVariable> result) throws FormatException {
        for (Node node : nodeIterator(submenu)) {

            switch (node.getNodeName()) {
            case "submenu":
                readSubMenu(node, result);
                break;

            case "menu":
                readMenu(node, result);
                break;

            case "#text":
                // ignore
                break;

            default:
                throw new FormatException("Unexpected tag in structure: " + node.getNodeName());
            }
        }
    }

    /**
     * Reads the XML RSF structure.
     * 
     * @return The variables read from the XML structure.
     * 
     * @throws IOException
     *             If reading the file fails.
     * @throws FormatException
     *             If the format of the file is invalid.
     */
    private Map<String, VariabilityVariable> readRsfVariables() throws IOException, FormatException {
        choiceId = 1;
        
        Map<String, VariabilityVariable> result = new HashMap<>();

        try (FileInputStream in = new FileInputStream(rsfFile)) {
            // skip everything until the "\n.\n"
            char[] lastThree = {' ', ' ', ' '};
            int index = 0;
            while (lastThree[0] != '\n' || lastThree[1] != '.' || lastThree[2] != '\n') {
                int read = in.read();
                if (read == -1) {
                    throw new FormatException();
                }
                lastThree[index] = (char) read;
                index = (index + 1) % lastThree.length;
            }

            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

                Document doc = builder.parse(in);

                Node element = doc.getDocumentElement();

                if (!element.getNodeName().equals("submenu")) {
                    throw new FormatException("Not a submenu");
                }

                readSubMenu(element, result);

                // Transformer t =
                // TransformerFactory.newInstance().newTransformer();
                // t.transform(new DOMSource(doc), new
                // StreamResult(System.out));

            } catch (ParserConfigurationException | SAXException e) {
                throw new FormatException(e);
            }

        }

        return result;
    }

    /**
     * Reads all variables from a DIMACS file created by KconfigReader. This
     * reads the comment section at the top of the file and also considers the
     * DIMACS number mapping.
     *
     * @return The variable read from the file. Never null.
     * 
     * @throws IOException
     *             If reading the file fails.
     * @throws FormatException
     *             If the file has not the correct format.
     */
    private Set<VariabilityVariable> readVariables() throws IOException, FormatException {
        BufferedReader in = null;
        variableCache = new HashMap<>();

        try {
            in = new BufferedReader(new FileReader(dimacsFile));

            String line;
            while ((line = in.readLine()) != null) {
                String[] elements = line.split(" ");

                // check if we found the "p cnf" line; this is at the bottom of
                // the comment section
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
        
        // search for tristate variables that are missing the non _MODULE part (the boolean part).
        // this means, that we found a _MODULE variable, but no corresponding variable without _MODULE
        // convert these variables back to boolean type with _MODULE (i.e. they are boolean variables that simply
        // happen to have a name ending in _MODULE)
        for (VariabilityVariable var : result) {
            if (var instanceof TristateVariable) {
                TristateVariable tri = (TristateVariable) var;
                
                if (tri.getDimacsNumber() == 0) { // if we haven't found the non _MODULE part
                    // replace this variable with a boolean one
                    VariabilityVariable newVar = new VariabilityVariable(tri.getName() + "_MODULE", "bool",
                            tri.getModuleNumber());
                    result.remove(var);
                    result.add(newVar);
                }
            }
        }
        
        return result;
    }

    /**
     * Reads the given line and adds the variable to the cache.
     * 
     * @param elements
     *            The parts read from the line. Must not be null.
     * 
     * @throws FormatException
     *             If the number is not parseable.
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
                // the boolean part was already found earlier, so we reuse its
                // number
                existingNumber = variableCache.get(name).getDimacsNumber();
            }
            variableCache.put(name, new TristateVariable(name, existingNumber, number));

        } else {
            // we found the boolean part of a variable

            if (!variableCache.containsKey(name)) {
                // we haven't found a module part yet, so we assume it is
                // boolean for now
                variableCache.put(name, new VariabilityVariable(name, "bool", number));
            } else {
                // we already found the module part, so we just set the number
                // for the boolean part
                variableCache.get(name).setDimacsNumber(number);
            }

        }
    }

}
