/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.validation;

import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.MapperParsingException;
import org.opensearch.knn.index.SpaceType;
import org.opensearch.knn.index.engine.KNNMethodContext;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

/**
 * Validation rule for space type consistency between top-level and method-level configurations
 */
public class SpaceTypeValidationRule implements ValidationRule {
    
    @Override
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        final KNNMethodContext knnMethodContext = builder.knnMethodContext.get();
        
        if (knnMethodContext != null) {
            final SpaceType knnMethodContextSpaceType = knnMethodContext.getSpaceType();
            final SpaceType topLevelSpaceType = SpaceType.getSpace(builder.topLevelSpaceType.get());
            
            if (topLevelSpaceType != SpaceType.UNDEFINED
                && topLevelSpaceType != knnMethodContextSpaceType
                && knnMethodContextSpaceType != SpaceType.UNDEFINED) {
                throw new MapperParsingException(
                    "Space type in \"method\" and top level space type should be same or one of them should be defined"
                );
            }
        }
    }
    
    @Override
    public boolean applies(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        return builder.knnMethodContext.get() != null;
    }
}