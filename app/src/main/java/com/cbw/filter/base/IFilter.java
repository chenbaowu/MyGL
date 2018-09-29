package com.cbw.filter.base;

import java.nio.FloatBuffer;

public interface IFilter {

    int getTextureTarget();

    void setRenderScale(float renderScale);

    void setViewSize(int width, int height);

    void setDrawType(boolean isRecord);

    void resetFilterData();

    void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex,
                int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride);

    void releaseProgram();

}
