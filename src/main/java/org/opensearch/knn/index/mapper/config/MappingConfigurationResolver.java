/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

import org.opensearch.Version;
import org.opensearch.common.settings.Settings;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.knn.index.mapper.KNNVectorFieldMapper;

import java.util.Arrays;
import java.util.List;

/**
 * Resolver class that determines which mapping configuration strategy to use
 * and applies the appropriate validation and configuration logic.
 */
public class MappingConfigurationResolver {
    
    private final List<MappingConfigurationStrategy> strategies;
    
    public MappingConfigurationResolver() {
        this.strategies = Arrays.asList(
            new ModelBasedConfigurationStrategy(),
            new FlatConfigurationStrategy(),
            new LegacyConfigurationStrategy(),
            new MethodBasedConfigurationStrategy() // Default fallback
        );
    }
    
    /**
     * Resolves and applies the appropriate mapping configuration strategy
     * 
     * @param builder The field mapper builder
     * @param parserContext The parser context
     * @throws IllegalArgumentException if no strategy applies or validation fails
     */
    public void resolveAndConfigure(KNNVectorFieldMapper.Builder builder, Mapper.TypeParser.ParserContext parserContext) {
        Settings settings = parserContext.getSettings();
        Version indexVersion = parserContext.indexVersionCreated();
        
        MappingConfigurationStrategy selectedStrategy = strategies.stream()
            .filter(strategy -> strategy.applies(builder, settings, indexVersion))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No applicable mapping configuration strategy found for field: " + builder.name()
            ));
        
        // Apply strategy-specific configuration first
        selectedStrategy.configure(builder, parserContext);
        
        // Then validate the configuration
        selectedStrategy.validate(builder, parserContext);
    }
    
    /**
     * Gets the configuration type for the given builder and context
     * 
     * @param builder The field mapper builder
     * @param parserContext The parser context
     * @return The mapping configuration type, or null if none applies
     */
    public MappingConfigurationType getConfigurationType(
        KNNVectorFieldMapper.Builder builder, 
        Mapper.TypeParser.ParserContext parserContext
    ) {
        Settings settings = parserContext.getSettings();
        Version indexVersion = parserContext.indexVersionCreated();
        
        return strategies.stream()
            .filter(strategy -> strategy.applies(builder, settings, indexVersion))
            .findFirst()
            .map(MappingConfigurationStrategy::getConfigurationType)
            .orElse(null);
    }
}