package com.speedata.libuhf;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.serialport.DeviceControlSpd;
import android.serialport.SerialPortSpd;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.power.control.DeviceControl;
import com.speedata.libuhf.utils.CommonUtils;
import com.speedata.libuhf.utils.ConfigUtils;
import com.speedata.libuhf.utils.ReadBean;
import com.speedata.libuhf.utils.SharedXmlUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static android.content.ContentValues.TAG;

/**
 * Created by brxu on 2016/12/13.
 */

public class UHFManager {
    private static IUHFService iuhfService;
    //飞利信读取制造商指令
    private static byte[] feilixin_cmd = {(byte) 0xbb, 0x00, 0x03, 0x00, 0x01, 0x02, 0x06, 0x7e};
    //R2000获取版本号
    private static byte[] r2000_cmd = {(byte) 0x7e, 0x00, 0x0e, (byte) 0xC0, 0x06, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, (byte) 0xd4, 0X7e};
    //芯联
    private static byte[] xinlian_cmd = {(byte) 0xFF, 0x00, 0x03, 0x1d, 0x0C};

    private final static String FACTORY_FEILIXIN = "feilixin";
    private final static String FACTORY_XINLIAN = "xinlian";
    private final static String FACTORY_R2000 = "r2k";
    private final static String FACTORY_3992 = "as3992";
    private static int fd;
    private static DeviceControlSpd pw;
    private static Context mContext;
    private static BatteryReceiver batteryReceiver;
    private static ReadBean mRead;
    private static String factory;
    private static volatile int stipulationLevel = 15;


    public static IUHFService getUHFService(Context context) {
        //  判断模块   返回不同的模块接口对象
        mContext = context.getApplicationContext();
        //注册广播接受者java代码
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //创建广播接受者对象
        batteryReceiver = new BatteryReceiver();
        //注册receiver
        mContext.registerReceiver(batteryReceiver, intentFilter);
        if (iuhfService == null) {
            if (!judgeModle())
                return null;
        }
        return iuhfService;
    }

    public static void closeUHFService() {
        iuhfService = null;
        unregisterReceiver();
    }

    public static void unregisterReceiver() {
        mContext.unregisterReceiver(batteryReceiver);
    }

    public static void setIuhfServiceNull() {
        iuhfService = null;
    }

    public static void setStipulationLevel(int level) {
        stipulationLevel = level;
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
                        if (iuhfService != null) {
                            iuhfService.closeDev();
                        }
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                boolean cn = mContext.getResources().getConfiguration().locale.getCountry().equals("CN");
                                if (cn) {
                                    Toast.makeText(mContext, "电量低禁用超高频", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(mContext, "Low power UHF is forbidden", Toast.LENGTH_LONG).show();
                                }

                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static boolean judgeModle() {
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            mRead = ConfigUtils.readConfig(mContext);
            factory = mRead.getUhf().getModule();
            SharedXmlUtil.getInstance(mContext).write("modle", factory);
        } else {
            //沒有配置文件判断模块
            noXmlJudgeModule();
        }

        boolean initResult = true;
        switch (factory) {
            case FACTORY_FEILIXIN:
                iuhfService = new FLX(mContext, 1);
                break;
            case FACTORY_XINLIAN:
                iuhfService = new XinLianQilian(mContext);
                break;
            case FACTORY_R2000:
                iuhfService = new FLX(mContext, 0);
                break;
//            case FACTORY_3992:
//                iuhfService = new com.android.uhflibs.as3992_native(mContext);
//                break;
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
        factory = SharedXmlUtil.getInstance(mContext).read("modle", "");
        if (TextUtils.isEmpty(factory)) {
            Log.d("getModle_start", String.valueOf(System.currentTimeMillis()));
            if (Build.VERSION.RELEASE.equals("4.4.2")) {
                powerOn(DeviceControlSpd.PowerType.MAIN, 64);
            } else {
                String xinghao = Build.MODEL;
                if (xinghao.equalsIgnoreCase("SD60RT") || xinghao.equalsIgnoreCase("SD60")) {
//                    powerOn(UHFDeviceControl.PowerType.NEW_MAIN, 86);
                    powerOn(DeviceControlSpd.PowerType.EXPAND, 9, 14);

                } else if (xinghao.contains("SD55L")) {
                    powerOn(DeviceControlSpd.PowerType.MAIN, 128);
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

                } else if (xinghao.contains("SD100")) {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.PowerType.GAOTONG_MAIN);
                        pw.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    powerOn(DeviceControlSpd.PowerType.MAIN, 94);
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            factory = getModle();
            SharedXmlUtil.getInstance(mContext).write("modle", factory);
            Log.d("getModle_end", String.valueOf(System.currentTimeMillis()));
        }
    }

    private static void powerOn(DeviceControlSpd.PowerType POWERCTL, int... gpios) {
        try {
            pw = new DeviceControlSpd(POWERCTL, gpios);
            pw.PowerOnDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @return 返回厂家信息
     */
    private static String getModle() {
        String factory = "";
        SerialPortSpd serialPort = new SerialPortSpd();
        String xinghao = Build.MODEL;
        if (xinghao.equalsIgnoreCase("SD60RT") || xinghao.equalsIgnoreCase("SD60")) {
            try {
                serialPort.OpenSerial("/dev/ttyMT0", 115200);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (xinghao.equalsIgnoreCase("SD100")) {
            try {
                serialPort.OpenSerial("/dev/ttyHSL2", 115200);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                serialPort.OpenSerial("/dev/ttyMT2", 115200);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fd = serialPort.getFd();
        byte[] bytes = new byte[1024];

        //判断是不是R2000
        serialPort.clearPortBuf(fd);
        serialPort.WriteSerialByte(fd, r2000_cmd);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bytes = serialPort.ReadSerial(fd, 1024);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            factory = bytesToHexString(bytes);
        }
        if (factory.equals("7E002A240349006D00700069006E006A00530065007200690061006C004E0075006D003000310006A97E")) {
            serialPort.CloseSerial(fd);
            powerOff();
            return FACTORY_R2000;
        } else if (factory.equals("7E0028220342004C0046005F00320030003100380030003300310033005F0030003000310004027E")) {
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
        int length = 0;
        if (bytes != null) {
            length = bytes.length;
            if (length == 27) {
                serialPort.CloseSerial(fd);
                powerOff();
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
            if (factory.equals("BB0103000A025261794D6174726978B17E")) {
                serialPort.CloseSerial(fd);
                powerOff();
                return FACTORY_FEILIXIN;
            }
        }


        serialPort.CloseSerial(fd);
        powerOff();
        return null;
    }

    private static void powerOff() {
        try {
            pw.PowerOffDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //byte转string
    private static String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }
}
