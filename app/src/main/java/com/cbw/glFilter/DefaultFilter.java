package com.cbw.glFilter;

import android.content.Context;
import android.opengl.GLES20;
import android.support.annotation.RawRes;

import com.cbw.glFilter.base.AbstractFilter;
import com.cbw.glFilter.base.GlUtil;

import java.nio.FloatBuffer;

public class DefaultFilter extends AbstractFilter {

    protected static final String vertexShaderCode =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 textureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = aPosition;\n" +
                    "    textureCoord = aTextureCoord.xy;\n" +
                    "}";

    protected static final String vertexShaderCode2 =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "uniform mat4 uMVPMatrix;\n" +   // MVP 的变换矩阵（整体变形）
                    "uniform mat4 uTexMatrix;\n" +   // Texture 的变换矩阵 （只对texture变形）
                    "varying vec2 textureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    textureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}";

    protected static final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "varying vec2 textureCoord;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(uTexture, textureCoord);\n" +
                    "}";


    public DefaultFilter(Context context) {
        super(context);
    }

    public DefaultFilter(Context context, int programHandle) {
        super(context, programHandle);
    }

    public DefaultFilter(Context context, @RawRes int vertexSourceRawId, @RawRes int fragmentSourceRawId) {
        super(context, vertexSourceRawId, fragmentSourceRawId);
    }

    public DefaultFilter(Context context, String vertexSource, String fragmentSource) {
        super(context, vertexSource, fragmentSource);
    }

    @Override
    protected int createProgram(Context context) {
        return GlUtil.createProgram(vertexShaderCode, fragmentShaderCode);
    }

    protected int createProgram2(Context context) {
        return GlUtil.createProgram(vertexShaderCode2, fragmentShaderCode);
    }


    @Override
    public int getTextureTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

    @Override
    public void resetFilterData() {

    }

    @Override
    public void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        useProgram();
        bindTexture(textureId);
        bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix, texBuffer, texStride);
        drawArrays(firstVertex, vertexCount);

        unbindGLSLValues();
        unbindTexture();
        disuseProgram();
    }

    @Override
    public void releaseProgram() {
        super.releaseProgram();
    }
}

