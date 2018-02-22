package net.ssehub.kernel_haven.kconfigreader;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.util.Logger;

/**
 * The Class AllTests.
 */
@RunWith(Suite.class)
@SuiteClasses({
    ConverterTest.class,
    KconfigReaderWrapperTest.class,
    KconfigReaderExtractorTest.class,
    })
public class AllTests {
    
    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void beforeClass() {
        Logger.init();
    }
    
}
