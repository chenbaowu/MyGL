package com.cbw.glFilter.camera;

import android.content.Context;
import android.opengl.GLES20;

import com.cbw.glFilter.DefaultFilter;
import com.cbw.glFilter.base.GlUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * created by cbw on 2018/9/28
 * yuv byte[] spilt y,u,v
 * http://blog.csdn.net/oshunz/article/details/50055057
 * http://stackoverflow.com/questions/22456884/how-to-render-androids-yuv-nv21-camera-image-on-the-background-in-libgdx-with-o
 */

public class CameraYUVFilter extends DefaultFilter {

    private static final String vertexShaderCode =
            "attribute vec4 a_position;\n" +
                    "attribute vec2 a_texCoord;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "void main() {\n" +
                    "   gl_Position = a_position;\n" +
                    "   v_texCoord = a_texCoord;\n" +
                    "}";

    private static final String vertexShaderCode2 =
            "attribute vec4 a_position;\n" +
                    "attribute vec4 a_texCoord;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = a_position;\n" +
                    "    v_texCoord = (uTexMatrix * a_texCoord).xy;\n" +
                    "}";

//    private static final String fragmentShaderCode =
//            "precision highp float;\n" +
//                    "varying vec2 v_texCoord;\n" +
//                    "uniform sampler2D y_texture;\n" +
//                    "uniform sampler2D uv_texture;\n" +
//                    "void main(void) {\n" +
//                    "   float y, u, v, r, g, b;\n" +
//                    "   //We had put the Y values of each pixel to the R,G,B components by GL_LUMINANCE,\n" +
//                    "   //that's why we're pulling it from the R component, we could also use G or B\n" +
//                    "   y = texture2D(y_texture, v_texCoord).r;\n" +
//                    "   //We had put the U and V values of each pixel to the A and R,G,B components of the\n" +
//                    "   //texture respectively using GL_LUMINANCE_ALPHA. Since U,V bytes are interspread\n" +
//                    "   //in the texture, this is probably the fastest way to use them in the shader\n" +
//                    "   u = texture2D(uv_texture, v_texCoord).a - 0.5;\n" +
//                    "   v = texture2D(uv_texture, v_texCoord).r - 0.5;\n" +
//                    "   //The numbers are just YUV to RGB conversion constants\n" +
//                    "   r = y + 1.402 * v;\n" +
//                    "   g = y - 0.34414 * u - 0.71414 * v;\n" +
//                    "   b = y + 1.772 * u;\n" +
//                    "   //We finally set the RGB color of our pixel\n" +
//                    "   gl_FragColor = vec4(r, g, b, 1.0);\n" +
//                    "}";

    private static final String fragmentShaderCode =
            "precision highp float;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "uniform sampler2D y_texture;\n" +
                    "uniform sampler2D uv_texture;\n" +
                    "void main(void) {\n" +
                    "   float y, u, v, r, g, b;\n" +
                    "   y = texture2D(y_texture, v_texCoord).r;\n" +
                    "   u = texture2D(uv_texture, v_texCoord).a - 0.5;\n" +
                    "   v = texture2D(uv_texture, v_texCoord).r - 0.5;\n" +
                    "   r = y + 1.370705 * v;\n" +
                    "   g = y - 0.337633 * u - 0.698001 * v;\n" +
                    "   b = y + 1.732446 * u;\n" +
                    "   gl_FragColor = vec4(r, g, b, 1.0);\n" +
                    "}";

    private int mPositionHandle;
    private int mCoordHandle;
    private int mYHandle;
    private int mUVHandle;

    private int mTempYTextureId;
    private int mTempUVTextureId;

    private float mViewHWRatio;
    private int mDataWidth;
    private int mDataHeight;

    private byte[] previewDataCache;
    private byte[] previewDataCache2;
    private ByteBuffer mYByteBuffer;
    private ByteBuffer mUVByteBuffer;
    private int mYTextureId;
    private int mUVTextureId;

    private boolean mCanDraw;
    private boolean mIsValidData;

    public CameraYUVFilter(Context context) {
        super(context);
    }

    @Override
    public int getTextureTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(vertexShaderCode2, fragmentShaderCode);
    }

