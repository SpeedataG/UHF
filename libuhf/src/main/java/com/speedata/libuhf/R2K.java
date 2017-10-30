package com.speedata.libuhf;


import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.serialport.DeviceControl;
import android.text.TextUtils;
import android.util.Log;

import com.speedata.libuhf.bean.Tag_Data;
import com.speedata.libuhf.utils.ByteCharStrUtils;
import com.speedata.libuhf.utils.StringUtils;
import com.speedata.libutils.CommonUtils;
import com.speedata.libutils.ConfigUtils;
import com.speedata.libutils.ReadBean;
import com.uhf.linkage.Linkage;
import com.uhf.linkage.Linkage.RFID_18K6C_TAG_MEM_PERM;
import com.uhf.linkage.Linkage.RFID_18K6C_TAG_PWD_PERM;
import com.uhf.structures.InventoryData;
import com.uhf.structures.InventoryParams;
import com.uhf.structures.OnInventoryListener;
import com.uhf.structures.OnReadWriteListener;
import com.uhf.structures.RW_Params;
import com.uhf.structures.Rfid_Value;
import com.uhf.structures.SelectCriteria;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
//R2000 接口实现

public class R2K implements IUHFService, OnInventoryListener, OnReadWriteListener {

    private static final String TAG = "r2000_native";
    private Linkage lk = null;
    private Handler h = null;
    private boolean inSearch = false;
    private DeviceControl pw = null;
    private Context mContext = null;
    private ReadBean mRead = null;
    private android.serialport.DeviceControl newDeviceControl = null;
    private byte[] epcData;
    private int writeStatus;

    public R2K(Context mContext) {
        this.mContext = mContext;
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
        ArrayList<Tag_Data> cx = new ArrayList<Tag_Data>();
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
            doSomething(new Tag_Data(strTIDTemp, strEPCTemp, strRSSITemp));
        } else {
            cx.add(new Tag_Data(strTIDTemp, strEPCTemp, strRSSITemp));
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
        if (rw_params.type == 2) {
            if (rw_params.status == 0) {
                epcData = rw_params.ReadData;
            } else {
                epcData = null;
            }
        } else if (rw_params.type == 3) {
            writeStatus = rw_params.status;
        }

    }

    public static final int InvModeType = 0;
    public static final int InvAddrType = 1;
    public static final int InvSizeType = 2;

    public int SetInvMode(int invm, int addr, int length) {
        InventoryParams inventoryParams = new InventoryParams();
        inventoryParams.inventoryArea = invm;
        inventoryParams.address = addr;
        inventoryParams.len = length;
        int i = getLinkage().Radio_SetInventoryParams(inventoryParams);
        return i;
    }

