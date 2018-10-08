package com.cbw.glFilter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;

import com.cbw.glFilter.base.GlUtil;
import com.cbw.mygl.R;

import java.nio.FloatBuffer;

/**
 * created by cbw on 2018/9/28
 * <p>
 * 画图片
 * 安卓y正方向朝下 跟gl的纹理坐标相反，为了画出来的图片正常 ，这里做了垂直翻转 {@link #mMvpMatrix}
 * 但是读取图片的时候要翻转回去
 */

public class PicFilter extends DefaultFilter {

    private float[] mMvpMatrix;
    private float[] modeMatrix;

    public PicFilter(Context context) {
        super(context);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.test);
        mTextureId = GlUtil.createTexture(getTextureTarget(), bitmap);

        mMvpMatrix = new float[16];
        Matrix.setIdentityM(mMvpMatrix, 0);

        modeMatrix = new float[16];
        Matrix.setIdentityM(modeMatrix, 0);
        Matrix.scaleM(modeMatrix, 0, 0.5f, 0.5f, 1);
        Matrix.scaleM(modeMatrix, 0, 1, -1f, 1);
    }

    @Override
    protected int createProgram(Context context) {
        return GlUtil.createProgram(vertexShaderCode2, fragmentShaderCode);
    }

    private int mTextureId;

    @Override
    protected void bindTexture(int textureId) {
        super.bindTexture(mTextureId);
    }

    @Override
    public void onDraw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {

        Matrix.multiplyMM(mMvpMatrix, 0, mvpMatrix, 0, modeMatrix, 0);
        super.onDraw(mMvpMatrix, vertexBuffer, firstVertex, vertexCount, coordsPerVertex, vertexStride, texMatrix, texBuffer, textureId, texStride);
    }
}
