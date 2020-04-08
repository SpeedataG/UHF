package com.speedata.libuhf;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.serialport.DeviceControlSpd;
import android.serialport.SerialPortSpd;
import android.util.Log;

import com.magicrf.uhfreaderlib.reader.UhfReader;
import com.magicrf.uhfreaderlib.readerInterface.CommendManager;
import com.speedata.libuhf.bean.SpdInventoryData;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;
import com.speedata.libuhf.utils.ByteCharStrUtils;
import com.speedata.libuhf.utils.CommonUtils;
import com.speedata.libuhf.utils.ConfigUtils;
import com.speedata.libuhf.utils.ReadBean;
import com.speedata.libuhf.utils.StringUtils;
import com.speedata.libutils.DataConversionUtils;
import com.uhf.speedatagapi.cls.Reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;

import static android.content.ContentValues.TAG;

public class QiLianSpd extends IUHFServiceAdapter {

    private static final String TAG = "QiLianSpd";
    private Context mContext;
    private ReadBean mRead;
    private ReaderParams Rparams = new ReaderParams();
    private DeviceControlSpd newUHFDeviceControl;
    private DeviceControlSpd deviceControlSpd;
    private SerialPortSpd serialPortSpd;
    private Handler handler = null;

    UhfReader uhfReader;

    public QiLianSpd(Context mContext) {
        this.mContext = mContext;
        Log.d("zzc", "QiLianSpd : QiLianSpd()");
    }

    //初始化模块
    @Override
    public int openDev() {
        Log.d(TAG, "openDev: start");
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            mRead = ConfigUtils.readConfig(mContext);
            String powerType = mRead.getUhf().getPowerType();
            int[] intArray = new int[mRead.getUhf().getGpio().size()];
            for (int i = 0; i < mRead.getUhf().getGpio().size(); i++) {
                intArray[i] = mRead.getUhf().getGpio().get(i);
            }
            try {
                newUHFDeviceControl = new DeviceControlSpd(powerType, intArray);
                newUHFDeviceControl.PowerOnDevice();
                serialPortSpd = new SerialPortSpd();
                serialPortSpd.OpenSerial(mRead.getUhf().getSerialPort(), 115200);
                uhfReader = UhfReader.getInstance();
                return 0;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            return NoXmlopenDev();
        }
    }

    private String readEm55() {
        String state = null;
        File file = new File("/sys/class/misc/aw9523/gpio");
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            state = bufferedReader.readLine();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("UHF", "readEm55state: " + state);
        return state;
    }

