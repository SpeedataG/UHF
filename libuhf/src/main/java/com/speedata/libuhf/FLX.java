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
import com.speedata.libuhf.utils.CommonUtils;
import com.speedata.libuhf.utils.ConfigUtils;
import com.speedata.libuhf.utils.ReadBean;
import com.speedata.libuhf.utils.SharedXmlUtil;
import com.speedata.libuhf.utils.StringUtils;
import com.uhf.linkage.Linkage;
import com.uhf.structures.AntennaPorts;
import com.uhf.structures.DynamicQParams;
import com.uhf.structures.FixedQParams;
import com.uhf.structures.InventoryData;
import com.uhf.structures.InventoryParams;
import com.uhf.structures.LowpowerParams;
import com.uhf.structures.OnInventoryListener;
import com.uhf.structures.OnReadWriteListener;
import com.uhf.structures.RW_Params;
import com.uhf.structures.Rfid_Value;
import com.uhf.structures.SelectCriteria;
import com.uhf.structures.TagGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/**
 * r2000+旗联方案 共用
 * Created by 张明_ on 2017/11/15.
 */

public class FLX extends IUHFServiceAdapter implements OnInventoryListener, OnReadWriteListener {
    private Linkage lk = null;
    private Handler h = null;
    private DeviceControlSpd pw = null;
    private Context mContext;
    private ReadBean mRead = null;
    private DeviceControlSpd newUHFDeviceControl = null;
    public static final int InvModeType = 0;
    public static final int InvAddrType = 1;
    public static final int InvSizeType = 2;
    private int type = 0;
    private int mode = 1;
    private byte[] readData;
    private int status = -1;

    public FLX(Context mContext, int type) {
        this.mContext = mContext;
        this.type = type;
    }

    @Override
    public int setInvMode(int invm, int addr, int length) {
        InventoryParams inventoryParams = new InventoryParams();
        inventoryParams.inventoryArea = invm;
        inventoryParams.address = addr;
        inventoryParams.len = length;
        return getLinkage().Radio_SetInventoryParams(inventoryParams);
    }

    @Override
    public int getInvMode(int type) {
        InventoryParams inventoryParams = new InventoryParams();
        int i = getLinkage().Radio_GetInventoryParams(inventoryParams);
        if (i == 0) {
            switch (type) {
                case InvModeType:
                    return inventoryParams.inventoryArea;
                case InvAddrType:
                    return inventoryParams.address;
                case InvSizeType:
                    return inventoryParams.len;
                default:
                    return -1;
            }
        } else {
            return -1;
        }
    }

    public Linkage getLinkage() {
        if (lk == null) {
            lk = new Linkage();
            lk.initRFID();
            lk.setRFModuleType(type);
            lk.setOnInventoryListener(this);
            lk.setOnReadWriteListener(this);
        }
        return lk;
    }

    //设置连接模式，默认传0本地模式，传1为蓝牙透传模式
    public void setRFConnectMode(int flag) {
        getLinkage().setRFConnectMode(flag);
    }

    //接收到蓝牙数据后传来的R2000数据交由库解析，解析完成后，原接口不变
    public void pushRemoteRFIDData(byte[] packageData) {
        getLinkage().pushRemoteRFIDData(packageData);
    }

    /**
     * 盘点回调
     *
     * @param inventoryData InventoryData
     */
    @Override
    public void getInventoryData(InventoryData inventoryData) {
        String strEPCTemp = "";
        String strTIDTemp = "";
        String strRSSITemp = "";
        ArrayList<SpdInventoryData> cx = new ArrayList<SpdInventoryData>();
        if (inventoryData.epcLength > 0 && inventoryData.epcLength < 66) {
            strEPCTemp = StringUtils.byteToHexString(inventoryData.EPC_Data,
                    inventoryData.epcLength);
            strRSSITemp = String.valueOf(inventoryData.RSSI);
        }
        if (inventoryData.dataLength > 0 && inventoryData.dataLength < 66) {
            strTIDTemp = StringUtils.byteToHexString(inventoryData.data,
                    inventoryData.dataLength);
        }
        if (h == null) {
            inventoryCallBack(new SpdInventoryData(strTIDTemp, strEPCTemp, strRSSITemp));
        } else {
            cx.add(new SpdInventoryData(strTIDTemp, strEPCTemp, strRSSITemp));
            Message message = new Message();
            message.what = 1;
            message.obj = cx;
            h.sendMessage(message);
        }
    }

