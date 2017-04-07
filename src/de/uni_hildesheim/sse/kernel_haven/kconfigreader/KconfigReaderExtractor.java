package de.uni_hildesheim.sse.kernel_haven.kconfigreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni_hildesheim.sse.kernel_haven.SetUpException;
import de.uni_hildesheim.sse.kernel_haven.config.VariabilityExtractorConfiguration;
import de.uni_hildesheim.sse.kernel_haven.util.ExtractorException;
import de.uni_hildesheim.sse.kernel_haven.util.FormatException;
import de.uni_hildesheim.sse.kernel_haven.util.Logger;
import de.uni_hildesheim.sse.kernel_haven.variability_model.SourceLocation;
import de.uni_hildesheim.sse.kernel_haven.variability_model.IVariabilityModelExtractor;
import de.uni_hildesheim.sse.kernel_haven.variability_model.VariabilityModel;
import de.uni_hildesheim.sse.kernel_haven.variability_model.VariabilityModelProvider;
import de.uni_hildesheim.sse.kernel_haven.variability_model.VariabilityVariable;

/**
 * Wrapper to run KconfigReader.
 * 
 * @author Adam
 * @author Manu
 * @author Johannes
 * @author Moritz
 */
public class KconfigReaderExtractor implements IVariabilityModelExtractor, Runnable {

    private static final Logger LOGGER = Logger.get();

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
     * The provider to notify about results.
     */
    private VariabilityModelProvider provider;

    /**
     * The directory where this extractor can store its resources. Not null.
     */
    private File resourceDir;

    private boolean stopRequested;

    /**
     * Creates a new KconfigReader wrapper.
     * 
     * @param config
     *            The configuration. Must not be null.
     * 
     * @throws SetUpException
     *             If the configuration is not valid.
     */
    public KconfigReaderExtractor(VariabilityExtractorConfiguration config) throws SetUpException {
        linuxSourceTree = config.getSourceTree();
        if (linuxSourceTree == null) {
            throw new SetUpException("Config does not contain source_tree setting");
        }
        arch = config.getArch();
        if (arch == null) {
            throw new SetUpException("Config does not contain arch setting");
        }
        
        findSourceLocations = Boolean.parseBoolean(config.getProperty("variability.extractor.find_locations"));

        resourceDir = config.getExtractorResourceDir(getClass());
    }

    @Override
    public void setProvider(VariabilityModelProvider provider) {
        this.provider = provider;
    }

    @Override
    public void start() {
        Thread th = new Thread(this);
        th.setName("KconfigReaderExtractor");
        th.start();
    }

    @Override
    public void stop() {
        synchronized (this) {
            stopRequested = true;
        }
    }

    /**
     * Checks if the provider requested that we stop our extraction.
     * 
     * @return Whether stop is requested.
     */
    private synchronized boolean isStopRequested() {
        return stopRequested;
    }

    /**
     * A complete execution of the KconfigReader. This does the following
     * things:
     * <ol>
     * <li>Prepare the Linux kernel by calling make</li>
     * <li>Compile dumpconf against the Linux kernel</li>
     * <li>Run KconfigReader on the Linux kernel</li>
     * <li>Convert the output of KconfigReader</li>
     * <li>Notify the output converter</li>
     * </ol>
     */
    @Override
    public void run() {
        LOGGER.logDebug("Starting extraction");

        File outputBase = null;

        try {
            KconfigReaderWrapper wrapper = new KconfigReaderWrapper(resourceDir, linuxSourceTree);

            boolean makeSuccess = wrapper.prepareLinux();
            if (makeSuccess && !isStopRequested()) {
                File dumpconfExe = wrapper.compileDumpconf();
                if (dumpconfExe != null && !isStopRequested()) {
                    outputBase = wrapper.runKconfigReader(dumpconfExe, arch);
                    dumpconfExe.delete();
                }
            }
        } catch (IOException e) {
            LOGGER.logException("Exception while running KconfigReader", e);
            if (!isStopRequested()) {
                provider.setException(new ExtractorException(e));
            }
        }

        if (outputBase != null && !isStopRequested()) {
            LOGGER.logDebug("KconfigReader run successful", "Output is at: " + outputBase.getAbsolutePath());

            Converter converter = new Converter(outputBase);
            VariabilityModel result = null;
            try {
                result = converter.convert();
            } catch (IOException | FormatException e) {
                LOGGER.logException("Exception while parsing KconfigReader output", e);
                if (!isStopRequested()) {
                    provider.setException(new ExtractorException(e));
                }
            }

            if (result != null && !isStopRequested() && findSourceLocations) {
                findSourceLocations(result);
            }

            if (result != null && !isStopRequested()) {
                provider.setResult(result);
            }

        } else if (!isStopRequested()) {
            LOGGER.logError("KconfigReader run not successful");
            provider.setException(new ExtractorException("KconfigReader run not succesful"));
        }
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

        Pattern pattern = Pattern.compile("^[^#]*config ([A-Za-z0-9_]+)");

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

}
