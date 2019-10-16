package com.speedata.libuhf;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.serialport.DeviceControlSpd;
import android.util.Log;
import android.widget.Toast;

import com.rscja.utility.StringUtility;
import com.speedata.libuhf.bean.SpdInventoryData;
import com.speedata.libuhf.bean.SpdReadData;
import com.speedata.libuhf.bean.SpdWriteData;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;
import com.speedata.libuhf.utils.CommonUtils;
import com.speedata.libuhf.utils.ConfigUtils;
import com.speedata.libuhf.utils.DataConversionUtils;
import com.speedata.libuhf.utils.ReadBean;
import com.speedata.libuhf.utils.StringUtils;
import com.uhf.structures.DynamicQParams;
import com.uhf.structures.FixedQParams;
import com.uhf.structures.OnReadWriteListener;
import com.uhf.structures.RW_Params;
import com.uhf.structures.SelectCriteria;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import cn.com.example.rfid.driver.Driver;
import cn.com.example.rfid.driver.RfidDriver;

/**
 * 作者:stw-420401567
 * 创建日期: 2019/8/26 15:36
 * 说明:
 **/
public class YiXin implements IUHFService, OnSpdInventoryListener {

    private ReadBean mRead = null;
    private Context mContext = null;
    private DeviceControlSpd newUHFDeviceControl = null;
    private DeviceControlSpd pw = null;
    private Driver driver = null;
    private OnSpdInventoryListener onInventoryListener = null;
    private OnSpdReadListener onSpdReadListener = null;
    private OnSpdWriteListener onSpdWriteListener = null;
    private int writeStatus;
    private int lockStatus;
    private byte[] epcData;
    private volatile boolean isReadOutTime = false;
    private volatile boolean isReadSuccess = false;
    private volatile boolean isWriteOutTime = false;
    private volatile boolean isWriteSuccess = false;

    private volatile boolean isLockOutTime = false;
    private volatile boolean isLockSuccess = false;
    private boolean loopFlag = false;
    private YiXinParams yiXinParams = new YiXinParams();

    public YiXin(Context context) {
        this.mContext = context;
    }

