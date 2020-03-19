package com.speedata.libuhf;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemProperties;
import android.serialport.DeviceControlSpd;
import android.serialport.SerialPortSpd;
import android.text.TextUtils;
import android.util.Log;

import com.speedata.libuhf.interfaces.OnSpdBanMsgListener;
import com.speedata.libuhf.utils.CommonUtils;
import com.speedata.libuhf.utils.ConfigUtils;
import com.speedata.libuhf.utils.DataConversionUtils;
import com.speedata.libuhf.utils.ReadBean;
import com.speedata.libuhf.utils.SharedXmlUtil;
import com.speedata.libuhf.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import cn.com.example.rfid.driver.Driver;
import cn.com.example.rfid.driver.RfidDriver;

import static android.content.ContentValues.TAG;

/**
 * @author brxu
 * @date 2016/12/13
 */

public class UHFManager {
    private static IUHFService iuhfService;
    /**
     * 飞利信读取制造商指令
     */
    private static byte[] feilixin_cmd = {(byte) 0xbb, 0x00, 0x03, 0x00, 0x01, 0x02, 0x06, 0x7e};
    /**
     * R2000获取版本号
     */
    private static byte[] r2000_cmd = {(byte) 0x7e, 0x00, 0x0e, (byte) 0xC0, 0x06, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, (byte) 0xd4, 0X7e};
    /**
     * 芯联
     */
    private static byte[] xinlian_cmd = {(byte) 0xFF, 0x00, 0x03, 0x1d, 0x0C};

    public final static String FACTORY_FEILIXIN = "feilixin";
    public final static String FACTORY_XINLIAN = "xinlian";
    public final static String FACTORY_XINLIAN_R2K = "xinlian_r2k";
    public final static String FACTORY_R2000 = "r2k";
    public final static String FACTORY_YIXIN = "yixin";
    public final static String FACTORY_3992 = "as3992";
    private static int fd;
    private static DeviceControlSpd pw;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private static BatteryReceiver batteryReceiver;
    private static ReadBean mRead;
    private static String factory;
    private static volatile int stipulationLevel = 15;
    private static volatile int TemperatureLevel = 75;
    private static volatile int battTemperatureLevel = 55;
    private static Timer timer;
    private static TimerTask myTimerTask;


    public static IUHFService getUHFService(Context context) {

        if (iuhfService == null) {
            //  判断模块   返回不同的模块接口对象
            mContext = context.getApplicationContext();
            createTempTimer();
            if (!judgeModel()) {
                return null;
            }
        }
        return iuhfService;
    }

