package com.cbw.glManager;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cbw.filter.DisplayFilter;
import com.cbw.filter.PicFilter;
import com.cbw.filter.TriangleFilter;
import com.cbw.filter.base.AbstractFilter;
import com.cbw.filter.base.Drawable2d;
import com.cbw.filter.base.GLFramebuffer;

import java.nio.FloatBuffer;
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
    private float[] mTargetMatrix;
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

    public void initFilter() {

        AbstractFilter filter = null;

        try {
            filter = new DisplayFilter(mContext);
            addToCache(FilterDrawOrder.FILTER_DISPLAY, filter);
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
            filter = new TriangleFilter(mContext);
            addToCache(FilterDrawOrder.FILTER_TRIANGLE, filter);
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
        GLES20.glViewport(mFrameBufferRect.left, mFrameBufferRect.top, mFrameBufferRect.right, mFrameBufferRect.bottom);

        if (mAllFilterCache != null) {
            for (Map.Entry<Integer, AbstractFilter> entry : mAllFilterCache.entrySet()) {
                AbstractFilter filter = entry.getValue();
                if (filter != null) {
                    filter.setRenderScale(mRenderScale);
                    filter.setViewSize(mFrameBufferWidth, mFrameBufferHeight);
                }
            }
        }
    }

    public void initGLFramebuffer() {
        mGLFramebuffer = new GLFramebuffer(5, mSurfaceWidth, mSurfaceHeight);
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
        GLES20.glViewport(mDisplayRect.left, mDisplayRect.top, mDisplayRect.width(), mDisplayRect.height());

        setFilterEnableByLayer(FilterDrawOrder.FILTER_PIC, true);
        setFilterEnableByLayer(FilterDrawOrder.FILTER_TRIANGLE, false);

//        getFilterByLayer(FilterDrawOrder.FILTER_TRIANGLE).onDraw(mvpMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, texMatrix, texBuffer, textureId, texStride);

        for (Map.Entry<Integer, AbstractFilter> entry : mAllFilterCache.entrySet()) {
            AbstractFilter filter = entry.getValue();

            if (filter == null || !filter.getFilterEnable()) {
                continue;
            }

            if (filter instanceof DisplayFilter) {
                textureId = mGLFramebuffer.getCurrentTextureId();
            } else {
                mGLFramebuffer.bindNext(true);
            }

//                blendEnable(true);
            filter.onDraw(mvpMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, texMatrix, texBuffer, textureId, texStride);
//                blendEnable(false);
            mGLFramebuffer.unbind();
        }
    }

    private boolean mBlendEnable;

    /**
     * @param enable
     */
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
