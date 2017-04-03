package de.uni_hildesheim.sse.kernel_haven.kconfigreader;

import de.uni_hildesheim.sse.kernel_haven.SetUpException;
import de.uni_hildesheim.sse.kernel_haven.config.VariabilityExtractorConfiguration;
import de.uni_hildesheim.sse.kernel_haven.variability_model.IVariabilityExtractorFactory;
import de.uni_hildesheim.sse.kernel_haven.variability_model.IVariabilityModelExtractor;

/**
 * Factory for KconfigReaderExtractor.
 * 
 * @author Adam
 * @author Alice
 */
public class KconfigReaderExtractorFactory implements IVariabilityExtractorFactory {

    @Override
    public IVariabilityModelExtractor create(VariabilityExtractorConfiguration config) throws SetUpException {
        return new KconfigReaderExtractor(config);
    }

}