    private int NoXmlopenDev() {
        if (Build.VERSION.RELEASE.equals("4.4.2")) {
            try {
                deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String xinghao = SystemProperties.get("ro.product.model");
            if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || "SD55L".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                    || xinghao.contains("DXD60RT") || "ST60E".equalsIgnoreCase(xinghao) || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao) || "smo_b2000".equals(xinghao)) {
                try {
//                    deviceControlSpd = new UHFDeviceControl(UHFDeviceControl.PowerType.NEW_MAIN, 86);
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.EXPAND, 9, 14);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.equals("SD55PTT")) {
                try {
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ("SN50".equals(xinghao) || "SD50".equals(xinghao) || "R550".equals(xinghao)) {
                try {
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 74,75);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56") || xinghao.contains("iGM80")) {
                if (ConfigUtils.getApiVersion() > 23) {
                    try {
                        deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 12);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 128);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")
                    || xinghao.equals("Biowolf LE") || xinghao.equals("FC-PK80")
                    || xinghao.equals("FC-K80") || xinghao.equals("T80") || xinghao.contains("80")) {
                try {
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 119);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.contains("55") || xinghao.equals("W2H")) {
                String readEm55 = readEm55();
                if (readEm55.equals("80")) {
                    try {
                        deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 5);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (readEm55.equals("48") || readEm55.equals("81")) {
                    try {
                        deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 6);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 88);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (xinghao.equals("SD100")) {
                // TODO: 2018/10/10   上电处理
                try {
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.POWER_GAOTONG);
                    deviceControlSpd.gtPower("uhf_open");
                    deviceControlSpd.gtPower("open");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if ("SD100T".equals(xinghao) || "X47".equalsIgnoreCase(xinghao)) {
                try {
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 52, 89, 71);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 94);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (!SystemProperties.get("ro.product.model").equals("SD100")) {
                deviceControlSpd.PowerOffDevice();
                deviceControlSpd.PowerOnDevice();
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        SystemClock.sleep(20);
        String port;
        String xinghao = SystemProperties.get("ro.product.model");
        if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                || xinghao.contains("DXD60RT") || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao) || "ST60E".equalsIgnoreCase(xinghao) || "smo_b2000".equals(xinghao)) {
            port = "/dev/ttyMT0";
        } else if (xinghao.equals("SD55PTT")) {
            port = "/dev/ttyMT1";
        } else if ("SD50".equals(xinghao) || "SN50".equals(xinghao) || "R550".equals(xinghao)) {
            port = "/dev/ttyMT0";
        } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56") || xinghao.contains("iGM80")) {
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
        uhfReader = UhfReader.getInstance();
        try {
            serialPortSpd = new SerialPortSpd();
            serialPortSpd.OpenSerial(port, 115200);
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 下电关串口
     */
    @Override
    public void closeDev() {
        if (serialPortSpd != null) {
            serialPortSpd.CloseSerial(serialPortSpd.getFd());
        }
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            try {
                newUHFDeviceControl.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if ("SD100T".equals(SystemProperties.get("ro.product.model"))) {
                    deviceControlSpd.PowerOffDevice();
                    Log.d("UHF", "closeDev");
                } else if (SystemProperties.get("ro.product.model").equals("SD100")) {
                    deviceControlSpd.gtPower("uhf_close");
                    deviceControlSpd.gtPower("close");
                } else {
                    deviceControlSpd.PowerOffDevice();
                    Log.d("UHF", "closeDev");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private OnSpdInventoryListener onInventoryListener = null;
    private OnSpdReadListener onSpdReadListener = null;
    private OnSpdWriteListener onSpdWriteListener = null;


    /**
     * 设置盘点数据监听
     *
     * @param onSpdInventoryListener
     */
    @Override
    public void setOnInventoryListener(OnSpdInventoryListener onSpdInventoryListener) {
        this.onInventoryListener = onSpdInventoryListener;
    }

    /**
     * 开始盘点
     */
    @Override
    public void inventoryStart() {
//        if (inSearch) {
//            return;
//        }
//        if (handler == null) {
//            handler = new Handler();
//        }
//        inSearch = true;
////        cancelSelect();
//        Log.d(TAG, "inventory_start: inv_thread" + inv_thread);
//        handler.postDelayed(inv_thread, 0);
//        Log.d(TAG, "inventory_start: start" + handler);
        List<byte[]> list = uhfReader.inventoryRealTime();
        for (byte[] b : list) {
            String epc = StringUtils.byteToHexString(b, b.length);
            Log.e(TAG, "inventory_start: start" + StringUtils.byteToHexString(b, b.length));
            onInventoryListener.getInventoryData(new SpdInventoryData(null, epc, null));
        }
    }

    /**
     * 停止盘点
     */
    @Override
    public void inventoryStop() {
        if (!inSearch) {
            return;
        }
        Log.d(TAG, "inventory_stop: start");
        inSearch = false;
        try {
            if (handler != null) {
                handler.removeCallbacks(inv_thread);
                Log.d(TAG, "inventory_stop: end" + handler);
                handler = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private volatile boolean inSearch = false;
    private Runnable inv_thread = new Runnable() {
        @Override
        public void run() {

//            byte[] cmd = new byte[]{(byte) 0xBB, 0x00, 0x22, 0x00, 0x00, 0x22, 0x7E};
//            cmd[5] = checkSum(cmd);
//            Log.d("zzc", "inventoryOnce()===write===" + DataConversionUtils.byteArrayToString(cmd));
//            write(cmd);
//            byte[] res = read();
//            Log.d("zzc", "inventoryOnce()===read===" + DataConversionUtils.byteArrayToString(res));
//            if (res != null) {
//                List<byte[]> listByte = new ArrayList<>();
//                for (int i = 0; i < res.length; i++) {
//                    if (res[i] == (byte) 0xBB) {
//                        int head = i;
//                        while (res[i] != (byte) 0x7E) {
//                            i++;
//                        }
//                        int end = i + 1;
//                        listByte.add(StringUtils.subByteArray(res, head, end));
//                    }
//                }
//                for (byte[] re : listByte) {
//                    Log.d("zzc", "inventoryOnce()===listByte===" + DataConversionUtils.byteArrayToString(re));
//                    if (re[1] == 0x02) {
//                        if (checkSum(re) == re[re.length - 2]) {
//                            //校验值正确
//                            int len = StringUtils.byteArrayToInt(StringUtils.subByteArray(re, 3, 5));
//                            String rssi = (int) re[5] + "";
//                            byte[] pc = StringUtils.subByteArray(re, 6, 8);
//                            byte[] epcByte = StringUtils.subByteArray(re, 8, len + 3);
//                            byte[] crc = StringUtils.subByteArray(re, len + 3, len + 5);
//                            String epc = StringUtils.byteToHexString(epcByte, epcByte.length);
//                            SpdInventoryData spdInventoryData = new SpdInventoryData(null, epc, rssi);
//                            spdInventoryData.setPc(pc);
//                            onInventoryListener.getInventoryData(spdInventoryData);
//                        } else {
//                            //校验值错误
////                        onInventoryListener.onInventoryStatus(7);
//                        }
//                    }else {
//                        //响应帧
////                        onInventoryListener.onInventoryStatus(4);
//                    }
//
//                }
//
//
//            } else {
////                onInventoryListener.onInventoryStatus(4);
//            }
            if (handler != null) {
                handler.postDelayed(this, Rparams.sleep);
            }
        }
    };

    /**
     * 校验和
     *
     * @param data 去掉第一个和最后两个
     * @return
     */
    public byte checkSum(byte[] data) {
        byte crc = 0;
        for (int i = 1; i < data.length - 2; ++i) {
            crc += data[i];
        }
        return crc;
    }

    public void write(byte[] data) {
        if (serialPortSpd != null) {
            int fd = serialPortSpd.getFd();
            int res = serialPortSpd.WriteSerialByte(fd, data);
            Log.d("zzc", "write()---" + res);
        }
    }

    public byte[] read() {
        byte[] result = null;
        int index = 0;
        if (serialPortSpd != null) {
            while (index < 10) {
                try {
                    Thread.sleep(50);
                    result = serialPortSpd.ReadSerial(serialPortSpd.getFd(), 1024);
                    Log.d("zzc", "res---" + DataConversionUtils.byteArrayToString(result));
                    if (result != null) {
                        return result;
                    }
                } catch (UnsupportedEncodingException | InterruptedException e) {
                    e.printStackTrace();
                }
                index++;
            }
        }
        return result;
    }

    private class ReaderParams {
        public int sleep;

        public ReaderParams() {
            sleep = 0;
        }
    }

}
