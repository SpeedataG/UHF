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
import com.speedata.libutils.CommonUtils;
import com.speedata.libutils.ConfigUtils;
import com.speedata.libutils.ReadBean;
import com.uhf.api.cls.ErrInfo;
import com.uhf.api.cls.Reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * 旗连芯片  芯联方案
 * Created by 张明_ on 2016/11/29.
 */
public class XinLianQilian implements IUHFService {

    private static Reader Mreader = new Reader();
    private static int antportc;
    private Handler handler_inventer = null;
    private ReaderParams Rparams = new ReaderParams();
    private int ThreadMODE = 0;
    private Handler handler = new Handler();
    public boolean nostop = false;
    Reader.TagFilter_ST g2tf = null;
    private DeviceControl deviceControl;
    private Context mContext;
    private ReadBean mRead;
    private android.serialport.DeviceControl newDeviceControl;
    private Thread myInvThread = null;

    public XinLianQilian(Context mContext) {
        this.mContext = mContext;
    }


    //初始化模块
    public int OpenDev() {
        Log.d(TAG, "OpenDev: start");
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            mRead = ConfigUtils.readConfig(mContext);
            String powerType = mRead.getUhf().getPowerType();
            int[] intArray = new int[mRead.getUhf().getGpio().size()];
            for (int i = 0; i < mRead.getUhf().getGpio().size(); i++) {
                intArray[i] = mRead.getUhf().getGpio().get(i);
            }
            try {
                newDeviceControl = new android.serialport.DeviceControl(powerType, intArray);
                newDeviceControl.PowerOnDevice();
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
            return NoXmlOpenDEV();
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

    private int NoXmlOpenDEV() {
        Log.d("xl_1", String.valueOf(System.currentTimeMillis()));
        if (Build.VERSION.RELEASE.equals("4.4.2")) {
            try {
                deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN, 64);
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
        } else if (Build.VERSION.RELEASE.equals("5.1")) {
            String xinghao = Build.MODEL;
            if (xinghao.equals("KT55")) {
                String readEm55 = readEm55();
                if (readEm55.equals("80")) {
                    try {
                        deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 5);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (readEm55.equals("48") || readEm55.equals("81")) {
                    try {
                        deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN_AND_EXPAND
                                , 88, 7, 6);
                        deviceControl.PowerOnDevice();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN, 88);
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
                    || xinghao.equals("FC-PK80") || xinghao.equals("FC-K80")) {
                try {
                    deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN, 119);
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
                try {
                    deviceControl = new DeviceControl(DeviceControl.PowerType.MAIN, 94);
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
        Log.d(TAG, "OpenDev: end");
        return -1;
    }

    //关闭模块
    public void CloseDev() {
        Log.d(TAG, "CloseDev: start");
        if (Mreader != null)
            Mreader.CloseReader();
        if (ConfigUtils.isConfigFileExists() && !CommonUtils.subDeviceType().contains("55")) {
            try {
                newDeviceControl.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                deviceControl.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "CloseDev: end");

    }

    //注册过 Handler 后调用此函数开始盘点过程
    public void inventory_start() {
        if (inSearch) {
            return;
        }
        inSearch = true;
        cancelSelect();

//        handler.postDelayed(inv_thread, 0);
        myInvThread = new Thread(inv_thread);
        myInvThread.start();
        Log.d(TAG, "inventory_start: end");
    }

    @Override
    public void inventory_start(Handler hd) {
        Log.d(TAG, "inventory_start: start");
        reg_handler(hd);
        inventory_start();
    }

    private Listener mListener;

    @Override
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void newInventoryStart() {
        inventory_start();
    }

    @Override
    public void newInventoryStop() {
        handler.removeCallbacks(inv_thread);
    }

    //停止盘点。
    public int inventory_stop() {
        if (!inSearch) {
            return -1;
        }
        Log.d(TAG, "inventory_stop: start");
        inSearch = false;
//        Reader.READER_ERR er = Mreader.AsyncStopReading();
        try {
            if (myInvThread != null) {
                myInvThread.interrupt();
                myInvThread = null;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        SystemClock.sleep(500);
//        if (er == Reader.READER_ERR.MT_OK_ERR) {
//            return 0;
//        } else if (er == Reader.READER_ERR.MT_IO_ERR) {
//            return 1;
//        } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
//            return 2;
//        } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
//            return 3;
//        } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
//            return 4;
//        } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
//            return 5;
//        } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
//            return 6;
//        } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
//            return 7;
//        } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
//            return 8;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
//            return 9;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
//            return 10;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
//            return 11;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
//            return 12;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
//            return 13;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
//            return 14;
//        } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
//            return 15;
//        } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
//            return 16;
//        } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
//            return 17;
//        } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
//            return 18;
//        } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
//            return 19;
//        } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
//            return 20;
//        } else {
//            return 20;
//        }
//        handler.removeCallbacks(inv_thread);
        Log.d(TAG, "inventory_stop: end");
        return 0;
    }

    //从标签 area 区的 addr 位置（以 word 计算）读取 count 个值（以 byte 计算）
    // passwd 是访问密码，如果区域没被锁就给 0 值。
    public byte[] read_area(int area, int addr, int count, String passwd) {
        Log.d(TAG, "read_area: start22222");
        if ((area > 3) || (area < 0) || ((count % 2) != 0)) {
            return new byte[]{(byte) 0xFF, 0x07, (byte) 0xEE};
        }
        try {
            byte[] rdata = new byte[count];
            byte[] rpaswd = new byte[4];
            if (!passwd.equals("")) {
                Mreader.Str2Hex(passwd, passwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.GetTagData(Rparams.opant,
                        (char) area, addr, count / 2,
                        rdata, rpaswd, (short) Rparams.optime);

                trycount--;
                if (trycount < 1)
                    break;
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            Log.d(TAG, "read_area: end");
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                return rdata;
            } else if (er == Reader.READER_ERR.MT_IO_ERR) {
                return new byte[]{(byte) 0xFF, 0x01, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                return new byte[]{(byte) 0xFF, 0x02, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                return new byte[]{(byte) 0xFF, 0x03, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                return new byte[]{(byte) 0xFF, 0x04, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                return new byte[]{(byte) 0xFF, 0x05, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                return new byte[]{(byte) 0xFF, 0x06, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                return new byte[]{(byte) 0xFF, 0x07, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                return new byte[]{(byte) 0xFF, 0x08, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                return new byte[]{(byte) 0xFF, 0x09, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                return new byte[]{(byte) 0xFF, 0x10, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                return new byte[]{(byte) 0xFF, 0x11, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                return new byte[]{(byte) 0xFF, 0x12, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                return new byte[]{(byte) 0xFF, 0x13, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                return new byte[]{(byte) 0xFF, 0x14, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                return new byte[]{(byte) 0xFF, 0x15, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                return new byte[]{(byte) 0xFF, 0x16, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                return new byte[]{(byte) 0xFF, 0x17, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                return new byte[]{(byte) 0xFF, 0x18, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                return new byte[]{(byte) 0xFF, 0x19, (byte) 0xEE};
            } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                return new byte[]{(byte) 0xFF, 0x20, (byte) 0xEE};
            } else {
                return new byte[]{(byte) 0xFF, 0x20, (byte) 0xEE};
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[]{(byte) 0xFF, 0x20, (byte) 0xEE};
        }
    }

    public String read_area(int area, String str_addr
            , String str_count, String str_passwd) {
        Log.d(TAG, "read_card: start1111");
        if (TextUtils.isEmpty(str_passwd)) {
            return null;
        }
        if (!ByteCharStrUtils.IsHex(str_passwd)) {
            return null;
        }
        int num_addr;
        int num_count;
        try {
            num_addr = Integer.parseInt(str_addr, 16);
            num_count = Integer.parseInt(str_count, 10);
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
        String hexs = ByteCharStrUtils.b2hexs(v, v.length);
        return hexs;
    }


    //把 content 中的数据写到标签 area 区中 addr（以 word 计算）开始的位 置。
    public int write_area(int area, int addr, String passwd, byte[] content) {
        Log.d(TAG, "write_area: start22222");
        try {
            if ((content.length % 2) != 0) {
                return -3;
            }
            byte[] rpaswd = new byte[4];
            if (!passwd.equals("")) {
                Mreader.Str2Hex(passwd, passwd.length(), rpaswd);
            }
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.WriteTagData(Rparams.opant,
                        (char) area, addr, content, content.length, rpaswd,
                        (short) Rparams.optime);
                trycount--;
                if (trycount < 1)
                    break;
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            Log.d(TAG, "write_area: end");
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                return 0;
            } else if (er == Reader.READER_ERR.MT_IO_ERR) {
                return 1;
            } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                return 2;
            } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                return 3;
            } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                return 4;
            } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                return 5;
            } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                return 6;
            } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                return 7;
            } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                return 8;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                return 9;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                return 10;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                return 11;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                return 12;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                return 13;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                return 14;
            } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                return 15;
            } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                return 16;
            } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                return 17;
            } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                return 18;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                return 19;
            } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                return 20;
            } else {
                return 20;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int write_area(int area, String addr, String pwd, String count, String content) {
        Log.d(TAG, "write_area: start11111");
        if (TextUtils.isEmpty(pwd)) {
            return -3;
        }
        if (!ByteCharStrUtils.IsHex(pwd)) {
            return -3;
        }
        int num_addr;
        int num_count;
        try {
            num_addr = Integer.parseInt(addr, 16);
            num_count = Integer.parseInt(count, 10);
        } catch (NumberFormatException p) {
            return -3;
        }
        int rev = write_card(area, num_addr, num_count * 2,
                pwd, content);
        return rev;
    }

    public int write_card(int area, int addr, int count, String passwd, String cnt) {
//        byte[] cf;
//        StringTokenizer cn = new StringTokenizer(cnt);
//        if (cn.countTokens() < count) {
//            return -2;
//        }
//        cf = new byte[count];
//        int index = 0;
//        while (cn.hasMoreTokens() && (index < count)) {
//            try {
//                int k = Integer.parseInt(cn.nextToken(), 16);
//                if (k > 0xff) {
//                    throw new NumberFormatException("can't bigger than 0xff");
//                }
//                cf[index++] = (byte) k;
//            } catch (NumberFormatException p) {
//                return -3;
//            }
//        }
        byte[] cf = ByteCharStrUtils.toByteArray(cnt);
        return write_area(area, addr, passwd, cf);
    }


    //选中要进行操作的 epc 标签
    public int select_card(int bank, byte[] epc, boolean mFlag) {
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

    public int select_card(int bank, String epc, boolean mFlag) {
        Log.d(TAG, "select_card: start");
        if (!mFlag) {
            epc = "0000";
        }
        byte[] writeByte = ByteCharStrUtils.toByteArray(epc);
        if (select_card(bank, writeByte, mFlag) != 0) {
            Log.d(TAG, "select_card: failed");
            return -1;
        }
        Log.d(TAG, "select_card: end");
        return 0;
    }


    //设置天线功率
    public int set_antenna_power(int power) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    //读取当前天线功率值
    public int get_antenna_power() {
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

    //设定区域锁定状态。
    public int setlock(int type, int area, int passwd) {
        try {
            Reader.Lock_Obj lobj = null;
            Reader.Lock_Type ltyp = null;
            if (area == 0) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD;
                if (type == 0)
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_LOCK;
                else if (type == 2)
                    return -1;
                else if (type == 3) {
                    ltyp = Reader.Lock_Type.ACCESS_PASSWD_PERM_LOCK;
                }

            } else if (area == 1) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD;
                if (type == 0)
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_LOCK;
                else if (type == 2)
                    return -1;
                else if (type == 3) {
                    ltyp = Reader.Lock_Type.KILL_PASSWORD_PERM_LOCK;
                }
            } else if (area == 2) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK1;
                if (type == 0)
                    ltyp = Reader.Lock_Type.BANK1_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.BANK1_LOCK;
                else if (type == 2)
                    return -1;
                else if (type == 3)
                    ltyp = Reader.Lock_Type.BANK1_PERM_LOCK;
            } else if (area == 3) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK2;
                if (type == 0)
                    ltyp = Reader.Lock_Type.BANK2_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.BANK2_LOCK;
                else if (type == 2)
                    return -1;
                else if (type == 3)
                    ltyp = Reader.Lock_Type.BANK2_PERM_LOCK;
            } else if (area == 4) {
                lobj = Reader.Lock_Obj.LOCK_OBJECT_BANK3;
                if (type == 0)
                    ltyp = Reader.Lock_Type.BANK3_UNLOCK;
                else if (type == 1)
                    ltyp = Reader.Lock_Type.BANK3_LOCK;
                else if (type == 2)
                    return -1;
                else if (type == 3)
                    ltyp = Reader.Lock_Type.BANK3_PERM_LOCK;
            }
            byte[] rpaswd = new byte[4];
            for (int i = 0; i < 4; i++) {
                rpaswd[i] = (byte) (passwd >>> (24 - i * 8));
            }
            Reader.READER_ERR er = Mreader.LockTag(Rparams.opant,
                    (byte) lobj.value(), (short) ltyp.value(),
                    rpaswd, (short) Rparams.optime);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
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

//            long cp = Long.parseLong(cur_pass, 16);
//            if ((cp > 0xffffffffL) || (cp < 0)) {
//                throw new NumberFormatException("can't bigger than 0xffffffff");
//            }
//            long np = Long.parseLong(new_pass, 16);
//            if ((np > 0xffffffffL) || (np < 0)) {
//                throw new NumberFormatException("can't bigger than 0xffffffff");
//            }
//
//            byte[] nps = new byte[4];
//            nps[3] = (byte) ((np >> 0) & 0xff);
//            nps[2] = (byte) ((np >> 8) & 0xff);
//            nps[1] = (byte) ((np >> 16) & 0xff);
//            nps[0] = (byte) ((np >> 24) & 0xff);
//            return write_area(0, which * 2, (int) cp, nps);
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    //设置频率区域
    public int set_freq_region(int region) {
        try {
            Reader.Region_Conf rre;
            switch (region) {
                case 0:
                    rre = Reader.Region_Conf.RG_PRC;
                    break;
                case 1:
                    rre = Reader.Region_Conf.RG_NONE;
                    break;
                case 2:
                    rre = Reader.Region_Conf.RG_NA;
                    break;
                case 3:
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

    public int get_freq_region() {
        try {
            Reader.Region_Conf[] rcf2 = new Reader.Region_Conf[1];
            Reader.READER_ERR er = Mreader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rcf2);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                switch (rcf2[0]) {
                    case RG_PRC:
                        return 0;
                    case RG_NA:
                        return 2;
                    case RG_EU3:
                        return 3;
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

    //设置模式
    public int set_inventory_mode(int m) {
//        int[] val = new int[]{m};
//        Reader.READER_ERR er = Mreader.ParamSet(
//                Reader.Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE, val);
//        if (er != Reader.READER_ERR.MT_OK_ERR) {
//            return -1;
//        }
        return -1;
    }

    //拿到最近一次详细内部错误信息
    @Override
    public String GetLastDetailError() {
        ErrInfo ei = new ErrInfo();
        Mreader.GetLastDetailError(ei);
        return ei.derrcode + "-" + ei.errstr;
    }

    @Override
    public int SetInvMode(int invm, int addr, int length) {
        return 0;
    }

    @Override
    public int GetInvMode(int type) {
        return 0;
    }

    @Override
    public int setFrequency(double frequency) {
        return -1;
    }

    @Override
    public int enableEngTest(int gain) {
        return 0;
    }

    //获得模式
    public int get_inventory_mode() {
//        int[] val = new int[]{-1};
//        Reader.READER_ERR er = Mreader.ParamGet(
//                Reader.Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE, val);
//        if (er == Reader.READER_ERR.MT_OK_ERR) {
//            return val[0];
//        }
        return -1;
    }

    private volatile boolean inSearch = false;
    private Runnable inv_thread = new Runnable() {
        public void run() {
            while (inSearch) {
                Log.d(TAG, "run: 1111111111111111111111");
                int[] tagcnt = new int[1];
                tagcnt[0] = 0;
                synchronized (this) {
                    Reader.READER_ERR er;
                    int[] uants = Rparams.uants;
                    if (!inSearch) {
                        continue;
                    }
                    Log.d(TAG, "run: 2222222222222222222222222222");
                    er = Mreader.TagInventory_Raw(uants,
                            Rparams.uants.length,
                            (short) Rparams.readtime, tagcnt);
                    if (er == Reader.READER_ERR.MT_OK_ERR) {
                        if (tagcnt[0] > 0) {
                            for (int i = 0; i < tagcnt[0]; i++) {
                                if (!inSearch) {
                                    return;
                                }
                                Log.d(TAG, "run: 33333333333");
                                Reader.TAGINFO tfs = Mreader.new TAGINFO();
                                if (nostop)
                                    er = Mreader.AsyncGetNextTag(tfs);
                                else
                                    er = Mreader.GetNextTag(tfs);

                                if (er == Reader.READER_ERR.MT_OK_ERR) {
                                    byte[] n_epc = tfs.EpcId;
                                    String strEPCTemp = ByteCharStrUtils.b2hexs(n_epc, n_epc.length);
                                    Log.d(TAG, "run: 4444444444");
                                    String rssi = String.valueOf(tfs.RSSI);
                                    ArrayList<Tag_Data> cx = new ArrayList<Tag_Data>();
                                    Tag_Data tagData = new Tag_Data(null, strEPCTemp, rssi);
                                    if (handler_inventer == null) {
                                        mListener.update(tagData);
                                    } else {
                                        cx.add(tagData);
                                        Message msg = new Message();
                                        if (cx != null) {
                                            msg.what = 1;
                                            msg.obj = cx;
                                            handler_inventer.sendMessage(msg);
                                        }
                                    }

                                }
                            }
                        }

                    } else {
//                        if (nostop && er != Reader.READER_ERR.MT_OK_ERR) {
//                            handler.removeCallbacks(inv_thread);
//                        }
//                        if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
//                            inventory_stop();
//                        }
                        Log.d(TAG, "run: err");
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
                        handler_inventer.sendMessage(handler_inventer.obtainMessage(2, errCode));
                        inventory_stop();
//                        return;
                    }
                }
            }

        }
    };

    private void cancelSelect() {
        Reader.TagFilter_ST tfst = Mreader.new TagFilter_ST();
        tfst = null;
        Reader.READER_ERR filter_er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst);
    }


    //用户需要自己实现一个 Handler，然后用些函数向 API 注册，然后才可以 开始盘点。
    // 盘点到的 EPC 等数据会通过向注册的 Handler 发消息 （Message）的方式来实现。
    // 当搜索到有效 EPC 和 TID 数据后，Handler 会收到 Message，其中 Meseage.what 值为 1，
    // Message.obj 就是保存了 EPC 数据的 Tag_Data类的ArrayList。
    // Tag_Data 类有两个byte[ ]型的public 成员，一个是 epc，一个是 tid，如果为 null，
    // 代表对应的值不存在。
    public void reg_handler(Handler hd) {
        handler_inventer = hd;
    }

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
        }
    }
}