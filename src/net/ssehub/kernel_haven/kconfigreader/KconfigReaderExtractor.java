package net.ssehub.kernel_haven.kconfigreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.variability_model.AbstractVariabilityModelExtractor;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Wrapper to run KconfigReader.
 * 
 * @author Adam
 * @author Manu
 * @author Johannes
 * @author Moritz
 */
public class KconfigReaderExtractor extends AbstractVariabilityModelExtractor {

    private static final Logger LOGGER = Logger.get();
    
    private static final Setting<Boolean> FIND_LOCATIONS
            = new Setting<>("variability.extractor.find_locations", Setting.Type.BOOLEAN, true, "false", "TODO");

    /**
     * The path to the linux source tree.
     */
    private File linuxSourceTree;

    /**
     * Signals whether source locations (the source of variables) should be
     * included in the variabilitymodel after extraction.
     */
    private boolean findSourceLocations;

    /**
     * The architecture to analyze.
     */
    private String arch;
    
    /**
     * The directory where this extractor can store its resources. Not null.
     */
    private File resourceDir;
    
    @Override
    protected void init(Configuration config) throws SetUpException {
        linuxSourceTree = config.getValue(DefaultSettings.SOURCE_TREE);
        arch = config.getValue(DefaultSettings.ARCH);
        if (arch == null) {
            throw new SetUpException("Config does not contain arch setting");
        }
        
        config.registerSetting(FIND_LOCATIONS);
        findSourceLocations = config.getValue(FIND_LOCATIONS);

        resourceDir = Util.getExtractorResourceDir(config, getClass());
    }

    @Override
    protected VariabilityModel runOnFile(File target) throws ExtractorException {
        LOGGER.logDebug("Starting extraction");

        File outputBase = null;

        try {
            KconfigReaderWrapper wrapper = new KconfigReaderWrapper(resourceDir, linuxSourceTree);

            boolean makeSuccess = wrapper.prepareLinux();
            if (!makeSuccess) {
                throw new ExtractorException("'make allyesconfig prepare' failed to execute");
            }
            
            File dumpconfExe = wrapper.compileDumpconf();
            if (dumpconfExe == null) {
                throw new ExtractorException("Compiling dumpconf failed");
            }
            dumpconfExe.deleteOnExit();
            
            outputBase = wrapper.runKconfigReader(dumpconfExe, arch);
            dumpconfExe.delete();
            
            if (outputBase == null) {
                throw new ExtractorException("KconfigReader run not succesful");
            }
        } catch (IOException e) {
            throw new ExtractorException(e);
        }
        
        LOGGER.logDebug("KconfigReader run successful", "Output is at: " + outputBase.getAbsolutePath());

        Converter converter = new Converter(outputBase);
        VariabilityModel result = null;
        try {
            result = converter.convert();
        } catch (IOException | FormatException e) {
            LOGGER.logException("Exception while parsing KconfigReader output", e);
            throw new ExtractorException(e);
        }

        if (result != null && findSourceLocations) {
            findSourceLocations(result);
        }

        return result;
    }
    
    /**
     * 
     * Finds the corresponding location in the sourcefiles for the variables
     * contained in the VariabilityModel.
     * 
     * @param vm
     *            the VariabilityModel.
     */
    void findSourceLocations(VariabilityModel vm) {
        Map<String, VariabilityVariable> vars = vm.getVariableMap();
        lukeFilewalker(vars, linuxSourceTree);
    }

    /**
     * Use the Source Luke. Recursively ...
     * 
     * @param vars
     *            the variables to find the location for.
     * @param dir
     *            the directory to use for finding the location.
     */
    private void lukeFilewalker(Map<String, VariabilityVariable> vars, File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory() && !file.getName().startsWith(".")) {
                lukeFilewalker(vars, file);
            } else if (file.getName().startsWith("Kconfig")) {
                useTheSourceLuke(vars, file);
            }

        }
    }

    /**
     * 
     * Finds variability variables in kconfig-files and stores them in the
     * according VariabilityVariable.
     * 
     * @param vars
     *            the variables to find the location for.
     * @param file
     *            the kconfig-file.
     */
    private void useTheSourceLuke(Map<String, VariabilityVariable> vars, File file) {

        Pattern pattern = Pattern.compile("^[^#]*config\\s*([A-Za-z0-9_]+)");

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            int lineNo = 1;
            while ((line = in.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    String varName = "CONFIG_" + m.group(1);
                    if (vars.containsKey(varName)) {
                        vars.get(varName).addLocation(new SourceLocation(
                                linuxSourceTree.toPath().relativize(file.toPath()).toFile(), lineNo));
                    }
                }

                lineNo++;
            }

        } catch (IOException e) {
            LOGGER.logException("Could not read kconfig-file", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing to do here.
                }
            }
        }
    }

    @Override
    protected String getName() {
        return "KconfigReaderExtractor";
    }

}
