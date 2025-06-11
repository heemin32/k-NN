/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

import org.opensearch.Version;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MapperParsingException;
import org.opensearch.knn.index.KNNSettings;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

import java.util.Locale;

import static org.opensearch.knn.index.mapper.ModelFieldMapper.UNSET_MODEL_DIMENSION_IDENTIFIER;

/**
 * Strategy for handling flat (exact search) KNN vector mapping configurations
 */
public class FlatConfigurationStrategy implements MappingConfigurationStrategy {
    
    @Override
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        if (builder.modelId.get() != null || builder.knnMethodContext.get() != null) {
            throw new IllegalArgumentException("Cannot set modelId or method parameters when index.knn setting is false");
        }
        
        validateDimensionSet(builder);
        validateCompressionAndModeNotSet(builder);
    }
    
    @Override
    public void configure(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        // Flat configuration doesn't need additional setup beyond validation
        // No method context or engine resolution needed for exact search
    }
    
    @Override
    public boolean applies(KNNVectorFieldMapper.Builder builder, Settings settings, Version indexVersion) {
        boolean isKNNDisabled = isKNNDisabled(settings);
        boolean isNewIndex = indexVersion.onOrAfter(Version.V_2_17_0);
        return isKNNDisabled && isNewIndex && builder.originalParameters.getResolvedKnnMethodContext() == null;
    }
    
    @Override
    public MappingConfigurationType getConfigurationType() {
        return MappingConfigurationType.FLAT;
    }
    
    private boolean isKNNDisabled(Settings settings) {
        boolean isSettingPresent = KNNSettings.IS_KNN_INDEX_SETTING.exists(settings);
        return !isSettingPresent || !KNNSettings.IS_KNN_INDEX_SETTING.get(settings);
    }
    
    private void validateDimensionSet(KNNVectorFieldMapper.Builder builder) {
        if (builder.dimension.getValue() == UNSET_MODEL_DIMENSION_IDENTIFIER) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Dimension value missing for vector: %s", builder.name())
            );
        }
    }
    
    private void validateCompressionAndModeNotSet(KNNVectorFieldMapper.Builder builder) {
        if (builder.mode.isConfigured() || builder.compressionLevel.isConfigured()) {
            throw new MapperParsingException(
                String.format(
                    Locale.ROOT,
                    "Compression and mode can not be specified in a flat mapping configuration for field: %s",
                    builder.name()
                )
            );
        }
    }
}