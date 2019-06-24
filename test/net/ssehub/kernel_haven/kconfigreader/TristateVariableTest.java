/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.JsonVariabilityModelCache;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link TristateVariable}.
 *
 * @author Adam
 */
public class TristateVariableTest {
    
    private static final @NonNull File TESTDATA = new File("testdata");

    /**
     * Tests the equals() method.
     */
    @Test
    public void testEquals() {
        assertThat(new TristateVariable("ABC"), is(new TristateVariable("ABC")));
        assertThat(new TristateVariable("ABC"), not(is(new TristateVariable("DEF"))));
        assertThat(new TristateVariable("ABC"), not(is(new VariabilityVariable("ABC", "tristate"))));
        
        assertThat(new TristateVariable("ABC", 5, 9), is(new TristateVariable("ABC", 5, 9)));
        assertThat(new TristateVariable("ABC", 5, 9), not(is(new TristateVariable("ABC", 4, 9))));
        assertThat(new TristateVariable("ABC", 5, 9), not(is(new TristateVariable("ABC", 5, 10))));
    }
    
    /**
     * Tests the hashCode() method.
     */
    @Test
    public void testHashCode() {
        assertThat(new TristateVariable("ABC").hashCode(), is(new TristateVariable("ABC").hashCode()));
        assertThat(new TristateVariable("ABC", 5, 9).hashCode(), is(new TristateVariable("ABC", 5, 9).hashCode()));
    }
    
    /**
     * Tests the getDimacsMapping() method.
     */
    @Test
    public void testGetDimacsMapping() {
        TristateVariable var = new TristateVariable("ABC", 1, 2);
        Map<Integer, String> mapping = new HashMap<>();
        
        var.getDimacsMapping(mapping);
        
        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "ABC");
        expected.put(2, "ABC_MODULE");
        
        assertThat(mapping, is(expected));
    }
    
    /**
     * Tests the toString() method.
     */
    @Test
    public void testToString() {
        TristateVariable var = new TristateVariable("ABC", 1, 2);
        
        assertThat(var.toString(),
                is("TristateVariable [name=ABC, type=tristate, dimacsNumber=1, moduleNumber=2, codeLocations=null]"));
        
        var.addLocation(new SourceLocation(new File("file.kconfig"), 15));
        
        assertThat(var.toString(),
                is("TristateVariable [name=ABC, type=tristate, dimacsNumber=1, moduleNumber=2, "
                + "codeLocations=[file.kconfig:15]]"));
    }
    
    /**
     * Tests that serialization properly works.
     * 
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    @SuppressWarnings("null")
    public void testSerialization() throws IOException, FormatException {
        File cacheDir = new File(TESTDATA, "tmpCache");
        cacheDir.mkdir();

        try {
            TristateVariable var1 = new TristateVariable("VAR_1");
            TristateVariable var2 = new TristateVariable("VAR_2", 1, 4);
            var2.addLocation(new SourceLocation(new File("file.kconfig"), 15));
            
            VariabilityModel varModel = new VariabilityModel(new File(TESTDATA, "testmodel.dimacs"),
                    new HashSet<>(Arrays.asList(var1, var2)));
            
            JsonVariabilityModelCache cache = new JsonVariabilityModelCache(cacheDir);
            cache.write(varModel);
            
            VariabilityModel read = cache.read(new File("."));
            
            TristateVariable read1 = (TristateVariable) read.getVariableMap().get("VAR_1");
            assertThat(read1, is(var1));
            
            TristateVariable read2 = (TristateVariable) read.getVariableMap().get("VAR_2");
            assertThat(read2, is(var2));
            
            assertThat(read.getVariableMap().size(), is(2));
            
        } finally {
            Util.deleteFolder(cacheDir);
        }
    }
    
}
