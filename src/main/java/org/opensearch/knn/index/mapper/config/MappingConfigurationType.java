/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.mapper.config;

/**
 * Enum defining the different types of KNN vector mapping configurations
 */
public enum MappingConfigurationType {
    /**
     * Configuration based on a pre-trained model
     */
    MODEL,
    
    /**
     * Configuration with explicit method parameters
     */
    METHOD,
    
    /**
     * Flat configuration for exact search (no approximation)
     */
    FLAT,
    
    /**
     * Legacy configuration using index settings
     */
    LEGACY
}