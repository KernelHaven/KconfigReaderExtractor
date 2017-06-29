package net.ssehub.kernel_haven.kconfigreader;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
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
     * Test if variables are correctly read from the feature file.
     * @throws FormatException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testVariableConversion() throws IOException, FormatException {
        Converter converter = new Converter(DIMACS_PATH);

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
        Converter converter = new Converter(DIMACS_PATH);

        VariabilityModel vm = converter.convert();
        assertThat(vm.getConstraintModel().getAbsolutePath(), is(DIMACS_PATH.getAbsolutePath() + ".dimacs"));
    }
    
    /**
     * Tests if the converter correctly detects wrong format (first name, then number).
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testWrongOrder() throws IOException, FormatException {
        Converter converter = new Converter(DIMACS_PATH2);
        converter.convert();
    }
    
    /**
     * Tests if the converter correctly detects wrong format (not a number).
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testWrongFormatWrongNumber() throws IOException, FormatException {
        Converter converter = new Converter(DIMACS_PATH3);
        converter.convert();
    }
    
    /**
     * Tests if the converter correctly detects wrong format (missing c).
     * @throws IOException unwanted.
     * @throws FormatException wanted.
     */
    @Test(expected = FormatException.class)
    public void testWrongFormatMissingC() throws IOException, FormatException {
        Converter converter = new Converter(DIMACS_PATH4);
        converter.convert();
    }

    /**
     * Tests whether spaces in variable names are handled correctly.
     * @throws IOException unwanted.
     * @throws FormatException unwanted.
     */
    @Test
    public void testSpaceInVarName() throws IOException, FormatException {
        Converter converter = new Converter(DIMACS_PATH5);
        VariabilityModel vm = converter.convert();
        
        assertThat(vm.getVariables().size(), is(1));
        for (VariabilityVariable var : vm.getVariables()) {
            assertThat(var.getName(), is("CONFIG_ARCH_HWEIGHT_CFLAGS"));
            assertThat(var.getType(), is("integer"));
            assertThat(var.getDimacsNumber(), is(0));
        }
    }
    
}
