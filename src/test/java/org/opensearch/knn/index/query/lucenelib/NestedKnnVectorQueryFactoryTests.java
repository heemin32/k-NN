/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.knn.index.query.lucenelib;

import junit.framework.TestCase;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.DiversifyingChildrenByteKnnVectorQuery;
import org.apache.lucene.search.join.DiversifyingChildrenFloatKnnVectorQuery;

import static org.mockito.Mockito.mock;

public class NestedKnnVectorQueryFactoryTests extends TestCase {
    public void testCreate_whenCalled_thenCreateQuery() {
        String fieldName = "field";
        byte[] byteVectors = new byte[3];
        float[] floatVectors = new float[3];
        int k = 3;
        Query queryFilter = mock(Query.class);
        BitSetProducer parentFilter = mock(BitSetProducer.class);
        boolean isInnerHitQuery = true;

        NestedKnnVectorInnerHitQuery expectedByteQuery = new NestedKnnVectorInnerHitQuery.NestedKnnVectorInnerHitQueryBuilder()
            .internalNestedKnnVectorQuery(new InternalNestedKnnByteVectoryQuery(fieldName, byteVectors, queryFilter, k, parentFilter))
            .queryUtils(null)
            .build();
        assertEquals(
            expectedByteQuery,
            NestedKnnVectorQueryFactory.createNestedKnnVectorQuery(fieldName, byteVectors, k, queryFilter, parentFilter, isInnerHitQuery)
        );

        NestedKnnVectorInnerHitQuery expectedFloatQuery = new NestedKnnVectorInnerHitQuery.NestedKnnVectorInnerHitQueryBuilder()
            .internalNestedKnnVectorQuery(new InternalNestedKnnFloatVectoryQuery(fieldName, floatVectors, queryFilter, k, parentFilter))
            .queryUtils(null)
            .build();
        assertEquals(
            expectedFloatQuery,
            NestedKnnVectorQueryFactory.createNestedKnnVectorQuery(fieldName, floatVectors, k, queryFilter, parentFilter, isInnerHitQuery)
        );
    }

    public void testCreate_whenNoInnerHit_thenDiversifyingQuery() {
        String fieldName = "field";
        byte[] byteVectors = new byte[3];
        float[] floatVectors = new float[3];
        int k = 3;
        Query queryFilter = mock(Query.class);
        BitSetProducer parentFilter = mock(BitSetProducer.class);
        boolean isInnerHitQuery = false;

        assertEquals(
            DiversifyingChildrenByteKnnVectorQuery.class,
            NestedKnnVectorQueryFactory.createNestedKnnVectorQuery(fieldName, byteVectors, k, queryFilter, parentFilter, isInnerHitQuery)
                .getClass()
        );

        assertEquals(
            DiversifyingChildrenFloatKnnVectorQuery.class,
            NestedKnnVectorQueryFactory.createNestedKnnVectorQuery(fieldName, floatVectors, k, queryFilter, parentFilter, isInnerHitQuery)
                .getClass()
        );
    }
}
