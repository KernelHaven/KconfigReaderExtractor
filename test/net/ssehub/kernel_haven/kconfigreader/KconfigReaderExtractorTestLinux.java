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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.test_utils.RunOnlyOnLinux;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.Util;
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
@RunWith(RunOnlyOnLinux.class)
public class KconfigReaderExtractorTestLinux {
    
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
     * Tests a full extractor execution on testdata/pseudoLinux.
     * 
     * @throws SetUpException unwanted.
     * @throws ExtractorException unwanted.
     */
    @Test
    public void testFullRunPseudoLinux() throws SetUpException, ExtractorException {
        Properties props = new Properties();

        props.setProperty("resource_dir", RESOURCE_DIR.getPath());
        props.setProperty("source_tree", "testdata/pseudoLinux/");
        props.setProperty("arch", "x86");
        TestConfiguration config = new TestConfiguration(props);

        KconfigReaderExtractor extractor = new KconfigReaderExtractor();
        extractor.init(config);
        
        VariabilityModel vm = extractor.runOnFile(new File("testdata/pseudoLinux"));
        
        assertThat(vm.getConstraintModel(), notNullValue());
        assertThat(vm.getConstraintModel().isFile(), is(true));
        
        Map<String, VariabilityVariable> vars = vm.getVariableMap();
        assertThat(vars.get("CONFIG_A"), notNullValue());
        assertThat(vars.get("CONFIG_B"), notNullValue());
        assertThat(vars.get("CONFIG_C"), notNullValue());
        assertThat(vars.get("CONFIG_MODULES"), notNullValue());
        assertThat(vars.size(), is(4));
        
        assertThat(vars.get("CONFIG_A").getType(), is("bool"));
        assertThat(vars.get("CONFIG_B").getType(), is("bool"));
        assertThat(vars.get("CONFIG_C").getType(), is("tristate"));
        assertThat(vars.get("CONFIG_MODULES").getType(), is("bool"));
    }
    
}
