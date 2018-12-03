package com.cbw.glRender;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.cbw.glFilter.DefaultFilter;
import com.cbw.glFilter.DisplayFilter;
import com.cbw.glFilter.PicFilter;
import com.cbw.glFilter.TriangleFilter;
import com.cbw.glFilter.base.AbstractFilter;
import com.cbw.glFilter.base.Drawable2d;
import com.cbw.glFilter.base.GLFramebuffer;
import com.cbw.glFilter.camera.CameraOesFilter;
import com.cbw.utils.ImageUtil;
import com.cbw.utils.PathUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * created by cbw on 2018/9/28
 * <p>
 * gl渲染管理类
 * gl矩阵旋转是逆时针
 */

public class MyRenderFilterManager {

    private Context mContext;

    private Drawable2d mDrawable2d;

    private HashMap<Integer, AbstractFilter> mAllFilterCache;
    private GLFramebuffer mGLFramebuffer;

    private float[] mIdentityMatrix;
    private float[] mTargetMvpMatrix;
    private float[] mTargettTexMatrix;
    private Rect mDisplayRect;
    private Rect mFrameBufferRect;
    private Rect mRecordRect;

    public MyRenderFilterManager(Context context) {
        mContext = context;
        mAllFilterCache = new HashMap<Integer, AbstractFilter>();

        mIdentityMatrix = new float[16];
        Matrix.setIdentityM(mIdentityMatrix, 0);

        mDrawable2d = new Drawable2d();
        mDisplayRect = new Rect();
        mFrameBufferRect = new Rect();
        mRecordRect = new Rect();
    }

    private DefaultFilter mFinalFilter; // 最后把fbo的图像画到屏幕

