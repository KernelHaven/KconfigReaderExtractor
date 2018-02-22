package net.ssehub.kernel_haven.kconfigreader;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests for Converter.
 * 
 * @author Moritz
 * @author Adam
 */
public class ConverterTest {

    private static final File DIMACS_PATH = new File("testdata/testmodel");
    
    private static final File DIMACS_PATH2 = new File("testdata/testmodel2");
    
    private static final File DIMACS_PATH3 = new File("testdata/testmodel3");
    
    private static final File DIMACS_PATH4 = new File("testdata/testmodel4");
    
    private static final File DIMACS_PATH5 = new File("testdata/testmodel5");
    
    /**
     * Creates the converter for a test case. Also checks that the .rsf file does not contain "\r\n" linefeeds.
     * 
     * @param path The outputBase to supply to the Converter.
     * @return The converter for the test case.
     */
    private Converter init(File path) {
        File rsfFile = new File(path.getPath() + ".rsf");
        
        if (rsfFile.isFile()) {
            try (FileInputStream in = new FileInputStream(rsfFile)) {
                
                int read;
                while ((read = in.read()) != -1) {
                    if (read == '\r' && in.read() == '\n') {
                        fail("Your checked out version of " + rsfFile.getPath() + " contains windows-style linefeeds ("
                                + "\\r\\n); this breaks this unit test.\n"
                                + "To fix this, tell git to not replace linefeeds with the windows version:\n"
                                + "Set this globally:\ngit config --global core.autocrlf input\n"
                                + "Or just for this repository:\ngit config --local core.autocrlf input\n"
                                + "After adjusting the configuration, remove and re-checkout the offending file:\n"
                                + "rm " + rsfFile.getPath() + "\n"
                                + "git checkout -- " + rsfFile.getPath());
                    }
                }
                
            } catch (IOException e) {
                // ignore...
            }
        }
        
        return new Converter(path);
    }

    /**
     * Test if variables are correctly read from the feature file.
     * @throws FormatException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testVariableConversion() throws IOException, FormatException {
        Converter converter = init(DIMACS_PATH);

        VariabilityModel vm = converter.convert();
        Set<VariabilityVariable> vars = vm.getVariables(); 
        assertThat(vars.size(), is(3));

        for (VariabilityVariable var : vars) {
            assertThat(var.getName(), anyOf(is("CONFIG_ALPHA"), is("CONFIG_BETA"), is("CONFIG_GAMMA")));
            if (var.getName().equals("CONFIG_GAMMA")) {
                assertThat(var.getType(), is("bool"));
            } else {
                assertThat(var.getType(), is("tristate"));
            }
        }

    }
    
    /**
     * Tests if the correct DIMACS file is stored in the {@link VariabilityModel}.
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testDimacsFile() throws IOException, FormatException {
        Converter converter = init(DIMACS_PATH);

        VariabilityModel vm = converter.convert();
        assertThat(vm.getConstraintModel().isFile(), is(true));
        // can't check file name, since it will be copied
    }
    
    /**
     * Tests if the converter correctly detects wrong format (first name, then number).
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testWrongOrder() throws IOException, FormatException {
        Converter converter = init(DIMACS_PATH2);
        converter.convert();
    }
    
    /**
     * Tests if the converter correctly detects wrong format (not a number).
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testWrongFormatWrongNumber() throws IOException, FormatException {
        Converter converter = init(DIMACS_PATH3);
        converter.convert();
    }
    
    /**
     * Tests if the converter correctly detects wrong format (missing c).
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testWrongFormatMissingC() throws IOException, FormatException {
        Converter converter = init(DIMACS_PATH4);
        converter.convert();
    }

    /**
     * Tests whether spaces in variable names are handled correctly.
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testSpaceInVarName() throws IOException, FormatException {
        Converter converter = init(DIMACS_PATH5);
        VariabilityModel vm = converter.convert();
        
        assertThat(vm.getVariables().size(), is(1));
        for (VariabilityVariable var : vm.getVariables()) {
            assertThat(var.getName(), is("CONFIG_ARCH_HWEIGHT_CFLAGS"));
            assertThat(var.getType(), is("integer"));
            assertThat(var.getDimacsNumber(), is(0));
        }
    }
    
    /**
     * Tests whether a missing CONFIG_MODULES variable is handled properly.
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testMissingConfigModules() throws IOException, FormatException {
        // testcase originally discovered in Saschas example product line (ICSE18 demo)
        Converter converter = init(new File("testdata/testmodel6"));
        VariabilityModel vm = converter.convert();
        
        assertThat(vm.getVariables().size(), is(9));
        
        VariabilityVariable modules = vm.getVariableMap().get("CONFIG_MODULES");
        assertThat(modules, notNullValue());
        assertThat(modules.getType(), is("bool"));
        assertThat(modules.getDimacsNumber(), is(9));
    }
    
    /**
     * Tests whether variables that are only in DIMACS but not in RSF throw an error.
     * @throws FormatException wanted.
     * @throws IOException unwanted.
     */
    @Test(expected = FormatException.class)
    public void testMissingInRsf() throws IOException, FormatException {
        Converter converter = init(new File("testdata/testmodel7"));
        converter.convert();
    }
    
    /**
     * Tests whether choices with explicit names are handled properly.
     * @throws FormatException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testChoiceWithName() throws IOException, FormatException {
     // testcase originally discovered in Saschas example product line (ICSE18 demo)
        Converter converter = init(new File("testdata/testmodel6"));
        VariabilityModel vm = converter.convert();
        
        assertThat(vm.getVariables().size(), is(9));
        
        VariabilityVariable choice = vm.getVariableMap().get("CONFIG_CALC_SELECTION");
        assertThat(choice, notNullValue());
        assertThat(choice.getType(), is("bool"));
        assertThat(choice.getDimacsNumber(), is(1));
    }
    
    /**
     * Tests whether choices without explicit names are handled properly.
     * @throws FormatException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testChoiceWithoutName() throws IOException, FormatException {
        Converter converter = init(new File("testdata/testmodel8"));
        VariabilityModel vm = converter.convert();
        
        assertThat(vm.getVariables().size(), is(9));
        
        VariabilityVariable choice = vm.getVariableMap().get("CONFIG_CHOICE_1");
        assertThat(choice, notNullValue());
        assertThat(choice.getType(), is("bool"));
        assertThat(choice.getDimacsNumber(), is(1));
    }
    
    /**
     * Tests whether boolean variables with a name ending in _MODULE are treated correctly.
     * 
     * @throws FormatException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testBooleanVariableEndingWithModule() throws IOException, FormatException {
        Converter converter = init(new File("testdata/testmodel9"));
        VariabilityModel vm = converter.convert();
        
        assertThat(vm.getVariables().size(), is(1));
        
        VariabilityVariable var = vm.getVariableMap().get("CONFIG_ALPHA_MODULE");
        assertThat(var, notNullValue());
        assertThat(var.getType(), is("bool"));
        assertThat(var.getDimacsNumber(), is(1));
    }
    
}