    public int GetInvMode(int type) {
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
            lk.setOnInventoryListener(this);
            lk.setOnReadWriteListener(this);
        }
        return lk;
    }

    public void reg_handler(Handler hd) {
        h = hd;
    }

    public int OpenDev() {
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            mRead = ConfigUtils.readConfig(mContext);
            String powerType = mRead.getUhf().getPowerType();
            int[] intArray = new int[mRead.getUhf().getGpio().size()];
            for (int i = 0; i < mRead.getUhf().getGpio().size(); i++) {
                intArray[i] = mRead.getUhf().getGpio().get(i);
            }
            try {
                newDeviceControl = new android.serialport.DeviceControl(powerType, intArray);
                newDeviceControl.PowerOffDevice();
                newDeviceControl.PowerOnDevice();
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
            return NoXmlOpenDev();
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "readEm55state: " + state);
        return state;
    }

    private int NoXmlOpenDev() {
        if (Build.VERSION.RELEASE.equals("4.4.2")) {
            try {
                pw = new DeviceControl(DeviceControl.PowerType.MAIN, 64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Build.VERSION.RELEASE.equals("5.1")) {
            String xinghao = Build.MODEL;
            if (xinghao.equals("KT80") || xinghao.equals("W6") || xinghao.equals("N80")
                    || xinghao.equals("Biowolf LE")) {
                try {
                    pw = new DeviceControl(DeviceControl.PowerType.MAIN, 119);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (xinghao.equals("KT55")) {
                String readEm55 = readEm55();
                if (readEm55.equals("80")) {
                    try {
                        pw = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 5);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (readEm55.equals("48") || readEm55.equals("81")) {
                    try {
                        pw = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 6);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        pw = new DeviceControl(DeviceControl.PowerType.MAIN, 88);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                try {
                    pw = new DeviceControl(DeviceControl.PowerType.MAIN, 94);
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
        int result = getLinkage().open_serial(SERIALPORT);
        if (result == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    public void CloseDev() {
        getLinkage().close_serial();
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            try {
                newDeviceControl.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                pw.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lk = null;

    }


    @Override
    public String GetLastDetailError() {
        //无定义
        return null;
    }


    public void inventory_start() {
//        if (inSearch) {
//            return;
//        }
        getLinkage().startInventory(0);
//        inSearch = true;
    }

    @Override
    public void inventory_start(Handler hd) {
        reg_handler(hd);
        inventory_start();
    }


    @Override
    public void newInventoryStart() {
//        if (!this.inSearch) {
//            this.inSearch = true;
        getLinkage().startInventory(0);
//        }
    }


    public void newInventoryStop() {
//        if (!inSearch) {
//            return;
//        }
//        inSearch = false;
        getLinkage().stopInventory();
    }

    public int inventory_stop() {
//        if (!inSearch) {
//            return -1;
//        }
//        inSearch = false;
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

    private static final int vp[] = {RFID_18K6C_TAG_PWD_PERM.ACCESSIBLE.getValue(),
            RFID_18K6C_TAG_PWD_PERM.SECURED_ACCESSIBLE.getValue(), RFID_18K6C_TAG_PWD_PERM
            .ALWAYS_ACCESSIBLE.getValue(), RFID_18K6C_TAG_PWD_PERM.ALWAYS_NOT_ACCESSIBLE.getValue
            (),};
    private static final int va[] = {RFID_18K6C_TAG_MEM_PERM.WRITEABLE.getValue(),
            RFID_18K6C_TAG_MEM_PERM.SECURED_WRITEABLE.getValue(), RFID_18K6C_TAG_MEM_PERM
            .ALWAYS_WRITEABLE.getValue(), RFID_18K6C_TAG_MEM_PERM.ALWAYS_NOT_WRITEABLE.getValue
            (),};

    public int setlock(int type, int area, int passwd) {
        int kp = RFID_18K6C_TAG_PWD_PERM.NO_CHANGE.getValue();
        int ap = RFID_18K6C_TAG_PWD_PERM.NO_CHANGE.getValue();
        int ta = RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();
        int ea = RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();
        int ua = RFID_18K6C_TAG_MEM_PERM.NO_CHANGE.getValue();

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
            byte[] rpaswd = new byte[4];
            for (int i = 0; i < 4; i++) {
                rpaswd[i] = (byte) (passwd >>> (24 - i * 8));
            }
            res = getLinkage().Radio_LockTag(rpaswd, ap, kp, ea, ta, ua);
        }
        if (res != 0) {
            return -1;
        }
        return 0;
    }

    public int setkill(int ap, int kp) {
        int res = getLinkage().Radio_KillTag(ap, kp);
        if (res != 0) {
            return -1;
        }
        return 0;
    }


    public byte[] read_area(int area, int addr, int count, String passwd) {
        epcData = null;
        if ((area > 3) || (area < 0) || ((count % 2) != 0)) {
            return null;
        }
        byte[] pwdBytes = StringUtils.stringToByte(passwd);
        int Read_status = getLinkage().Radio_ReadTag(count / 2, addr, area, pwdBytes);
        if (Read_status == 0) {
            SystemClock.sleep(100);
            return epcData;
        } else {
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
        String res = read_card(area, num_addr, num_count * 2, str_passwd);
        return res;
    }

    private String read_card(int area, int addr, int count, String passwd) {
        byte[] v = read_area(area, addr, count, passwd);
        if (v == null) {
            return null;
        }
        return StringUtils.byteToHexString(v, count);
    }

    public int set_freq_region(int region) {
        int res = -1;
        switch (region) {
            case REGION_CHINA_840_845:
                res = getLinkage().Radio_MacSetRegion(Linkage.RFID_18K6C_COUNTRY_REGION.China840_845);
                break;
            case REGION_CHINA_920_925:
                res = getLinkage().Radio_MacSetRegion(Linkage.RFID_18K6C_COUNTRY_REGION.China920_925);
                break;
            case REGION_CHINA_902_928:
                res = getLinkage().Radio_MacSetRegion(Linkage.RFID_18K6C_COUNTRY_REGION.Open_Area902_928);
                break;
            case REGION_EURO_865_868:
                res = getLinkage().Radio_MacSetRegion(Linkage.RFID_18K6C_COUNTRY_REGION.user_Area);
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
        return getLinkage().enableEngTest(gain);
    }

    public int get_freq_region() {
        Rfid_Value rfid_value = new Rfid_Value();
        int result;
        // 定频
        result = getLinkage().Radio_GetSingleFrequency(rfid_value);
        if (result == 0) {
            if (rfid_value.value < 840) {
                if (rfid_value.value == 0) {
                    return REGION_CHINA_840_845;
                } else if (rfid_value.value == 1) {
                    return REGION_CHINA_920_925;
                } else if (rfid_value.value == 2) {
                    return REGION_CHINA_902_928;
                } else if (rfid_value.value == 3) {
                    return REGION_920_5_924_5;
                }
            } else {
                return rfid_value.value;
            }
        }
        return -1;
    }

    public int write_area(int area, int addr, String passwd, byte[] content) {
        if ((content.length % 2) != 0) {
            return -3;
        }
        if ((area >= 0) && (area <= 3) && ((content.length % 2) == 0)) {
            byte[] pwdBytes = StringUtils.stringToByte(passwd);
            int status = getLinkage().Radio_WriteTag(content.length / 2,
                    addr, area, pwdBytes, content);
            if (status == 0) {
                SystemClock.sleep(50);
                return writeStatus;
            } else {
                return -1;
            }
        }
        return -1;
    }

    public int write_area(int area, String addr, String pwd, String count, String content) {
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
        return write_card(area, num_addr, num_count * 2, pwd, content);
    }


    public int write_card(int area, int addr, int count, String passwd, String content) {
        if ((content.length() % 2) != 0) {
            return -3;
        }
        if ((area >= 0) && (area <= 3) && ((content.length() % 2) == 0)) {
            byte[] stringToByte = StringUtils.stringToByte(content);
            byte[] pwdBytes = StringUtils.stringToByte(passwd);
            int status = getLinkage().Radio_WriteTag(content.length(),
                    addr, area, pwdBytes, stringToByte);
            if (status == 0) {
                SystemClock.sleep(50);
                return writeStatus;
            } else {
                return -1;
            }
        }
        return -1;
    }


    private final int ANTENNA_P_MIN = 10;
    private final int ANTENNA_P_MAX = 30;

    public int set_antenna_power(int power) {
        int res = -1;
        if ((power >= ANTENNA_P_MIN) && (power <= ANTENNA_P_MAX)) {
            res = getLinkage().Radio_SetAntennaPower(power * 10);
        }
        return res;
    }

    public int get_antenna_power() {
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
    public int select_card(int bank, byte[] epc, boolean mFlag) {
        SelectCriteria selectCriteria = new SelectCriteria();
        if (mFlag) {
            selectCriteria.status = 1;
        } else {
            selectCriteria.status = 0;
        }
        selectCriteria.length = epc.length * 8;
        selectCriteria.offset = 0;
        System.arraycopy(epc, 0, selectCriteria.maskData, 0, epc.length);
        return getLinkage().Radio_SetPostMatchCriteria(selectCriteria);
    }

    public int select_card(int bank, String epc, boolean mFlag) {
        byte[] writeByte = StringUtils.stringToByte(epc);
        return select_card(bank, writeByte, mFlag);
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


    /**
     * 盘点回调的listener
     */
    private Listener mListener;

    @Override
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    private void doSomething(Tag_Data result) {
        if (this.mListener != null) {
            this.mListener.update(result);
        }
    }

}