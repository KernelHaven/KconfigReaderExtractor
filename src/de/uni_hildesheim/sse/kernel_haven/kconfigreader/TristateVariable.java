package de.uni_hildesheim.sse.kernel_haven.kconfigreader;

import java.util.List;
import java.util.Map;

import de.uni_hildesheim.sse.kernel_haven.util.FormatException;
import de.uni_hildesheim.sse.kernel_haven.variability_model.SourceLocation;
import de.uni_hildesheim.sse.kernel_haven.variability_model.VariabilityVariable;

/**
 * A variability variable of type tristate. This additionally saves the DIMACS
 * number of the _MODULE variable.
 * 
 * @author Adam
 * @author Moritz
 */
public class TristateVariable extends VariabilityVariable {

    private int moduleNumber;

    /**
     * Creates a new variable.
     * 
     * @param name
     *            The name of the new variable. Must not be null.
     */
    public TristateVariable(String name) {
        super(name, "tristate");
    }

    /**
     * Creates a new variable.
     * 
     * @param name
     *            The name of the new variable. Must not be null.
     * @param dimacsNumber
     *            The number that this variable has in the DIMACS representation
     *            of the variability model.
     * @param moduleNumber
     *            The number that the module part of this variable has in the
     *            DIMACS representation of the variability model.
     */
    public TristateVariable(String name, int dimacsNumber, int moduleNumber) {
        super(name, "tristate", dimacsNumber);
        this.moduleNumber = moduleNumber;
    }

    /**
     * Creates a tristate variable from a given VariabilityVariable.
     * 
     * @param var
     *            the variability variable.
     * @param moduleNumber
     *            the tristate module number.
     */
    private TristateVariable(VariabilityVariable var, int moduleNumber) {
        this(var.getName(), var.getDimacsNumber(), moduleNumber);

        if (var.getSourceLocations() != null) {
            for (SourceLocation location : var.getSourceLocations()) {
                this.addLocation(location);
            }
        }
    }

    /**
     * Returns the number that the module part of this variable has in the
     * DIMACS representation of the variability model. This is 0, if this
     * variable is not associated with a specific DIMACS representation.
     * 
     * @return The DIMACS number.
     */
    public int getModuleNumber() {
        return moduleNumber;
    }

    /**
     * Sets the number that the module part of this variable has in the DIMACS
     * representation of the variability model. Use 0, if this variable is not
     * associated with a specific DIMACS representation.
     * 
     * @param moduleNumber
     *            The DIMACS number.
     */
    public void setModuleNumber(int moduleNumber) {
        this.moduleNumber = moduleNumber;
    }

    @Override
    public List<String> serializeCsv() {
        List<String> result = super.serializeCsv();

        result.add("" + moduleNumber);

        return result;
    }

    @Override
    public void getDimacsMapping(Map<Integer, String> mapping) {
        mapping.put(getDimacsNumber(), getName());
        mapping.put(getModuleNumber(), getName() + "_MODULE");
    }

    @Override
    public String toString() {
        return "TristateVariable [name=" + getName() + ", type=" + getType() + ", dimacsNumber=" + getDimacsNumber()
                + ", moduleNumber=" + moduleNumber + ", codeLocations="
                + (getSourceLocations() == null ? "null" : getSourceLocations().toString()) + "]";
    }

    @Override
    public int hashCode() {
        return super.hashCode() + new Integer(moduleNumber).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = super.equals(obj);
        if (result) {
            if (obj instanceof TristateVariable) {
                TristateVariable other = (TristateVariable) obj;
                result = other.moduleNumber == this.moduleNumber;

            } else {
                result = false;
            }
        }
        return result;
    }

    /**
     * Creates a {@link TristateVariable} from the given CSV.
     * 
     * @param csvParts
     *            The CSV that is converted into a {@link TristateVariable}.
     * @return The {@link TristateVariable} created by the CSV.
     * 
     * @throws FormatException
     *             If the CSV cannot be read into a variable.
     */
    public static TristateVariable createFromCsv(String[] csvParts) throws FormatException {
        if (csvParts.length < 5) {
            throw new FormatException("Invalid CSV");
        }

        try {
            VariabilityVariable var = VariabilityVariable.createFromCsv(csvParts);

            return new TristateVariable(var, Integer.parseInt(csvParts[4]));

        } catch (NumberFormatException e) {
            throw new FormatException(e);
        }
    }

}
