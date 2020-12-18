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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
    private static final String MISSING_KCONFIG_FILE = "assertion failed: kconfig file does not exist";
    private static final String OBSOLETE_KCONFIG_GRAMMAR_ENV = "P_ENV";
    private static final String OBSOLETE_KCONFIG_GRAMMAR_LIST = "E_LIST";
    private static final String OBSOLETE_MAKE_SYNTAX = "*** mixed implicit and normal rules: deprecated syntax";

    private @NonNull DumpconfVersion dumpconfVersion;
    
    private @NonNull File resourceDir;

    private @NonNull File linuxSourceTree;
    
    private @NonNull List<@NonNull String> extraMakeParameters;
    
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
        this.extraMakeParameters = new LinkedList<>();
    }
    
    /**
     * Sets a list of extra parameters to pass to make. These will be inserted between 'make' and
     * 'allyesconfig prepare'.
     * 
     * @param extraMakeParameters The list of extra parameters. Empty if not needed.
     */
    public void setExtraMakeParameters(@NonNull List<@NonNull String> extraMakeParameters) {
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
        if (!extraMakeParameters.isEmpty()) {
            parameters.addAll(1, extraMakeParameters);
        }
        ProcessBuilder processBuilder = createPrepareProcess(parameters);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        boolean success = Util.executeProcess(processBuilder, "make", stdout, stderr, 0);
        
        if (!success && dumpconfVersion == DumpconfVersion.LINUX) {
            // Old Linux versions may use an obsolete MAKE syntax, try to fix them according to
            // https://www.programmersought.com/article/88083080956/
            if (checkForOccurence(stdout, stderr, OBSOLETE_MAKE_SYNTAX)) {
                LOGGER.logInfo2("Linux preparation crashed since the Makefile uses a deprecated syntax, "
                    + "try to fix it.");
                
                File makeFile = new File(linuxSourceTree, "Makefile");
                String content = new String(Files.readAllBytes(makeFile.toPath()));
                String modifiedContent = content.replace("config %config:", "%config %config:");
                modifiedContent = modifiedContent.replace("/ %/: prepare scripts FORCE", "%/: prepare scripts FORCE");
                Files.write(makeFile.toPath(), modifiedContent.getBytes());
                
                processBuilder = createPrepareProcess(parameters);
                try {
                    success = Util.executeProcess(processBuilder, "make");
                } catch (IOException e) {
                    // Restore original content
                    Files.write(makeFile.toPath(), content.getBytes());
                    throw e;
                }
                
                // Restore original content
                Files.write(makeFile.toPath(), content.getBytes());
            }
        }

        return Util.executeProcess(processBuilder, "make");
    }
    
    /**
     * Creates a {@link ProcessBuilder} to prepare Linux, which is required to be able to compile dumpconf.
     * @param parameters The command to execute
     * @return The {@link ProcessBuilder} to create the process.
     */
    private @NonNull ProcessBuilder createPrepareProcess(List<@NonNull String> parameters) {
        ProcessBuilder processBuilder = new ProcessBuilder(parameters.toArray(new String[0]));
        processBuilder.directory(linuxSourceTree);
        
        return processBuilder;
    }
    
    /**
     * Checks that all searches <tt>elements</tt> are entailed either in <tt>stdout</tt> or <tt>stderr</tt>.
     * @param stdout The standard output of an executed process.
     * @param stderr The error output of an executed process.
     * @param elements The elements to search
     * @return <tt>true</tt> if all elements where found or if elements was empty, <tt>false</tt> otherwise.
     */
    private boolean checkForOccurence(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr, String... elements) {
        String out = stdout.toString();
        String err = stderr.toString();
        boolean result = true;
        if (null != elements && elements.length > 0) {
            for (int i = 0; i < elements.length && result; i++) {
                result = out.contains(elements[i]) || err.contains(elements[i]);
            }
        }
        
        return result;
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
        File dumpconfSource = new File(resourceDir, "dumpconf.c");
        if (!dumpconfSource.isFile()) {
            Util.extractJarResourceToFile("net/ssehub/kernel_haven/kconfigreader/res/dumpconf.c", dumpconfSource);
        }
        File dumpconfExe = File.createTempFile("dumpconf", ".exe");
        
        ProcessBuilder processBuilder = createCompilationProcess(dumpconfSource, dumpconfExe, dumpconfVersion);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        
        boolean success = Util.executeProcess(processBuilder, "gcc", stdout, stderr, 0);
        if (!success && dumpconfVersion == DumpconfVersion.LINUX) {
            // Old Linux versions may use an old Kconfig grammar, not supported by the the Linux version of dumpconf
            if (checkForOccurence(stdout, stderr, OBSOLETE_KCONFIG_GRAMMAR_ENV, OBSOLETE_KCONFIG_GRAMMAR_LIST)) {
                LOGGER.logInfo2("Dumpconf compilation crashed since the Kconfig uses an old syntax, "
                    + "try to fix it.");
                
                // The old grammar is still used by Busybox -> Try to use its Dumpconf version
                processBuilder = createCompilationProcess(dumpconfSource, dumpconfExe, DumpconfVersion.BUSYBOX);
                success = Util.executeProcess(processBuilder, "make");
            }
        }
        
        if (!success) {
            dumpconfExe.delete();
        }

        return success ? dumpconfExe : null;
    }

    /**
     * Creates a {@link ProcessBuilder} to compile dumpconf.
     * @param dumpconfSource The source code file to be compiled
     * @param dumpconfExe The destination path of the compiled executable
     * @param dumpconfVersion The version (preprocessor definitions) to sue for the compilation.
     * 
     * @return The {@link ProcessBuilder} to create the process.
     */
    private @NonNull ProcessBuilder createCompilationProcess(File dumpconfSource, File dumpconfExe,
        DumpconfVersion dumpconfVersion) {
        
        ProcessBuilder processBuilder = new ProcessBuilder("gcc",
            "-D" + dumpconfVersion.getCompileFlag(),
            "-fPIC",
            "-I", linuxSourceTree.getAbsolutePath() + "/scripts/kconfig/",
            "-o", dumpconfExe.getAbsolutePath(),
            linuxSourceTree.getAbsolutePath() + "/scripts/kconfig/zconf.tab.o",
            dumpconfSource.getAbsolutePath()
        );
        
        return processBuilder;
    }
    
    /**
     * Runs KconfigReader on the Linux tree.
     * 
     * @param dumpconfExe The compiled dumpconf executable file. Must not be <code>null</code>.
     * @param arch The architecture to analyze. Must not be <code>null</code>.
     * @param timeout An optional timeout (in ms) for the process to stop (0 = no timeout).
     * 
     * @return The base path to the output files of KconfigReader. Append ".features", ".dimacs",
     *      etc. for the different output files. <code>null</code> if not successful.
     * 
     * @throws IOException If executing KconfigReader fails.
     */
    public @Nullable File runKconfigReader(File dumpconfExe, String arch, long timeout)
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
        
        ProcessBuilder processBuilder = createKcReaderProcess(dumpconfExe, arch, kconfigReaderJar, outputBase);       
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        
        boolean success = Util.executeProcess(processBuilder, "KconfigReader", stdout, stderr, timeout);
        
        if (!success && dumpconfVersion == DumpconfVersion.LINUX) {
            // Old Linux versions may not have a top level Kconfig file, check if this was the case
            if (checkForOccurence(stdout, stderr, MISSING_KCONFIG_FILE)) {
                LOGGER.logInfo2("KconfigReader crashed since there is no Kconfig file in root directory, "
                    + "copying Kconfig file from arch directory to root.");
                
                // Copy Kconfig file from arch folder and try again
                File kconfigSrc = new File(linuxSourceTree, "arch/" + arch + "/Kconfig");
                File kconfigTrg = new File(linuxSourceTree, "Kconfig");
                boolean copied = false;
                if (kconfigSrc.exists() && !kconfigTrg.exists()) {
                    try {
                        Files.copy(kconfigSrc.toPath(), kconfigTrg.toPath());
                    } catch (IOException e) {
                        LOGGER.logException("Could not copy " + kconfigSrc.getAbsolutePath() + " to " + kconfigTrg, e);
                    }
                    
                    copied = kconfigTrg.exists();
                }
                
                if (copied) {
                    // Try again
                    try {
                        processBuilder = createKcReaderProcess(dumpconfExe, arch, kconfigReaderJar, outputBase);
                        success = Util.executeProcess(processBuilder, "KconfigReader", timeout);
                    } catch (IOException e) {
                        // Clean-up copied file
                        kconfigTrg.delete();
                        throw e;
                    }
                    
                    // Clean-up copied file
                    kconfigTrg.delete();
                }
            }
        }
        
        
        if (!success) {
            KconfigReaderExtractor.deleteAllFiles(outputBase);
        }
        
        return success ? outputBase : null;
    }

    /**
     * Creates a {@link ProcessBuilder} to execute KconfigReader.
     * @param dumpconfExe The pre-compiled dumpconf to be used ({@link #compileDumpconf()}).
     * @param arch The architecture to extract.
     * @param kconfigReaderJar The KconfigReader executable to use.
     * @param outputBase The destination folder of the produced output
     * @return The {@link ProcessBuilder} to create the process.
     */
    private @NonNull ProcessBuilder createKcReaderProcess(File dumpconfExe, String arch, File kconfigReaderJar,
        File outputBase) {
        
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xmx2G", "-cp",
            kconfigReaderJar.getAbsolutePath(), "de.fosd.typechef.kconfig.KConfigReader",
            "--writeDimacs",
            "--fast",
            "--dumpconf", dumpconfExe.getAbsolutePath(),
            linuxSourceTree.getAbsolutePath() + "/Kconfig",
            outputBase.getAbsolutePath());
        
        processBuilder.environment().put("ARCH", arch);
        processBuilder.environment().put("SRCARCH", arch);
        
        return processBuilder;
    }
    
    
}
