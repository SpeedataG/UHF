package com.speedata.libuhf;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.serialport.UHFDeviceControl;
import android.text.TextUtils;
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
import com.speedata.libuhf.utils.StringUtils;
import com.uhf.linkage.Linkage;
import com.uhf.structures.DynamicQParams;
import com.uhf.structures.InventoryData;
import com.uhf.structures.InventoryParams;
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

public class FLX implements IUHFService, OnInventoryListener, OnReadWriteListener {
    private Linkage lk = null;
    private Handler h = null;
    private UHFDeviceControl pw = null;
    private Context mContext = null;
    private ReadBean mRead = null;
    private UHFDeviceControl newUHFDeviceControl = null;
    private byte[] epcData;
    private volatile boolean isReadOutTime = false;
    private volatile boolean isReadSuccess = false;
    private int writeStatus;
    private int lockStatus;
    private volatile boolean isLockOutTime = false;
    private volatile boolean isLockSuccess = false;
    //    private volatile List<Integer> writeStatusLists = new ArrayList<>();
    public static final int InvModeType = 0;
    public static final int InvAddrType = 1;
    public static final int InvSizeType = 2;
    private int type = 0;
    private volatile boolean isWriteOutTime = false;
    private volatile boolean isWriteSuccess = false;

    public FLX(Context mContext, int type) {
        this.mContext = mContext;
        this.type = type;
    }

