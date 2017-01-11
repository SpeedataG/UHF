package com.android.uhflibs;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.speedata.libuhf.DeviceControl;
import com.speedata.libuhf.INV_TIME;
import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.Tag_Data;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by 张明_ on 2016/12/30.
 */

public class as3992_native implements IUHFService {
    private int fd;
    private byte[] buf;
    private byte[] lock = new byte[0];
    private boolean read_ok = false;
    private boolean isChecking = false;
    private Handler mHandler = null;
    private static final String TAG = "as3992_native";
    private get_inventoryData_thread mGetInventoryDataThread;
    private DeviceControl deviceControl;
    private ReadThread rthread;
    private byte[] mEpc;

    @Override
    public int OpenDev() {
        if (android.os.Build.VERSION.RELEASE.equals("4.4.2")) {
            deviceControl = new DeviceControl(POWERCTL, 64);
        }else if (android.os.Build.VERSION.RELEASE.equals("5.1")){
            deviceControl = new DeviceControl(POWERCTL, 94);
        }
        if (OpenComPort(SERIALPORT) != 0) {
            return -1;
        }
        try {
            deviceControl.PowerOnDevice();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
//        String version = String.valueOf(get_version(1));
        return 0;
    }

    public int OpenComPort(String device) {
        fd = open_serial(device);
        Log.i(TAG, "file is " + fd);
        if (fd < 0) {
            Log.e(TAG, "native open returns null");
            return -1;
        }
        return 0;
    }


    @Override
    public void CloseDev() {
//        rthread.interrupt();
        CloseComPort();
        deviceControl.PowerOffDevice();
    }

    public void CloseComPort() {
        close_serial(fd);
    }

    //获得当前软硬件版本信息
    //0 代表获取固件版本信息；1 代表获取软件版本信息
    public byte[] get_version(int which) {
        byte[] cmd = new byte[3];
        cmd[0] = 0x10;
        cmd[1] = (byte) cmd.length;
        cmd[2] = (byte) which;
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            Log.e(TAG, "write get_ver cmd failed");
            return null;
        }
        synchronized (lock) {
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (read_ok) {
                read_ok = false;
                byte[] res = buf;
                Log.i(TAG, "get vaild ver data");
                return res;
            } else {
                Log.e(TAG, "time out or no valid ver data");
                return null;
            }
        }
    }

    private class get_inventoryData_thread extends Thread {
        @Override
        public void run() {
            super.run();
            while (isChecking) {
                Message msg = new Message();
                ArrayList<Tag_Data> tg = search_card();
                if (tg != null) {
                    msg.what = 1;
                    msg.obj = tg;
                    mHandler.sendMessage(msg);
                }
            }

        }
    }


    public void read_thread() {
        byte[] head = try_read(fd, 5, 10);
        if (head == null) {
            return;
        }
        if (head[0] != 2) {
            drop_data(fd);
            return;
        }
        int lens = btoi(head[1]) * 256 + btoi(head[2]);
        if ((lens - 2) != head[4]) {
            Log.e("as3992_thread", "frame head is " + lens + " and data head is " + head[4] + " not equal");
            drop_data(fd);
            return;
        }
        if (head[4] < 3) {
            Log.e("as3992_thread", "wrong data length is " + head[4] + "!!!!!!!!!!!!!!!!!!!!!!");
            drop_data(fd);
            return;
        }
        switch (head[3]) {
            case 0x40: // for iso6b
            case 0x48: // for iso6b
            case 0x50: // for iso6b
            case 0x11:
            case 0x19:
            case 0x1b:
            case 0x1d:
            case 0x32:
            case 0x34:
            case 0x36:
            case 0x38:
            case 0x3c:
            case 0x3e:
            case 0x42:
            case 0x44:
            case 0x56:
            case 0x58:
            case 0x5a:
                Log.d("as3992", "read thread get in sync");
                byte[] tp = try_read(fd, head[4], 10);
                if (tp == null) {
                    drop_data(fd);
                    Log.e("as3992", "read valid data failed");
                    return;
                }
                if (tp[tp.length - 2] != 0x3) {
                    drop_data(fd);
                    Log.e("as3992", "etx value is not 3");
                    return;
                }
                byte lrc = (byte) (0xff + head[3] + head[4]);
                for (int sl = 0; sl < tp.length - 1; sl++) {
                    lrc += tp[sl];
                }
                if (lrc != tp[tp.length - 1]) {
                    drop_data(fd);
                    Log.e("as3992", "lrc checksum error");
                    return;
                }
                synchronized (lock) {
                    buf = new byte[tp.length - 2];
                    System.arraycopy(tp, 0, buf, 0, buf.length);
                    Log.d("as3992", "read valid data ok, ins is " + head[3]);
                    read_ok = true;
                    lock.notify();
                    break;
                }
            default:
                Log.e("as3992", "get error data");
                break;
        }
        Log.d("as3992", "leave success");
        return;
    }

