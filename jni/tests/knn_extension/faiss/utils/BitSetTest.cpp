/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

#include "knn_extension/faiss/utils/BitSet.h"

#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using ::testing::NiceMock;
using ::testing::Return;
using idx_t = faiss::idx_t;

TEST(FixedBitSetTest, BasicAssertions) {
    int ids1[4] = {3, 7, 11, 15};
    FixedBitSet single_block(ids1, 4);

    ASSERT_EQ(3, single_block.next_set_bit(0));
    ASSERT_EQ(3, single_block.next_set_bit(1));
    ASSERT_EQ(3, single_block.next_set_bit(2));
    ASSERT_EQ(3, single_block.next_set_bit(3));
    ASSERT_EQ(7, single_block.next_set_bit(4));
    ASSERT_EQ(7, single_block.next_set_bit(5));
    ASSERT_EQ(7, single_block.next_set_bit(6));
    ASSERT_EQ(7, single_block.next_set_bit(7));
    ASSERT_EQ(11, single_block.next_set_bit(8));
    ASSERT_EQ(11, single_block.next_set_bit(9));
    ASSERT_EQ(11, single_block.next_set_bit(10));
    ASSERT_EQ(11, single_block.next_set_bit(11));
    ASSERT_EQ(15, single_block.next_set_bit(12));
    ASSERT_EQ(15, single_block.next_set_bit(13));
    ASSERT_EQ(15, single_block.next_set_bit(14));
    ASSERT_EQ(15, single_block.next_set_bit(15));
    ASSERT_EQ(single_block.NO_MORE_DOCS, single_block.next_set_bit(16));

    int ids2[2] = {3, 64};
    FixedBitSet double_block(ids2, 2);
    ASSERT_EQ(3, double_block.next_set_bit(0));
    ASSERT_EQ(3, double_block.next_set_bit(1));
    ASSERT_EQ(3, double_block.next_set_bit(2));
    ASSERT_EQ(3, double_block.next_set_bit(3));
    for (int i = 4; i < 64; i++) {
        ASSERT_EQ(64, double_block.next_set_bit(i));
    }
    ASSERT_EQ(double_block.NO_MORE_DOCS, double_block.next_set_bit(65));
}
