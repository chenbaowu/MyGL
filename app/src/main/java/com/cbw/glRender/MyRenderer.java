package com.cbw.glRender;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.cbw.glFilter.base.GlUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * created by cbw on 2018/9/27
 */

public class MyRenderer implements GLSurfaceView.Renderer, IRenderer {

    private Context mContext;
    private MyRenderFilterManager myRenderFilterManager;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureId;

    public MyRenderer(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        onSurfaceCreated();
    }

    @Override
    public void onSurfaceCreated() {
        initMatrix();
        createSourceTexture();
        myRenderFilterManager = new MyRenderFilterManager(mContext);
    }

    private float[] mProjectionMatrix;     // 投影矩阵
    private float[] mViewMatrix;           // 视觉矩阵
    private float[] mViewProjectionMatrix; // 视觉投影
    private float[] mSTMatrix; // surface texture matrix

    private void initMatrix() {

        mProjectionMatrix = new float[16];
        mViewMatrix = new float[16];
        mViewProjectionMatrix = new float[16];
        mSTMatrix = new float[16];

        Matrix.setIdentityM(mViewProjectionMatrix, 0);
        Matrix.setIdentityM(mSTMatrix, 0);
    }

    private void createSourceTexture() {
        mTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        if (mOnRenderListener != null) {
            mOnRenderListener.onSurfaceCreated(mSurfaceTexture);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        onSurfaceChanged(width, height);
    }

    private float mRenderScale = 1f;
    private int mSurfaceWidth, mSurfaceHeight;

    @Override
    public void onSurfaceChanged(int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        initMVPMatrix(mSurfaceWidth, mSurfaceHeight);

        myRenderFilterManager.initFilter();
        myRenderFilterManager.setSurfaceSize(width, height, mRenderScale);
        myRenderFilterManager.initGLFramebuffer();
    }

    private void initMVPMatrix(int width, int height) {
        float mRatio;
        int offset = 0;

      /*
        // 高满surface
        mRatio = width * 1.0f / height
        float left = -mRatio;
        float right = mRatio;
        float bottom = -1.0f;
        float top = 1.0f;
      */

      /*
        // 宽满surface
        mRatio = height * 1.0f / width;
        float left = -1;
        float right = 1;
        float bottom = -mRatio;
        float top = mRatio;
      */

        // 宽高满surface
        float left = -1;
        float right = 1;
        float bottom = -1;
        float top = 1;

        float near = 3.0f;// near <= eyeZ
        float far = 7.0f;

        /**
         * gl顶点坐标是 -1 到 1 的区间
         * 这里的 left, right, bottom, top 相当于指定你的surface的gl空间是 [left ,right][top ,bottom]
         * 你的顶点坐标是根据你指定的空间对号入座
         * 比如你指定是-0.5f到0.5f，你的顶点坐标是-1到1，那么你画的东西将是从surface外开始画（画图理解）
         *
         * near, far 近平面和远平面 ，这个数值不是坐标，而是相对 eyeZ 的位置
         * 从相机看出去，近平面和远平面构成的视椎体是物体的可视范围
         * 物体越接近近平面就越大
         */
        Matrix.frustumM(mProjectionMatrix, offset, left, right, bottom, top, near, far);//透视投影矩阵

        // Set the camera position (View matrix)
        int rmOffset = 0;
        float eyeX = 0.0f;    // 相机的坐标
        float eyeY = 0.0f;
        float eyeZ = 3.0f;
        float centerX = 0.0f; // 相机看向的坐标（观测点）
        float centerY = 0.0f;
        float centerZ = 0.0f;
        float upX = 0.0f;
        float upY = 1.0f;     // 相机的方向（列如正(Y)着看，躺(X)着看）
        float upZ = 0.0f;
        Matrix.setLookAtM(mViewMatrix, rmOffset, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);//视觉矩阵
        Matrix.multiplyMM(mViewProjectionMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        onDrawFrame();
    }

    @Override
    public void onDrawFrame() {

        try {
            mSurfaceTexture.updateTexImage(); // 更新数据源，回调 onFrameAvailable
            mSurfaceTexture.getTransformMatrix(mSTMatrix);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myRenderFilterManager.drawFrame(mViewProjectionMatrix, mTextureId, mSTMatrix);
    }

    public MyRenderFilterManager getMyRenderFilterManager() {
        return myRenderFilterManager;
    }

    @Override
    public void onSurfaceDestroyed() {

        if (myRenderFilterManager != null) {
            myRenderFilterManager.release();
            myRenderFilterManager = null;
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.setOnFrameAvailableListener(null);
            mSurfaceTexture.release(); // release方法必须在 "对应Thread" 内调用否则出现 "PreviewBufferQueue has been abandoned"错误
            mSurfaceTexture = null;
        }

        if (mOnRenderListener != null) {
            mOnRenderListener.onSurfaceDestroyed();
        }

    }

    @Override
    public void setOnRenderListener(IRenderer.OnRenderListener listener) {
        mOnRenderListener = listener;
    }

    private OnRenderListener mOnRenderListener;

}
