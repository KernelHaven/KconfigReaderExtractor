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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * 
 * Tests the KconfigReaderExtractor.
 * 
 * @author moritz
 * @author adam
 *
 */
@SuppressWarnings("null")
public class KconfigReaderExtractorTest {
    
    private static final File RESOURCE_DIR = new File("testdata/tmp_res");
    
    /**
     * Creates the temporary resource dir.
     */
    @Before
    public void createTmpRes() {
        RESOURCE_DIR.mkdir();
    }
    
    /**
     * Deletes the temporary resource directory.
     * 
     * @throws IOException If deleting fails.
     */
    @After
    public void deleteTmpRes() throws IOException {
        Util.deleteFolder(RESOURCE_DIR);
    }
    
    /**
     * tests whether codelocations are found and assigned to variables
     * correctly.
     * 
     * @throws SetUpException
     *             unwanted.
     */
    @Test
    public void testSourceLocation() throws SetUpException {
        Properties props = new Properties();

        props.setProperty("resource_dir", RESOURCE_DIR.getPath());
        props.setProperty("source_tree", "testdata/pseudoLinux/");
        props.setProperty("arch", "x86");
        TestConfiguration config = new TestConfiguration(props);

        KconfigReaderExtractor extractor = new KconfigReaderExtractor();
        extractor.init(config);

        Set<VariabilityVariable> variables = new HashSet<>();
        variables.add(new VariabilityVariable("CONFIG_A", "bool"));
        variables.add(new VariabilityVariable("CONFIG_B", "bool"));
        variables.add(new VariabilityVariable("CONFIG_D", "bool"));

        VariabilityModel vm = new VariabilityModel(null, variables);

        extractor.findSourceLocations(vm);

        assertThat(vm.getVariableMap().get("CONFIG_A").getSourceLocations().size(), is(1));
        assertThat(vm.getVariableMap().get("CONFIG_A").getSourceLocations().get(0),
                is(new SourceLocation(new File("Kconfig"), 1)));

        assertThat(vm.getVariableMap().get("CONFIG_B").getSourceLocations().size(), is(1));
        assertThat(vm.getVariableMap().get("CONFIG_B").getSourceLocations().get(0),
                is(new SourceLocation(new File("Kconfig"), 4)));

        assertThat(vm.getVariableMap().get("CONFIG_D").getSourceLocations(), nullValue());

    }

}
