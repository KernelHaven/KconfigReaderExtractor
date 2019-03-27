package net.ssehub.kernel_haven.kconfigreader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor.DumpconfVersion;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.Util;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Methods for running the Linux processes required for KconfigReader.
 * All these methods only work on a Linux system, which has a gcc compiler installed.
 * Needs the resources from the {@link net.ssehub.kernel_haven.kconfigreader.res} package.
 * 
 * @author Adam
 * @author Johannes
 * @author Manu
 */
public class KconfigReaderWrapper {
    
    private static final Logger LOGGER = Logger.get();

    private @NonNull DumpconfVersion dumpconfVersion;
    
    private @NonNull File resourceDir;

    private @NonNull File linuxSourceTree;
    
    private @Nullable List<@NonNull String> extraMakeParameters;
    
    /**
     * Creates a new KconfigReaderWrapper.
     * 
     * @param resourceDir The path to the resource directory of this extractor. Must not be null.
     * @param linuxSourceTree The path to the linux source tree. Must not be <code>null</code>.
     * @param dumpconfVersion The dumpconf.c version to use.
     */
    public KconfigReaderWrapper(@NonNull File resourceDir, @NonNull File linuxSourceTree,
            @NonNull DumpconfVersion dumpconfVersion) {
        this.resourceDir = resourceDir;
        this.linuxSourceTree = linuxSourceTree;
        this.dumpconfVersion = dumpconfVersion;
    }
    
    /**
     * Sets a list of extra parameters to pass to make. These will be inserted between 'make' and
     * 'allyesconfig prepare'.
     * 
     * @param extraMakeParameters The list of extra parameters. <code>null</code> if not needed.
     */
    public void setExtraMakeParameters(@Nullable List<@NonNull String> extraMakeParameters) {
        this.extraMakeParameters = extraMakeParameters;
    }
    
    /**
     * Prepares the Linux source tree by executing <code>make allyesconfig prepare</code> on it.
     * 
     * @return <code>true</code> is succesful; <code>false</code> otherwise.
     * 
     * @throws IOException If executing make fails.
     */
    public boolean prepareLinux() throws IOException {
        LOGGER.logDebug("prepareLinux() called");
        
        List<@NonNull String> parameters = new ArrayList<>(Arrays.asList("make", "allyesconfig", "prepare"));
        if (extraMakeParameters != null) {
            parameters.addAll(1, extraMakeParameters);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(parameters.toArray(new String[0]));
        processBuilder.directory(linuxSourceTree);

        return Util.executeProcess(processBuilder, "make");
    }
        
    
    
    /**
     * Compiles dumpconf against the Linux tree.
     * 
     * @return The compiled dumpconf executable file. <code>null</code> if compilation was not
     *          successful.
     * 
     * @throws IOException If executing dumpconf fails.
     */
    public @Nullable File compileDumpconf() throws IOException {
        LOGGER.logDebug("compileDumpconf() called");
        
        // extract dumpconf.c to temporary file
        File dumpconfSource = new File(resourceDir, dumpconfVersion.getFilename());
        if (!dumpconfSource.isFile()) {
            Util.extractJarResourceToFile("net/ssehub/kernel_haven/kconfigreader/res/" + dumpconfVersion.getFilename(),
                    dumpconfSource);
        }
        File dumpconfExe = File.createTempFile("dumpconf", ".exe");
        
        ProcessBuilder processBuilder = new ProcessBuilder("gcc",
                "-fPIC",
                "-I", linuxSourceTree.getAbsolutePath() + "/scripts/kconfig/",
                "-o", dumpconfExe.getAbsolutePath(),
                linuxSourceTree.getAbsolutePath() + "/scripts/kconfig/zconf.tab.o",
                dumpconfSource.getAbsolutePath()
        );

        boolean success = Util.executeProcess(processBuilder, "gcc");

        return success ? dumpconfExe : null;
    }
    
    /**
     * Runs KconfigReader on the Linux tree.
     * 
     * @param dumpconfExe The compiled dumpconf executable file. Must not be <code>null</code>.
     * @param arch The architecture to analyze. Must not be <code>null</code>.
     * 
     * @return The base path to the output files of KconfigReader. Append ".features", ".dimacs",
     *      etc. for the different output files. <code>null</code> if not successful.
     * 
     * @throws IOException If executing KconfigReader fails.
     */
    public @Nullable File runKconfigReader(File dumpconfExe, String arch)
            throws IOException {
        LOGGER.logDebug("runKconfigReader() called");
        
        // extract jar to run kconfigreader
        File kconfigReaderJar = new File(resourceDir, "kconfigreader.jar");
        if (!kconfigReaderJar.isFile()) {
            Util.extractJarResourceToFile("net/ssehub/kernel_haven/kconfigreader/res/kconfigreader.jar",
                    kconfigReaderJar);
        }
        
        File outputBase = File.createTempFile("kconfigreader_output", "");
        outputBase.delete();
        
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xmx2G", "-cp",
                kconfigReaderJar.getAbsolutePath(), "de.fosd.typechef.kconfig.KConfigReader",
                "--writeDimacs",
                "--fast",
                "--dumpconf", dumpconfExe.getAbsolutePath(),
                linuxSourceTree.getAbsolutePath() + "/Kconfig",
                outputBase.getAbsolutePath());
        
        processBuilder.environment().put("ARCH", arch);
        processBuilder.environment().put("SRCARCH", arch);
        
        boolean success = Util.executeProcess(processBuilder, "KconfigReader");
        
        if (!success) {
            KconfigReaderExtractor.deleteAllFiles(outputBase);
        }
        
        return success ? outputBase : null;
    }
    
    
}