    @Override
    public void getGLSLValues() {
        super.getGLSLValues();
        /* get handle for "vPosition" and "a_texCoord" */
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_position");
        mCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_texCoord");

        /* get uniform location for mYByteBuffer/mUVByteBuffer, we pass data through these uniforms */
        mYHandle = GLES20.glGetUniformLocation(mProgramHandle, "y_texture");
        mUVHandle = GLES20.glGetUniformLocation(mProgramHandle, "uv_texture");
//        GlUtil.checkGlError("getGLSLValues");

        if (mTempYTextureId <= 0) {
            mTempYTextureId = getTextureId();
        }
        if (mTempUVTextureId <= 0) {
            mTempUVTextureId = getTextureId();
        }
    }

    private int getTextureId() {
        return GlUtil.createTexture(GLES20.GL_TEXTURE_2D);
    }

    @Override
    public void setViewSize(int width, int height) {
        super.setViewSize(width, height);
        if (mWidth > 0 && mHeight > 0) {
            mViewHWRatio = Math.round(mHeight / mRenderScale) * 1.0f / Math.round(mWidth / mRenderScale);
            if (mViewHWRatio > 16.0f / 9) {
                mViewHWRatio = 16.0f / 9;
                mHeight = Math.round(mWidth * mViewHWRatio);

            } else if (mViewHWRatio > 4.0f / 3 && mViewHWRatio < 16.0f / 9) {
                mViewHWRatio = 4.0f / 3;
                mHeight = Math.round(mWidth * mViewHWRatio);
            }
        }
        mDataWidth = 0;
        mDataHeight = 0;
        mYTextureId = 0;
        mUVTextureId = 0;
    }

    /**
     * @param previewData
     * @param width       dest:640
     * @param height      dest:480
     */
    public void updatePreviewFrame(byte[] previewData, int width, int height) {
        previewDataCache = previewData;
        if (width > 0 && height > 0) {
            if (width != mDataWidth && height != mDataHeight) {
                mDataWidth = width;
                mDataHeight = height;
                mYTextureId = 0;
                mUVTextureId = 0;
                initBuffer(width, height);
            }
            mCanDraw = true;
        } else {
            if (width == -1 || height == -1) {
                mDataWidth = 0;
                mDataHeight = 0;
                mYTextureId = 0;
                mUVTextureId = 0;
                previewDataCache = null;
                previewDataCache2 = null;
                mIsValidData = false;
            }
            mCanDraw = false;
        }
    }

    private void initBuffer(int width, int height) {
        int yArraySize = width * height;
        int uvArraySize = yArraySize / 2;
        synchronized (this) {
            mYByteBuffer = ByteBuffer.allocate(yArraySize);
            mUVByteBuffer = ByteBuffer.allocate(uvArraySize);
        }
    }

    /**
     * this method will be called from native code, it's used for passing yuv data to me.
     */
    private void updateBuffer() {
        if (mDataHeight <= 0 || mWidth <= 0 || mDataWidth * 1.0f / mDataHeight != mViewHWRatio) {
            previewDataCache = null;
            previewDataCache2 = null;
            mIsValidData = false;
            return;
        }
        if (previewDataCache != null && previewDataCache.length > 0) {
            previewDataCache2 = previewDataCache;
            previewDataCache = null;
        }
        if (previewDataCache2 != null) {//4 1 1
            if (previewDataCache2.length == mDataWidth * mDataHeight * 3 / 2) {
                mIsValidData = true;
            } else {
                mIsValidData = false;
                return;
            }

            int yLen = previewDataCache2.length / 6 * 4;
            int uvLen = previewDataCache2.length / 6 * 2;
            //dataLen = w * h * 3 / 2;
//            Log.i("bbb", "previewDataCache2.length:"+previewDataCache2.length);//460800
            synchronized (this) {
                mYByteBuffer.clear();
                mUVByteBuffer.clear();
                mYByteBuffer.put(previewDataCache2, 0, yLen);
                mUVByteBuffer.put(previewDataCache2, yLen, uvLen);
            }
        }
    }