    /**
     * 读内核信息获取CPU温度
     *
     * @return
     */
    private static String getCpuTemp() {
        String temp = "Unknow";
        float tempFloat = 0;
        try {
            File dir = new File("/sys/class/thermal/");
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (Pattern.matches("thermal_zone[0-9]+", file.getName())) {
                        return true;
                    }
                    return false;
                }
            });

            final int SIZE = files.length;
            String line = "";
            String type = "";
            for (int i = 0; i < SIZE; i++) {
                FileReader frType = new FileReader("/sys/class/thermal/thermal_zone" + i + "/type");
                BufferedReader brType = new BufferedReader(frType);
                line = brType.readLine();
                if (line != null) {
                    type = line;
                }
                //不再使用的时候要及时释放对象
                try {
                    frType.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    brType.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                FileReader frTemp = new FileReader("/sys/class/thermal/thermal_zone" + i + "/temp");
                BufferedReader brTemp = new BufferedReader(frTemp);
                line = brTemp.readLine();
                try {
                    frTemp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    brTemp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (line != null) {
                    // MTK CPU mt6356tsbuck1 mt6356tsbuck2
                    if (type.contains("mt6356tsbuck")) {
                        long temperature = Long.parseLong(line);
                        if (temperature < 0) {
                            temp = "Unknow";
                            tempFloat = 0;
                        } else {
                            if ((float) (temperature / 1000.0) > tempFloat) {
                                temp = (float) (temperature / 1000.0) + "";
                                tempFloat = (float) (temperature / 1000.0);
                            }
                        }
                    } else if (type.contains("tsens_tz_sensor")) {
                        // Qualcomm CPU
                        long temperature = Long.parseLong(line);
                        if (temperature < 0) {
                            temp = "Unknow";
                        } else if (temperature > 100) {
                            temp = (float) (temperature / 10.0) + "";
                        } else {
                            temp = temperature + "";
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    private static void createTempTimer() {
        if (timer == null) {
            timer = new Timer();
            if (myTimerTask != null) {
                myTimerTask.cancel();
            }
            myTimerTask = new TimerTask() {
                @Override
                public void run() {
                    String mtTemp = "Unknow";
                    double mtTemperature = 0.0;
                    try {
                        if (ConfigUtils.getApiVersion() > 23) {
                            mtTemp = getCpuTemp();
                            if (!"Unknow".equals(mtTemp)) {
                                mtTemperature = Double.parseDouble(mtTemp);
                            }
                            if (mtTemperature >= TemperatureLevel) {
                                stopUseUHFByTemp();
                            }
                        } else {
                            InputStream battTempFile = new FileInputStream("sys/class/power_supply/battery/batt_temp");
                            String battTempFileStr = convertStreamToString(battTempFile);
                            double t = Integer.parseInt(battTempFileStr) / 10.0;
                            if (t >= battTemperatureLevel) {
                                stopUseUHFByTemp();
                            }
                            battTempFile.close();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            timer.schedule(myTimerTask, 0, 1000);
        }
    }


    public static void stopTimer() {
        if (myTimerTask != null) {
            myTimerTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

    }


    public static void closeUHFService() {
        iuhfService = null;
        stopTimer();
    }

    public static void unregisterReceiver() {
        if (batteryReceiver != null) {
            mContext.unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
        }
    }

    public static void setIuhfServiceNull() {
        iuhfService = null;
    }

    public static void setStipulationLevel(int level) {
        stipulationLevel = level;
    }

    public static void setTemperatureLevel(int temp) {
        TemperatureLevel = temp;
    }

    public static void setBattTemperatureLevel(int battTemperatureLevel) {
        UHFManager.battTemperatureLevel = battTemperatureLevel;
    }

    /**
     * 广播接受者
     */
    static class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            //判断它是否是为电量变化的Broadcast Action
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                try {
                    //获取当前电量
                    int level = intent.getIntExtra("level", 0);
                    if (level < stipulationLevel) {
                        stopUseUHFByBattery();
                    }
                    Log.d("zzc:", "level: " + level);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void stopUseUHFByTemp() {
        if (iuhfService != null) {
            iuhfService.closeDev();
            Log.e("zzc:", "UHFService===closeDev()===high_temp");
        }
        if (onSpdBanMsgListener != null) {
            callBack("High temperature UHF is forbidden");
        }
    }

    private static void stopUseUHFByBattery() {
        if (iuhfService != null) {
            iuhfService.closeDev();
            Log.e("zzc:", "UHFService===closeDev()===low_power");
        }
        if (onSpdBanMsgListener != null) {
            callBack("Low power UHF is forbidden");
        }
    }

    private static OnSpdBanMsgListener onSpdBanMsgListener = null;

    public void setOnBanMsgListener(OnSpdBanMsgListener onSpdBanMsgListener) {
        UHFManager.onSpdBanMsgListener = onSpdBanMsgListener;
    }

    private static OnSpdBanMsgListener getOnBanMsgListener() {
        return onSpdBanMsgListener;
    }

    private static void callBack(String msg) {
        if (onSpdBanMsgListener != null && getOnBanMsgListener() != null) {
            getOnBanMsgListener().getBanMsg(msg);
        }

    }


    private static boolean judgeModel() {
        noXmlJudgeModule();
//        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
//            mRead = ConfigUtils.readConfig(mContext);
//            factory = mRead.getUhf().getModule();
//            SharedXmlUtil.getInstance(mContext).write("model", factory);
//        } else {
//            //沒有配置文件判断模块
//            noXmlJudgeModule();
//        }

        boolean initResult = true;
        switch (factory) {
            case FACTORY_FEILIXIN:
                iuhfService = new FLX(mContext, 1);
                break;
            case FACTORY_XINLIAN:
            case FACTORY_XINLIAN_R2K:
                iuhfService = new XinLianQilian(mContext);
                break;
            case FACTORY_R2000:
                iuhfService = new FLX(mContext, 0);
                break;
            case FACTORY_YIXIN:
                // TODO: 2019/8/26   初始化一芯sdk
                iuhfService = new YiXin(mContext);
                break;
            default:
                initResult = false;
                break;
        }
        return initResult;
    }

    private static String readEm55() {
        String state = null;
        File file = new File("/sys/class/misc/aw9523/gpio");
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            state = bufferedReader.readLine();
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "readEm55state: " + state);
        return state;
    }

    private static void noXmlJudgeModule() {
        factory = getUHFModel();
        if (TextUtils.isEmpty(factory)) {
            Log.d("ZM", String.valueOf(System.currentTimeMillis()));
            if (Build.VERSION.RELEASE.equals("4.4.2")) {
                powerOn(DeviceControlSpd.PowerType.MAIN, 64);
            } else {
                String xinghao = SystemProperties.get("ro.product.model");
                Log.d("ZM", "Build.MODEL: " + xinghao);
                if ("SC200T".equals(xinghao) || "iPick".equals(xinghao)) {
                    factory = getModel();
                    SharedXmlUtil.getInstance(mContext).write("model", factory);
                    Log.d("ZM", String.valueOf(System.currentTimeMillis()));
                    return;
                }
                if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || "SD55L".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                        || xinghao.contains("DXD60RT") || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao) || "ST60E".equalsIgnoreCase(xinghao) || "smo_b2000".equals(xinghao)) {
                    powerOn(DeviceControlSpd.PowerType.EXPAND, 9, 14);

                } else if (xinghao.equals("SD55PTT")) {
                    powerOn(DeviceControlSpd.PowerType.NEW_MAIN, 8);
                } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56")) {
                    if (ConfigUtils.getApiVersion() > 23) {
                        powerOn(DeviceControlSpd.PowerType.NEW_MAIN, 12);
                    } else {
                        powerOn(DeviceControlSpd.PowerType.MAIN, 128);
                    }
                } else if ("SD50".equals(xinghao) || "SN50".equals(xinghao) || "R550".equals(xinghao)) {
                    powerOn(DeviceControlSpd.PowerType.NEW_MAIN, 75);
                } else if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")
                        || xinghao.equals("Biowolf LE") || xinghao.equals("FC-PK80")
                        || xinghao.equals("FC-K80") || xinghao.equals("T80") || xinghao.contains("80")) {
                    powerOn(DeviceControlSpd.PowerType.MAIN, 119);
                } else if (xinghao.contains("55") || xinghao.equals("W2H")) {
                    String readEm55 = readEm55();
                    if (readEm55.equals("80")) {
                        powerOn(DeviceControlSpd.PowerType.MAIN_AND_EXPAND, 88, 7, 5);
                    } else if (readEm55.equals("48") || readEm55.equals("81")) {
                        powerOn(DeviceControlSpd.PowerType.MAIN_AND_EXPAND, 88, 6, 7);
                    } else {
                        powerOn(DeviceControlSpd.PowerType.MAIN, 88);
                    }

                } else if (xinghao.equals("SD100")) {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.POWER_GAOTONG);
                        pw.gtPower("uhf_open");
                        pw.gtPower("open");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("ZM", "SD100 powerOn-Exception: " + e.toString());
                    }
                } else if (xinghao.equals("SD100T") || xinghao.equals("X47")) {
                    powerOn(DeviceControlSpd.PowerType.NEW_MAIN, 52, 89, 71);
                } else {
                    powerOn(DeviceControlSpd.PowerType.MAIN, 94);
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            factory = getModel();
            SharedXmlUtil.getInstance(mContext).write("model", factory);
            Log.d("ZM", String.valueOf(System.currentTimeMillis()));
        }
    }

    private static void powerOn(DeviceControlSpd.PowerType POWERCTL, int... gpios) {
        try {
            pw = new DeviceControlSpd(POWERCTL, gpios);
            pw.PowerOnDevice();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ZM", "powerOn-Exception: " + e.toString());
        }
    }


    /**
     * @return 返回厂家信息
     */
    private static String getModel() {
        String factory = "";
        SerialPortSpd serialPort = new SerialPortSpd();
        String port = "/dev/ttyMT0";
        String xinghao = SystemProperties.get("ro.product.model");
        if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                || xinghao.contains("DXD60RT") || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao) || "ST60E".equalsIgnoreCase(xinghao) || "smo_b2000".equals(xinghao)) {
            port = "/dev/ttyMT0";
        } else if (xinghao.equals("SD55PTT")) {
            port = "/dev/ttyMT1";
        } else if ("SD50".equals(xinghao) || "SN50".equals(xinghao) || "R550".equals(xinghao)) {
            port = "/dev/ttyMT0";
        } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56")) {
            if (ConfigUtils.getApiVersion() > 23) {
                port = "/dev/ttyMT0";
            } else {
                port = "/dev/ttyMT2";
            }
        } else if ("SD100".equalsIgnoreCase(xinghao)) {
            port = "/dev/ttyHSL2";
        } else if ("SD100T".equalsIgnoreCase(xinghao) || "X47".equalsIgnoreCase(xinghao)) {
            port = "/dev/ttyMT0";
        } else if ("SC200T".equalsIgnoreCase(xinghao) || "iPick".equalsIgnoreCase(xinghao)) {
            port = "/dev/ttyMT7";
        } else {
            port = "/dev/ttyMT2";
        }
        try {
            serialPort.OpenSerial(port, 115200);
//            serialPort.OpenSerial(port, 9600);
            fd = serialPort.getFd();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = new byte[128];

        //判断是不是R2000
        serialPort.clearPortBuf(fd);
        serialPort.WriteSerialByte(fd, r2000_cmd);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bytes = serialPort.ReadSerial(fd, 128);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            factory = bytesToHexString(bytes);
        }
        Log.d("ZM", "判断是不是R2000: " + factory);
        if ("7E002A240349006D00700069006E006A00530065007200690061006C004E0075006D003000310006A97E".equals(factory)) {
            serialPort.CloseSerial(fd);
            powerOff();
            return FACTORY_R2000;
        } else if ("7E0028220342004C0046005F00320030003100380030003300310033005F0030003000310004027E".equals(factory)) {
            serialPort.CloseSerial(fd);
            powerOff();
            return FACTORY_R2000;
        }

        //判断是不是旗联-芯联
        serialPort.clearPortBuf(fd);
        serialPort.WriteSerialByte(fd, xinlian_cmd);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bytes = serialPort.ReadSerial(fd, 30);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int length;
        if (bytes != null) {
            length = bytes.length;
            Log.d("ZM", "判断是不是旗联-芯联 length: " + length);
            if (length == 27) {
                String hexStr = Integer.toHexString(bytes[9] & 0xFF);
//
//                serialPort.clearPortBuf(fd);
//                serialPort.WriteSerialByte(fd, new byte[]{(byte) 0xFF, 0x14, (byte) 0xAA, 0x4D, 0x6F, 0x64, 0x75, 0x6C, 0x65, 0x74, 0x65,
//                        0x63, 0x68, (byte) 0xAA, 0x40, 0x06, 0x01, 0x00, 0x00, 0x25, (byte) 0x80, (byte) 0x96, (byte) 0xBB, (byte) 0x90, 0x76});
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                try {
//                    bytes = serialPort.ReadSerial(fd, 30);
//
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
                serialPort.CloseSerial(fd);
                powerOff();
//                String r = "FF0EAA00004D6F64756C6574656368AA400601C35F";
//                if (bytes != null) {
//                    if (!bytesToHexString(bytes).equals(r)) {
//                        return FACTORY_XINLIAN_R2K + "-失败";
//                    }
//                }
                if ("A0".equalsIgnoreCase(hexStr) || "A1".equalsIgnoreCase(hexStr)) {
                    return FACTORY_XINLIAN_R2K;
                }
                return FACTORY_XINLIAN;
            }
        }

        //判断是不是旗联-飞利信
        serialPort.clearPortBuf(fd);
        serialPort.WriteSerialByte(fd, feilixin_cmd);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bytes = serialPort.ReadSerial(fd, 17);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            factory = bytesToHexString(bytes);
            Log.d("ZM", "判断是不是旗联-飞利信: " + factory);
            if ("BB0103000A025261794D6174726978B17E".equals(factory)) {
                serialPort.CloseSerial(fd);
                powerOff();
                return FACTORY_FEILIXIN;
            }
        }

        serialPort.CloseSerial(fd);
        //判断是不是一芯uhf
        if (!getyiXinVersion(port).isEmpty()) {
            return FACTORY_YIXIN;
        }
        powerOff();
        return null;
    }

    /**
     * 读取判断一芯uhf版本
     *
     * @return 一芯固件版本
     */
    private static String getyiXinVersion(String serialPort) {
        String fwBuffer;
        Driver driver = new RfidDriver();
        int status = driver.initRFID(serialPort);
        if (-1000 == status) {
            return "";
        }
        fwBuffer = driver.readUM7fwOnce();
        if (fwBuffer.equals("-1000")) {
            Log.d("ZM", "判断是不是一芯: " + fwBuffer);
            return "";
        } else if (fwBuffer.equals("-1020")) {
            Log.d("ZM", "判断是不是一芯: " + fwBuffer);
            return "";
        }

        StringBuilder fw = new StringBuilder(fwBuffer);
        fw.insert(0, "固件版本号：");
        fw.insert(8, ".");
        fw.insert(11, ".");
        String Sfw = fw.toString();
        Log.d("ZM", "判断是不是一芯: " + Sfw);
        driver.Close_Com();
        return Sfw;
    }

    private static void powerOff() {
        try {
            String xinghao = SystemProperties.get("ro.product.model");
            if ("SC200T".equalsIgnoreCase(xinghao) || "iPick".equalsIgnoreCase(xinghao)) {
                return;
            }
            if (SystemProperties.get("ro.product.model").equals("SD100")) {
                pw.gtPower("uhf_close");
                pw.gtPower("close");
            } else {
                pw.PowerOffDevice();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ZM", "powerOff-Exception: " + e.toString());
        }
    }

    //byte转string
    private static String bytesToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (byte aBArray : bArray) {
            sTemp = Integer.toHexString(0xFF & aBArray);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static String getUHFModel() {
        return SharedXmlUtil.getInstance(mContext).read("model", "");
    }
}
