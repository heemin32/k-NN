/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.query.lucenelib;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.DiversifyingChildrenByteKnnVectorQuery;
import org.apache.lucene.search.join.DiversifyingChildrenFloatKnnVectorQuery;
import org.opensearch.knn.index.query.common.QueryUtils;

/**
 * A class to create a nested knn vector query for lucene
 */
public class NestedKnnVectorQueryFactory {
    /**
     * QueryUtils does not have internal state. Therefore, it is safe to be shared.
     * Defining here as static so that we don't create QueryUtils instance for every query.
     */
    private static final QueryUtils QUERY_UTILS = new QueryUtils();

    /**
     * Create a query for k-NN nested field.
     *
     * The query is generated two times when inner_hits() parameter exist in the request.
     * For inner hit, we return all filtered nested field documents belongs to the final result of parent documents.
     *
     * @param fieldName field name for search
     * @param vector target vector for search
     * @param k k nearest neighbor for search
     * @param filterQuery efficient filtering query
     * @param parentFilter has mapping data between parent doc and child doc
     * @param isInnerHit tells if this query is for innerHit block
     * @return Query for k-NN nested field
     */
    public static Query createNestedKnnVectorQuery(
        final String fieldName,
        final byte[] vector,
        final int k,
        final Query filterQuery,
        final BitSetProducer parentFilter,
        final boolean isInnerHit
    ) {
        if (isInnerHit) {
            return new NestedKnnVectorInnerHitQuery.NestedKnnVectorInnerHitQueryBuilder().internalNestedKnnVectorQuery(
                new InternalNestedKnnByteVectoryQuery(fieldName, vector, filterQuery, k, parentFilter)
            ).queryUtils(QUERY_UTILS).build();
        }
        return new DiversifyingChildrenByteKnnVectorQuery(fieldName, vector, filterQuery, k, parentFilter);
    }

    /**
     * Create a query for k-NN nested field.
     *
     * The query is generated two times when inner_hits() parameter exist in the request.
     * For inner hit, we return all filtered nested field documents belongs to the final result of parent documents.
     *
     * @param fieldName field name for search
     * @param vector target vector for search
     * @param k k nearest neighbor for search
     * @param filterQuery efficient filtering query
     * @param parentFilter has mapping data between parent doc and child doc
     * @param isInnerHit tells if this query is for innerHit block
     * @return Query for k-NN nested field
     */
    public static Query createNestedKnnVectorQuery(
        final String fieldName,
        final float[] vector,
        final int k,
        final Query filterQuery,
        final BitSetProducer parentFilter,
        final boolean isInnerHit
    ) {
        if (isInnerHit) {
            return new NestedKnnVectorInnerHitQuery.NestedKnnVectorInnerHitQueryBuilder().internalNestedKnnVectorQuery(
                new InternalNestedKnnFloatVectoryQuery(fieldName, vector, filterQuery, k, parentFilter)
            ).queryUtils(QUERY_UTILS).build();
        }
        return new DiversifyingChildrenFloatKnnVectorQuery(fieldName, vector, filterQuery, k, parentFilter);
    }
}
