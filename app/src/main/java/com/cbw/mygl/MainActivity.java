package com.cbw.mygl;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Choreographer;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.cbw.bean.BaseVideoInfo;
import com.cbw.glRender.GLSurfaceView;
import com.cbw.glRender.MyGLSurfaceView;
import com.cbw.glRender.MyGLTextureView;
import com.cbw.glRender.MyRenderer;
import com.cbw.player.MediaPlayerHelper;
import com.cbw.utils.OnAnimatorTouchListener;
import com.cbw.utils.PathUtil;

/**
 *  系统GLSurfaceView 和 自定义MyGLSurfaceView , MyGLTextureView
 */
public class MainActivity extends AppCompatActivity implements Choreographer.FrameCallback {

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

        if(baseVideoInfo != null){
           FrameLayout frameLayout =  findViewById(R.id.GL);
           frameLayout.getLayoutParams().width = baseVideoInfo.width;
           frameLayout.getLayoutParams().height = baseVideoInfo.height;
        }

        myRenderer = new MyRenderer(this);
        myRenderer.setOnRenderListener(mOnRenderListener);

        mGLSurfaceView = findViewById(R.id.GLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.setPreserveEGLContextOnPause(true);
        mGLSurfaceView.setRenderer(myRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mMyGLSurfaceView = findViewById(R.id.MyGLSurfaceView);
        mMyGLSurfaceView.setPreserveEGLContextOnPause(true);
        mMyGLSurfaceView.setRenderer(myRenderer);

        myGLTextureView = findViewById(R.id.MyGLTextureView);
//        myGLTextureView.setRenderer(myRenderer);

        tv_op = findViewById(R.id.tv_op);
        tv_op.setOnTouchListener(animatorTouchListener);
    }

    private MyRenderer.OnRenderListener mOnRenderListener = new MyRenderer.OnRenderListener() {
        @Override
        public void onSurfaceCreated(SurfaceTexture surfaceTexture) {

            surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    mGLSurfaceView.requestRender();
                    myGLTextureView.requestRender();
                    mMyGLSurfaceView.requestRender();
                }
            });

//            surfaceTexture.setOnFrameAvailableListener(mMyGLSurfaceView);

            mediaPlayerHelper.setSurface(new Surface(surfaceTexture));
            if (isPause) {
                return;
            }
            mediaPlayerHelper.start(true);
            isPause = true;
        }

        @Override
        public void onSurfaceDestroyed() {
            mediaPlayerHelper.pause();
        }
    };

    private boolean isPause = false;

    OnAnimatorTouchListener animatorTouchListener = new OnAnimatorTouchListener() {
        @Override
        public void onActionClick(View v) {
            mediaPlayerHelper.start(true);

            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    myRenderer.getMyRenderFilterManager().saveFrame = true;
                }
            });

        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayerHelper.pause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
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
        mGLSurfaceView.requestRender();
        mMyGLSurfaceView.requestRender();

        Choreographer.getInstance().postFrameCallback(this);
    }
}
