package net.ssehub.kernel_haven.kconfigreader;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.VariabilityExtractorConfiguration;
import net.ssehub.kernel_haven.variability_model.IVariabilityExtractorFactory;
import net.ssehub.kernel_haven.variability_model.IVariabilityModelExtractor;

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
