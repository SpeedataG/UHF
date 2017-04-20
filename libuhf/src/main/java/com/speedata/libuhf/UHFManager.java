package com.speedata.libuhf;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.serialport.SerialPort;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.speedata.libuhf.utils.SharedXmlUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by brxu on 2016/12/13.
 */

public class UHFManager {
    private static IUHFService iuhfService;
    //飞利信读取制造商指令
    private static byte[] feilixin_cmd = {(byte) 0xbb, 0x00, 0x03, 0x00, 0x01, 0x02, 0x06, 0x7e};
    //R2000获取版本号
    private static byte[] r2000_cmd = {(byte) 0xC0, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    //芯联
    private static byte[] xinlian_cmd = {(byte) 0xFF, 0x00, 0x03, 0x1d, 0x0C};
    private final static String FACTORY_FEILIXIN = "FEILIXIN";
    private final static String FACTORY_XINLIAN = "XINLIAN";
    private final static String FACTORY_R2000 = "R2000";
    private final static String FACTORY_3992 = "3992";
    private static int fd;
    private static DeviceControl pw94;
    private static Context mContext;
    private static BatteryReceiver batteryReceiver;

    public static IUHFService getUHFService(Context context) {
        //  判断模块   返回不同的模块接口对象
        mContext = context;
        //注册广播接受者java代码
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //创建广播接受者对象
        batteryReceiver = new BatteryReceiver();
        //注册receiver
        context.registerReceiver(batteryReceiver,intentFilter);
        if (iuhfService == null) {
            if (!judgeModle())
                return null;
        }
        return iuhfService;
    }

    public static void closeUHFService() {
        iuhfService = null;
        mContext.unregisterReceiver(batteryReceiver);
    }
    /**
     * 广播接受者
     */
    static class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            //判断它是否是为电量变化的Broadcast Action
            if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                //获取当前电量
                int level = intent.getIntExtra("level", 0);
                if (level<20){
                    iuhfService.CloseDev();
                    Toast.makeText(mContext,"电量低禁用超高频",Toast.LENGTH_LONG).show();
                }
            }
        }

    }
    private static boolean judgeModle() {
        String factory = SharedXmlUtil.getInstance(mContext).read("modle", "");
        if (TextUtils.isEmpty(factory)) {
            Log.d("getModle_start", String.valueOf(System.currentTimeMillis()));
            if (android.os.Build.VERSION.RELEASE.equals("4.4.2")) {
                powerOn("/sys/class/misc/mtgpio/pin", 64);
            } else if (android.os.Build.VERSION.RELEASE.equals("5.1")) {
                String xinghao = Build.MODEL;
                if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")) {
                    powerOn("/sys/class/misc/mtgpio/pin", 119);
                } else if (xinghao.equals("KT55")){
                    powerOn("/sys/class/misc/mtgpio/pin", 88);
                } else {
                    powerOn("/sys/class/misc/mtgpio/pin", 94);
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
        boolean initResult = true;
        switch (factory) {
            case FACTORY_FEILIXIN:
                iuhfService = new FLX_QiLian();
                break;
            case FACTORY_XINLIAN:
                iuhfService = new XinLianQilian();
                break;
            case FACTORY_R2000:
                iuhfService = new R2K();
                break;
            case FACTORY_3992:
                iuhfService = new com.android.uhflibs.as3992_native();
                break;
            default:
                initResult = false;
                break;
        }
        return initResult;
    }

    private static void powerOn(String POWERCTL, int PW_GPIO) {
        pw94 = new DeviceControl(POWERCTL, PW_GPIO);
        pw94.PowerOnDevice();
    }


    /**
     * @return 返回厂家信息
     */
    private static String getModle() {
        String factory = "";
        SerialPort serialPort = new SerialPort();
        try {
            serialPort.OpenSerial("/dev/ttyMT2", 115200);
        } catch (IOException e) {
            e.printStackTrace();
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
        if (factory.equals("240349006D00700069006E006A00530065007200690061006C004E0075006D0030003100")) {
            serialPort.CloseSerial(fd);
            pw94.PowerOffDevice();
            return FACTORY_R2000;
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
            bytes = serialPort.ReadSerial(fd, 1024);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            factory = bytesToHexString(bytes);
            if (factory.equals("BB0103000A025261794D6174726978B17E")) {
                serialPort.CloseSerial(fd);
                pw94.PowerOffDevice();
                return FACTORY_FEILIXIN;
            }
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
            bytes = serialPort.ReadSerial(fd, 1024);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int length = 0;
        if (bytes != null) {
            length = bytes.length;
            if (length == 27) {
                serialPort.CloseSerial(fd);
                pw94.PowerOffDevice();
                return FACTORY_XINLIAN;
            }
        }

        serialPort.CloseSerial(fd);
        pw94.PowerOffDevice();
        return FACTORY_3992;
    }

    public static final String bytesToHexString(byte[] bArray) {
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
