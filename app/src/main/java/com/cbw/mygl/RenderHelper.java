package com.cbw.mygl;

import android.opengl.GLES20;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by: fwc
 * Date: 2018/6/12
 */
public class RenderHelper {

    /**
     * ex:18960 1:华为，8:8核，960:960系列<br/>
     * 1:华为、2:高通、3:MTK
     */
    private int mCpuLevel;
    private int mGpuLevel;//1:高、2:中、3:低

    private boolean mIsLive;

    /**
     * 分析 CPU 型号、核数
     */
    public void analyseDeviceInfo() {

        CpuUtils.CpuInfo cpuInfo = CpuUtils.getCpuInfo();
        if (cpuInfo == null) {
            return;
        }

        mIsLive = false;
        if (cpuInfo.mHardware != null) {
            try {
                if (cpuInfo.mHardware.contains("HI")) {
                    mCpuLevel = 18000;

                } else if (cpuInfo.mHardware.contains("QUALCOMM") || cpuInfo.mHardware.contains("MSM")) {
                    mCpuLevel = 28000;
                    if (cpuInfo.mHardware.contains("450")) {
                        mCpuLevel += 661;//骁龙450
                    } else if (cpuInfo.mHardware.contains("8956") || cpuInfo.mHardware.contains("8976")) {
                        mCpuLevel += 653;
                    } else {
                        mCpuLevel += 660;
                    }
                } else if (cpuInfo.mHardware.contains("MT")) {
                    mCpuLevel = 38000;
                    if (cpuInfo.mHardware.contains("6750") || cpuInfo.mHardware.contains("6755")) {
                        mCpuLevel += 860;
                    } else {
                        mCpuLevel += 880;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (cpuInfo.mCoresNum < 6 || cpuInfo.mProcessorCount < cpuInfo.mCoresNum) {
                mCpuLevel -= 4000;
            }
        }
    }

    /**
     * 分析 GPU 型号
     */
    public void analyseGPUInfo() {
        String glRenderer = GLES20.glGetString(GL10.GL_RENDERER);//GPU 渲染器
//        Log.i(TAG, "analyseGPUInfo: glRenderer:" + glRenderer);

//        String glVendor = GLES20.glGetString(GL10.GL_VENDOR);//GPU 供应商
//        String glVersion = GLES20.glGetString(GL10.GL_VERSION);//GPU 版本
//        String glExtensions = GLES20.glGetString(GL10.GL_EXTENSIONS);//GPU 扩展名
//        Log.i(TAG, "analyseGPUInfo: vendor:" + glVendor + ", renderer:" + glRenderer + ", ver:" + glVersion/*+", ext:"+glExtensions*/);

        //vendor:Qualcomm, renderer:Adreno (TM) 405, ver:OpenGL ES 3.2 V@145.0 (GIT@I8e5c908169)
        //vendor:ARM, renderer:Mali-T830, ver:OpenGL ES 3.2 v1.r12p1-04bet0.b125fee3fb58301c951089b9a402d362
        //vendor:Imagination Technologies, renderer:PowerVR Rogue G6200, ver:OpenGL ES 3.1 build 1.4@3300288

        int code = 0;
        try {
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(glRenderer);
            if (matcher.find()) {
                code = Integer.parseInt(matcher.group(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.i(TAG, "analyseGPUInfo: code:"+code);

        mGpuLevel = 0;//中等
        if (glRenderer.startsWith("Adreno")) {//Qualcomm
            //http://www.mydrivers.com/zhuanti/tianti/01/index_gaotong.html
            if (code <= 0) {
                String[] str = glRenderer.split(" ");
                String temp = str[str.length - 1];
                try {
                    code = Integer.parseInt(temp.replace("Adreno", "").replace("(TM)", "").trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (code >= 508) { //高
                mGpuLevel = 1;
            } else if (code >= 400) { //中
                mGpuLevel = 2;
//                if (code == 506 && mCpuLevel == 28661) {
//                    mGpuLevel = 1;
//                }
            } else { //低
                mGpuLevel = 3;
            }

        } else if (glRenderer.startsWith("Mali-G")) {//ARM
            //https://www.arm.com/products/graphics-and-multimedia/mali-gpu
            if (code <= 0) {
                String[] str = glRenderer.split(" ");//Mali-G71 MP20
                String temp = str[0];//str[str.length - 1];
                try {
                    code = Integer.parseInt(temp.trim().replace("Mali-G", "").trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if ((code >= 70 && code < 100) || (code >= 700 && code < 1000)) {
                mGpuLevel = 1;
            } else {
                mGpuLevel = 2;
            }
            if (mCpuLevel > 10000 && mCpuLevel <= 18000) {
                if (mGpuLevel == 1) {
                    mCpuLevel += 960;// >=960
                } else {
                    mCpuLevel += 930;// >=930 || >650
                }
            }

        } else if (glRenderer.startsWith("Mali-T") || glRenderer.startsWith("Mali-")) {//ARM
            //http://www.mydrivers.com/zhuanti/tianti/01/index_other.html
            if (code <= 0) {
                String[] str = glRenderer.split(" ");//Mali-T720 MP2
                String temp = str[0];//str[str.length - 1];
                try {
                    code = Integer.parseInt(temp.trim().replace("Mali-T", "").replace("Mali-", "").trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (code >= 860) { //高
                mGpuLevel = 1;
            } else if (code >= 830) { //中
                mGpuLevel = 2;
            } else { //低
                mGpuLevel = 3;
            }
            if (mCpuLevel > 10000 && mCpuLevel <= 18000) {
                if (code <= 880 && code >= 628) {
                    mCpuLevel += 930;// >=930 || >650
                }
            }

        } else if (glRenderer.startsWith("PowerVR")) {
            if (code <= 0) {
                String[] str = glRenderer.split(" ");
                String temp = str[str.length - 1];
                if (temp.startsWith("GT") || temp.startsWith("GX") || temp.startsWith("G")) {
                    try {
                        code = Integer.parseInt(temp.trim().replace("G", "").replace("X", "").trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (code > 6430) { //高
                mGpuLevel = 1;
            /*} else if (code >= 6200) { //中
                mGpuLevel = 2;*/
            } else { //低
                mGpuLevel = 3;
            }
        }
    }

    public float calculateRenderSizeScale(int width, int height) {
//        Log.i(TAG, "calculateRenderSizeScale: GPU Level:" + mGpuLevel + ", CPU Level:" + mCpuLevel);
        final float ratio = height * 1.0f / width;
        ;
        float mRenderScale = 1f;
        if (ratio == 1.0f) {
            mRenderScale = 1.0f;
        } else if (ratio == 4.0f / 3) {
            if (width > 1080) {//(1080, +] (1140P)    1440x2560 2k屏
                mRenderScale = 0.75f;//1440x1920 -> 1080x1440
                //mRenderScale = 0.5f;//1440x1920 -> 720x960
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (!mIsLive && mGpuLevel == 1) {
                    if (mCpuLevel >= 18960 || mCpuLevel >= 28660 || mCpuLevel >= 38880) {
                        mRenderScale = 1.0f;
                    }
                }
            } else if (width > 720) {//(720, 1080] (1080P)
                mRenderScale = 0.88888f;//1080x1440 -> 960x1280
                //mRenderScale = 0.8f;//1080x1440 -> 864x1152
                //mRenderScale = 0.75f;//1080x1440 -> 810x1080
                //mRenderScale = 0.66666f;//1080x1440 -> 720x960
                //mRenderScale = 0.55555f;//1080x1440 -> 600x800
                //mRenderScale = 0.44444f;//1080x1440 -> 480x640
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (mGpuLevel == 1) {
                    if ((mCpuLevel >= 18930 && mCpuLevel < 18960)) {
                        //mRenderScale = 0.8f;
                        mRenderScale = 0.75f;
                    } else if ((mCpuLevel >= 18000 && mCpuLevel < 18930)
                            || (mCpuLevel >= 28000 && mCpuLevel < 28660)
                            || (mCpuLevel >= 38000 && mCpuLevel < 38880)) {
                        mRenderScale = 0.75f;
                    } else if ((mCpuLevel >= 14000 && mCpuLevel < 18000)
                            //|| (mCpuLevel >= 24000 && mCpuLevel < 28000)
                            || (mCpuLevel / 10000 == 2 && mCpuLevel - (mCpuLevel / 1000 * 1000) < 660)
                            || (mCpuLevel >= 34000 && mCpuLevel < 38000)) {
                        mRenderScale = 0.66666f;
                    } else {
                        mRenderScale = 1.0f;
                    }
                } else if (mGpuLevel == 2) {
                    if ((mCpuLevel >= 10000 && mCpuLevel < 18960)) {
                        mRenderScale = 0.75f;
                    } else if ((mCpuLevel >= 18960 && mCpuLevel < 20000) || (mCpuLevel >= 28660 && mCpuLevel < 30000) || (mCpuLevel >= 38880)) {//红米note4x
                        mRenderScale = 0.75f;
                    } else {
                        mRenderScale = 0.66666f;
                    }
                } else if (mGpuLevel == 3) {
                    mRenderScale = 0.66666f;
                }
            } else if (width > 600) {//(600, 720] (720P)
                mRenderScale = 0.83333f;//720x960 -> 600x800
                //mRenderScale = 0.75f;//720x960 -> 540x720
                //mRenderScale = 0.66666f;//720x960 -> 480x640
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (mGpuLevel == 1) {
                    if (mCpuLevel >= 18960 || mCpuLevel >= 28660 || mCpuLevel >= 38880) {
                        mRenderScale = 1.0f;
                    }
                } else if (mGpuLevel == 2 || mGpuLevel == 3) {
                    if ((mCpuLevel >= 18000 && mCpuLevel < 20000) || (mCpuLevel >= 28660 && mCpuLevel < 30000) || (mCpuLevel >= 38880)) {//红米note4x
                        mRenderScale = 0.75f;
                    } else {
                        mRenderScale = 0.66666f;
                    }
                }
            }
        } else if (ratio == 16.0f / 9) {
            if (width > 1080) {
                mRenderScale = 0.75f;//1440x2560 -> 1080x1920
                //mRenderScale = 0.5f;//1440x2560 -> 720x1280
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (!mIsLive && mGpuLevel == 1) {
                    if (mCpuLevel >= 18960 || mCpuLevel >= 28660 || mCpuLevel >= 38880) {
                        mRenderScale = 1.0f;
                    }
                }
            } else if (width > 720) {
                //mRenderScale = 0.75f;//1080x1920 -> 810x1440
                mRenderScale = 0.66666f;//1080x1920 -> 720x1280
                //mRenderScale = 0.5f;//1080x1920 -> 540x960
                //mRenderScale = 0.44476f;//1080x1920 -> 480x854
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (mGpuLevel == 1) {
                    if ((mCpuLevel >= 18000 && mCpuLevel < 18960)
                            || (mCpuLevel >= 28000 && mCpuLevel < 28660)
                            || (mCpuLevel >= 38000 && mCpuLevel < 38880)) {
                        mRenderScale = 0.75f;
                    } else if ((mCpuLevel >= 14000 && mCpuLevel < 18000)
                            //|| (mCpuLevel >= 24000 && mCpuLevel < 28000)
                            || (mCpuLevel / 10000 == 2 && mCpuLevel - (mCpuLevel / 1000 * 1000) < 660)
                            || (mCpuLevel >= 34000 && mCpuLevel < 38000)) {
                        //mRenderScale = 0.5f;
                    } else {
                        mRenderScale = 1.0f;
                    }
                } else if (mGpuLevel == 2) {
                    if (mCpuLevel >= 18000 && mCpuLevel < 20000) {
                        mRenderScale = 0.75f;
                    }
                    //mRenderScale = 0.5f;// f23
                } else if (mGpuLevel == 3) {
                    //mRenderScale = 0.5f;
                    //mRenderScale = 0.44476f;// f27
                }
            } else if (width > 600) {
                mRenderScale = 0.75f;//720x1280 -> 540x960
                //mRenderScale = 0.66718f;//720x1280 -> 480x854
                //mRenderScale = 0.5f;//720x1280 -> 360x640
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (mGpuLevel == 1) {
                    if (mCpuLevel >= 18960 || mCpuLevel >= 28660 || mCpuLevel >= 38880) {
                        mRenderScale = 1.0f;
                    }
                } else if (mGpuLevel == 2) {
                    mRenderScale = 0.66718f;
                } else if (mGpuLevel == 3) {
                    mRenderScale = 0.5f;
                }
            }
        } else {
            if (width > 1080) {
                mRenderScale = 0.75f;
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (!mIsLive && mGpuLevel == 1) {
                    if (mCpuLevel >= 18960 || mCpuLevel >= 28660 || mCpuLevel >= 38880) {
                        mRenderScale = 1.0f;
                    }
                } else if (mGpuLevel == 2 || mGpuLevel == 3) {
                    //mRenderScale = 0.7f;
                    mRenderScale = 0.5f;
                }
            } else if (width > 720) {
                mRenderScale = 0.75f;
                //mRenderScale = 0.66666f;
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (mGpuLevel == 1) {
                    if ((mCpuLevel >= 18000 && mCpuLevel < 18960)
                            || (mCpuLevel >= 28000 && mCpuLevel < 28660)
                            || (mCpuLevel >= 38000 && mCpuLevel < 38880)) {
                        //mRenderScale = 0.75f;
                    } else {
                        mRenderScale = 1.0f;
                        //mRenderScale = 0.8f;
                        //mRenderScale = 0.75f;
                    }
                } else if (mGpuLevel == 2) {
                    if (mCpuLevel >= 18000 && mCpuLevel < 20000) {
                        mRenderScale = 0.75f;
                    } else {
                        mRenderScale = 0.66666f;
                    }
                    //mRenderScale = 0.5f;
                } else if (mGpuLevel == 3) {
                    mRenderScale = 0.66666f;
                    //mRenderScale = 0.5f;
                }
            } else if (width > 600) {
                mRenderScale = 0.66666f;
                if (mGpuLevel == 0) {
                    mRenderScale = 1.0f;
                } else if (mGpuLevel == 1) {
                    if (mCpuLevel >= 18960 || mCpuLevel >= 28660 || mCpuLevel >= 38880) {
                        mRenderScale = 1.0f;
                    }
                }
            }
        }

        return mRenderScale;
    }

}
