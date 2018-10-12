package com.cbw.glFilter.base;

import android.content.Context;
import android.opengl.GLES20;
import android.support.annotation.RawRes;

import java.nio.FloatBuffer;

public abstract class AbstractFilter implements IFilter {

    protected Context mContext;
    protected int mProgramHandle;
    protected float mRenderScale = 1.0f;
    protected int mWidth, mHeight; // 注意传进来的是 surface的宽高，还是Framebuffer的
    protected boolean mInRecordDraw = false;
    protected boolean mFilterEnable = true;
    private GLFramebuffer mGLFramebuffer;

    public AbstractFilter(Context context) {
        mContext = context;
        mProgramHandle = createProgram(context);
        checkProgram();
    }

    public AbstractFilter(Context context, int programHandle) {
        mContext = context;
        mProgramHandle = programHandle;
        checkProgram();
    }

    public AbstractFilter(Context context, @RawRes int vertexSourceRawId, @RawRes int fragmentSourceRawId) {
        mContext = context;
        mProgramHandle = GlUtil.createProgram(context, vertexSourceRawId, fragmentSourceRawId);
        checkProgram();
    }

    public AbstractFilter(Context context, String vertexSource, String fragmentSource) {
        mContext = context;
        mProgramHandle = GlUtil.createProgram(vertexSource, fragmentSource);
        checkProgram();
    }

    protected void checkProgram() {
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        getGLSLValues();
    }

    protected abstract int createProgram(Context context);

    @Override
    public void setViewSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void setRenderScale(float renderScale) {
        mRenderScale = renderScale;
    }

    @Override
    public void setDrawType(boolean isRecord) {
        mInRecordDraw = isRecord;
    }

    public void setFilterEnable(boolean enable) {
        mFilterEnable = enable;
    }

    public boolean getFilterEnable() {
        return mFilterEnable;
    }

    public void setFramebuffer(GLFramebuffer glFramebuffer) {
        this.mGLFramebuffer = glFramebuffer;
    }

    protected int maPositionLoc;
    protected int maTextureCoordLoc;
    protected int muMVPMatrixLoc;
    protected int muTexMatrixLoc;
    protected int muTextureLoc;

    protected void getGLSLValues() {
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");

        muTextureLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexture");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
    }

    protected void useProgram() {
        GLES20.glUseProgram(mProgramHandle);
    }

    protected int mDefaultTextureId = -1;

    protected void bindTexture(int textureId) {
        mDefaultTextureId = textureId;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(getTextureTarget(), textureId);
        GLES20.glUniform1i(muTextureLoc, 0);
    }

    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, texStride, texBuffer);
    }

    protected void drawArrays(int firstVertex, int vertexCount) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
    }

    protected void unbindGLSLValues() {
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
    }

    protected void unbindTexture() {
        GLES20.glBindTexture(getTextureTarget(), 0);
//        GLES20.glDeleteTextures(1, new int[]{mDefaultTextureId}, 0);
    }

    protected void deleteTexture() {
        if (mDefaultTextureId != -1) {
            mDefaultTextureId = -1;
            GLES20.glDeleteTextures(1, new int[]{mDefaultTextureId}, 0);
        }
    }

    protected void disuseProgram() {
        GLES20.glUseProgram(0);
    }

    @Override
    public void releaseProgram() {
        mContext = null;

        deleteTexture();
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

}