    private int btoi(byte a) {
        return (a < 0 ? a + 256 : a);
    }

    class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                read_thread();
            }
        }
    }


    public ArrayList<Tag_Data> search_card() {
        ArrayList<Tag_Data> cx = new ArrayList<Tag_Data>();
        byte[] cmd = new byte[3];
        cmd[0] = 0x31;
        cmd[1] = 3;
        cmd[2] = 1;
        int card_num = 0;
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            return null;
        }
        synchronized (lock) {
            do {
                try {
                    Log.d("as3992", "search begin to wait");
                    lock.wait(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (read_ok && buf.length >= 2 && (buf[1] > 2) && (buf.length >= buf[1] + 2)) {
                    Log.d("as3992", "search card read ok");
                    read_ok = false;
                    if (card_num == 0) {
                        if (buf[0] == 0) {
                            Log.e("as3992", "no card found");
                            return null;
                        }
                        card_num = buf[0];
                        Log.d("as3992", "get at last one card");
                    }
                    int epc_length = buf[1] - 2;
                    byte[] pc = new byte[2];
                    byte[] epc = new byte[epc_length];
                    System.arraycopy(buf, 2, pc, 0, 2);
                    System.arraycopy(buf, 4, epc, 0, epc_length);
                    cx.add(new Tag_Data(pc, epc));
                    card_num--;
                } else {
                    read_ok = false;
                    return null;
                }
            } while (card_num > 0);
        }
        return cx;
    }

    @Override
    public void inventory_start() {
        if (isChecking) {
            return;
        }
        isChecking = true;
        rthread = new ReadThread();
        rthread.start();
        mGetInventoryDataThread = new get_inventoryData_thread();
        mGetInventoryDataThread.start();
    }

    @Override
    public void inventory_start(Handler hd) {
        reg_handler(hd);
        inventory_start();
    }

    @Override
    public void inventory_stop() {
        if (!isChecking) {
            return;
        }
        isChecking = false;
        mGetInventoryDataThread.interrupt();

    }

    @Override
    public byte[] read_area(int area, int addr, int count, int passwd) {
        byte[] res = null;
        do {
            if (mEpc != null && select_card(mEpc) < 0) {
                Log.e("as3992", "read select failed");
                continue;
            }
            res = read_area(area, addr, count / 2);
        } while (res == null);

        return res;
    }

    public String read_area(int area, String str_addr
            , String str_count, String str_passwd) {
        int num_addr;
        int num_count;
        long passwd;
        try {
            num_addr = Integer.parseInt(str_addr, 16);
            num_count = Integer.parseInt(str_count, 10);
            passwd = Long.parseLong(str_passwd, 16);
        } catch (NumberFormatException p) {
            return null;
        }
        String res = read_card(area, num_addr, num_count * 2, (int) passwd);
        return res;
    }

    private String read_card(int area, int addr, int count, int passwd) {
        byte[] v = read_area(area, addr, count, passwd);
        if (v == null) {
            return null;
        }
        String j = new String();
        for (byte i : v) {
            j += String.format("%02x ", i);
        }
        return j;
    }

    public byte[] read_area(int area, int addr, int count) {
        byte[] cmd = new byte[5];
        cmd[0] = 0x37;
        cmd[1] = 5;
        cmd[2] = (byte) area;
        cmd[3] = (byte) addr;
        cmd[4] = (byte) count;
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            return null;
        }
        synchronized (lock) {
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (read_ok && (buf[1] > 0)
                    && (buf.length > 1 ? (count == 0 ? true : buf[1] == count)
                    : false) && (count == 0 ? true : buf[0] == 0)) {
                read_ok = false;
                Log.i("as3992_native", "readed " + buf[1]);
                if ((count == 0) && (buf[1] == 0)) {
                    Log.e("as3992_native", "try read more faild");
                    return null;
                }
                if (buf.length < (2 + buf[1] * 2)) {
                    Log.e("as3992_native", "read_area don't get enough data");
                    return null;
                }
                byte[] res = new byte[buf[1] * 2];
                System.arraycopy(buf, 2, res, 0, res.length);
                return res;
            } else {
                if (buf.length >= 2) {
                    Log.e("as3992", "read area failed " + read_ok + " error "
                            + buf[0] + " readed number " + buf[1]);
                } else {
                    Log.e("as3992", "read area failed " + read_ok + " error");
                }
                read_ok = false;
                return null;
            }
        }
    }


    @Override
    public int write_area(int area, int addr, int passwd, byte[] content) {
        byte[] rpaswd = new byte[4];
        for (int i = 0; i < 4; i++) {
            rpaswd[i] = (byte) (passwd >>> (24 - i * 8));
        }
        int i = -1;
        do {
            if (mEpc != null && select_card(mEpc) < 0) {
                Log.e("as3992", "read select failed");
                continue;
            }
            i = write_area(area, addr, content.length / 2, rpaswd, content);
        } while (i == -1);

        return 0;
    }

    public int write_area(int area, String addr, String pwd, String count, String content) {
        int num_addr;
        int num_count;
        long passwd;
        try {
            num_addr = Integer.parseInt(addr, 16);
            num_count = Integer.parseInt(count, 10);
            passwd = Long.parseLong(pwd, 16);
        } catch (NumberFormatException p) {
            return -3;
        }
        byte[] cf;
        StringTokenizer cn = new StringTokenizer(content);
        if (cn.countTokens() < num_count) {
            return -2;
        }
        cf = new byte[num_count];
        int index = 0;
        while (cn.hasMoreTokens() && (index < num_count)) {
            try {
                int k = Integer.parseInt(cn.nextToken(), 16);
                if (k > 0xff) {
                    throw new NumberFormatException("can't bigger than 0xff");
                }
                cf[index++] = (byte) k;
            } catch (NumberFormatException p) {
                return -3;
            }
        }
        return write_area(area, num_addr, (int) passwd, cf);
    }

    public int write_area(int area, int addr, int count, byte[] passwd,
                          byte[] content) {
        if (count * 2 != content.length) {
            return -1;
        }
        byte[] cmd = new byte[11];
        cmd[0] = 0x35;
        cmd[1] = (byte) cmd.length;
        cmd[2] = (byte) area;
        System.arraycopy(passwd, 0, cmd, 4, passwd.length);
        cmd[8] = 1;
        int rts = 0;
        for (int i = 0; i < count; ) {
            cmd[3] = (byte) (addr + i);
            System.arraycopy(content, i * 2, cmd, 9, 2);
            int reval = try_write(fd, wrap(cmd));
            if (reval < 0) {
                return -1;
            }
            synchronized (lock) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (read_ok && (buf.length >= 2) && buf[0] == 0) {
                    read_ok = false;
                    Log.i("as3992_native", "writed " + buf[1] + " words");
                    i++;
                    rts = 0;
                } else {
                    if (buf.length >= 2) {
                        Log.e("as3992_native", "wrtie area failed " + read_ok
                                + " error " + buf[0] + " write ok : write number "
                                + buf[1]);
                    } else {
                        Log.e("as3992_native", "wrtie area failed " + read_ok);
                    }
                    read_ok = false;
                    rts++;
                    if (rts == 10) {
                        Log.e("as3992_debug", "only write " + i + " words");
                        return i;
                    }
                }
            }
        }
        return count;
    }


    @Override
    public int select_card(byte[] epc) {
        mEpc = epc;
        byte[] cmd = new byte[3 + epc.length];
        cmd[0] = 0x33;
        cmd[1] = (byte) (cmd.length);
        cmd[2] = (byte) epc.length;
        System.arraycopy(epc, 0, cmd, 3, epc.length);
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            return -1;
        }
        synchronized (lock) {
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (read_ok && (buf.length >= 1) && buf[0] == 0) {
                read_ok = false;
                return 0;
            } else {
                if (buf.length >= 1) {
                    Log.e("as3992", "selsect card " + read_ok + " error "
                            + buf[0]);
                } else {
                    Log.e("as3992", "selsect card " + read_ok);
                }
                read_ok = false;
                return -1;
            }
        }
    }

    public int select_card(String epc) {
        byte[] eepc;
        StringTokenizer sepc = new StringTokenizer(epc);
        eepc = new byte[sepc.countTokens()];
        int index = 0;
        while (sepc.hasMoreTokens()) {
            try {
                eepc[index++] = (byte) Integer.parseInt(sepc.nextToken(), 16);
            } catch (NumberFormatException p) {
                return -1;
            }
        }
        if (select_card(eepc) != 0) {
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
            long cp = Long.parseLong(cur_pass, 16);
            if ((cp > 0xffffffffL) || (cp < 0)) {
                throw new NumberFormatException("can't bigger than 0xffffffff");
            }
            long np = Long.parseLong(new_pass, 16);
            if ((np > 0xffffffffL) || (np < 0)) {
                throw new NumberFormatException("can't bigger than 0xffffffff");
            }

            byte[] nps = new byte[4];
            nps[3] = (byte) ((np >> 0) & 0xff);
            nps[2] = (byte) ((np >> 8) & 0xff);
            nps[1] = (byte) ((np >> 16) & 0xff);
            nps[0] = (byte) ((np >> 24) & 0xff);
            return write_area(0, which * 2, (int) cp, nps);
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    @Override
    public int set_antenna_power(int power) {
        return -1;
    }

    public int set_antenna_power(boolean power) {
        byte[] cmd = new byte[3];
        cmd[0] = 0x18;
        cmd[1] = (byte) cmd.length;
        cmd[2] = (byte) (power ? 0xff : 0);
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            return -1;
        }
        synchronized (lock) {
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (read_ok) {
                read_ok = false;
                return 0;
            } else {
                return -1;
            }
        }
    }


    @Override
    public int get_antenna_power() {
        return -1;
    }

    @Override
    public int set_freq_region(int region) {
        int result = -1;
        int time = 0;
        do {
            switch (region) {
                case REGION_CHINA_840_845:
                    result = set_freq(840125, 844875, 250, -40, 0);
                    break;
                case REGION_CHINA_920_925:
                    result = set_freq(920625, 924375, 750, -40, 0);
                    break;
                case REGION_CHINA_902_928:
                    result = set_freq(902750, 927250, 500, -40, 0);
                    break;
                case REGION_EURO_865_868:
                    result = set_freq(865700, 867500, 600, -40, 0);
                    break;
            }
            time++;
        } while (result != 0 && time < 15);

        if (result != 0) {
            return -1;
        }
        return result;
    }

    public int set_freq(int start, int stop, int increment, int rssi, int id) {
        if (increment <= 0) {
            Log.e("as3993", "inc value can't be 0");
            return -1;
        }
        byte mode = 8;
        byte[] cmd = new byte[8];
        cmd[0] = 0x41;
        cmd[1] = (byte) cmd.length;
        cmd[6] = (byte) rssi;
        cmd[7] = (byte) id;
        for (int freq = start; freq <= stop; freq += increment) {
            cmd[2] = mode;
            cmd[3] = (byte) freq;
            cmd[4] = (byte) (freq >> 8);
            cmd[5] = (byte) (freq >> 16);
            int reval = try_write(fd, wrap(cmd));
            if (reval < 0) {
                Log.e("as3992", "write set freq cmd failed in " + freq);
                return -1;
            }
            synchronized (lock) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (read_ok && (buf.length >= 1) && buf[0] != 0) {
                    Log.d("as3992", "set freq " + freq + " ok");
                    read_ok = false;
                } else {
                    Log.e("as3992", "set freq failed or wait too long time "
                            + freq);
                    if (buf != null && (buf.length >= 1) && (buf[0] == 0)) {
                        Log.e("as3992", "freq list is full");
                        return -2;
                    }
                    read_ok = false;
                    return -1;
                }
            }
            mode = 4;
        }
        return 0;
    }

    public Freq_Msg get_freq() {
        byte[] cmd = {0x41, 0x3, 0x11};
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            Log.e("as3992", "write get freq cmd failed");
            return null;
        }
        synchronized (lock) {
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (read_ok && (buf.length >= 18)) {
                Log.d("as3992", "get freq ok");
                Log.i("as3392_native", "number of freq " + buf[15]);
                int start = btoi(buf[9]) + (btoi(buf[10]) << 8)
                        + (btoi(buf[11]) << 16);
                int stop = btoi(buf[12]) + (btoi(buf[13]) << 8)
                        + (btoi(buf[14]) << 16);
                int inc;
                if (btoi(buf[15]) == 1) {
                    inc = 0;
                } else {
                    inc = (stop - start) / (btoi(buf[15]) - 1);
                }
                int rssi = buf[16];
                int id = buf[2];
                int listentime = btoi(buf[3]) + (btoi(buf[4]) << 8);
                int allocationtime = btoi(buf[5]) + (btoi(buf[6]) << 8);
                int idletime = btoi(buf[7]) + (btoi(buf[8]) << 8);
                Log.i("as3992_natvie", "start " + start + " stop " + stop
                        + " inc " + inc + " rssi " + rssi + " listen "
                        + listentime + " alloc " + allocationtime + " idle "
                        + idletime);
                read_ok = false;
                return new Freq_Msg(start, stop, inc, rssi, id, listentime,
                        allocationtime, idletime);
            } else {
                Log.e("as3992", "get freq failed " + read_ok + " buflength "
                        + buf.length);
                read_ok = false;
                return null;
            }
        }
    }

    public class Freq_Msg {
        Freq_Msg(int n_start, int n_stop, int n_inc, int n_rssi, int n_id,
                 int n_listentime, int n_allocationtime, int n_idletime) {
            start = n_start;
            stop = n_stop;
            inc = n_inc;
            rssi = n_rssi;
            id = n_id;
            listentime = n_listentime;
            allocationtime = n_allocationtime;
            idletime = n_idletime;
        }

        public int start;
        public int stop;
        public int inc;
        public int rssi;
        public int id;
        public int listentime;
        public int allocationtime;
        public int idletime;
    }


    @Override
    public int get_freq_region() {
        Freq_Msg res = get_freq();
        if (res.start == 840125) {
            return REGION_CHINA_840_845;
        } else if (res.start == 920625) {
            return REGION_CHINA_920_925;
        } else if (res.start == 902750) {
            return REGION_CHINA_902_928;
        } else if (res.start == 865700) {
            return REGION_EURO_865_868;
        }
        return -1;
    }

    @Override
    public void reg_handler(Handler hd) {
        mHandler = hd;
    }

    @Override
    public INV_TIME get_inventory_time() {
        return null;
    }

    @Override
    public int set_inventory_time(int work_t, int rest_t) {
        return 0;
    }

    @Override
    public int MakeSetValid() {
        return 0;
    }

    //public int setlock(byte type, byte area, byte[] passwd)
    //以前3992接口定义
    public int setlock(byte type, byte area, byte[] passwd) {
        byte[] cmd = new byte[8];
        cmd[0] = 0x3B;
        cmd[1] = (byte) cmd.length;
        cmd[2] = type;
        cmd[3] = area;
        System.arraycopy(passwd, 0, cmd, 4, 4);
        int reval = try_write(fd, wrap(cmd));
        if (reval < 0) {
            return -1;
        }
        synchronized (lock) {
            try {
                lock.wait(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (read_ok && (buf.length >= 1) && buf[0] == 0) {
                read_ok = false;
                return 0;
            } else {
                if (buf.length >= 1) {
                    Log.e("as3992", "lock card failed " + read_ok + " error "
                            + buf[0]);
                } else {
                    Log.e("as3992", "lock card failed " + read_ok);
                }
                read_ok = false;
                return -1;
            }
        }
    }

    @Override
    public int setlock(int type, int area, int passwd) {
        byte[] rpaswd = new byte[4];
        for (int i = 0; i < 4; i++) {
            rpaswd[i] = (byte) (passwd >>> (24 - i * 8));
        }
        int setlock = setlock((byte) type, (byte) area, rpaswd);
        return setlock;
    }

    private byte[] wrap(byte[] in) {
        byte[] res = new byte[in.length + 5];
        res[0] = 0x2;
        res[1] = (byte) ((in.length + 2) >> 8);
        res[2] = (byte) (in.length + 2);
        System.arraycopy(in, 0, res, 3, in.length);
        res[3 + in.length] = 0x3;
        byte cc = (byte) 0xff;
        for (int i = 0; i < in.length; i++) {
            cc += in[i];
        }
        cc += 3;
        res[4 + in.length] = cc;
        return res;
    }


    @Override
    public int get_inventory_mode() {
        return 0;
    }

    @Override
    public int set_inventory_mode(int m) {
        return 0;
    }

    private native int open_serial(String port);

    private native void close_serial(int fd);

    private native int try_write(int fd, byte[] buf);

    private native byte[] try_read(int fd, int count, int delay);

    private native void drop_data(int fd);

    static {
        System.loadLibrary("package");
        System.loadLibrary("uhfrfid");
    }
}
