/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.knn.training;

import org.apache.commons.lang.ArrayUtils;
import org.opensearch.knn.index.IndexUtil;
import org.opensearch.knn.index.VectorDataType;
import org.opensearch.knn.jni.JNICommons;
import org.opensearch.knn.jni.JNIService;
import org.opensearch.knn.index.memory.NativeMemoryAllocation;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

/**
 * Transfers vectors from JVM to native memory.
 */
public class TrainingDataConsumer implements Consumer<List<Float[]>> {

    private final NativeMemoryAllocation.TrainingDataAllocation trainingDataAllocation;
    private final VectorDataType vectorDataType;

    /**
     * Constructor
     *
     * @param trainingDataAllocation NativeMemoryAllocation that contains information about native memory allocation.
     */
    public TrainingDataConsumer(NativeMemoryAllocation.TrainingDataAllocation trainingDataAllocation, VectorDataType vectorDataType) {
        this.trainingDataAllocation = trainingDataAllocation;
        this.vectorDataType = vectorDataType;
    }

    @Override
    public void accept(List<Float[]> floats) {
        long memoryAddress = trainingDataAllocation.getMemoryAddress();

        if (IndexUtil.isBinaryIndex(this.vectorDataType)) {
            byte[][] byteArray = floats.stream()
                .map(ArrayUtils::toPrimitive)
                .map(this::convertFloatArrayToByteArray)
                .toArray(byte[][]::new);

            memoryAddress = JNICommons.storeByteVectorData(memoryAddress, byteArray, byteArray.length);
        } else {
            float[][] primitiveArray = floats.stream().map(ArrayUtils::toPrimitive).toArray(float[][]::new);

            memoryAddress = JNIService.transferVectors(memoryAddress, primitiveArray);
        }

        trainingDataAllocation.setMemoryAddress(memoryAddress);
    }

    private byte[] convertFloatArrayToByteArray(float[] floatArray) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(floatArray.length * Float.BYTES);
        for (float f : floatArray) {
            byteBuffer.putFloat(f);
        }
        return byteBuffer.array();
    }
}