    @Override
    public int openDev() {
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            mRead = ConfigUtils.readConfig(mContext);
            String powerType = mRead.getUhf().getPowerType();
            int[] intArray = new int[mRead.getUhf().getGpio().size()];
            for (int i = 0; i < mRead.getUhf().getGpio().size(); i++) {
                intArray[i] = mRead.getUhf().getGpio().get(i);
            }
            try {
                newUHFDeviceControl = new DeviceControlSpd(powerType, intArray);
                newUHFDeviceControl.PowerOffDevice();
                newUHFDeviceControl.PowerOnDevice();
                int result = getDeriver().initRFID(mRead.getUhf().getSerialPort());
                if (result == -1000) {
                    return -1;
                } else {
                    return 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            return NoXmlopenDev();
        }
    }

    private Driver getDeriver() {
        if (driver == null) {
            driver = new RfidDriver();
        }
        return driver;
    }

    private int NoXmlopenDev() {
        if (Build.VERSION.RELEASE.equals("4.4.2")) {
            try {
                pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String xinghao = SystemProperties.get("ro.product.model");
            if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || "SD55L".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                    || xinghao.contains("DXD60RT") || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao)) {
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.PowerType.EXPAND, 9, 14);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.equals("SD55PTT")) {
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56")) {
                if (ConfigUtils.getApiVersion() > 23) {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 12);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 128);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")
                    || xinghao.equals("Biowolf LE") || xinghao.equals("FC-PK80")
                    || xinghao.equals("FC-K80") || xinghao.equals("T80") || xinghao.contains("80")) {
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 119);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.contains("55") || xinghao.equals("W2H")) {
                String readEm55 = readEm55();
                if (readEm55.equals("80")) {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 5);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (readEm55.equals("48") || readEm55.equals("81")) {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 6);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 88);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (xinghao.equals("SD100")) {
                // TODO: 2018/10/10   上电处理
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.POWER_GAOTONG);
                    pw.gtPower("uhf_open");
                    pw.gtPower("open");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if ("SD100T".equals(xinghao) || "X47".equalsIgnoreCase(xinghao)) {
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 52, 89, 71);
                    Log.e("UHFService", "==PowerOnDevice()==成功==52, 89, 71");
                } catch (IOException e) {
                    Log.e("UHFService", "==PowerOnDevice()==失败==");
                    e.printStackTrace();
                }

            } else {
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 94);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (!SystemProperties.get("ro.product.model").contains("SD100") || "SD100T".equals(SystemProperties.get("ro.product.model"))) {
                pw.PowerOffDevice();
                pw.PowerOnDevice();
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        SystemClock.sleep(20);
        int result;
        String xinghao = SystemProperties.get("ro.product.model");
        if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                || xinghao.contains("DXD60RT") || xinghao.contains("C6000") || "SD100T".equals(xinghao) || "X47".equalsIgnoreCase(xinghao)
                || "ESUR-H600".equals(xinghao)) {
            result = getDeriver().initRFID(SERIALPORT_SD60);
        } else if (xinghao.equals("SD55PTT")) {
            result = getDeriver().initRFID(SERIALPORT1);
        } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56")) {
            if (ConfigUtils.getApiVersion() > 23) {
                result = getDeriver().initRFID(SERIALPORT0);
            } else {
                result = getDeriver().initRFID(SERIALPORT);
            }
        } else if (xinghao.contains("SD100")) {
            SystemClock.sleep(240);
            result = getDeriver().initRFID(SERIALPORT_SD100);
        } else {
            result = getDeriver().initRFID(SERIALPORT);
        }
        if (result == -1000) {
            return -1;
        } else {
            return 0;
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

    @Override
    public void closeDev() {
        int ret = getDeriver().Close_Com();
        if (ret != 0) {
            // TODO: 2019/8/26  异常
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
                    pw.PowerOffDevice();
                    Log.d("UHF", "closeDev");
                } else if (SystemProperties.get("ro.product.model").contains("SD100")) {
                    pw.gtPower("uhf_close");
                    pw.gtPower("close");
                } else {
                    pw.PowerOffDevice();
                    Log.d("UHF", "closeDev");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        driver = null;
    }

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

    @Override
    public void getInventoryData(SpdInventoryData var1) {

    }

    class TagThread extends Thread {
        TagThread() {
        }

        @Override
        public void run() {
            while (loopFlag) {
                String[] strEpc1 = {getDeriver().GetBufData()};
                String strEpc = strEpc1[0];
                if (!StringUtility.isEmpty(strEpc)) {
                    int Hb = 0;
                    int Lb = 0;
                    int rssi = 0;
                    String[] tmp = new String[3];
                    String text = strEpc.substring(4);
                    String len = strEpc.substring(0, 2);
                    int epclen = (Integer.parseInt(len, 16) / 8) * 4;
                    tmp[0] = text.substring(epclen, text.length() - 6);
                    tmp[1] = text.substring(0, epclen);
                    tmp[2] = text.substring(text.length() - 6, text.length() - 2);

                    if (4 != tmp[2].length()) {
                        tmp[2] = "0000";
                    } else {
                        Hb = Integer.parseInt(tmp[2].substring(0, 2), 16);
                        Lb = Integer.parseInt(tmp[2].substring(2, 4), 16);
                        rssi = ((Hb - 256 + 1) * 256 + (Lb - 256)) / 10;
                    }
                    inventoryCallBack(new SpdInventoryData("", tmp[1], Integer.toString(rssi)));
                }
            }
        }
    }

    @Override
    public void inventoryStart() {
        loopFlag = true;
        getDeriver().readMore();
        new TagThread().start();
    }

    @Override
    public void inventoryStop() {
        getDeriver().stopRead();
        loopFlag = false;
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
        if (area > 3 || area < 0) {
            return -1;
        }
        //默认指定标签区域为epc 起始地址32  长度96
        String result = getDeriver().Read_Data_Tag(passwd, yiXinParams.bank, yiXinParams.ads,
                yiXinParams.len, yiXinParams.epcData, area, addr, count);
        if (result != null) {
            SpdReadData spdReadData = new SpdReadData();
            spdReadData.setReadData(DataConversionUtils.hexStringToByteArray(result));
            spdReadData.setDataLen(DataConversionUtils.hexStringToByteArray(result).length);
            readCallBack(spdReadData);
            return 0;
        } else {
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
        int length = content.length;
        if ((length % 2) != 0) {
            return -3;
        }
        if ((length / 2) != count) {
            return -3;
        }
        if ((area >= 0) && (area <= 3)) {
            int ret = getDeriver().Write_Data_Tag(passwd, yiXinParams.bank, yiXinParams.ads, yiXinParams.len,
                    yiXinParams.epcData, area, addr, count, StringUtils.byteToHexString(content, content.length));
            SpdWriteData spdWriteData = new SpdWriteData();
            if (ret == 0) {
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            } else {
                spdWriteData.setStatus(-1);
                writeCallBack(spdWriteData);
                return -1;
            }
        }
        return -1;
    }

    @Override
    public int setPassword(int which, String cur_pass, String new_pass) {
        if (which > 1 || which < 0) {
            return -1;
        }
        try {
            byte[] stringToByte = StringUtils.stringToByte(new_pass);
            if (which == 0) {
                return writeArea(0, 0, 2, cur_pass, stringToByte);
            } else {
                return writeArea(0, 2, 2, cur_pass, stringToByte);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public int setLock(int type, int area, String passwd) {
        if (type == 0) {
            int ret = getDeriver().unLock_Tag_Data(passwd, yiXinParams.bank, yiXinParams.ads,
                    yiXinParams.len, yiXinParams.epcData, area);
            if (ret == 0) {
                SpdWriteData spdWriteData = new SpdWriteData();
                spdWriteData.setEPCData(StringUtils.stringToByte(yiXinParams.epcData));
                spdWriteData.setEPCLen(StringUtils.stringToByte(yiXinParams.epcData).length);
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            } else {
                return -1;
            }
        } else if (type == 1) {
            int ret = getDeriver().Lock_Tag_Data(passwd, yiXinParams.bank, yiXinParams.ads,
                    yiXinParams.len, yiXinParams.epcData, area);
            if (ret == 0) {
                SpdWriteData spdWriteData = new SpdWriteData();
                spdWriteData.setEPCData(StringUtils.stringToByte(yiXinParams.epcData));
                spdWriteData.setEPCLen(StringUtils.stringToByte(yiXinParams.epcData).length);
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }


    class LockTimeOutThread extends Thread {
        @Override
        public void run() {
            super.run();
            SystemClock.sleep(1000);
            isLockOutTime = true;
        }
    }

    @Override
    public int setKill(String accessPassword, String killPassword) {
        return getDeriver().Kill_Tag(killPassword, yiXinParams.bank, yiXinParams.ads, yiXinParams.len, yiXinParams.epcData);
    }

    @Override
    public int yixinSetNewEpc(String PwdWr, int len, byte[] data) {
        int ret = getDeriver().Write_Epc_Data(PwdWr, 2, len, StringUtils.byteToHexString(data, data.length));
        if (ret == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int yixinFilterEpc(int bank, int ads, int len, String data, Boolean save) {
        return getDeriver().Set_Filter_Data(bank, ads, len, data, save);
    }

    @Override
    public int setQueryTagGroup(int selected, int session, int target) {
        return 0;
    }

    @Override
    public int getQueryTagGroup() {
        return 0;
    }

    @Override
    public int mask(int area, int addr, int length, byte[] content) {
        return 0;
    }

    @Override
    public int cancelMask() {
        return 0;
    }

    @Override
    public SelectCriteria getMask() {
        return null;
    }

    @Override
    public int setQT(byte[] rpaswd, int cmdType, int memType, int persistType, int rangeType) {
        return 0;
    }

    @Override
    public int setMonzaQtTagMode(int memMap, int maskFlag, byte[] accessPassword) {
        return 0;
    }

    @Override
    public int readMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length) {
        return 0;
    }

    @Override
    public int readMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, int timeOutMs, RW_Params rw_params) {
        return 0;
    }

    @Override
    public int writeMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData) {
        return 0;
    }

    @Override
    public int writeMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData, int timeOutMs, RW_Params rw_params) {
        return 0;
    }

    @Override
    public void inventory_start() {

    }

    @Override
    public void inventory_start(Handler hd) {

    }

    @Override
    public int set_Password(int which, String cur_pass, String new_pass) {
        return 0;
    }

    @Override
    public int inventory_stop() {
        return 0;
    }

    @Override
    public byte[] read_area(int area, int addr, int count, String passwd) {
        return new byte[0];
    }

    @Override
    public String read_area(int area, String str_addr, String str_count, String str_passwd) {
        return null;
    }

    @Override
    public int write_area(int area, int addr, int count, String passwd, byte[] content) {
        return 0;
    }

    @Override
    public int write_area(int area, String addr, String pwd, String count, String content) {
        return 0;
    }

    @Override
    public int selectCard(int bank, byte[] epc, boolean mFlag) {
        String epcData = StringUtils.byteToHexString(epc, epc.length);
        return selectCard(bank, epcData, mFlag);
    }

    @Override
    public int selectCard(int bank, String epc, boolean mFlag) {
        if (mFlag) {
            yiXinParams.bank = bank;
            yiXinParams.epcData = epc;
            yiXinParams.ads = 32;
            yiXinParams.len = epc.length() * 4;
        } else {
            yiXinParams.bank = bank;
            yiXinParams.epcData = epc;
            yiXinParams.ads = 0;
            yiXinParams.len = 0;
        }
        return 0;
    }

    @Override
    public int setAntennaPower(int power) {

        if (power < 5 || power > 30) {
            return -1;
        } else {
            int status = getDeriver().setTxPowerOnce(power);
            if (status == 1) {
                return 0;
            } else {
                return -1;
            }
        }

    }

    @Override
    public int getAntennaPower() {
        int power = getDeriver().GetTxPower();
        if (-1020 == power) {
            //"数据错误"
            return -1;
        } else if (-1000 == power) {
            //"设备未连接"
            return -1;
        } else if (power < 5) {
            //"获取失败"
            return -1;
        } else {
            //"获取成功"
            return power;
        }
    }

    @Override
    public int setFreqRegion(int region) {
        int status = getDeriver().SetRegion(region);
        if (-1000 == status) {
            //"设备未连接", Toast.LENGTH_SHORT).show();
            return -1;
        } else if (-1020 == status) {
            // "数据错误", Toast.LENGTH_SHORT).show();
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public int getFreqRegion() {
        String sum;
        sum = getDeriver().getRegion();
        if (sum.equals("-1000")) {
            //"设备未连接"
            return -1;
        } else if (sum.equals("-1020")) {
            //"获取失败"
            return -1;
        } else {
            String text1 = sum.substring(2, 4);
            return Integer.parseInt(text1, 16);
        }
    }

    @Override
    public void reg_handler(Handler hd) {

    }

    @Override
    public int setlock(int type, int area, String passwd) {
        return 0;
    }

    @Override
    public String GetLastDetailError() {
        return null;
    }

    @Override
    public int setInvMode(int invm, int addr, int length) {
        return 0;
    }

    @Override
    public int getInvMode(int type) {
        return 0;
    }

    @Override
    public int setFrequency(double frequency) {
        return 0;
    }

    @Override
    public int enableEngTest(int gain) {
        return 0;
    }

    @Override
    public int setDynamicAlgorithm() {
        return 0;
    }

    public class YiXinParams {
        String epcData;
        int bank;
        boolean save;
        int ads;
        int len;

        YiXinParams() {
            bank = 1;
            ads = 32;
            len = 96;
            save = false;
        }
    }

    //*****************************************************************************************************//
    @Override
    public int yixinReadArea(String pwd, String data, int bank1, int ads1, int len1) {
        if (bank1 > 3 || bank1 < 0) {
            return -1;
        }
        //默认指定标签区域为epc 起始地址32  长度96
        String result = getDeriver().Read_Data_Tag(pwd, 1, 32, 96, data, bank1, ads1, len1);
        if (result != null) {
            SpdReadData spdReadData = new SpdReadData();
            spdReadData.setReadData(DataConversionUtils.hexStringToByteArray(result));
            spdReadData.setDataLen(DataConversionUtils.hexStringToByteArray(result).length);
            readCallBack(spdReadData);
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int yixinWriteArea(String pwd, String data, int bank1, int ads1, int len1, byte[] data1) {
        int length = data1.length;
        if ((length % 2) != 0) {
            return -3;
        }
        if ((length / 2) != len1) {
            return -3;
        }
        if ((bank1 >= 0) && (bank1 <= 3)) {
            int ret = getDeriver().Write_Data_Tag(pwd, 1, 32, 96, data, bank1, ads1, len1, StringUtils.byteToHexString(data1, data1.length));
            SpdWriteData spdWriteData = new SpdWriteData();
            if (ret == 0) {
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            } else {
                spdWriteData.setStatus(-1);
                writeCallBack(spdWriteData);
                return -1;
            }
        }
        return -1;
    }

    @Override
    public int yixinSetPwd(int which, String cur_pass, String new_pass, String data) {
        if (which > 1 || which < 0) {
            return -1;
        }
        byte[] stringToByte = StringUtils.stringToByte(new_pass);
        try {
            if (which == 0) {
                return yixinWriteArea(cur_pass, data, 0, 0, 2, stringToByte);
            } else {
                return yixinWriteArea(cur_pass, data, 0, 2, 2, stringToByte);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public int yixinSetLock(int which, String pwd, String Data, int bankvalue) {

        if (which == 0) {
            int ret = getDeriver().unLock_Tag_Data(pwd, 1, 32, 96, Data, bankvalue);
            if (ret == 0) {
                SpdWriteData spdWriteData = new SpdWriteData();
                spdWriteData.setEPCData(StringUtils.stringToByte(Data));
                spdWriteData.setEPCLen(StringUtils.stringToByte(Data).length);
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            } else {
                return -1;
            }
        } else if (which == 1) {
            int ret = getDeriver().Lock_Tag_Data(pwd, 1, 32, 96, Data, bankvalue);
            if (ret == 0) {
                SpdWriteData spdWriteData = new SpdWriteData();
                spdWriteData.setEPCData(StringUtils.stringToByte(Data));
                spdWriteData.setEPCLen(StringUtils.stringToByte(Data).length);
                spdWriteData.setStatus(0);
                writeCallBack(spdWriteData);
                return 0;
            } else {
                return -1;
            }
        } else if (which == 2) {
            return -1;
        } else if (which == 3) {
            return -1;
        } else {
            return -1;
        }
    }

    @Override
    public int yixinSetKill(String pwd, String data) {
        return getDeriver().Kill_Tag(pwd, 1, 32, 96, data);
    }


}
