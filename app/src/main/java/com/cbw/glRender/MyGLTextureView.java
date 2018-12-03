package com.cbw.glRender;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by cbw on 2018/10/12.
 */
public class MyGLTextureView extends TextureView implements TextureView.SurfaceTextureListener, SurfaceTexture.OnFrameAvailableListener {

    private GLThread mGLThread;
    private IRenderer mRenderer;

    public MyGLTextureView(Context context) {
        super(context);
        init();
    }

    public MyGLTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    public void setRenderer(IRenderer renderer) {
        this.mRenderer = renderer;
        mGLThread = new GLThread(mRenderer);
        mGLThread.start();
    }

    private boolean mDetached;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mDetached && mRenderer != null) {
            mGLThread = new GLThread(mRenderer);
            mGLThread.start();
        }
        mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mGLThread != null) {
            mGLThread.onRelease();
        }
        mDetached = true;
        super.onDetachedFromWindow();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mGLThread.onSurfaceCreated(surface);
        mGLThread.onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mGLThread.onSurfaceChanged(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mGLThread.onSurfaceDestroy();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    /**
     * @param surfaceTexture
     * @see SurfaceTexture#updateTexImage()
     * @see SurfaceTexture#setOnFrameAvailableListener
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public void requestRender() {
        if (mGLThread == null) return;
        mGLThread.requestRender();
    }

    public void queueEvent(Runnable r) {
        if (mGLThread == null) return;
        mGLThread.queueEvent(r);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mGLThread != null) {
                mGLThread.onRelease();
            }
        } finally {
            super.finalize();
        }
    }
}
