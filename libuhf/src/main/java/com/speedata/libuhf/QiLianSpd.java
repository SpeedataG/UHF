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
import com.speedata.libuhf.bean.SpdReadData;
import com.speedata.libuhf.bean.SpdWriteData;
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
import java.util.Arrays;
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
                UhfReader.setPortPath(mRead.getUhf().getSerialPort());
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
                    deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 74, 75);
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
        UhfReader.setPortPath(port);
        uhfReader = UhfReader.getInstance();
        return 0;
    }

    /**
     * 下电关串口
     */
    @Override
    public void closeDev() {
        if (uhfReader != null) {
            uhfReader.close();
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
        if (inSearch) {
            return;
        }
        if (handler == null) {
            handler = new Handler();
        }
        inSearch = true;
        Log.d(TAG, "inventory_start: inv_thread" + inv_thread);
        handler.postDelayed(inv_thread, 0);
        Log.d(TAG, "inventory_start: start" + handler);
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
        uhfReader.stopInventoryMulti();
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

    @Override
    public int selectCard(int bank, String epc, boolean mFlag) {
        byte[] data = DataConversionUtils.HexString2Bytes(epc);
        return selectCard(bank, data, mFlag);
    }

    @Override
    public int selectCard(int bank, byte[] epc, boolean mFlag) {
        if (mFlag) {
            if (bank == 1) {
                uhfReader.selectEpc(epc);
            } else {
                uhfReader.setSelectPara((byte) 0, (byte) 0, (byte) bank, 32, (byte) (epc.length * 8), false, epc);
            }
            uhfReader.setSelectMode((byte) 0x00);
        } else {
            uhfReader.unSelect();
        }
        return 0;
    }

    @Override
    public void setOnReadListener(OnSpdReadListener onSpdReadListener) {
        this.onSpdReadListener = onSpdReadListener;
    }

    @Override
    public int readArea(int area, int addr, int count, String passwd) {
        byte[] password = DataConversionUtils.HexString2Bytes(passwd);
        byte[] readData = uhfReader.readFrom6C(area, addr, count, password);
        Log.d(TAG, "readArea:" + Arrays.toString(readData));
        SpdReadData spdReadData = new SpdReadData();
        if (readData != null) {
            spdReadData.setStatus(0);
            spdReadData.setReadData(readData);
            spdReadData.setDataLen(readData.length);
        } else {
            spdReadData.setStatus(-1);
        }
        onSpdReadListener.getReadData(spdReadData);
        return 0;
    }

    @Override
    public int writeArea(int area, int addr, int count, String passwd, byte[] content) {
        byte[] password = DataConversionUtils.HexString2Bytes(passwd);
        boolean res = uhfReader.writeTo6C(password, area, addr, count, content);
        int i = res ? 0 : -1;
        SpdWriteData spdWriteData = new SpdWriteData();
        spdWriteData.setStatus(i);
        onSpdWriteListener.getWriteData(spdWriteData);
        return i;
    }

    @Override
    public int setLock(int type, int area, String passwd) {
        byte[] password = DataConversionUtils.HexString2Bytes(passwd);
        boolean res = uhfReader.lock6C(password, area, type);
        int i = res ? 0 : -1;
        SpdWriteData spdWriteData = new SpdWriteData();
        spdWriteData.setStatus(i);
        onSpdWriteListener.getWriteData(spdWriteData);
        return i;
    }

    @Override
    public int setKill(String accessPassword, String killPassword) {
        byte[] password = DataConversionUtils.HexString2Bytes(killPassword);
        boolean res = uhfReader.kill6C(password);
        return res ? 0 : -1;
    }

    @Override
    public int setFreqRegion(int region) {
        boolean res = uhfReader.setFrequency(region);
        return res ? 0 : -1;
    }

    @Override
    public int getFreqRegion() {
        return uhfReader.getFrequency();
    }

    @Override
    public int setAntennaPower(int power) {
        boolean res = uhfReader.setOutputPower(power * 100);
        return res ? 0 : -1;
    }

    private volatile boolean inSearch = false;
    private Runnable inv_thread = new Runnable() {
        @Override
        public void run() {
            List<byte[]> list = uhfReader.inventoryMulti();
            for (int i = 0; i < list.size(); i++) {
                String epc = StringUtils.byteToHexString(list.get(i), list.get(i).length);
                Log.e(TAG, "inventory_start: start" + epc);
                onInventoryListener.getInventoryData(new SpdInventoryData(null, epc, null));
            }
            if (handler != null) {
                handler.postDelayed(this, Rparams.sleep);
            }
        }
    };

    private class ReaderParams {
        public int sleep;

        public ReaderParams() {
            sleep = 0;
        }
    }

}
