package com.cbw.glRender;

/**
 * created by cbw on 2018/9/29
 * 绘制顺序/层级
 */

public class FilterDrawOrder {

    public static final int FILTER_INVALID = -1;
    public static final int FILTER_OES = 0; // 数据源
//    public static final int FILTER_YUV = 0; // 数据源
    public static final int FILTER_TRIANGLE = 1; // 三角形
    public static final int FILTER_PIC = 2; // 图片

    public static final int FILTER_DISPLAY = 3; // 显示最终图像、人脸框、水印
    public static final int FILTER_MAX_LAYER = 3; // 总共有几种filter
}
