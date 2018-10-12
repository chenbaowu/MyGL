package com.cbw.glRender;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

/**
 * Created by cbw on 2018/10/12.
 */
public class GLSurfaceView extends android.opengl.GLSurfaceView {

    public GLSurfaceView(Context context) {
        super(context);
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }
}