    /**
     * 读写回调
     *
     * @param rw_params RW_Params
     */
    @Override
    public void getReadWriteData(RW_Params rw_params) {
        byte[] resultData = new byte[rw_params.EPCLen];
        try {
            byte[] epcData = rw_params.EPCData;
            System.arraycopy(epcData, 0, resultData, 0, rw_params.EPCLen);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (rw_params.type == 2) {
            SpdReadData spdReadData = new SpdReadData();
            spdReadData.setEPCData(resultData);
            spdReadData.setEPCLen(rw_params.EPCLen);
            Log.d("ZM", "读卡状态: " + rw_params.status);
            this.status = rw_params.status;
            if (rw_params.status == 0) {
                byte[] readResultData = new byte[rw_params.DataLen];
                byte[] readData = rw_params.ReadData;
                System.arraycopy(readData, 0, readResultData, 0, rw_params.DataLen);
                spdReadData.setReadData(readResultData);
                this.readData = readResultData;
            } else {
                spdReadData.setReadData(null);
            }
            spdReadData.setDataLen(rw_params.DataLen);
            spdReadData.setRSS(rw_params.RSS);
            spdReadData.setStatus(rw_params.status);
            readCallBack(spdReadData);
        } else if (rw_params.type == 3) {
            Log.d("ZM", "写卡状态: " + rw_params.status);
            SpdWriteData spdWriteData = new SpdWriteData();
            spdWriteData.setEPCData(resultData);
            spdWriteData.setEPCLen(rw_params.EPCLen);
            spdWriteData.setRSS(rw_params.RSS);
            spdWriteData.setStatus(rw_params.status);
            writeCallBack(spdWriteData);
        } else if (rw_params.type == 4 || rw_params.type == 5) {
            Log.d("ZM", "锁卡状态: " + rw_params.status);
            SpdWriteData spdWriteData = new SpdWriteData();
            spdWriteData.setEPCData(resultData);
            spdWriteData.setEPCLen(rw_params.EPCLen);
            spdWriteData.setRSS(rw_params.RSS);
            spdWriteData.setStatus(rw_params.status);
            writeCallBack(spdWriteData);
        }

    }

    /**
     * 上电开串口
     *
     * @return 0成功-1失败
     */
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
                int result = getLinkage().open_serial(mRead.getUhf().getSerialPort());
                if (result == 0) {
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
                pw = new DeviceControlSpd(DeviceControlSpd.PowerType.MAIN, 64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String xinghao = SystemProperties.get("ro.product.model");
            if ("SD60RT".equalsIgnoreCase(xinghao) || "MST-II-YN".equalsIgnoreCase(xinghao) || "SD60".equalsIgnoreCase(xinghao) || "SD55L".equalsIgnoreCase(xinghao) || xinghao.contains("SC60")
                    || xinghao.contains("DXD60RT") || "ST60E".equalsIgnoreCase(xinghao) || xinghao.contains("C6000") || "ESUR-H600".equals(xinghao) || "smo_b2000".equals(xinghao)) {
                try {
//                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.NEW_MAIN, 86);
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
            } else if ("SN50".equals(xinghao) || "SD50".equals(xinghao) || "R550".equals(xinghao)) {
                try {
                    pw = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 75);
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
                } catch (IOException e) {
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
            if (!SystemProperties.get("ro.product.model").equals("SD100")) {
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
                || "ESUR-H600".equals(xinghao) || "ST60E".equalsIgnoreCase(xinghao) || "smo_b2000".equals(xinghao)) {
            result = getLinkage().open_serial(SERIALPORT_SD60);
        } else if (xinghao.equals("SD55PTT")) {
            result = getLinkage().open_serial(SERIALPORT1);
        } else if ("SN50".equals(xinghao) || "SD50".equals(xinghao) || "R550".equals(xinghao)) {
            result = getLinkage().open_serial(SERIALPORT0);
        } else if (xinghao.contains("SD55") || xinghao.contains("R66") || xinghao.contains("A56")) {
            if (ConfigUtils.getApiVersion() > 23) {
                result = getLinkage().open_serial(SERIALPORT0);
            } else {
                result = getLinkage().open_serial(SERIALPORT);
            }
        } else if (xinghao.contains("SD100")) {
            SystemClock.sleep(240);
            result = getLinkage().open_serial(SERIALPORT_SD100);
        } else {
            result = getLinkage().open_serial(SERIALPORT);
        }
        if (result == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * 下电关串口
     */
    @Override
    public void closeDev() {
        getLinkage().close_serial();
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
                } else if (SystemProperties.get("ro.product.model").equals("SD100")) {
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
        lk = null;
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
        getLinkage().startInventory(mode, 0);
    }

    /**
     * 停止盘点
     */
    @Override
    public void inventoryStop() {
        getLinkage().stopInventory();
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
        if ((area > 3) || (area < 0)) {
            return -1;
        }
        byte[] pwdBytes = StringUtils.stringToByte(passwd);
        if (pwdBytes.length != 4) {
            return -3;
        }
        return getLinkage().Radio_ReadTag(count, addr, area, pwdBytes);
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
            byte[] pwdBytes = StringUtils.stringToByte(passwd);
            return getLinkage().Radio_WriteTag(count,
                    addr, area, pwdBytes, content);
        }
        return -1;
    }

    /**
     * 设置密码
     *
     * @param which    区域
     * @param cur_pass 原密码
     * @param new_pass 新密码
     * @return
     */
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

    @Override
    public int setLock(int type, int area, String passwd) {
        int kp = Linkage.RFID_18K6C_TAG_PWD_PERM.NO_CHANGE.getValue();
        int ap = Linkage.RFID_18K6C_TAG_PWD_PERM.NO_CHANGE.getValue();
        int ta = Linkage.RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();
        int ea = Linkage.RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();
        int ua = Linkage.RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();

        if ((type >= 0) && (type <= 3) && (area >= 0) && (area <= 4)) {
            switch (area) {
                case KILL_PW_L:
                    kp = vp[type];
                    break;
                case ACCESS_PW_L:
                    ap = vp[type];
                    break;
                case EPC_L:
                    ea = va[type];
                    break;
                case TID_L:
                    ta = va[type];
                    break;
                case USER_L:
                    ua = va[type];
                    break;
                default:
                    break;
            }
            byte[] rpaswd = StringUtils.stringToByte(passwd);
            return getLinkage().Radio_LockTag(rpaswd, ap, kp, ea, ta, ua);
        }
        return -1;
    }

    @Override
    public int setKill(String accessPassword, String killPassword) {
        byte[] accessPwd = StringUtils.stringToByte(accessPassword);
        byte[] killPwd = StringUtils.stringToByte(killPassword);
        return getLinkage().Radio_KillTag(accessPwd, killPwd);
    }

    @Override
    public int setQueryTagGroup(int selected, int session, int target) {
        TagGroup tg = new TagGroup(selected, session, target);
        return getLinkage().Radio_SetQueryTagGroup(tg);
    }

    @Override
    public int getQueryTagGroup() {
        TagGroup tagGroup = new TagGroup();
        int value = getLinkage().Radio_GetQueryTagGroup(tagGroup);
        if (value == 0) {
            return tagGroup.session;
        }
        return -1;
    }

    @Override
    public int mask(int area, int addr, int length, byte[] content) {
        int queryTagGroup = setQueryTagGroup(2, 2, 0);
        if (queryTagGroup != 0) {
            return queryTagGroup;
        }

        SelectCriteria selectCriteria = new SelectCriteria();
        selectCriteria.status = 1;
        selectCriteria.length = length;
        selectCriteria.offset = addr;
        selectCriteria.bank = area;
        selectCriteria.session = 4;
        selectCriteria.jq = 0;
        selectCriteria.action = 0;
        System.arraycopy(content, 0, selectCriteria.maskData, 0, content.length);
        return getLinkage().set18K6CSelectCriteria(selectCriteria);
    }

    @Override
    public int cancelMask() {
        int queryTagGroup = setQueryTagGroup(0, 2, 0);
        if (queryTagGroup != 0) {
            return queryTagGroup;
        }
        getLinkage().set18K6CSelectCriteria(new SelectCriteria(0));
        return 0;
    }

    @Override
    public SelectCriteria getMask() {
        int status;
        SelectCriteria sc = new SelectCriteria();
        status = getLinkage().get18K6CSelectCriteria(sc);
        if (status == 0) {
            return sc;
        }
        return null;
    }

    @Override
    public int setMonzaQtTagMode(int memMap, int maskFlag, byte[] accessPassword) {
        return getLinkage().setMonzaQtTagMode(memMap, maskFlag, accessPassword);
    }

    @Override
    public int readMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length) {
        return getLinkage().readMonzaQtTag(memMap, pwd, bank, address, length);
    }

    @Override
    public int readMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, int timeOutMs, RW_Params rw_params) {
        return getLinkage().readMonzaQtTagSync(memMap, pwd, bank, address, length, timeOutMs, rw_params);
    }

    @Override
    public int writeMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData) {
        return getLinkage().writeMonzaQtTag(memMap, pwd, bank, address, length, writeData);
    }

    @Override
    public int writeMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData, int timeOutMs, RW_Params rw_params) {
        return getLinkage().writeMonzaQtTagSync(memMap, pwd, bank, address, length, writeData, timeOutMs, rw_params);
    }

    @Override
    public int setDynamicAlgorithm(int startQ, int minQ, int maxQ, int tryCount, int target, int threshold) {
        if (startQ < 0 || startQ > 15 || minQ < 0 || minQ > 15 || maxQ < 0 || maxQ > 15) {
            return -1;
        }
        if (minQ > maxQ) {
            return -2;
        }
        if (threshold < 0 || threshold > 255) {
            return -3;
        }
        if (tryCount < 0 || tryCount > 10) {
            return -4;
        }
        DynamicQParams dp = new DynamicQParams();
        dp.setValue(startQ, minQ, maxQ, tryCount, target, threshold);
        int dynamicResult = getLinkage().Radio_SetSingulationAlgorithmDyParameters(dp);

        if (dynamicResult == 0) {
            return 0;
        } else {
            return dynamicResult;
        }

    }

    @Override
    public int setFixedAlgorithm(int qValue, int tryCount, int target, int repeat) {
        if (qValue < 0 || qValue > 15) {
            return -1;
        }
        if (tryCount < 0 || tryCount > 10) {
            return -4;
        }
        FixedQParams fd = new FixedQParams();
        fd.setValue(qValue, tryCount, target, repeat);
        int fixedResult = getLinkage().Radio_SetSingulationAlgorithmFixedParameters(fd);

        if (fixedResult == 0) {
            return 0;
        } else {
            return fixedResult;
        }
    }

    @Override
    public int getDynamicAlgorithm(com.speedata.libuhf.bean.DynamicQParams dynamicQParams) {
        DynamicQParams dynamicQParams1 = new DynamicQParams();
        int res = getLinkage().Radio_GetSingulationAlgorithmDyParameters(dynamicQParams1);
        if (res == 0) {
            dynamicQParams.setValue(dynamicQParams1.startQValue,
                    dynamicQParams1.minQValue,
                    dynamicQParams1.maxQValue,
                    dynamicQParams1.retryCount,
                    dynamicQParams1.toggleTarget,
                    dynamicQParams1.thresholdMultiplier);
        }
        return res;
    }

    @Override
    public int getFixedAlgorithm(com.speedata.libuhf.bean.FixedQParams fixedQParams) {
        FixedQParams fixedQParams1 = new FixedQParams();
        int res = getLinkage().Radio_GetSingulationAlgorithmFixedParameters(fixedQParams1);
        if (res == 0) {
            fixedQParams.setValue(fixedQParams1.qValue,
                    fixedQParams1.retryCount,
                    fixedQParams1.toggleTarget,
                    fixedQParams1.repeatUntiNoTags);
        }
        return res;
    }

//    @Override
//    public int krSm7Inventory(InventoryData inventoryData) {
//        int result = getLinkage().krSm7Inventory(inventoryData);
//        return result;
//    }
//
//    @Override
//    public int krSm7Blockwrite(int length, int addr, int area, byte[] pwd, byte[] content) {
//        int result = getLinkage().krSm7Blockwrite(length, addr, area, pwd, content);
//        return result;
//    }
//
//    @Override
//    public int krSm7Write(int length, int addr, int area, byte[] pwd, byte[] content) {
//        int result = getLinkage().krSm7Write(length, addr, area, pwd, content);
//        return result;
//    }
//
//    @Override
//    public int krSm7Read(int length, int addr, int area, byte[] pwd, KrReadData krReadData) {
//        int result = getLinkage().krSm7Read(pwd, addr, area, length, krReadData);
//        return result;
//    }

    @Override
    public int krSm7End() {
        int result = getLinkage().krSm7End();
        return result;
    }

    @Override
    public int setNewEpc(String password, int len, byte[] epc) {
        if (len > 31) {
            return -3;
        }
        if (len * 2 < epc.length) {
            return -3;
        }
        status = -1;
        readArea(IUHFService.EPC_A, 1, 1, password);
        Log.d("ZM", "读卡状态: " + status);
        int i = 0;
        while (status != 0 && i < 10) {
            if (readData != null) {
                break;
            }
            i++;
            SystemClock.sleep(100);
        }
        Log.d("ZM", "读卡状态: " + status);
        Log.d("ZM", "readData: " + readData);
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
    public int switchInvMode(int mode) {
        this.mode = mode;
        return 0;
    }

    @Override
    public int setLowpowerScheduler(int invOnTime, int invOffTime) {
        LowpowerParams lowpoerParams = new LowpowerParams(0, invOnTime, invOffTime);
        int result = getLinkage().setLowpowerScheduler(lowpoerParams);
        return result;
    }

    @Override
    public int[] getLowpowerScheduler() {
        int[] a = new int[2];
        LowpowerParams lowpowerParams = new LowpowerParams();
        int res = getLinkage().getLowpowerScheduler(lowpowerParams);
        if (res == 0) {
            a[0] = lowpowerParams.getInventoryOnTime();
            a[1] = lowpowerParams.getInventoryOffTime();
            return a;
        } else {
            return null;
        }
    }

    @Override
    public int setDwellTime(int dwellTime) {
        AntennaPorts antennaPorts = new AntennaPorts();
        int i = getLinkage().getAntennaPort(0, antennaPorts);
        if (i != 0) {
            return -1;
        }
        int power = antennaPorts.getPowerLevel();
        return getLinkage().setAntennaPort(0, 1, power, dwellTime, antennaPorts.getNumberInventoryCycles());
    }

    @Override
    public int getDwellTime() {
        AntennaPorts antennaPorts = new AntennaPorts();
        int i = getLinkage().getAntennaPort(0, antennaPorts);
        if (i == 0) {
            return antennaPorts.getDwellTime();
        } else {
            return -1;
        }
    }

    public final int KILL_PW_L = 0;
    public final int ACCESS_PW_L = 1;
    public final int EPC_L = 2;
    public final int TID_L = 3;
    public final int USER_L = 4;

    public final int UNLOCK = 0;
    public final int LOCK = 1;
    public final int P_UNLOCK = 2;
    public final int P_LOCK = 3;

    private static final int vp[] = {Linkage.RFID_18K6C_TAG_PWD_PERM.ACCESSIBLE.getValue(),
            Linkage.RFID_18K6C_TAG_PWD_PERM.SECURED_ACCESSIBLE.getValue(), Linkage.RFID_18K6C_TAG_PWD_PERM
            .ALWAYS_ACCESSIBLE.getValue(), Linkage.RFID_18K6C_TAG_PWD_PERM.ALWAYS_NOT_ACCESSIBLE.getValue
            (),};
    private static final int va[] = {Linkage.RFID_18K6C_TAG_MEM_PERM.WRITEABLE.getValue(),
            Linkage.RFID_18K6C_TAG_MEM_PERM.SECURED_WRITEABLE.getValue(), Linkage.RFID_18K6C_TAG_MEM_PERM
            .ALWAYS_WRITEABLE.getValue(), Linkage.RFID_18K6C_TAG_MEM_PERM.ALWAYS_NOT_WRITEABLE.getValue
            (),};

    @Override
    public int setFreqRegion(int region) {
        int res = -1;
        if (type == 1 && region == 0) {
            region = 4;
        }
        switch (region) {
            case REGION_CHINA_840_845:
                res = getLinkage().Radio_MacSetRegion(0);
                break;
            case REGION_CHINA_920_925:
                res = getLinkage().Radio_MacSetRegion(1);
                break;
            case REGION_CHINA_902_928:
                res = getLinkage().Radio_MacSetRegion(2);
                break;
            case REGION_EURO_865_868:
                res = getLinkage().Radio_MacSetRegion(3);
                break;
            case 4:
                res = getLinkage().Radio_MacSetRegion(4);
                break;
            case 6:
                res = getLinkage().Radio_MacSetRegion(6);
                break;
            default:
                break;
        }
        return res;
    }

    @Override
    public int getFreqRegion() {
        Rfid_Value rfid_value = new Rfid_Value();
        int result;
        // 定频
        if (type == 1) {
            result = getLinkage().Radio_MacGetRegion(rfid_value);
        } else {
            result = getLinkage().Radio_GetSingleFrequency(rfid_value);
        }
        if (result == 0) {
            Log.d("zm", "getFreqRegion: " + rfid_value.value);
            if (rfid_value.value < 840) {
                if (rfid_value.value == 0 || rfid_value.value == 4) {
                    return REGION_CHINA_840_845;
                } else if (rfid_value.value == 1) {
                    return REGION_CHINA_920_925;
                } else if (rfid_value.value == 2) {
                    return REGION_CHINA_902_928;
                } else if (rfid_value.value == 3) {
                    return REGION_EURO_865_868;
                }
            } else {
                return rfid_value.value;
            }
        }
        return -1;
    }

    private final int ANTENNA_P_MIN = 10;
    private final int ANTENNA_P_MAX = 33;

    @Override
    public int setAntennaPower(int power) {
        int res = -1;
        if ((power >= ANTENNA_P_MIN) && (power <= ANTENNA_P_MAX)) {
            res = getLinkage().Radio_SetAntennaPower(power * 10);
        }
        if (res == 0) {
            SharedXmlUtil.getInstance(mContext).write("AntennaPower", power);
        }
        return res;
    }

    @Override
    public int getAntennaPower() {
        Rfid_Value rv = new Rfid_Value();
        int p = getLinkage().Radio_GetAntennaPower(rv);
        if (p != 0) {
            return -1;
        }
        return rv.value / 10;
    }

    @Override
    public int selectCard(int bank, byte[] epc, boolean mFlag) {
        SelectCriteria selectCriteria = new SelectCriteria();
        if (mFlag) {
            selectCriteria.status = 1;
        } else {
            selectCriteria.status = 0;
        }
        selectCriteria.length = epc.length * 8;
        selectCriteria.offset = 0;
        System.arraycopy(epc, 0, selectCriteria.maskData, 0, epc.length);
        if (type == 1) {
            selectCriteria.bank = bank;
            selectCriteria.offset = 32;
            return getLinkage().set18K6CSelectCriteria(selectCriteria);
        } else {
            return getLinkage().Radio_SetPostMatchCriteria(selectCriteria);
        }

    }

    @Override
    public int selectCard(int bank, String epc, boolean mFlag) {
        byte[] writeByte = StringUtils.stringToByte(epc);
        return selectCard(bank, writeByte, mFlag);
    }

    //********************************************老版接口（不再维护）******************************************

    //设置定频频点
    @Override
    public int setFrequency(double frequency) {
        int singleFrequency = (int) (frequency * 1000);
        if (singleFrequency < 840000 || singleFrequency > 960000 || (singleFrequency % 125 != 0)) {
            return -1;
        }
        return getLinkage().Radio_SetSingleFrequency(singleFrequency);
    }

    //载波测试接口
    @Override
    public int enableEngTest(int gain) {
//        return getLinkage().enableEngTest(gain);
        return 0;
    }

}
