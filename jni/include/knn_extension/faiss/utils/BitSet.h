/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <faiss/MetricType.h>
#include <faiss/impl/platform_macros.h>
#include <limits>

using idx_t = faiss::idx_t;
/**
 * This class is used to store parent and child doc id mapping
 *
 * For example, let's say there are two documents with 3 nested field each. Then, lucene store each nested field as
 * individual document with its own doc id. The document ids are assigned as following.
 *
 * 0, 1, 2, 3(parent doc for 0, 1, 2), 4, 5, 6, 7(parent doc for 4, 5, 6)
 *
 * Therefore, we can represent the value in BitSet like 10001000 where parent doc id position is set as 1
 * and child doc id position is set as 0. Finally, by using nextSetBit method, we can find parent ID of a
 * given document ID.
 */
struct BitSet {
    const int NO_MORE_DOCS = std::numeric_limits<int>::max();
    /**
     * Returns the index of the first set bit starting at the index specified.
     * NO_MORE_DOCS is returned if there are no more set bits.
     */
    virtual idx_t next_set_bit(idx_t index) const = 0;
    virtual ~BitSet() = default;
};


/**
 * BitSet of fixed length (numBits), implemented using an array of unit64.
 * See https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/util/FixedBitSet.java
 *
 * Here a block is 64 bit. However, for simplicity let's assume its size is 8 bits.
 * Then, if have an array of 3, 7, and 10, it will be represented in bitmap as follow.
 *            [0]      [1]
 * bitmap: 10001000 00000100
 *
 * for next_set_bit call with 4
 * 1. it looks for bitmap[0]
 * 2. bitmap[0] >> 4
 * 3. count trailing zero of the result from step 2 which is 3
 * 4. return 4(current index) + 3(result from step 3)
 */
struct FixedBitSet : public BitSet {
    // Length of bitmap
    size_t numBits;

    // Pointer to an array of uint64_t
    // Using uint64_t to leverage function __builtin_ctzll which is defined in faiss/impl/platform_macros.h
    uint64_t* bitmap;

    // Duplicated data in int_array will be lost
    FixedBitSet(const int* int_array, const int length);
    idx_t next_set_bit(idx_t index) const;
    ~FixedBitSet();
};