    @Override
    public void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex,
                       int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        if (mCanDraw && mYByteBuffer != null) {
            updateBuffer();
            if (!mIsValidData) {
                return;
            }

            useProgram();
            loadTexture();
            bindTexture(textureId);
            bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix, texBuffer, texStride);
            drawArrays(firstVertex, vertexCount);

            unbindGLSLValues();
            unbindTexture();
            disuseProgram();
        }
    }

    /**
     * build a set of textures, one for Y, one for U, and one for V.
     */
    protected void loadTexture() {
        // reset position, have to be done
        mYByteBuffer.position(0);
        mUVByteBuffer.position(0);

//        // building texture for Y data
////        Log.i(TAG, "glGenTextures Y");
//        mYTextureId = createTexture(mDataWidth, mDataHeight, GLES20.GL_LUMINANCE, mYByteBuffer);
//
//        // building texture for UV data
////        Log.i(TAG, "glGenTextures UV");
//        mUVTextureId = createTexture(mDataWidth / 2, mDataHeight / 2, GLES20.GL_LUMINANCE_ALPHA, mUVByteBuffer);

        if (mYTextureId <= 0) {
            mYTextureId = mTempYTextureId;
            if (mYTextureId <= 0) {
                mYTextureId = getTextureId();
            }
            bindImage(false, mYTextureId, mDataWidth, mDataHeight, GLES20.GL_LUMINANCE, mYByteBuffer);
        } else {
            bindImage(true, mYTextureId, mDataWidth, mDataHeight, GLES20.GL_LUMINANCE, mYByteBuffer);
        }
        if (mUVTextureId <= 0) {
            mUVTextureId = mTempUVTextureId;
            if (mUVTextureId <= 0) {
                mUVTextureId = getTextureId();
            }
            bindImage(false, mUVTextureId, mDataWidth / 2, mDataHeight / 2, GLES20.GL_LUMINANCE_ALPHA, mUVByteBuffer);
        } else {
            bindImage(true, mUVTextureId, mDataWidth / 2, mDataHeight / 2, GLES20.GL_LUMINANCE_ALPHA, mUVByteBuffer);
        }
    }

    private int createTexture(int width, int height, int internalFormat, Buffer pixels) {
        return GlUtil.createIndexTexture(width, height, GLES20.GL_LINEAR, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE, internalFormat, pixels);
    }

    private void bindImage(boolean isUpdate, int textureId, int width, int height, int internalFormat, Buffer pixels) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        if (isUpdate) {
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width, height, internalFormat, GLES20.GL_UNSIGNED_BYTE, pixels);
        } else {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, internalFormat, GLES20.GL_UNSIGNED_BYTE, pixels);
        }
    }

    @Override
    protected void bindTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mYTextureId);
        GLES20.glUniform1i(mYHandle, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mUVTextureId);
        GLES20.glUniform1i(mUVHandle, 1);
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mCoordHandle);
        GLES20.glVertexAttribPointer(mCoordHandle, 2, GLES20.GL_FLOAT, false, texStride, texBuffer);
    }

    @Override
    protected void drawArrays(int firstVertex, int vertexCount) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
    }

    @Override
    protected void unbindGLSLValues() {
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mCoordHandle);
    }

    @Override
    public void releaseProgram() {
        super.releaseProgram();

        GLES20.glDeleteTextures(2, new int[]{mYTextureId, mUVTextureId}, 0);

        previewDataCache = null;
        previewDataCache2 = null;
    }
}
