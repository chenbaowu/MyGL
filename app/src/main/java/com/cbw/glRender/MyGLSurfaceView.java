package com.cbw.glRender;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by cbw on 2018/10/10.
 */
public class MyGLSurfaceView extends SurfaceView implements SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {

    private GLThread mGLThread;
    private IRenderer mRenderer;
    private boolean mPreserveEGLContextOnPause;

    public MyGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }

    public void setRenderer(IRenderer renderer) {
        this.mRenderer = renderer;
        mGLThread = new GLThread(mRenderer);
        mGLThread.setPreserveEGLContextOnPause(mPreserveEGLContextOnPause);
        mGLThread.start();
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        mPreserveEGLContextOnPause = preserveOnPause;
        if(mGLThread != null){
            mGLThread.setPreserveEGLContextOnPause(mPreserveEGLContextOnPause);
        }
    }

    public boolean getPreserveEGLContextOnPause() {
        return mPreserveEGLContextOnPause;
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
    public void surfaceCreated(SurfaceHolder holder) {
        mGLThread.onSurfaceCreated(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mGLThread.onSurfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mGLThread.onSurfaceDestroy();
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
