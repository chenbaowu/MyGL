package com.cbw.filter.base;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class VertexArray {

    public FloatBuffer vertexBuffer, textureVerticesBuffer;

    private static final int SIZEOF_FLOAT = 4;
    public static int COORDS_PER_VERTEX = 2; // number of coordinates per vertex in this array (2,3,4)
    public static int vertexStride = COORDS_PER_VERTEX * SIZEOF_FLOAT;
    public static int texStride = 2 * SIZEOF_FLOAT;

    private static float squareCoords[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };
    private static float textureVertices[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f,      // 3 top right
    };

    /**
     * 默认4个顶点
     */
    public VertexArray() {
        this(squareCoords, textureVertices);
    }

    /**
     * 默认4个顶点
     */
    public VertexArray(float[] vertexData) {
        this(vertexData, textureVertices);
    }

    public VertexArray(float[] vertexData, float[] textureVerticesData) {
        // initialize vertex byte buffer for shape coordinates 创建顶点缓冲
        ByteBuffer bb = ByteBuffer.allocateDirect(vertexData.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertexData);
        vertexBuffer.position(0);

        // 创建纹理坐标缓冲
        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVerticesData.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVerticesData);
        textureVerticesBuffer.position(0);
    }

    /**
     * Updates the float buffer with the specified vertex data, assuming that
     * the vertex data and the float buffer are the same size.
     */
    public void updateBuffer(float[] vertexData, int start, int count) {
        vertexBuffer.clear();
        vertexBuffer.position(start);
        vertexBuffer.put(vertexData, start, count);
        vertexBuffer.position(0);

        textureVerticesBuffer.position(0);
    }

    public void updateBuffer(float[] vertexData, float[] textureVertexData) {
        vertexBuffer.clear();
        vertexBuffer.put(vertexData);
        vertexBuffer.position(0);

        textureVerticesBuffer.clear();
        textureVerticesBuffer.put(textureVertexData);
        textureVerticesBuffer.position(0);
    }

    public void resetPosition() {
        vertexBuffer.position(0);
        textureVerticesBuffer.position(0);
    }
}
