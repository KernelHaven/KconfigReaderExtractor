/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ssehub.kernel_haven.kconfigreader;

import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.io.json.JsonNumber;
import net.ssehub.kernel_haven.util.io.json.JsonObject;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.HierarchicalVariable;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * A variability variable of type tristate. This additionally saves the DIMACS
 * number of the _MODULE variable.
 * 
 * @author Adam
 * @author Moritz
 */
public class TristateVariable extends HierarchicalVariable {
    
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
     * @param name The name of the new variable.
     * @param type Will be ignored (type is always "tristate").
     */
    public TristateVariable(@NonNull String name, @NonNull String type) {
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
    
    @Override
    protected @NonNull JsonObject toJson() {
        JsonObject result = super.toJson();
        
        result.putElement("dimacsModuleNumber", new JsonNumber(moduleNumber));
        
        return result;
    }
    
    @Override
    protected void setJsonData(@NonNull JsonObject data, Map<@NonNull String, VariabilityVariable> vars)
            throws FormatException {
        super.setJsonData(data, vars);
        
        this.moduleNumber = data.getInt("dimacsModuleNumber");
    }

}
