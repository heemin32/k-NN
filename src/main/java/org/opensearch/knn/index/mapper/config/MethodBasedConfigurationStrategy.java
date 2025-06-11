/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

import org.opensearch.Version;
import org.opensearch.common.ValidationException;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.SpaceType;
import org.opensearch.knn.index.VectorDataType;
import org.opensearch.knn.index.engine.EngineResolver;
import org.opensearch.knn.index.engine.KNNEngine;
import org.opensearch.knn.index.engine.KNNMethodConfigContext;
import org.opensearch.knn.index.engine.ResolvedMethodContext;
import org.opensearch.knn.index.engine.SpaceTypeResolver;
import org.opensearch.knn.index.mapper.CompressionLevel;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;
import org.opensearch.index.mapper.MapperParsingException;
import org.opensearch.knn.index.mapper.Mode;

import java.util.Locale;

import static org.opensearch.knn.common.KNNConstants.KNN_METHOD;
import static org.opensearch.knn.index.mapper.ModelFieldMapper.UNSET_MODEL_DIMENSION_IDENTIFIER;

/**
 * Strategy for handling method-based KNN vector mapping configurations
 */
public class MethodBasedConfigurationStrategy implements MappingConfigurationStrategy {
    
    @Override
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        ValidationException validationException;
        
        if (builder.originalParameters.getResolvedKnnMethodContext().isTrainingRequired()) {
            validationException = new ValidationException();
            validationException.addValidationError(String.format(Locale.ROOT, "\"%s\" requires training.", KNN_METHOD));
            throw validationException;
        }
        
        if (builder.originalParameters.getResolvedKnnMethodContext() != null) {
            validationException = builder.originalParameters.getResolvedKnnMethodContext()
                .validate(builder.knnMethodConfigContext);
            if (validationException != null) {
                throw validationException;
            }
        }
        
        validateDimensionSet(builder);
        validateModeAndCompressionForDataType(builder);
    }
    
    @Override
    public void configure(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        // Resolve space type
        SpaceType resolvedSpaceType = SpaceTypeResolver.INSTANCE.resolveSpaceType(
            builder.originalParameters.getKnnMethodContext(),
            builder.vectorDataType.get(),
            builder.topLevelSpaceType.get()
        );
        setSpaceType(builder.originalParameters.getKnnMethodContext(), resolvedSpaceType);
        
        // Setup method configuration context
        setupMethodConfigContext(builder, parserContext);
        
        // Resolve KNN method components
        resolveKNNMethodComponents(builder, parserContext, resolvedSpaceType);
    }
    
    @Override
    public boolean applies(KNNVectorFieldMapper.Builder builder, Settings settings, Version indexVersion) {
        return builder.modelId.get() == null && 
               (builder.originalParameters.getResolvedKnnMethodContext() != null ||
                builder.knnMethodContext.get() != null);
    }
    
    @Override
    public MappingConfigurationType getConfigurationType() {
        return MappingConfigurationType.METHOD;
    }
    
    private void validateDimensionSet(KNNVectorFieldMapper.Builder builder) {
        if (builder.dimension.getValue() == UNSET_MODEL_DIMENSION_IDENTIFIER) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Dimension value missing for vector: %s", builder.name())
            );
        }
    }
    
    private void validateModeAndCompressionForDataType(KNNVectorFieldMapper.Builder builder) {
        boolean isModeOrCompressionConfigured = builder.mode.isConfigured() || builder.compressionLevel.isConfigured();
        if (isModeOrCompressionConfigured && builder.vectorDataType.getValue() != VectorDataType.FLOAT) {
            throw new MapperParsingException(
                String.format(
                    Locale.ROOT, 
                    "Compression and mode cannot be used for non-float32 data type for field %s", 
                    builder.name()
                )
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
    
    private void resolveKNNMethodComponents(
        KNNVectorFieldMapper.Builder builder,
        Mapper.TypeParser.ParserContext parserContext,
        SpaceType resolvedSpaceType
    ) {
        // Based on config context, if the user does not set the engine, set it
        KNNEngine resolvedKNNEngine = EngineResolver.INSTANCE.resolveEngine(
            builder.knnMethodConfigContext,
            builder.originalParameters.getResolvedKnnMethodContext(),
            false
        );
        setEngine(builder.originalParameters.getResolvedKnnMethodContext(), resolvedKNNEngine);
        
        // Create a copy of the KNNMethodContext and fill in the parameters left blank by configuration context
        ResolvedMethodContext resolvedMethodContext = resolvedKNNEngine.resolveMethod(
            builder.originalParameters.getResolvedKnnMethodContext(),
            builder.knnMethodConfigContext,
            false,
            resolvedSpaceType
        );
        
        // Update the resolved method context in original parameters
        builder.originalParameters.setResolvedKnnMethodContext(resolvedMethodContext.getKnnMethodContext());
        builder.knnMethodConfigContext.setCompressionLevel(resolvedMethodContext.getCompressionLevel());
    }
    
    private void setSpaceType(org.opensearch.knn.index.engine.KNNMethodContext knnMethodContext, SpaceType spaceType) {
        if (knnMethodContext == null) {
            return;
        }
        knnMethodContext.setSpaceType(spaceType);
    }
    
    private void setEngine(org.opensearch.knn.index.engine.KNNMethodContext knnMethodContext, KNNEngine knnEngine) {
        if (knnMethodContext == null || knnMethodContext.isEngineConfigured()) {
            return;
        }
        knnMethodContext.setKnnEngine(knnEngine);
    }
}