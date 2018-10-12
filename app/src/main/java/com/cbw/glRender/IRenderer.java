package com.cbw.glRender;

import android.graphics.SurfaceTexture;

/**
 * Created by cbw on 2018/10/10.
 */
public interface IRenderer {

    void onSurfaceCreated();

    void onSurfaceChanged(int width, int height);

    void onDrawFrame();

    void onSurfaceDestroyed();

    void setOnRenderListener(OnRenderListener listener);

    interface OnRenderListener {
        void onSurfaceCreated(SurfaceTexture surfaceTexture);
        void onSurfaceDestroyed();
    }
}
