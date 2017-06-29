package net.ssehub.kernel_haven.kconfigreader;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
    // runs tests defined in SuiteClasses
}
