/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.validation;

import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

/**
 * Interface for validation rules in the validation chain
 */
public interface ValidationRule {
    
    /**
     * Validates the builder configuration
     * 
     * @param builder The field mapper builder
     * @param parserContext The parser context
     * @throws IllegalArgumentException if validation fails
     */
    void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext);
    
    /**
     * Determines if this validation rule applies to the given configuration
     * 
     * @param builder The field mapper builder
     * @param parserContext The parser context
     * @return true if this rule should be applied
     */
    boolean applies(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext);
}