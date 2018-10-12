package com.cbw.glRender;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.view.Surface;

import com.cbw.egl.EglCore;
import com.cbw.egl.WindowSurface;

import java.lang.ref.WeakReference;

/**
 * Created by cbw on 2018/10/10.
 */
public class GLThread extends Thread {

    private WeakReference<IRenderer> mRenderer;
    private GLHandler mGLHandler;
    private boolean mPreserveEGLContextOnPause;

    public GLThread(IRenderer renderer) {
        mRenderer = new WeakReference<>(renderer);
    }

    @Override
    public void run() {
        Looper.prepare();
        Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);

        mGLHandler = new GLHandler(mRenderer);
        mGLHandler.setPreserveEGLContextOnPause(mPreserveEGLContextOnPause);

        Looper.loop();
    }

    /**
     * Control whether the EGL context is preserved when the GLSurfaceView is paused and resumed.
     *
     * @param preserveOnPause preserve the EGL context when paused
     */
    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        mPreserveEGLContextOnPause = preserveOnPause;
        if (mGLHandler != null) {
            mGLHandler.setPreserveEGLContextOnPause(mPreserveEGLContextOnPause);
        }
    }

    public void onSurfaceCreated(Object surface) {
        Message.obtain(mGLHandler, MSG_SURFACE_CREATED, surface).sendToTarget();
    }

    public void onSurfaceChanged(int width, int height) {
        Message.obtain(mGLHandler, MSG_SURFACE_CHANGED, width, height).sendToTarget();
    }

    public void onSurfaceDestroy() {
        Message.obtain(mGLHandler, MSG_SURFACE_DESTROY).sendToTarget();
    }

    public void onRelease() {
        Message.obtain(mGLHandler, MSG_RELEASE).sendToTarget();
    }

    public void requestRender() {
        if (Thread.currentThread() == this) {
            mGLHandler.requestRender();
        } else {
            mGLHandler.removeMessages(MSG_REQUEST_RENDER);
            Message.obtain(mGLHandler, MSG_REQUEST_RENDER).sendToTarget();
        }
    }

    public void queueEvent(Runnable r) {
        mGLHandler.post(r);
    }

    private static final int MSG_SURFACE_CREATED = 1;
    private static final int MSG_SURFACE_CHANGED = 2;
    private static final int MSG_REQUEST_RENDER = 3;
    private static final int MSG_SURFACE_DESTROY = 4;
    private static final int MSG_RELEASE = 5;

    public static class GLHandler extends Handler {

        private WeakReference<IRenderer> mRenderer;

        private EglCore mEglCore;
        private WindowSurface mWindowSurface;

        private GLHandler(WeakReference<IRenderer> renderer) {
            mRenderer = renderer;
            // FLAG_RECORDABLE : 告诉EGL它创建的surface必须和视频编解码器兼容。没有这个标志，EGL可能会使用一个MediaCodec不能理解的Buffer
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SURFACE_CREATED:
                    onSurfaceCreated(msg.obj);
                    break;
                case MSG_SURFACE_CHANGED:
                    onSurfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_REQUEST_RENDER:
                    requestRender();
                    break;
                case MSG_SURFACE_DESTROY:
                    onSurfaceDestroy();
                    break;
                case MSG_RELEASE:
                    onRelease();
                    break;
            }
        }

        private boolean mPreserveEGLContextOnPause;
        private boolean mWaitingForSurface;
        private boolean mShouldDestroy;

        public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
            mPreserveEGLContextOnPause = preserveOnPause;
        }

        private void onSurfaceCreated(Object surface) {

            if (surface instanceof SurfaceTexture) {
                mWindowSurface = new WindowSurface(mEglCore, (SurfaceTexture) surface);
            } else if (surface instanceof Surface) {
                mWindowSurface = new WindowSurface(mEglCore, (Surface) surface, false);
            }
            mWindowSurface.makeCurrent();

            if (mWaitingForSurface) {
                return;
            }
            IRenderer renderer = mRenderer.get();
            if (renderer != null) {
                renderer.onSurfaceCreated();
            }
        }

        private void onSurfaceChanged(int width, int height) {
            if (mWindowSurface == null) {
                return;
            }
            if (mWaitingForSurface) {
                mWaitingForSurface = false;
                requestRender();
                return;
            }
            IRenderer renderer = mRenderer.get();
            if (renderer != null) {
                renderer.onSurfaceChanged(width, height);
            }
        }

        private void onSurfaceDestroy() {
            releaseSurface();

            if (mPreserveEGLContextOnPause && !mShouldDestroy) {
                mWaitingForSurface = true;
            }

            if (!mWaitingForSurface) {
                IRenderer renderer = mRenderer.get();
                if (renderer != null) {
                    renderer.onSurfaceDestroyed();
                }
            }
        }

        private void requestRender() {
            IRenderer renderer = mRenderer.get();
            if (renderer != null && mWindowSurface != null) {
                renderer.onDrawFrame();
                mWindowSurface.swapBuffers();
            }
        }

        private void onRelease() {
            removeCallbacksAndMessages(null);
            mWaitingForSurface = false;
            mShouldDestroy = true;
            onSurfaceDestroy();
            releaseGL();
            quitSafely();
        }

        private void releaseSurface() {
            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
        }

        private void releaseGL() {
            if (mEglCore != null) {
                mEglCore.release();
                mEglCore = null;
            }
        }

        private void quitSafely() {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quitSafely();
            }
        }
    }

}