    public void initFilter() {

        mFinalFilter = new DefaultFilter(mContext);

        AbstractFilter filter = null;

        try {
            filter = new TriangleFilter(mContext);
            addToCache(FilterDrawOrder.FILTER_TRIANGLE, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            filter = new CameraOesFilter(mContext);
            addToCache(FilterDrawOrder.FILTER_OES, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            filter = new PicFilter(mContext);
            addToCache(FilterDrawOrder.FILTER_PIC, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            filter = new DisplayFilter(mContext);
            addToCache(FilterDrawOrder.FILTER_DISPLAY, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addToCache(Integer layer, AbstractFilter filter) {
        if (mAllFilterCache != null) {
            mAllFilterCache.put(layer, filter);
        }
    }

    private AbstractFilter getFilterByLayer(Integer layer) {
        if (mAllFilterCache == null) {
            return null;
        }
        return mAllFilterCache.get(layer);
    }

    private boolean setFilterEnableByLayer(Integer layer, boolean enable) {
        AbstractFilter filter = getFilterByLayer(layer);
        if (filter != null) {
            filter.setFilterEnable(enable);
            return true;
        }
        return false;
    }

    private int mSurfaceWidth, mSurfaceHeight;
    private int mFrameBufferWidth, mFrameBufferHeight; // 画的宽高
    private float mRenderScale = 1.0f;

    public void setSurfaceSize(int width, int height, float renderScale) {
        if (width <= 0 || height <= 0 || renderScale <= 0.0f) {
            return;
        }
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mRenderScale = renderScale;

        mFrameBufferWidth = Math.round(width * renderScale);
        mFrameBufferHeight = Math.round(height * renderScale);

        mDisplayRect.set(0, 0, mSurfaceWidth, mSurfaceHeight);
        mFrameBufferRect.set(0, 0, mFrameBufferWidth, mFrameBufferHeight);

        if (mAllFilterCache != null) {
            for (Map.Entry<Integer, AbstractFilter> entry : mAllFilterCache.entrySet()) {
                AbstractFilter filter = entry.getValue();
                if (filter != null) {
                    filter.setRenderScale(mRenderScale);
                    filter.setViewSize(mFrameBufferWidth, mFrameBufferHeight);
                }
            }
        }

        if(mFinalFilter != null){
            mFinalFilter.setRenderScale(mRenderScale);
            mFinalFilter.setViewSize(mFrameBufferWidth,mFrameBufferHeight);
        }
    }

    public void initGLFramebuffer() {
        mGLFramebuffer = new GLFramebuffer(5, mFrameBufferWidth, mFrameBufferHeight);
    }

    public void drawFrame(float[] mvpMatrix, int textureId, float[] texMatrix) {
        onDraw(mvpMatrix, mDrawable2d.getVertexArray(), 0, mDrawable2d.getVertexCount(), mDrawable2d.getCoordsPerVertex(),
                mDrawable2d.getVertexStride(), texMatrix, mDrawable2d.getTexCoordArray(), textureId, mDrawable2d.getTexCoordStride());
    }

    /**
     * @param mvpMatrix
     * @param vertexBuffer
     * @param firstVertex
     * @param vertexCount
     * @param coordsPerVertex
     * @param vertexStride
     * @param texMatrix
     * @param texBuffer
     * @param textureId
     * @param texStride
     */
    private void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex,
                        int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {

        // GLES20.glScissor(); // 裁剪
        GLES20.glViewport(mFrameBufferRect.left, mFrameBufferRect.top, mFrameBufferRect.width(), mFrameBufferRect.height());

        setFilterEnableByLayer(FilterDrawOrder.FILTER_OES, true);
        setFilterEnableByLayer(FilterDrawOrder.FILTER_TRIANGLE, false);
        setFilterEnableByLayer(FilterDrawOrder.FILTER_PIC, true);


//        getFilterByLayer(FilterDrawOrder.FILTER_TRIANGLE).onDraw(mIdentityMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, mIdentityMatrix, texBuffer, textureId, texStride);

        for (Map.Entry<Integer, AbstractFilter> entry : mAllFilterCache.entrySet()) {

            AbstractFilter filter = entry.getValue();

            if (filter == null || !filter.getFilterEnable()) {
                continue;
            }

            /* 第一个filter校正方向（即纹理矩阵），最后一个filter校正变形（即顶点矩阵）*/
            if (entry.getKey() == FilterDrawOrder.FILTER_OES) {
                mGLFramebuffer.bindNext(true);
                mTargetMvpMatrix = mIdentityMatrix;
                mTargettTexMatrix = texMatrix;
            } else if (entry.getKey() == FilterDrawOrder.FILTER_DISPLAY) {
                mTargetMvpMatrix = mvpMatrix;
                mTargettTexMatrix = mIdentityMatrix;
                textureId = mGLFramebuffer.getCurrentTextureId();
                mGLFramebuffer.bindNext(true);
            } else {
                mTargetMvpMatrix = mIdentityMatrix;
                mTargettTexMatrix = mIdentityMatrix;
            }

            /*blendEnable(true);*/
            filter.onDraw(mTargetMvpMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, mTargettTexMatrix, texBuffer, textureId, texStride);
            /*blendEnable(false);*/
        }

        if (saveFrame) {
            saveFrame = false;
            readPixels(mFrameBufferWidth, mFrameBufferHeight);
        }
        textureId = mGLFramebuffer.getCurrentTextureId();
        mGLFramebuffer.unbind();
        GLES20.glViewport(mDisplayRect.left, mDisplayRect.top, mDisplayRect.width(), mDisplayRect.height());
        mFinalFilter.onDraw(mIdentityMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, mIdentityMatrix, texBuffer, textureId, texStride);

    }

    public boolean saveFrame;
    private IntBuffer mReadBuf;

    public void readPixels(int width, int height) {

        long time = System.currentTimeMillis();
        GLES20.glFinish(); // 等待渲染结束 解决某些机型录制闪屏、图像不完整问题 ，但是耗时
        if (mReadBuf == null) {
            mReadBuf = IntBuffer.allocate(width * height);
        } else {
            mReadBuf.rewind();
        }
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mReadBuf);
        Log.i("bbb", "readPixels: " + (System.currentTimeMillis() - time));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(mReadBuf);
        String path = PathUtil.GetAppPath(mContext) + "/gl.jpg";
        ImageUtil.WriterBitmapToSd(path, bitmap, 100);
        mReadBuf.clear();
    }

    private boolean mBlendEnable;

    private void blendEnable(boolean enable) {
        if (enable == mBlendEnable) {
            return;
        }
        mBlendEnable = enable;
        if (enable) {
            GLES20.glDepthMask(false);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
            GLES20.glBlendFuncSeparate(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE, GLES20.GL_ONE);
        } else {
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDepthMask(true);
        }
    }

    public void release() {

        mContext = null;

        if (mFinalFilter != null) {
            mFinalFilter.releaseProgram();
            mFinalFilter = null;
        }

        if (mAllFilterCache != null) {
            for (Map.Entry<Integer, AbstractFilter> entry : mAllFilterCache.entrySet()) {
                AbstractFilter filter = entry.getValue();
                if (filter != null) {
                    filter.releaseProgram();
                    filter = null;
                }
            }
            if (mGLFramebuffer != null) {
                mGLFramebuffer.destroy();
                mGLFramebuffer = null;
            }
            mAllFilterCache.clear();
            mAllFilterCache = null;
        }
    }
}
