/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

import org.opensearch.Version;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MapperParsingException;
import org.opensearch.knn.index.SpaceType;
import org.opensearch.knn.index.VectorDataType;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

import java.util.Locale;

import static org.opensearch.knn.index.mapper.ModelFieldMapper.UNSET_MODEL_DIMENSION_IDENTIFIER;

/**
 * Strategy for handling model-based KNN vector mapping configurations
 */
public class ModelBasedConfigurationStrategy implements MappingConfigurationStrategy {
    
    @Override
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        // Dimension should not be null unless modelId is used
        if (builder.dimension.getValue() == UNSET_MODEL_DIMENSION_IDENTIFIER && builder.modelId.get() == null) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Dimension value missing for vector: %s", builder.name())
            );
        }
        
        // ensure model and top level spaceType is not defined
        if (builder.modelId.get() != null && SpaceType.getSpace(builder.topLevelSpaceType.get()) != SpaceType.UNDEFINED) {
            throw new IllegalArgumentException("TopLevel Space type and model can not be both specified in the mapping");
        }
        
        validateCompressionAndModeNotSet(builder);
    }
    
    @Override
    public void configure(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        // Model-based configuration doesn't need additional setup beyond validation
        // The model metadata will be resolved at field mapper creation time
    }
    
    @Override
    public boolean applies(KNNVectorFieldMapper.Builder builder, Settings settings, Version indexVersion) {
        return builder.modelId.get() != null;
    }
    
    @Override
    public MappingConfigurationType getConfigurationType() {
        return MappingConfigurationType.MODEL;
    }
    
    private void validateCompressionAndModeNotSet(KNNVectorFieldMapper.Builder builder) {
        if (builder.mode.isConfigured() || builder.compressionLevel.isConfigured()) {
            throw new MapperParsingException(
                String.format(
                    Locale.ROOT,
                    "Compression and mode can not be specified in a model mapping configuration for field: %s",
                    builder.name()
                )
            );
        }
    }
}