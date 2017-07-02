package net.ssehub.kernel_haven.kconfigreader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.TestConfiguration;
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
public class KconfigReaderExtractorTest {
    
    private static final File RESOURCE_DIR = new File("testdata/tmp_res");

    /**
     * Cleans the resource directory after each test.
     */
    @After
    public void tearDown() {
        for (File file : RESOURCE_DIR.listFiles()) {
            if (!file.getName().equals(".gitignore")) {
                file.delete();
            }
        }
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
        extractor.init(config.getVariabilityConfiguration());

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
