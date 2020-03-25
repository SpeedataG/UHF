package com.speedata.libuhf;


import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.serialport.DeviceControlSpd;
import android.util.Log;

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
import com.speedata.libuhf.utils.SharedXmlUtil;
import com.speedata.libuhf.utils.StringUtils;
import com.uhf.speedatagapi.cls.ErrInfo;
import com.uhf.speedatagapi.cls.Reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * 旗连芯片  芯联方案
 * Created by 张明_ on 2016/11/29.
 */
public class XinLianQilian extends IUHFServiceAdapter {

    private static Reader Mreader = new Reader();
    private static int antportc;
    private Handler handler_inventer = null;
    private Handler handler = null;
    private ReaderParams Rparams = new ReaderParams();
    public boolean nostop = false;
    Reader.TagFilter_ST g2tf = null;
    private DeviceControlSpd deviceControl;
    private Context mContext;
    private ReadBean mRead;
    private DeviceControlSpd newUHFDeviceControl;
    private byte[] readData;
    private Reader.READER_ERR status;

    public XinLianQilian(Context mContext) {
        this.mContext = mContext;
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
                Reader.READER_ERR er = Mreader.InitReader_Notype(mRead.getUhf().getSerialPort(), 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            return NoXmlopenDev();
        }

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

    private int NoXmlopenDev() {
        Log.d("xl_1", String.valueOf(System.currentTimeMillis()));
        if (Build.VERSION.RELEASE.equals("4.4.2")) {
            try {
                deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 64);
                deviceControl.PowerOnDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT, 1);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                antportc = 1;
                return 0;
            } else {
                return -1;
            }
        } else {
            String xinghao = SystemProperties.get("ro.product.model");
            if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || "SD55L".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                    || xinghao.contains("DXD60RT") || "ST60E".equalsIgnoreCase(xinghao) || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao) || "smo_b2000".equals(xinghao)) {
                try {
//                    deviceControl = new UHFDeviceControl(UHFDeviceControl.PowerType.NEW_MAIN, 86);
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.EXPAND, 9, 14);
                    deviceControl.PowerOnDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if ("SD55L".equalsIgnoreCase(xinghao)) {
                    Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT, 1);
                    if (er == Reader.READER_ERR.MT_OK_ERR) {
                        antportc = 1;
                        return 0;
                    } else {
                        return -1;
                    }
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT_SD60, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            } else if (xinghao.equals("SD55PTT")) {
                try {
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 8);
                    deviceControl.PowerOnDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT1, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            } else if ("SD50".equals(xinghao) || "SN50".equals(xinghao) || "R550".equals(xinghao)) {
                try {
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 75);
                    deviceControl.PowerOnDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT0, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56")) {
                if (ConfigUtils.getApiVersion() > 23) {
                    try {
                        deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 12);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT0, 1);
                    if (er == Reader.READER_ERR.MT_OK_ERR) {
                        antportc = 1;
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    try {
                        deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 128);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT, 1);
                    if (er == Reader.READER_ERR.MT_OK_ERR) {
                        antportc = 1;
                        return 0;
                    } else {
                        return -1;
                    }
                }
            } else if (xinghao.contains("55") || xinghao.equals("W2H")) {
                String readEm55 = readEm55();
                if (readEm55.equals("80")) {
                    try {
                        deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 5);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (readEm55.equals("48") || readEm55.equals("81")) {
                    try {
                        deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 6);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 88);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            } else if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")
                    || xinghao.equals("Biowolf LE") || xinghao.equals("FC-PK80")
                    || xinghao.equals("FC-K80") || xinghao.equals("T80") || xinghao.contains("80")) {
                try {
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 119);
                    deviceControl.PowerOnDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }

            } else if (xinghao.equals("SD100")) {
                try {
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.POWER_GAOTONG);
                    deviceControl.gtPower("uhf_open");
                    deviceControl.gtPower("open");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT_SD100, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }

            } else if ("SD100T".equals(xinghao) || "X47".equalsIgnoreCase(xinghao)) {
                try {
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 52, 89, 71);
                    deviceControl.PowerOnDevice();
                    Log.e("UHFService", "==PowerOnDevice()==成功==52, 89, 71");
                } catch (IOException e) {
                    Log.e("UHFService", "==PowerOnDevice()==失败==");
                    e.printStackTrace();
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT0, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }

            } else if ("SC200T".equalsIgnoreCase(xinghao) || "iPick".equalsIgnoreCase(xinghao)) {
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT7, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            } else {
                try {
                    deviceControl = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 94);
                    deviceControl.PowerOnDevice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT, 1);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    antportc = 1;
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }

    //关闭模块
    @Override
    public void closeDev() {
        Log.d(TAG, "closeDev: start");
        if (Mreader != null) {
            Mreader.CloseReader();
        }
        if (handler != null) {
            Log.d(TAG, "handler==null==" + handler);
            handler = null;
        }
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            try {
                newUHFDeviceControl.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (deviceControl != null) {
                    String xinghao = SystemProperties.get("ro.product.model");
                    if ("SC200T".equalsIgnoreCase(xinghao) || "iPick".equalsIgnoreCase(xinghao)) {
                        return;
                    }
                    if (SystemProperties.get("ro.product.model").equals("SD100")) {
                        deviceControl.gtPower("uhf_close");
                        deviceControl.gtPower("close");
                    } else {
                        deviceControl.PowerOffDevice();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "closeDev: end");

    }

    //*************************************************新版接口************************************************
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

    private OnSpdInventoryListener getOnInventoryListener() {
        return onInventoryListener;
    }

    private void inventoryCallBack(SpdInventoryData inventoryData) {
        if (inventoryData != null && getOnInventoryListener() != null) {
            getOnInventoryListener().getInventoryData(inventoryData);
        }

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
        cancelSelect();
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
        try {
            if (handler != null) {
                handler.removeCallbacks(inv_thread);
                Log.d(TAG, "inventory_stop: end" + handler);
                handler = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SystemClock.sleep(500);
    }

    /**
     * 设置读数据监听
     *
     * @param onSpdReadListener
     */
    @Override
    public void setOnReadListener(OnSpdReadListener onSpdReadListener) {
        this.onSpdReadListener = onSpdReadListener;
    }

    private OnSpdReadListener getOnReadListener() {
        return onSpdReadListener;
    }

    private void readCallBack(SpdReadData spdReadData) {
        if (spdReadData != null && getOnReadListener() != null) {
            getOnReadListener().getReadData(spdReadData);
        }

    }

    @Override
    public int readArea(int area, int addr, int count, String passwd) {
        Log.d(TAG, "read_area: start22222");
        if ((area > 3) || (area < 0)) {
            return -3;
        }
        try {
            byte[] rdata = new byte[count * 2];
            byte[] rpaswd = new byte[4];
            if (!passwd.equals("")) {
                Mreader.Str2Hex(passwd, passwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;

            do {
                er = Mreader.GetTagData(Rparams.opant,
                        (char) area, addr, count,
                        rdata, rpaswd, (short) Rparams.optime);

                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            Log.d(TAG, "read_area: end");
            status = er;
            SpdReadData spdReadData = new SpdReadData();
            int errorCode;
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                errorCode = 0;
                spdReadData.setReadData(rdata);
                spdReadData.setDataLen(rdata.length);
                spdReadData.setStatus(0);
                readCallBack(spdReadData);
                this.readData = rdata;
            } else if (er == Reader.READER_ERR.MT_IO_ERR) {
                errorCode = 1;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x01, (byte) 0xEE});
                spdReadData.setStatus(1);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                errorCode = 2;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x02, (byte) 0xEE});
                spdReadData.setStatus(2);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                errorCode = 3;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x03, (byte) 0xEE});
                spdReadData.setStatus(3);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                errorCode = 4;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x04, (byte) 0xEE});
                spdReadData.setStatus(4);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                errorCode = 5;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x05, (byte) 0xEE});
                spdReadData.setStatus(5);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                errorCode = 6;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x06, (byte) 0xEE});
                spdReadData.setStatus(6);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                errorCode = 7;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x07, (byte) 0xEE});
                spdReadData.setStatus(7);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                errorCode = 8;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x08, (byte) 0xEE});
                spdReadData.setStatus(8);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                errorCode = 9;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x09, (byte) 0xEE});
                spdReadData.setStatus(9);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                errorCode = 10;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x10, (byte) 0xEE});
                spdReadData.setStatus(10);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                errorCode = 11;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x11, (byte) 0xEE});
                spdReadData.setStatus(11);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                errorCode = 12;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x12, (byte) 0xEE});
                spdReadData.setStatus(12);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                errorCode = 13;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x13, (byte) 0xEE});
                spdReadData.setStatus(13);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                errorCode = 14;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x14, (byte) 0xEE});
                spdReadData.setStatus(14);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                errorCode = 15;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x15, (byte) 0xEE});
                spdReadData.setStatus(15);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                errorCode = 16;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x16, (byte) 0xEE});
                spdReadData.setStatus(16);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                errorCode = 17;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x17, (byte) 0xEE});
                spdReadData.setStatus(17);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                errorCode = 18;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x18, (byte) 0xEE});
                spdReadData.setStatus(18);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                errorCode = 19;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x19, (byte) 0xEE});
                spdReadData.setStatus(19);
                readCallBack(spdReadData);
            } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                errorCode = 20;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x20, (byte) 0xEE});
                spdReadData.setStatus(20);
                readCallBack(spdReadData);
            } else {
                errorCode = 20;
                spdReadData.setReadData(new byte[]{(byte) 0xFF, 0x20, (byte) 0xEE});
                spdReadData.setStatus(20);
                readCallBack(spdReadData);
            }
            return errorCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 设置写数据监听
     *
     * @param onSpdWriteListener
     */
    @Override
    public void setOnWriteListener(OnSpdWriteListener onSpdWriteListener) {
        this.onSpdWriteListener = onSpdWriteListener;
    }

    private OnSpdWriteListener getOnWriteListener() {
        return onSpdWriteListener;
    }

    private void writeCallBack(SpdWriteData spdWriteData) {
        if (spdWriteData != null && getOnWriteListener() != null) {
            getOnWriteListener().getWriteData(spdWriteData);
        }

    }

    @Override
    public int writeArea(int area, int addr, int count, String passwd, byte[] content) {
        Log.d(TAG, "write_area: start22222");
        try {
            if ((content.length % 2) != 0) {
                return -3;
            }
            if ((content.length / 2) != count) {
                return -3;
            }
            byte[] rpaswd = new byte[4];
            if (!"".equals(passwd)) {
                Mreader.Str2Hex(passwd, passwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.WriteTagData(Rparams.opant,
                        (char) area, addr, content, content.length, rpaswd,
                        (short) Rparams.optime);
                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            Log.d(TAG, "write_area: end");
            SpdWriteData spdWriteData = new SpdWriteData();
            int errorCode;
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                errorCode = 0;
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_IO_ERR) {
                errorCode = 1;
                spdWriteData.setStatus(1);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                errorCode = 2;
                spdWriteData.setStatus(2);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                errorCode = 3;
                spdWriteData.setStatus(3);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                errorCode = 4;
                spdWriteData.setStatus(4);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                errorCode = 5;
                spdWriteData.setStatus(5);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                errorCode = 6;
                spdWriteData.setStatus(6);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                errorCode = 7;
                spdWriteData.setStatus(7);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                errorCode = 8;
                spdWriteData.setStatus(8);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                errorCode = 9;
                spdWriteData.setStatus(9);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                errorCode = 10;
                spdWriteData.setStatus(10);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                errorCode = 11;
                spdWriteData.setStatus(11);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                errorCode = 12;
                spdWriteData.setStatus(12);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                errorCode = 13;
                spdWriteData.setStatus(13);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                errorCode = 14;
                spdWriteData.setStatus(14);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                errorCode = 15;
                spdWriteData.setStatus(15);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                errorCode = 16;
                spdWriteData.setStatus(16);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                errorCode = 17;
                spdWriteData.setStatus(17);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                errorCode = 18;
                spdWriteData.setStatus(18);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                errorCode = 19;
                spdWriteData.setStatus(19);
                writeCallBack(spdWriteData);
            } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                errorCode = 20;
                spdWriteData.setStatus(20);
                writeCallBack(spdWriteData);
            } else {
                errorCode = 20;
                spdWriteData.setStatus(20);
                writeCallBack(spdWriteData);
            }
            return errorCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    //设置密码
    @Override
    public int setPassword(int which, String cur_pass, String new_pass) {
        if (which > 1 || which < 0) {
            return -1;
        }
        byte[] stringToByte = StringUtils.stringToByte(new_pass);
        try {
            if (which == 0) {
                return writeArea(0, 0, 2, cur_pass, stringToByte);
            } else {
                return writeArea(0, 2, 2, cur_pass, stringToByte);
            }

        } catch (NumberFormatException e) {
            return -1;
        }
    }

    //设定区域锁定状态。
    @Override
    public int setLock(int type, int area, String passwd) {
        try {
            Reader.Lock_Obj lobj = null;
            Reader.Lock_Type ltyp = null;
            if (area == 0) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD;
                if (type == 0) {
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_UNLOCK;
                } else if (type == 1) {
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_LOCK;
                } else if (type == 2) {
                    return -1;
                } else if (type == 3) {
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_PERM_LOCK;
                }

            } else if (area == 1) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD;
                if (type == 0) {
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_UNLOCK;
                } else if (type == 1) {
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_LOCK;
                } else if (type == 2) {
                    return -1;
                } else if (type == 3) {
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_PERM_LOCK;
                }
            } else if (area == 2) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK1;
                if (type == 0) {
                    ltyp = Reader.Lock_Type.BANK1_UNLOCK;
                } else if (type == 1) {
                    ltyp = Reader.Lock_Type.BANK1_LOCK;
                } else if (type == 2) {
                    return -1;
                } else if (type == 3) {
                    ltyp = Reader.Lock_Type.BANK1_PERM_LOCK;
                }
            } else if (area == 3) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK2;
                if (type == 0) {
                    ltyp = Reader.Lock_Type.BANK2_UNLOCK;
                } else if (type == 1) {
                    ltyp = Reader.Lock_Type.BANK2_LOCK;
                } else if (type == 2) {
                    return -1;
                } else if (type == 3) {
                    ltyp = Reader.Lock_Type.BANK2_PERM_LOCK;
                }
            } else if (area == 4) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK3;
                if (type == 0) {
                    ltyp = Reader.Lock_Type.BANK3_UNLOCK;
                } else if (type == 1) {
                    ltyp = Reader.Lock_Type.BANK3_LOCK;
                } else if (type == 2) {
                    return -1;
                } else if (type == 3) {
                    ltyp = Reader.Lock_Type.BANK3_PERM_LOCK;
                }
            }
            byte[] rpaswd = new byte[4];
            if (!"".equals(passwd)) {
                Mreader.Str2Hex(passwd, passwd.length(), rpaswd);
            }

            assert lobj != null;
            assert ltyp != null;
            Reader.READER_ERR er = Mreader.LockTag(Rparams.opant,
                    (byte) lobj.value(), (short) ltyp.value(),
                    rpaswd, (short) Rparams.optime);
            SpdWriteData spdWriteData = new SpdWriteData();
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                spdWriteData.setStatus(-1);
                writeCallBack(spdWriteData);
                return -1;
            } else {
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int setQueryTagGroup(int selected, int session, int target) {
        try {
            int[] val = new int[]{-1};
            val[0] = session;
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.session = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return -1;
        }
    }

    @Override
    public int getQueryTagGroup() {
        try {
            int[] val = new int[]{-1};
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, val);

            if (er == Reader.READER_ERR.MT_OK_ERR) {
                return val[0];
            } else {
                return -1;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return -1;
        }
    }

    @Override
    public int setNewEpc(String password, int len, byte[] epc) {
        if (len > 31) {
            return -3;
        }
        if (len * 2 < epc.length) {
            return -3;
        }
        readArea(IUHFService.EPC_A, 1, 1, password);
        while (status != Reader.READER_ERR.MT_OK_ERR) {
            if (readData != null) {
                break;
            }
        }
        if (readData == null) {
            return -5;
        }
        readData[0] = (byte) ((readData[0] & 0x7) | (len << 3));
        byte[] f = new byte[2 + len * 2];
        try {
            System.arraycopy(readData, 0, f, 0, 2);
            System.arraycopy(epc, 0, f, 2, len * 2);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        SystemClock.sleep(500);
        readData = null;
        return writeArea(IUHFService.EPC_A, 1, f.length / 2, password, f);
    }

    @Override
    public int setQT(byte[] rpaswd, int cmdType, int memType, int persistType, int rangeType) {
        // m4 qt
        try {
            Reader.IMPINJM4QtPara CustomPara = Mreader.new IMPINJM4QtPara();
            CustomPara.TimeOut = 1000;
            CustomPara.CmdType = cmdType;
            if (CustomPara.CmdType == 1) {
                CustomPara.MemType = memType;
                CustomPara.PersistType = persistType;
                CustomPara.RangeType = rangeType;
            }
            CustomPara.AccessPwd = rpaswd;

            Reader.IMPINJM4QtResult CustomRet = Mreader.new IMPINJM4QtResult();
            Mreader.CustomCmd(Rparams.opant, Reader.CustomCmdType.IMPINJ_M4_Qt, CustomPara, CustomRet);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    @Override
    public int setGen2QValue(int qValue) {
        try {
            int[] val = new int[]{-1};
            val[0] = qValue - 1;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_Q, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.qv = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setGen2WriteMode(int wMode) {
        try {
            int[] val = new int[]{-1};
            val[0] = wMode;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_WRITEMODE, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.wmode = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setGen2Blf(int blf) {
        try {
            int[] val = new int[]{-1};
            val[0] = blf;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_BLF, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.blf = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setGen2MaxLen(int maxLen) {
        try {
            int[] val = new int[]{-1};
            val[0] = maxLen;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_MAXEPCLEN, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.maxlen = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setGen2Target(int target) {
        try {
            int[] val = new int[]{-1};
            val[0] = target;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.target = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setGen2Code(int code) {
        try {
            int[] val = new int[]{-1};
            val[0] = code;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.gen2code = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int setGen2Tari(int tari) {
        try {
            int[] val = new int[]{-1};
            val[0] = tari;
            Reader.READER_ERR er0 = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TARI, val);
            if (er0 == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.gen2tari = val[0];
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int[] getGen2AllValue() {
        int[] res = new int[2];
        try {
            int[] val1 = new int[]{-1};
            int[] val2 = new int[]{-1};
            Reader.READER_ERR er;
            er = Mreader.ParamGet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_Q, val1);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                res[0] = val1[0] + 1;
            } else {
                return null;
            }

            er = Mreader.ParamGet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET, val2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                res[1] = val2[0];
            } else {
                return null;
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }
        return res;
    }

    @Override
    public int switchInvMode(int mode) {
        if (mode == 1) {
            try {
                int[] uants = Rparams.uants;
                Reader.READER_ERR er = Mreader.AsyncStartReading(uants, Rparams.uants.length, 0);
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    nostop = true;
                    return 0;
                } else {
                    return -1;
                }
            } catch (Exception e) {
                return -1;
            }
        } else {
            try {
                Reader.READER_ERR er = Mreader.AsyncStopReading();
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    nostop = false;
                    return 0;
                } else {
                    return -1;
                }
            } catch (Exception e) {
                return -1;
            }
        }
    }

    @Override
    public int setLowpowerScheduler(int invOnTime, int invOffTime) {
        Rparams.readtime = invOnTime;
        Rparams.sleep = invOffTime;
        return 0;
    }

    @Override
    public int[] getLowpowerScheduler() {
        int[] a = new int[2];
        a[0] = Rparams.readtime;
        a[1] = Rparams.sleep;
        return a;
    }

    //选中要进行操作的 epc 标签
    @Override
    public int selectCard(int bank, byte[] epc, boolean mFlag) {
        Reader.READER_ERR er;
        try {
            if (mFlag) {
                if (epc == null) {
                    return -1;
                }
                g2tf = Mreader.new TagFilter_ST();
                g2tf.fdata = epc;
                g2tf.flen = epc.length * 8;
                g2tf.isInvert = 0;
                g2tf.bank = bank;
                g2tf.startaddr = 32;
                er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
            } else {
                er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, null);
            }

            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    @Override
    public int selectCard(int bank, String epc, boolean mFlag) {
        Log.d(TAG, "selectCard: start");
        if (!mFlag) {
            epc = "0000";
        }
        int ln = epc.length();
        if (ln == 1 || ln % 2 == 1) {
            ln++;
        }
        byte[] fdata = new byte[ln / 2];
        Mreader.Str2Hex(epc, epc.length(), fdata);
        if (selectCard(bank, fdata, mFlag) != 0) {
            Log.d(TAG, "selectCard: failed");
            return -1;
        }
        Log.d(TAG, "selectCard: end");
        return 0;
    }

    //设置天线功率
    @Override
    public int setAntennaPower(int power) {
        Reader.AntPowerConf apcf = Mreader.new AntPowerConf();
        apcf.antcnt = antportc;
        int[] rpow = new int[apcf.antcnt];
        int[] wpow = new int[apcf.antcnt];
        for (int i = 0; i < apcf.antcnt; i++) {
            Reader.AntPower jaap = Mreader.new AntPower();
            jaap.antid = i + 1;
            jaap.readPower = (short) (power * 100);
            rpow[i] = jaap.readPower;
            jaap.writePower = (short) (power * 100);
            wpow[i] = jaap.writePower;
            apcf.Powers[i] = jaap;
        }
        try {
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            } else {
                Rparams.rpow = rpow;
                Rparams.wpow = wpow;
                SharedXmlUtil.getInstance(mContext).write("AntennaPower", power);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    //读取当前天线功率值
    @Override
    public int getAntennaPower() {
        int rv = 0;
        try {
            Reader.AntPowerConf apcf2 = Mreader.new AntPowerConf();
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                for (int i = 0; i < apcf2.antcnt; i++) {
                    if (i == 0) {
                        rv = (apcf2.Powers[i].readPower) / 100;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return rv;
    }

    //设置频率区域
    @Override
    public int setFreqRegion(int region) {
        try {
            Reader.Region_Conf rre;
            switch (region) {
                case 0:
                    rre = Reader.Region_Conf.RG_PRC;
                    break;
                case 1:
                    rre = Reader.Region_Conf.RG_NA;
                    break;
                case 2:
                    rre = Reader.Region_Conf.RG_EU3;
                    break;
                default:
                    rre = Reader.Region_Conf.RG_NONE;
                    break;
            }
            if (rre == Reader.Region_Conf.RG_NONE) {
                return -1;
            }
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rre);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @Override
    public int getFreqRegion() {
        try {
            Reader.Region_Conf[] rcf2 = new Reader.Region_Conf[1];
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rcf2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                switch (rcf2[0]) {
                    case RG_PRC:
                        return 0;
                    case RG_NA:
                        return 1;
                    case RG_EU3:
                        return 2;
                    default:
                        return -1;
                }
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int setInvMode(int invm, int addr, int length) {
        try {
            Reader.EmbededData_ST edst = Mreader.new EmbededData_ST();
            edst.accesspwd = null;
            if (invm == 0) {
                edst.bank = invm + 1;
                edst.bytecnt = 0;
            } else {
                edst.bank = invm + 1;
                edst.startaddr = addr;
                edst.bytecnt = length;
            }
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                Rparams.emdadr = edst.startaddr;
                Rparams.emdbank = edst.bank;
                Rparams.emdbytec = edst.bytecnt;
                Rparams.emdenable = 1;
                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int setNxpu8(int mode) {
//设置使能或禁用u8特殊盘点功能(set eanble or disenable inventory u8 tag functions)
        //*
        Reader.NXP_U8_InventoryModePara u8para = Mreader.new NXP_U8_InventoryModePara();

        u8para.Mode[0] = 0;
        Rparams.nxpu8 = 0;

        Reader.READER_ERR er = Mreader.ParamSet(
                Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, null);
        if (er == Reader.READER_ERR.MT_OK_ERR) {

        }


        er = Mreader.ParamSet(
                Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, null);
        if (er == Reader.READER_ERR.MT_OK_ERR) {

        }

        if (mode == 0) {
        } else if (mode == 1) {

            Reader.TagFilter_ST tfst = Mreader.new TagFilter_ST();

            tfst.fdata = new byte[1];
            tfst.fdata[0] = (byte) 0x80;
            tfst.bank = 1;
            tfst.flen = 1;
            tfst.startaddr = 0x204;
            tfst.isInvert = 0;
            er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);

            Reader.EmbededData_ST edst = Mreader.new EmbededData_ST();
            edst.accesspwd = null;

            edst.bank = 2;
            edst.startaddr = 0;
            edst.bytecnt = 12;

            er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
            }

            u8para.Mode[0] = 1;
            Rparams.nxpu8 = 1;
        } else if (mode == 2) {
            Reader.TagFilter_ST tfst = Mreader.new TagFilter_ST();

            tfst.fdata = new byte[1];
            tfst.fdata[0] = (byte) 0x80;
            tfst.bank = 1;
            tfst.flen = 1;
            tfst.startaddr = 0x203;
            tfst.isInvert = 0;
            er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);

            u8para.Mode[0] = 1;
            Rparams.nxpu8 = 2;
        } else if (mode == 3) {
            Reader.TagFilter_ST tfst = Mreader.new TagFilter_ST();

            tfst.fdata = new byte[1];
            tfst.fdata[0] = (byte) 0x80;
            tfst.bank = 1;
            tfst.flen = 1;
            tfst.startaddr = 0x204;
            tfst.isInvert = 0;
            er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);
            u8para.Mode[0] = 1;
            Rparams.nxpu8 = 3;
        }
        Mreader.CustomCmd(0, Reader.CustomCmdType.NXP_U8_InventoryMode, u8para, null);
        //*/
        return Rparams.nxpu8;
    }

    @Override
    public int getInvMode(int type) {
        try {
            Reader.EmbededData_ST edst2 = Mreader.new EmbededData_ST();
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                switch (type) {
                    case 0:
                        return edst2.bank - 1;
                    case 1:
                        return edst2.startaddr;
                    case 2:
                        return edst2.bytecnt;
                    default:
                        return -1;
                }
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private volatile boolean inSearch = false;
    private Runnable inv_thread = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run: 1111111111111111111111");
            String tag = null;
            int[] tagcnt = new int[1];
            tagcnt[0] = 0;
            synchronized (this) {
                Reader.READER_ERR er;
//                int[] uants = Rparams.uants;
                Log.d(TAG, "run: 2222222222222222222222222222");
                if (nostop) {
                    Log.d(TAG, "run: 2222222222222222222222222222==AsyncGetTagCount==");
                    er = Mreader.AsyncGetTagCount(tagcnt);
                } else {
                    Log.d(TAG, "run: 2222222222222222222222222222==TagInventory_Raw==");
                    er = Mreader.TagInventory_Raw(Rparams.uants,
                            Rparams.uants.length,
                            (short) Rparams.readtime, tagcnt);
                }
                if (er == Reader.READER_ERR.MT_OK_ERR) {
                    if (tagcnt[0] > 0) {
                        for (int i = 0; i < tagcnt[0]; i++) {
                            Log.d(TAG, "run: 33333333333");
                            Reader.TAGINFO tfs = Mreader.new TAGINFO();
                            if (nostop) {
                                Log.d(TAG, "run: 33333333333==AsyncGetNextTag");
                                er = Mreader.AsyncGetNextTag(tfs);
                            } else {
                                Log.d(TAG, "run: 33333333333==GetNextTag");
                                er = Mreader.GetNextTag(tfs);
                            }

                            if (er == Reader.READER_ERR.MT_OK_ERR) {
                                byte[] n_epc = tfs.EpcId;
                                byte[] n_tid = tfs.EmbededData;
                                String strEPCTemp = ByteCharStrUtils.b2hexs(n_epc, n_epc.length);
                                String strDataTemp = null;
                                if (n_tid != null) {
                                    strDataTemp = ByteCharStrUtils.b2hexs(n_tid, n_tid.length);
                                }
                                Log.d(TAG, "run: 4444444444");
                                String rssi = String.valueOf(tfs.RSSI);
                                ArrayList<SpdInventoryData> cx = new ArrayList<SpdInventoryData>();
                                SpdInventoryData tagData = new SpdInventoryData(strDataTemp, strEPCTemp, rssi);
                                tagData.setFrequency(tfs.Frequency);
                                tagData.setPc(tfs.PC);
                                tagData.setReadCnt(tfs.ReadCnt);
                                tagsBufferResh(Reader.bytes_Hexstr(tfs.EpcId), tagData);
                                if (handler_inventer == null) {
                                    Log.d(TAG, "run: 4444444444==inventoryCallBack");
                                    inventoryCallBack(tagData);
                                } else {
                                    cx.add(tagData);
                                    Message msg = new Message();
                                    msg.what = 1;
                                    msg.obj = cx;
                                    handler_inventer.sendMessage(msg);
                                }
                                cx.clear();
                            } else {
                                break;
                            }
                        }
                    }
                    Log.d(TAG, "run:5555555555555==next");
                    if (handler != null) {
                        handler.postDelayed(this, Rparams.sleep);
                    }
                } else {
                    Log.d(TAG, "run: err");
                    inSearch = false;
                    int errCode = -1;
                    if (er == Reader.READER_ERR.MT_IO_ERR) {
                        errCode = 1;
                    } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                        errCode = 2;
                    } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                        errCode = 3;
                    } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                        errCode = 4;
                    } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                        errCode = 5;
                    } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                        errCode = 6;
                    } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                        errCode = 7;
                    } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                        errCode = 8;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                        errCode = 9;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                        errCode = 10;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                        errCode = 11;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                        errCode = 12;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                        errCode = 13;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                        errCode = 14;
                    } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                        errCode = 15;
                    } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                        errCode = 16;
                    } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                        errCode = 17;
                    } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                        errCode = 18;
                    } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                        errCode = 19;
                    } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                        errCode = 20;
                    } else {
                        errCode = 20;
                    }
                    if (getOnInventoryListener() != null) {
                        getOnInventoryListener().onInventoryStatus(errCode);
                    }
                    if (handler_inventer == null) {
                        inventoryCallBack(null);
                    } else {
                        handler_inventer.sendMessage(handler_inventer.obtainMessage(2, errCode));
                    }
                }
            }
        }
    };

    /**
     * 刷新标签缓冲，更新标签列表信息，根据是否u8标签，是否附加数据唯一，天线唯一来列表
     *
     * @param EPC
     * @param tagData
     */
    private void tagsBufferResh(String EPC, SpdInventoryData tagData) {

        String epcstr = EPC;
        String u8tid = "", bid = "";
        if (Rparams.nxpu8 == 1) {
            bid = epcstr.substring(epcstr.length() - 4);
            epcstr = epcstr.substring(0, epcstr.length() - 4);
        } else if (Rparams.nxpu8 == 2) {
            u8tid = epcstr.substring(epcstr.length() - 24);
            epcstr = epcstr.substring(0, epcstr.length() - 24);
        } else if (Rparams.nxpu8 == 3) {
            bid = epcstr.substring(epcstr.length() - 4);
            epcstr = epcstr.substring(0, epcstr.length() - 4);
        }else {
            return;
        }

        if (epcstr.length() < 24) {
            epcstr = String.format("%-24s", epcstr);
        }

        if (tagData.getTid() != null) {
            if (Rparams.nxpu8 == 1) {
                u8tid = tagData.getTid();
            }
        }
        tagData.setEpc(epcstr);
        tagData.setBid(bid);
        tagData.setU8Tid(u8tid);

    }

    //********************************************老版接口（不再维护）******************************************

    //拿到最近一次详细内部错误信息
    @Override
    public String GetLastDetailError() {
        ErrInfo ei = new ErrInfo();
        Mreader.GetLastDetailError(ei);
        return ei.derrcode + "-" + ei.errstr;
    }

    private void cancelSelect() {
        Reader.TagFilter_ST tfst = Mreader.new TagFilter_ST();
        tfst = null;
        Reader.READER_ERR filter_er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);
    }


    //用户需要自己实现一个 Handler，然后用些函数向 API 注册，然后才可以 开始盘点。
    // 盘点到的 EPC 等数据会通过向注册的 Handler 发消息 （Message）的方式来实现。
    // 当搜索到有效 EPC 和 TID 数据后，Handler 会收到 Message，其中 Meseage.what 值为 1，
    // Message.obj 就是保存了 EPC 数据的 Tag_Data类的ArrayList。
    // SpdInventoryData 类有两个byte[ ]型的public 成员，一个是 epc，一个是 tid，如果为 null，
    // 代表对应的值不存在。

    public class ReaderParams {

        //save param
        public int opant;

        public List<String> invpro;
        public String opro;
        public int[] uants;
        public int readtime;
        public int sleep;

        public int checkant;
        public int[] rpow;
        public int[] wpow;

        public int region;
        public int[] frecys;
        public int frelen;

        public int session;
        public int qv;
        public int wmode;
        public int blf;
        public int maxlen;
        public int target;
        public int gen2code;
        public int gen2tari;

        public String fildata;
        public int filadr;
        public int filbank;
        public int filisinver;
        public int filenable;

        public int emdadr;
        public int emdbytec;
        public int emdbank;
        public int emdenable;

        public int antq;
        public int adataq;
        public int rhssi;
        public int invw;
        public int iso6bdeep;
        public int iso6bdel;
        public int iso6bblf;
        public int option;
        //other params

        public String password;
        public int optime;
        public int nxpu8;

        public ReaderParams() {
            opant = 1;
            invpro = new ArrayList<String>();
            invpro.add("GEN2");
            uants = new int[1];
            uants[0] = 1;
            sleep = 0;
            readtime = 50;
            optime = 1000;
            opro = "GEN2";
            checkant = 1;
            rpow = new int[]{2700, 2000, 2000, 2000};
            wpow = new int[]{2000, 2000, 2000, 2000};
            region = 1;
            frelen = 0;
            session = 0;
            qv = -1;
            wmode = 0;
            blf = 0;
            maxlen = 0;
            target = 0;
            gen2code = 2;
            gen2tari = 0;

            fildata = "";
            filadr = 32;
            filbank = 1;
            filisinver = 0;
            filenable = 0;

            emdadr = 0;
            emdbytec = 0;
            emdbank = 1;
            emdenable = 0;

            adataq = 0;
            rhssi = 1;
            invw = 0;
            iso6bdeep = 0;
            iso6bdel = 0;
            iso6bblf = 0;
            option = 0;
            nxpu8 = 0;
        }
    }
}