    public int setInvMode(int invm, int addr, int length) {
        InventoryParams inventoryParams = new InventoryParams();
        inventoryParams.inventoryArea = invm;
        inventoryParams.address = addr;
        inventoryParams.len = length;
        return getLinkage().Radio_SetInventoryParams(inventoryParams);
    }

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
            if (rw_params.status == 0) {
                byte[] readResultData = new byte[rw_params.DataLen];
                byte[] readData = rw_params.ReadData;
                System.arraycopy(readData, 0, readResultData, 0, rw_params.DataLen);
                spdReadData.setReadData(readResultData);
                this.epcData = readResultData;
                isReadSuccess = true;
                isReadOutTime = true;
                Log.d("ZM", "读卡状态: " + isReadSuccess + isReadOutTime);
            } else {
//                this.epcData = null;
                isReadOutTime = true;
                spdReadData.setReadData(null);
            }
            spdReadData.setDataLen(rw_params.DataLen);
            spdReadData.setRSS(rw_params.RSS);
            spdReadData.setStatus(rw_params.status);
            readCallBack(spdReadData);
        } else if (rw_params.type == 3) {
            writeStatus = rw_params.status;
            Log.d("ZM", "写卡状态: " + rw_params.status);
            if (rw_params.status == 0) {
                isWriteSuccess = true;
                isWriteOutTime = true;
            }

            SpdWriteData spdWriteData = new SpdWriteData();
            spdWriteData.setEPCData(resultData);
            spdWriteData.setEPCLen(rw_params.EPCLen);
            spdWriteData.setRSS(rw_params.RSS);
            spdWriteData.setStatus(rw_params.status);
            writeCallBack(spdWriteData);
        } else if (rw_params.type == 4 || rw_params.type == 5) {
            lockStatus = rw_params.status;
            Log.d("ZM", "锁卡状态: " + rw_params.status);
            if (rw_params.status == 0) {
                isLockSuccess = true;
                isLockOutTime = true;
            }

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
    public int openDev() {
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            mRead = ConfigUtils.readConfig(mContext);
            String powerType = mRead.getUhf().getPowerType();
            int[] intArray = new int[mRead.getUhf().getGpio().size()];
            for (int i = 0; i < mRead.getUhf().getGpio().size(); i++) {
                intArray[i] = mRead.getUhf().getGpio().get(i);
            }
            try {
                newUHFDeviceControl = new UHFDeviceControl(powerType, intArray);
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
                pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN, 64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String xinghao = Build.MODEL;
            if (xinghao.equalsIgnoreCase("SD60RT") || xinghao.equalsIgnoreCase("SD60")) {
                try {
//                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.NEW_MAIN, 86);
                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.EXPAND, 9, 14);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.contains("SD55L")) {
                try {
                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN, 128);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")
                    || xinghao.equals("Biowolf LE") || xinghao.equals("FC-PK80")
                    || xinghao.equals("FC-K80") || xinghao.equals("T80") || xinghao.contains("80")) {
                try {
                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN, 119);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.contains("55") || xinghao.equals("W2H")) {
                String readEm55 = readEm55();
                if (readEm55.equals("80")) {
                    try {
                        pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 5);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (readEm55.equals("48") || readEm55.equals("81")) {
                    try {
                        pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 6);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN, 88);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (xinghao.contains("SD100")) {
                // TODO: 2018/10/10   上电处理
                try {
                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.GAOTONG_MAIN);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    pw = new UHFDeviceControl(UHFDeviceControl.PowerType.MAIN, 94);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            pw.PowerOffDevice();
            pw.PowerOnDevice();
        } catch (IOException e) {
            e.printStackTrace();

        }
        SystemClock.sleep(20);
        int result;
        String xinghao = Build.MODEL;
        if (xinghao.equalsIgnoreCase("SD60RT") || xinghao.equalsIgnoreCase("SD60")) {
            result = getLinkage().open_serial(SERIALPORT_SD60);
        } else if (xinghao.contains("SD100")) {
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
    public void closeDev() {
        getLinkage().close_serial();
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            try {
                newUHFDeviceControl.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                pw.PowerOffDevice();
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
        getLinkage().startInventory(0);
    }

    /**
     * 停止盘点
     */
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
            }
            byte[] rpaswd = StringUtils.stringToByte(passwd);
            return getLinkage().Radio_LockTag(rpaswd, ap, kp, ea, ta, ua);
        }
        return -1;
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

    //********************************************老版接口（不再维护）******************************************


    public void reg_handler(Handler hd) {
        h = hd;
    }

    @Override
    public String GetLastDetailError() {
        //无定义
        return null;
    }


    public void inventory_start() {
        getLinkage().startInventory(0);
    }

    @Override
    public void inventory_start(Handler hd) {
        reg_handler(hd);
        inventory_start();
    }

    public int inventory_stop() {
        getLinkage().stopInventory();
        return 0;
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

    public int setlock(int type, int area, String passwd) {
        lockStatus = -1;
        isLockOutTime = false;
        isLockSuccess = false;
        int kp = Linkage.RFID_18K6C_TAG_PWD_PERM.NO_CHANGE.getValue();
        int ap = Linkage.RFID_18K6C_TAG_PWD_PERM.NO_CHANGE.getValue();
        int ta = Linkage.RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();
        int ea = Linkage.RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();
        int ua = Linkage.RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();

        int res = -1;
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
            }
            byte[] rpaswd = StringUtils.stringToByte(passwd);
            res = getLinkage().Radio_LockTag(rpaswd, ap, kp, ea, ta, ua);
            if (res == 0) {
                LockTimeOutThread timeOutThread = new LockTimeOutThread();
                timeOutThread.start();

                while (!isLockOutTime) {
                    if (isLockSuccess) {
                        return 0;
                    }
                }
                return lockStatus;
            } else {
                return -1;
            }
        }
        return -1;
    }

    public int setkill(int ap, int kp) {
//        int res = getLinkage().Radio_KillTag(ap, kp);
//        if (res != 0) {
//            return -1;
//        }
        return 0;
    }


    public byte[] read_area(int area, int addr, int count, String passwd) {
        epcData = null;
        isReadOutTime = false;
        isReadSuccess = false;
        if ((area > 3) || (area < 0)) {
            return null;
        }
        byte[] pwdBytes = StringUtils.stringToByte(passwd);
        int Read_status = getLinkage().Radio_ReadTag(count, addr, area, pwdBytes);
        if (Read_status == 0) {
            ReadTimeOutThread timeOutThread = new ReadTimeOutThread();
            timeOutThread.start();

            while (!isReadOutTime) {
                Log.d("zm", "read_area-isReadSuccess状态： " + isReadSuccess + "isReadOutTime状态：" + isReadOutTime);
                if (isReadSuccess) {
                    Log.d("zm", "read_area: success");
                    return epcData;
                }
            }
            Log.d("zm", "read_area: failed");
            return null;
        } else {
            Log.d("zm", "read_area: failed");
            return null;
        }
    }

    public String read_area(int area, String str_addr
            , String str_count, String str_passwd) {
        if (TextUtils.isEmpty(str_passwd)) {
            return null;
        }
        if (!ByteCharStrUtils.IsHex(str_passwd)) {
            return null;
        }
        int num_addr;
        int num_count;
        try {
            num_addr = Integer.parseInt(str_addr);
            num_count = Integer.parseInt(str_count);
        } catch (NumberFormatException p) {
            return null;
        }
        return read_card(area, num_addr, num_count, str_passwd);
    }

    private String read_card(int area, int addr, int count, String passwd) {
        byte[] v = read_area(area, addr, count, passwd);
        if (v == null) {
            return null;
        }
        return StringUtils.byteToHexString(v, count);
    }

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
        }
        return res;
    }

    //设置定频频点
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

    @Override
    public int setDynamicAlgorithm() {
//        if (TextUtils.isEmpty(etTry.getText().toString())) {
//            MyApp.getToast().showShortToast(this, "请输入重试次数");
//            return;
//
//        } else if (TextUtils.isEmpty(etStartQ.getText().toString())) {
//            MyApp.getToast().showShortToast(this, "请输入起始Q值");
//            return;
//        } else if (TextUtils.isEmpty(etMinValue.getText().toString())) {
//            MyApp.getToast().showShortToast(this, "请输入最小Q值");
//            return;
//        } else if (TextUtils.isEmpty(etMaxValue.getText().toString())) {
//            MyApp.getToast().showShortToast(this, "请输入最大Q值");
//            return;
//        } else if (TextUtils.isEmpty(etThreshold.getText().toString())) {
//            MyApp.getToast().showShortToast(this, "请输入阀值");
//            return;
//        }
        int tryCount = 0;
        int start_Q = 4;
        int minQ = 0;
        int maxQ = 15;
        int threshold = 4;
//        if (start_Q < 0 || start_Q > 15 || minQ < 0 || minQ > 15 || maxQ < 0 || maxQ > 15 || threshold < 0 || threshold > 255) {
//            MyApp.getToast().showShortToast(this, "不在Q值0～15的取值范围内，无效Q值");
//            return;
//        }
//
//        if (minQ > maxQ) {
//            MyApp.getToast().showShortToast(this, "Q值范围设置错误");
//            return;
//        }
//        if (threshold < 0 || threshold > 255) {
//            MyApp.getToast().showShortToast(this, "不在阀值0～255的取值范围内，无效阀值");
//            return;
//        }
//        if (tryCount < 0 || tryCount > 10) {
//            MyApp.getToast().showShortToast(this, "不在重试次数0～10的取值范围内，无效重试次数");
//            return;
//        }

        DynamicQParams dp = new DynamicQParams();
        dp.setValue(start_Q, minQ, maxQ, tryCount, 1, threshold);
        int dynamicResult = getLinkage().Radio_SetSingulationAlgorithmDyParameters(dp);

        if (dynamicResult == 0) {
            return 0;
//            MyApp.getToast().showShortToast(this, "设置成功");
        } else if (dynamicResult == -1000) {
            return -1000;
//            MyApp.getToast().showShortToast(this, "正在盘点");
        } else {
//            MyApp.getToast().showShortToast(this, "设置失败");
            return -1;
        }

    }

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

    public int write_area(int area, int addr, int count, String passwd, byte[] content) {
        isWriteOutTime = false;
        isWriteSuccess = false;
        writeStatus = -1;
        int length = content.length;
        if ((length % 2) != 0) {
            return -3;
        }
        if ((length / 2) != count) {
            return -3;
        }
        if ((area >= 0) && (area <= 3) && ((length % 2) == 0)) {
            byte[] pwdBytes = StringUtils.stringToByte(passwd);
            int status = getLinkage().Radio_WriteTag(count,
                    addr, area, pwdBytes, content);
            Log.d("ZM", "write_card: 状态" + status);
            if (status == 0) {
                TimeOutThread timeOutThread = new TimeOutThread();
                timeOutThread.start();

                while (!isWriteOutTime) {
                    if (isWriteSuccess) {
                        Log.d("ZM", "write_card: 状态" + "成功");
                        return 0;
                    }
                }
                return writeStatus;
            } else {
                return -1;
            }
        }
        return -1;
    }

    public int write_area(int area, String addr, String pwd, String count, String content) {
        isWriteOutTime = false;
        isWriteSuccess = false;
        writeStatus = -1;
        if (TextUtils.isEmpty(pwd)) {
            return -3;
        }
        if (!ByteCharStrUtils.IsHex(pwd)) {
            return -3;
        }
        int num_addr;
        int num_count;
        try {
            num_addr = Integer.parseInt(addr);
            num_count = Integer.parseInt(count);
        } catch (NumberFormatException p) {
            return -3;
        }
        return write_card(area, num_addr, num_count, pwd, content);
    }


    public int write_card(int area, int addr, int count, String passwd, String content) {
        if ((content.length() % 2) != 0) {
            return -3;
        }
        byte[] stringToByte = StringUtils.stringToByte(content);
        if ((stringToByte.length / 2) != count) {
            return -3;
        }
        if ((area >= 0) && (area <= 3)) {
            byte[] pwdBytes = StringUtils.stringToByte(passwd);
            int status = getLinkage().Radio_WriteTag(count,
                    addr, area, pwdBytes, stringToByte);
            Log.d("ZM", "write_card: 状态" + status);
            if (status == 0) {
                TimeOutThread timeOutThread = new TimeOutThread();
                timeOutThread.start();
                while (!isWriteOutTime) {
                    if (isWriteSuccess) {
                        Log.d("ZM", "write_card: 状态" + "成功");
                        return 0;
                    }
                }
                return writeStatus;
            } else {
                return -1;
            }
        }
        return -1;
    }

    class TimeOutThread extends Thread {
        @Override
        public void run() {
            super.run();
            SystemClock.sleep(1000);
            isWriteOutTime = true;
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

    class ReadTimeOutThread extends Thread {
        @Override
        public void run() {
            super.run();
            SystemClock.sleep(1000);
            isReadOutTime = true;
        }
    }

    private final int ANTENNA_P_MIN = 10;
    private final int ANTENNA_P_MAX = 32;

    public int setAntennaPower(int power) {
        int res = -1;
        if ((power >= ANTENNA_P_MIN) && (power <= ANTENNA_P_MAX)) {
            res = getLinkage().Radio_SetAntennaPower(power * 10);
        }
        return res;
    }

    public int getAntennaPower() {
        Rfid_Value rv = new Rfid_Value();
        int p = getLinkage().Radio_GetAntennaPower(rv);
        if (p != 0) {
            return -1;
        }
        return rv.value / 10;
    }

    public int set_link_prof(int pf) {
        int res = -1;
        if ((pf >= 0) && (pf < 4)) {
            res = getLinkage().Radio_SetCurrentLinkProfile(pf);
        }
        return res;
    }

    public int get_link_prof() {
        Rfid_Value rv = new Rfid_Value();
        int p = getLinkage().Radio_GetCurrentLinkProfile(rv);
        if (p != 0) {
            return -1;
        }
        return rv.value;
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

    public int selectCard(int bank, String epc, boolean mFlag) {
        byte[] writeByte = StringUtils.stringToByte(epc);
        return selectCard(bank, writeByte, mFlag);
    }

    //设置密码
    public int set_Password(int which, String cur_pass, String new_pass) {
        if (which > 1 || which < 0) {
            return -1;
        }
        try {
            if (which == 0) {
                return write_area(0, "0", cur_pass, "2", new_pass);
            } else {
                return write_area(0, "2", cur_pass, "2", new_pass);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
