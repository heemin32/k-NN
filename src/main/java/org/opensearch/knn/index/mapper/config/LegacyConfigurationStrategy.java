/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

import org.opensearch.Version;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.SpaceType;
import org.opensearch.knn.index.engine.KNNMethodConfigContext;
import org.opensearch.knn.index.mapper.CompressionLevel;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;
import org.opensearch.knn.index.mapper.Mode;

import java.util.Locale;

import static org.opensearch.knn.index.mapper.KNNVectorFieldMapperUtil.createKNNMethodContextFromLegacy;
import static org.opensearch.knn.index.mapper.ModelFieldMapper.UNSET_MODEL_DIMENSION_IDENTIFIER;

/**
 * Strategy for handling legacy KNN vector mapping configurations using index settings
 */
public class LegacyConfigurationStrategy implements MappingConfigurationStrategy {
    
    @Override
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        validateDimensionSet(builder);
        // Legacy configurations inherit validation from the resolved method context
    }
    
    @Override
    public void configure(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        // Setup the initial configuration context
        setupMethodConfigContext(builder, parserContext);
        
        // Create KNNMethodContext from legacy index settings if applicable
        if (useKNNMethodContextFromLegacy(builder, parserContext)) {
            SpaceType resolvedSpaceType = SpaceType.getSpace(builder.topLevelSpaceType.get());
            if (resolvedSpaceType == SpaceType.UNDEFINED) {
                resolvedSpaceType = SpaceType.L2; // Default for legacy
            }
            
            builder.originalParameters.setResolvedKnnMethodContext(
                createKNNMethodContextFromLegacy(
                    parserContext.getSettings(), 
                    parserContext.indexVersionCreated(), 
                    resolvedSpaceType
                )
            );
        }
    }
    
    @Override
    public boolean applies(KNNVectorFieldMapper.Builder builder, Settings settings, Version indexVersion) {
        return builder.originalParameters.isLegacyMapping() && 
               indexVersion.onOrBefore(Version.V_2_17_2);
    }
    
    @Override
    public MappingConfigurationType getConfigurationType() {
        return MappingConfigurationType.LEGACY;
    }
    
    private void validateDimensionSet(KNNVectorFieldMapper.Builder builder) {
        if (builder.dimension.getValue() == UNSET_MODEL_DIMENSION_IDENTIFIER) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Dimension value missing for vector: %s", builder.name())
            );
        }
    }
    
    private void setupMethodConfigContext(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        builder.setKnnMethodConfigContext(
            KNNMethodConfigContext.builder()
                .vectorDataType(builder.originalParameters.getVectorDataType())
                .versionCreated(parserContext.indexVersionCreated())
                .dimension(builder.originalParameters.getDimension())
                .mode(Mode.fromName(builder.originalParameters.getMode()))
                .compressionLevel(CompressionLevel.fromName(builder.originalParameters.getCompressionLevel()))
                .build()
        );
    }
    
    private boolean useKNNMethodContextFromLegacy(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        return parserContext.indexVersionCreated().onOrBefore(Version.V_2_17_2) && 
               builder.originalParameters.isLegacyMapping();
    }
}