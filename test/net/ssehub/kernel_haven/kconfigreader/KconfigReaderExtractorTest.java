package net.ssehub.kernel_haven.kconfigreader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.Logger;
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
public class KconfigReaderExtractorTest {
    
    private static final File RESOURCE_DIR = new File("testdata/tmp_res");
    
    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void initLogger() {
        Logger.init();
    }
    
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
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        
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
        System.out.println(vars);
        assertThat(vars.get("CONFIG_A"), notNullValue());
        assertThat(vars.get("CONFIG_B"), notNullValue());
        assertThat(vars.get("CONFIG_C"), notNullValue());
        assertThat(vars.size(), is(3));
        
        assertThat(vars.get("CONFIG_A").getType(), is("bool"));
        assertThat(vars.get("CONFIG_B").getType(), is("bool"));
        assertThat(vars.get("CONFIG_C").getType(), is("tristate"));
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
