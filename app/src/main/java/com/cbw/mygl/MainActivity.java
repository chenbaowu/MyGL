package com.cbw.mygl;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.cbw.base.BaseActivity;
import com.cbw.bean.BaseVideoInfo;
import com.cbw.glRender.GLSurfaceView;
import com.cbw.glRender.MyGLSurfaceView;
import com.cbw.glRender.MyGLTextureView;
import com.cbw.glRender.MyRenderer;
import com.cbw.player.MediaPlayerHelper;
import com.cbw.utils.OnAnimatorTouchListener;
import com.cbw.utils.PathUtil;
import com.cbw.utils.PercentUtil;
import com.cbw.utils.ShareData;

/**
 * 系统GLSurfaceView 和 自定义MyGLSurfaceView , MyGLTextureView
 * 3种渲染对比，{@link #init()}初始化改变可见
 */
public class MainActivity extends BaseActivity implements Choreographer.FrameCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private TextView tv_op;
    private GLSurfaceView mGLSurfaceView;
    private MyGLSurfaceView mMyGLSurfaceView;
    private MyGLTextureView myGLTextureView;
    private MyRenderer myRenderer;
    private MediaPlayerHelper mediaPlayerHelper;

    private void init() {

        PathUtil.GetAppPath(this);
        mediaPlayerHelper = new MediaPlayerHelper(this);
        String path = PathUtil.GetAppPath(this) + "test.mp4";
        BaseVideoInfo baseVideoInfo = mediaPlayerHelper.getVideoInfo(path);
        mediaPlayerHelper.setDataSource(path);

        if (baseVideoInfo != null) {
            FrameLayout frameLayout = findViewById(R.id.GL);
            float w, h;
            if (baseVideoInfo.rotation % 180 == 0) { // 竖拍
                w = 1080;
                h = baseVideoInfo.width * 1.0f / baseVideoInfo.height * w;
            } else {
                w = 1080;
                h = baseVideoInfo.height * 1.0f / baseVideoInfo.width * w;
            }
            frameLayout.getLayoutParams().width = (int) w;
            frameLayout.getLayoutParams().height = (int) h;
        }

        myRenderer = new MyRenderer(this);
        myRenderer.setOnRenderListener(mOnRenderListener);

//        mGLSurfaceView = findViewById(R.id.GLSurfaceView);
//        mGLSurfaceView.setVisibility(View.VISIBLE);
//        mGLSurfaceView.setEGLContextClientVersion(2);
//        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
//        mGLSurfaceView.setPreserveEGLContextOnPause(true);
//        mGLSurfaceView.setRenderer(myRenderer);
//        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mMyGLSurfaceView = findViewById(R.id.MyGLSurfaceView);
        mMyGLSurfaceView.setVisibility(View.VISIBLE);
        mMyGLSurfaceView.setPreserveEGLContextOnPause(true);
        mMyGLSurfaceView.setRenderer(myRenderer);

//        myGLTextureView = findViewById(R.id.MyGLTextureView);
//        myGLTextureView.setVisibility(View.VISIBLE);
//        myGLTextureView.setRenderer(myRenderer);

        tv_op = findViewById(R.id.tv_op);
        tv_op.setOnTouchListener(animatorTouchListener);
    }

    private MyRenderer.OnRenderListener mOnRenderListener = new MyRenderer.OnRenderListener() {
        @Override
        public void onSurfaceCreated(SurfaceTexture surfaceTexture) {

            if (mGLSurfaceView != null) {
                surfaceTexture.setOnFrameAvailableListener(mGLSurfaceView);
            }
            if (mMyGLSurfaceView != null) {
                surfaceTexture.setOnFrameAvailableListener(mMyGLSurfaceView);
            }
            if (myGLTextureView != null) {
                surfaceTexture.setOnFrameAvailableListener(myGLTextureView);
            }

            mediaPlayerHelper.setSurface(new Surface(surfaceTexture));
            if (isPause) {
                return;
            }
            mediaPlayerHelper.start(true);
            isPause = true;

            RenderHelper mRenderHelper = new RenderHelper();
            mRenderHelper.analyseDeviceInfo();
            mRenderHelper.analyseGPUInfo();
            mRenderScale = mRenderHelper.calculateRenderSizeScale(ShareData.m_screenRealWidth, ShareData.m_screenHeight - PercentUtil.HeightPxxToPercent2(1920 - 1223));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_op.setText(mRenderScale + "");
                }
            });
        }

        @Override
        public void onSurfaceDestroyed() {
            mediaPlayerHelper.pause();
        }
    };

    private float mRenderScale = 1f;
    private boolean isPause = false;

    OnAnimatorTouchListener animatorTouchListener = new OnAnimatorTouchListener() {
        @Override
        public void onActionClick(View v) {
            mediaPlayerHelper.start(true);

            if (mGLSurfaceView != null) {
                mGLSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        myRenderer.getMyRenderFilterManager().saveFrame = true;
                    }
                });
            }
            if (mMyGLSurfaceView != null) {
                mMyGLSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        myRenderer.getMyRenderFilterManager().saveFrame = true;
                    }
                });
            }
            if (myGLTextureView != null) {
                myGLTextureView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        myRenderer.getMyRenderFilterManager().saveFrame = true;
                    }
                });
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayerHelper.pause();
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onResume();
        }
//        Choreographer.getInstance().postFrameCallback(this); // post一次回调一次
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Called when a new display frame is being rendered.
     *
     * @param frameTimeNanos nanoseconds
     */
    @Override
    public void doFrame(long frameTimeNanos) {

//        Log.i("bbb", "doFrame: " + frameTimeNanos);
        if (mGLSurfaceView != null && mGLSurfaceView.getRenderMode() == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
            mGLSurfaceView.requestRender();
        }
        if (mMyGLSurfaceView != null) {
            mMyGLSurfaceView.requestRender();
        }

        Choreographer.getInstance().postFrameCallback(this);
    }
}
