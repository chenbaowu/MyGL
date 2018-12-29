package com.cbw.mygl;

import android.os.Build;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Created by zwq on 2016/05/05 15:13.<br/><br/>
 * 获取CPU信息
 */
public class CpuUtils {

    public static class CpuInfo {

        public String mProcessor = "";
        public int mProcessorCount = 0;
        public String mBogoMIPS = "";
        public String mImplementer = "";
        public String mArchitecture = "";
        public String mVariant = "";
        public String mPart = "";
        public String mRevision = "";
        public String mCpuAllInfo = "";

        /**
         * cpu 产商信息
         */
        public String mHardware = "";
        public int mCoresNum;//核数

    }

    private static CpuInfo mCpuInfo;

    public synchronized static CpuInfo getCpuInfo() {
        if (mCpuInfo == null) {
            mCpuInfo = new CpuInfo();

            mCpuInfo.mProcessorCount = 0;
            try {
                // 读取CPU信息
                Process pp = Runtime.getRuntime().exec("cat /proc/cpuinfo");
                InputStreamReader ir = new InputStreamReader(pp.getInputStream());
                LineNumberReader input = new LineNumberReader(ir);

                // 查找CPU序列号
                String str = null;
                StringBuffer buf = null;
                String[] temp = null;
                while ((str = input.readLine()) != null) {
                    if (buf == null) {
                        buf = new StringBuffer();
                    }
                    buf.append(str).append("\n");

                    temp = str.split(":");
                    if (temp == null || temp.length != 2 || temp[0] == null || temp[1] == null) {
                        continue;
                    }
                    if (temp[0].contains("Processor")) {//aarch64
                        mCpuInfo.mProcessor = temp[1];

                    } else if (temp[0].contains("processor")) {
                        mCpuInfo.mProcessorCount++;

                    } else if (temp[0].contains("BogoMIPS")) {
                        mCpuInfo.mBogoMIPS = temp[1];

                    } else if (temp[0].contains("implementer")) {
                        mCpuInfo.mImplementer = temp[1];

                    } else if (temp[0].contains("architecture")) {
                        mCpuInfo.mArchitecture = temp[1];

                    } else if (temp[0].contains("variant")) {
                        mCpuInfo.mVariant = temp[1];

                    } else if (temp[0].contains("part")) {
                        mCpuInfo.mPart = temp[1];

                    } else if (temp[0].contains("revision")) {
                        mCpuInfo.mRevision = temp[1];

                    } else if (temp[0].contains("Hardware")) {
                        //Hardware	: Qualcomm Technologies, Inc MSM8953
                        //Hardware	: Qualcomm Technologies, Inc MSM8994
                        //Hardware	: MT6795M
                        mCpuInfo.mHardware = temp[1].toUpperCase();
                    }
                }
                str = null;
                temp = null;
                if (mCpuInfo.mHardware.equals("")) {
                    mCpuInfo.mHardware = Build.HARDWARE.toUpperCase();//华为MHA-AL00:hi3600
                }
                if (buf != null) {
                    mCpuInfo.mCpuAllInfo = buf.toString();
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            mCpuInfo.mCoresNum = getNumberOfCPUCores();
        }

        // 单位mb
        //mMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024.0f / 1024.0f);

        return mCpuInfo;
    }

    /**
     * Reads the number of CPU cores from {@code /sys/devices/system/cpu/}.
     *
     * @return Number of CPU cores in the phone, or -1 in the event of an error.
     */
    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;
        }
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException e) {
            cores = -1;
        } catch (NullPointerException e) {
            cores = -1;
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

}
