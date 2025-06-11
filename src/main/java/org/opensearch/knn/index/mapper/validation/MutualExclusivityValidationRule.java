/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.validation;

import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

import java.util.Locale;

/**
 * Validation rule for ensuring method and model are not both specified
 */
public class MutualExclusivityValidationRule implements ValidationRule {
    
    @Override
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        if (builder.knnMethodContext.get() != null && builder.modelId.get() != null) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Method and model can not be both specified in the mapping: %s", builder.name())
            );
        }
    }
    
    @Override
    public boolean applies(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        return true; // This rule always applies
    }
}