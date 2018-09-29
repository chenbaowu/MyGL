package com.cbw.filter;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * created by cbw on 2018/9/28
 * <p>
 * 画三角形
 */

public class TriangleFilter extends DefaultFilter {

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n"
                    + "void main() {\n"
                    + " gl_Position = aPosition;\n"
                    + "}";

    private static final String VERTEX_SHADER2 =
            "attribute vec4 aPosition;\n"
                    + "uniform mat4 uMVPMatrix;\n"
                    + "void main() {\n"
                    + " gl_Position = uMVPMatrix * aPosition;\n"
                    + "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "void main() {\n"
                    + " gl_FragColor = vec4(0.5, 0, 0, 1);\n"
                    + "}";

    private static final float[] VERTEX = {   // in counterclockwise order:
            0, 1, 0,  // top
            -0.5f, -1, 0,  // bottom left
            0.5f, -1, 0,  // bottom right
    };

    private FloatBuffer mVertexBuffer;

    public TriangleFilter(Context context) {
        super(context, VERTEX_SHADER2, FRAGMENT_SHADER);

        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX);
        mVertexBuffer.position(0);
    }

    @Override
    public void getGLSLValues() {
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
    }

    @Override
    public void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {

        useProgram();
        bindGLSLValues(mvpMatrix, mVertexBuffer, coordsPerVertex, vertexStride, texMatrix, texBuffer, texStride);
        drawArrays(firstVertex, vertexCount);

        unbindGLSLValues();
        disuseProgram();
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLES20.glVertexAttribPointer(maPositionLoc, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
    }

    @Override
    protected void drawArrays(int firstVertex, int vertexCount) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    @Override
    protected void unbindGLSLValues() {
        GLES20.glDisableVertexAttribArray(maPositionLoc);
    }
}
