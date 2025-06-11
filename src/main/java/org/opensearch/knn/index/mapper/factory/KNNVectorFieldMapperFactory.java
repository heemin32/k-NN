/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.factory;

import org.opensearch.Version;
import org.opensearch.index.mapper.BuilderContext;
import org.opensearch.knn.index.engine.KNNEngine;
import org.opensearch.knn.index.engine.KNNMethodConfigContext;
import org.opensearch.knn.index.mapper.FlatVectorFieldMapper;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;
import org.opensearch.knn.index.mapper.LuceneFieldMapper;
import org.opensearch.knn.index.mapper.MethodFieldMapper;
import org.opensearch.knn.index.mapper.ModelFieldMapper;
import org.opensearch.knn.index.mapper.config.MappingConfigurationType;

/**
 * Factory class for creating the appropriate KNNVectorFieldMapper instances
 * based on the configuration type and engine.
 */
public class KNNVectorFieldMapperFactory {
    
    /**
     * Creates the appropriate field mapper based on the builder configuration
     * 
     * @param builder The builder containing the field configuration
     * @param context The build context
     * @param configurationType The type of configuration being used
     * @return The appropriate KNNVectorFieldMapper implementation
     */
    public static KNNVectorFieldMapper createFieldMapper(
        KNNVectorFieldMapper.Builder builder,
        BuilderContext context,
        MappingConfigurationType configurationType
    ) {
        String fullName = builder.buildFullName(context);
        
        // Model-based mapper
        if (builder.modelId.get() != null) {
            return ModelFieldMapper.createFieldMapper(
                fullName,
                builder.name,
                builder.meta.getValue(),
                builder.vectorDataType.getValue(),
                builder.multiFieldsBuilder.build(builder, context),
                builder.copyTo.build(),
                builder.ignoreMalformed(context),
                builder.stored.get(),
                builder.hasDocValues.get(),
                builder.modelDao,
                builder.indexCreatedVersion,
                builder.originalParameters,
                builder.knnMethodConfigContext
            );
        }
        
        // Flat vector mapper for new indices without method context
        if (configurationType == MappingConfigurationType.FLAT) {
            return FlatVectorFieldMapper.createFieldMapper(
                fullName,
                builder.name,
                builder.meta.getValue(),
                KNNMethodConfigContext.builder()
                    .vectorDataType(builder.vectorDataType.getValue())
                    .versionCreated(builder.indexCreatedVersion)
                    .dimension(builder.dimension.getValue())
                    .build(),
                builder.multiFieldsBuilder.build(builder, context),
                builder.copyTo.build(),
                builder.ignoreMalformed(context),
                builder.stored.get(),
                builder.hasDocValues.get(),
                builder.originalParameters
            );
        }
        
        // Lucene-based mapper
        if (builder.originalParameters.getResolvedKnnMethodContext().getKnnEngine() == KNNEngine.LUCENE) {
            LuceneFieldMapper.CreateLuceneFieldMapperInput createLuceneFieldMapperInput = 
                LuceneFieldMapper.CreateLuceneFieldMapperInput.builder()
                    .name(builder.name)
                    .multiFields(builder.multiFieldsBuilder.build(builder, context))
                    .copyTo(builder.copyTo.build())
                    .ignoreMalformed(builder.ignoreMalformed(context))
                    .stored(builder.stored.getValue())
                    .hasDocValues(builder.hasDocValues.getValue())
                    .originalKnnMethodContext(builder.knnMethodContext.get())
                    .build();
            return LuceneFieldMapper.createFieldMapper(
                fullName,
                builder.meta.getValue(),
                builder.knnMethodConfigContext,
                createLuceneFieldMapperInput,
                builder.originalParameters
            );
        }
        
        // Default method-based mapper
        return MethodFieldMapper.createFieldMapper(
            fullName,
            builder.name(),
            builder.meta.getValue(),
            builder.knnMethodConfigContext,
            builder.multiFieldsBuilder.build(builder, context),
            builder.copyTo.build(),
            builder.ignoreMalformed(context),
            builder.stored.getValue(),
            builder.hasDocValues.getValue(),
            builder.originalParameters
        );
    }
}