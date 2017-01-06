package com.speedata.libuhf.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.serialport.SerialPort;
import android.support.annotation.Nullable;
import android.util.Log;

import com.speedata.libuhf.DeviceControl;
import com.speedata.libuhf.utils.SharedXmlUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by 张明_ on 2016/12/19.
 */

public class GetModleService extends Service {
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        powerOn("/sys/class/misc/mtgpio/pin", 94);
        String modle = getModle();
        SharedXmlUtil.getInstance(this).write("modle", modle);

        Intent myIntent = new Intent();
        myIntent.setAction("com.speedata.libuhf.service.GetModleService");
        myIntent.setClass(this, GetModleService.class);
        stopService(myIntent);
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
        Log.d("getModle_start", String.valueOf(System.currentTimeMillis()));
        SerialPort serialPort = new SerialPort();
        try {
            serialPort.OpenSerial("/dev/ttyMT2", 115200);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fd = serialPort.getFd();
        byte[] bytes = new byte[1024];
        //判断是不是R2000
        serialPort.WriteSerialByte(fd, r2000_cmd);
        try {
            bytes = serialPort.ReadSerial(fd, 1024 * 2);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            factory = bytesToHexString(bytes);
        }
        if (factory.equals("240349006D00700069006E006A00530065007200690061006C004E0075006D0030003100")) {
            return FACTORY_R2000;
        }


        //判断是不是旗联-飞利信
        serialPort.WriteSerialByte(fd, feilixin_cmd);
        try {
            bytes = serialPort.ReadSerial(fd, 1024 * 2);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (bytes != null) {
            factory = bytesToHexString(bytes);
            if (factory.equals("BB01030008024D616769635266A77E")) {
                return FACTORY_FEILIXIN;
            }
        }

        //判断是不是旗联-芯联
        serialPort.WriteSerialByte(fd, xinlian_cmd);
        try {
            bytes = serialPort.ReadSerial(fd, 1024 * 2);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int length = 0;
        if (bytes != null) {
            length = bytes.length;
            if (length == 27) {
                return FACTORY_XINLIAN;
            }
        }

        serialPort.CloseSerial(fd);
        pw94.PowerOffDevice();
        Log.d("getModle_end", String.valueOf(System.currentTimeMillis()));
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
