package net.ssehub.kernel_haven.kconfigreader;

import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.HierarchicalVariable;
import net.ssehub.kernel_haven.variability_model.SourceLocation;

/**
 * A variability variable of type tristate. This additionally saves the DIMACS
 * number of the _MODULE variable.
 * 
 * @author Adam
 * @author Moritz
 */
public class TristateVariable extends HierarchicalVariable {
    
    private static final long serialVersionUID = 797460760210025032L;
    
    private int moduleNumber;

    /**
     * Creates a new variable.
     * 
     * @param name
     *            The name of the new variable. Must not be null.
     */
    public TristateVariable(@NonNull String name) {
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
    public TristateVariable(@NonNull String name, int dimacsNumber, int moduleNumber) {
        super(name, "tristate", dimacsNumber);
        this.moduleNumber = moduleNumber;
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
    public void getDimacsMapping(@NonNull Map<Integer, String> mapping) {
        mapping.put(getDimacsNumber(), getName());
        mapping.put(getModuleNumber(), getName() + "_MODULE");
    }

    @Override
    public @NonNull String toString() {
        List<@NonNull SourceLocation> sourceLocations = getSourceLocations();
        return "TristateVariable [name=" + getName() + ", type=" + getType() + ", dimacsNumber=" + getDimacsNumber()
                + ", moduleNumber=" + moduleNumber + ", codeLocations="
                + (sourceLocations == null ? "null" : sourceLocations.toString()) + "]";
    }

    @Override
    public int hashCode() {
        return super.hashCode() + new Integer(moduleNumber).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
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

}
