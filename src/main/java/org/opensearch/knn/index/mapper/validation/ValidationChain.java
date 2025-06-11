/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.validation;

import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

import java.util.List;

/**
 * Chain of responsibility pattern for validation logic.
 * Allows for modular validation rules that can be easily added or removed.
 */
public class ValidationChain {
    
    private final List<ValidationRule> rules;
    
    public ValidationChain(List<ValidationRule> rules) {
        this.rules = rules;
    }
    
    /**
     * Executes all validation rules in the chain
     * 
     * @param builder The field mapper builder to validate
     * @param parserContext The parser context
     * @throws IllegalArgumentException if any validation rule fails
     */
    public void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        for (ValidationRule rule : rules) {
            if (rule.applies(builder, parserContext)) {
                rule.validate(builder, parserContext);
            }
        }
    }
}