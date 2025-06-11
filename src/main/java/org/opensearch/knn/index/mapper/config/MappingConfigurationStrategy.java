/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

import org.opensearch.Version;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

/**
 * Strategy interface for handling different KNN vector mapping configurations.
 * Each implementation handles a specific configuration type (model, method, flat, legacy).
 */
public interface MappingConfigurationStrategy {
    
    /**
     * Validates the mapping configuration for this strategy type
     * 
     * @param builder The field mapper builder
     * @param parserContext The parser context
     * @throws IllegalArgumentException if validation fails
     */
    void validate(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext);
    
    /**
     * Configures the builder with strategy-specific settings
     * 
     * @param builder The field mapper builder
     * @param parserContext The parser context
     */
    void configure(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext);
    
    /**
     * Determines if this strategy applies to the given builder configuration
     * 
     * @param builder The field mapper builder
     * @param settings The index settings
     * @param indexVersion The index creation version
     * @return true if this strategy should be used
     */
    boolean applies(KNNVectorFieldMapper.Builder builder, Settings settings, Version indexVersion);
    
    /**
     * Returns the configuration type this strategy handles
     * 
     * @return The mapping configuration type
     */
    MappingConfigurationType getConfigurationType();
}