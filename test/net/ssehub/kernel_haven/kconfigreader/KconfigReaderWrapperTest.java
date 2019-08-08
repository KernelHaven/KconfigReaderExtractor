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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor.DumpconfVersion;
import net.ssehub.kernel_haven.test_utils.RunOnlyOnLinux;
import net.ssehub.kernel_haven.util.Util;

/**
 * Tests the KconfigReaderWrapper. These tests can only run on Linux, since
 * KconfigReader can only run on Linux.
 * 
 * @author Adam
 * @author Johannes
 * @author Manu
 */
@SuppressWarnings("null")
@RunWith(RunOnlyOnLinux.class)
public class KconfigReaderWrapperTest {

    private static final File LINUX_DIR = new File("testdata/pseudoLinux/");

    private static final File RESOURCE_DIR = new File("testdata/tmp_res");

    private KconfigReaderWrapper wrapper;

    /**
     * Creates a new wrapper object for each test and creates the resource dir.
     */
    @Before
    public void setUp() {
        RESOURCE_DIR.mkdir();
        wrapper = new KconfigReaderWrapper(RESOURCE_DIR, LINUX_DIR, DumpconfVersion.LINUX);
    }

    /**
     * Deletes the resource directory after each test.
     * 
     * @throws IOException If deleting fails. 
     */
    @After
    public void tearDown() throws IOException {
        Util.deleteFolder(RESOURCE_DIR);
    }

    /**
     * Tests if dumpconf.exe is created by compileDumpconf and if the file is
     * executable.
     * 
     * @throws IOException
     *             unwanted.
     */
    @Test
    public void testCompileDumpconf() throws IOException {
        File dumpconfExe = wrapper.compileDumpconf();

        assertThat(dumpconfExe.isFile(), is(true));
        assertThat(dumpconfExe.canExecute(), is(true));

        dumpconfExe.delete();
    }

    /**
     * Tests if dumpconf.exe is created by compileDumpconf and if the file is
     * executable.
     * 
     * @throws IOException
     *             unwanted.
     */
    @Test
    public void testRunKconfigReader() throws IOException {
        File dumpconfExe = new File("testdata/dumpconf");

        // prerequisites
        dumpconfExe.setExecutable(true);
        assertThat(dumpconfExe.canExecute(), is(true));

        File basepath = wrapper.runKconfigReader(dumpconfExe, "x86", 0);
        assertThat(basepath, notNullValue());

        // features check
        Set<String> allowedSet = new HashSet<>();
        allowedSet.add("CONFIG_A");
        allowedSet.add("CONFIG_B");
        allowedSet.add("CONFIG_C");
        allowedSet.add("CONFIG_C_MODULE");

        File modules = new File(basepath.getAbsoluteFile() + ".features");
        assertThat(modules.isFile(), is(true));
        BufferedReader br = new BufferedReader(new FileReader(modules));

        String line = br.readLine();
        while (line != null) {
            assertThat(allowedSet.contains(line), is(true));
            line = br.readLine();
        }

        br.close();

        // dimacs check
        File dimacs = new File(basepath.getAbsoluteFile() + ".dimacs");
        assertThat(dimacs.isFile(), is(true));

        // TODO: check if dimacs is valid

        // clean up dimacs
        File[] files = modules.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(basepath.getName());
            }
        });

        for (File file : files) {
            file.delete();
        }
    }
}
