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

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.ListSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.AbstractVariabilityModelExtractor;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;
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

    /**
     * Different versions of dumpconf.c, depending on which kconfig version / project should be analyzed.
     */
    public static enum DumpconfVersion {
        
        LINUX("KH_COMPILE_FOR_LINUX"),
        
        BUSYBOX("KH_COMPILE_FOR_BUSYBOX");
        
        private @NonNull String compileFlag;
        
        /**
         * Creates a dumpconf version.
         * 
         * @param compileFlag The flag to pass to the compiler to compile for this version (with -D)
         */
        private DumpconfVersion(@NonNull String compileFlag) {
            this.compileFlag = compileFlag;
        }
        
        /**
         * The flag to pass to the compiler via -D, that compiles dumpconf for this version.
         * 
         * @return The flag for this version.
         */
        public @NonNull String getCompileFlag() {
            return compileFlag;
        }
        
    }
    
    
    /**
     * A setting that specifies which dumpconf.c to use.
     */
    public static final @NonNull EnumSetting<@NonNull DumpconfVersion> DUMPCONF_VERSION
            = new EnumSetting<>("variability.extractor.dumpconf_version", DumpconfVersion.class, true,
                    DumpconfVersion.LINUX, "Defines which version of dumpconf to use. Choose this based on which "
                    + "product line to analyze.");
    
    public static final @NonNull ListSetting<@NonNull String> EXTRA_MAKE_PARAMETERS
            = new ListSetting<>("variability.extractor.extra_make_parameters", Type.STRING, false,
                    "Defines list of extra parameters to pass to make. These will be inserted between 'make' and"
                    + " 'allyesconfig prepare'. For example, you could set this to \"CC=gcc-4.8\" to set a specific "
                    + "compiler to use (only for the 'make allyesconfig prepare' call!).");
    
    /**
     * A setting that specifies whether to find code locations of the variables or not.
     */
    public static final @NonNull Setting<@NonNull Boolean> FIND_LOCATIONS
            = new Setting<>("variability.extractor.find_locations", Setting.Type.BOOLEAN, true, "false", "If set to "
                    + "true, the extractor will store source locations for each variable. Those locations represent "
                    + "occurences of the variable in the files that kconfigreader used for generating the "
                    + "VariabilityModel.");
    
    private static final Logger LOGGER = Logger.get();
    
    /**
     * The path to the linux source tree.
     */
    private @NonNull File linuxSourceTree = new File(""); // will be initialized in init()

    /**
     * Signals whether source locations (the source of variables) should be
     * included in the variabilitymodel after extraction.
     */
    private boolean findSourceLocations; // will be initialized in init()

    /**
     * The architecture to analyze.
     */
    private @NonNull String arch = ""; // will be initialized in init()
    
    /**
     * The directory where this extractor can store its resources. Not null.
     */
    private @NonNull File resourceDir = new File("");  // will be initialized in init()
    
    private @NonNull DumpconfVersion dumpconfVersion = DumpconfVersion.LINUX; // will be initialized in init()
    
    private @NonNull List<@NonNull String> extraMakeParameters = new LinkedList<>(); // will be initialized in init()
    
    private long timeout;
    
    @Override
    protected void init(@NonNull Configuration config) throws SetUpException {
        linuxSourceTree = config.getValue(DefaultSettings.SOURCE_TREE);
        String arch = config.getValue(DefaultSettings.ARCH);
        if (arch == null) {
            throw new SetUpException("Config does not contain arch setting");
        }
        this.arch = arch;
        
        config.registerSetting(FIND_LOCATIONS);
        findSourceLocations = config.getValue(FIND_LOCATIONS);
        
        config.registerSetting(DUMPCONF_VERSION);
        dumpconfVersion = config.getValue(DUMPCONF_VERSION);
        
        config.registerSetting(EXTRA_MAKE_PARAMETERS);
        extraMakeParameters = config.getValue(EXTRA_MAKE_PARAMETERS);

        resourceDir = Util.getExtractorResourceDir(config, getClass());
        
        // Use for the process the same timeout as for the calling provider (0 = no timeout)
        config.registerSetting(DefaultSettings.VARIABILITY_PROVIDER_TIMEOUT);
        timeout = config.getValue(DefaultSettings.VARIABILITY_PROVIDER_TIMEOUT);
    }

    @Override
    protected @NonNull VariabilityModel runOnFile(@NonNull File target) throws ExtractorException {
        LOGGER.logDebug("Starting extraction");

        File outputBase = null;

        try {
            KconfigReaderWrapper wrapper = new KconfigReaderWrapper(resourceDir, linuxSourceTree, dumpconfVersion);
            wrapper.setExtraMakeParameters(extraMakeParameters);

            boolean makeSuccess = wrapper.prepareLinux();
            if (!makeSuccess) {
                throw new ExtractorException("'make allyesconfig prepare' failed to execute");
            }
            
            File dumpconfExe = wrapper.compileDumpconf();
            if (dumpconfExe == null) {
                throw new ExtractorException("Compiling dumpconf failed");
            }
            dumpconfExe.deleteOnExit();
            
            outputBase = wrapper.runKconfigReader(dumpconfExe, arch, timeout);
            dumpconfExe.delete();
            
            if (outputBase == null) {
                throw new ExtractorException("KconfigReader run not succesful");
            }
            
        } catch (IOException e) {
            // outputBase can only be null here; no cleanup needed
            
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
            
        } finally {
            deleteAllFiles(outputBase);
        }
        
        if (findSourceLocations) {
            findSourceLocations(result);
            
            VariabilityModelDescriptor descriptor = result.getDescriptor();
            descriptor.addAttribute(Attribute.SOURCE_LOCATIONS);
        }

        return result;
    }
    
    /**
     * Deletes all files that start with outputBase. This is useful to clean up the temporary output files that
     * KconfigReader creates.
     * 
     * @param outputBase The base name of all files to delete.
     */
    static void deleteAllFiles(@NonNull File outputBase) {
        LOGGER.logDebug("Deleting temporary files at " + outputBase);
        
        File folder = outputBase.getParentFile();
        
        for (File child : folder.listFiles()) {
            if (child.getName().startsWith(outputBase.getName()) && child.isFile()) {
                child.delete();
            }
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
    void findSourceLocations(@NonNull VariabilityModel vm) {
        Map<@NonNull String, VariabilityVariable> vars = vm.getVariableMap();
        lukeFilewalker(vars, notNull(linuxSourceTree));
    }

    /**
     * Use the Source Luke. Recursively ...
     * 
     * @param vars
     *            the variables to find the location for.
     * @param dir
     *            the directory to use for finding the location.
     */
    private void lukeFilewalker(@NonNull Map<@NonNull String, VariabilityVariable> vars, @NonNull File dir) {
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
    private void useTheSourceLuke(@NonNull Map<@NonNull String, VariabilityVariable> vars, @NonNull File file) {

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
                                notNull(linuxSourceTree.toPath().relativize(file.toPath()).toFile()), lineNo));
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
    protected @NonNull String getName() {
        return "KconfigReaderExtractor";
    }

}
