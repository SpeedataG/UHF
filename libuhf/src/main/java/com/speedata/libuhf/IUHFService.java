package com.speedata.libuhf;


import android.os.Handler;

import com.speedata.libuhf.bean.Tag_Data;

/**
 * Created by brxu on 2016/12/13.
 */

public interface IUHFService {

    public static final int REGION_CHINA_840_845 = 0;
    public static final int REGION_CHINA_920_925 = 1;
    public static final int REGION_CHINA_902_928 = 2;
    public static final int REGION_EURO_865_868 = 3;
    public static final int REGION_920_5_924_5 = 3;

    public static final int RESERVED_A = 0;
    public static final int EPC_A = 1;
    public static final int TID_A = 2;
    public static final int USER_A = 3;
    public static final int FAST_MODE = 0;
    public static final int SMART_MODE = 1;
    public static final int LOW_POWER_MODE = 2;
    public static final int USER_MODE = 3;
    public static final String SERIALPORT = "/dev/ttyMT2";
    public static final String POWERCTL = "/sys/class/misc/mtgpio/pin";


    //默认参数初始化模块
    public int OpenDev();

    //释放模块
    public void CloseDev();

    //开始盘点
    public void inventory_start();

    // Handler用于处理返回的盘点数据
    public void inventory_start(Handler hd);

    public void setListener(Listener listener);

    public interface Listener {
        void update(Tag_Data var1);
    }
    //新盘点方法，Listener回调
    public void newInventoryStart();

    public void newInventoryStop();


    //设置密码
    public int set_Password(int which, String cur_pass, String new_pass);
     //停止盘点
    public int inventory_stop();

    /**
     * 从标签 area 区的 addr 位置（以 word 计算）读取 count 个值（以 byte 计算）
     * passwd 是访问密码，如果区域没被锁就给 0 值。
     *
     * @param area
     * @param addr
     * @param count
     * @param passwd
     * @return
     */
    public byte[] read_area(int area, int addr, int count, String passwd);
    public String read_area(int area, String str_addr
            , String str_count, String str_passwd);


    //把 content 中的数据写到标签 area 区中 addr（以 word 计算）开始的位 置。
    public int write_area(int area, int addr, String passwd, byte[] content);
    public int write_area(int area, String addr, String pwd, String count, String content);


    //选中要进行操作的 epc 标签
    public int select_card(int bank,byte[] epc,boolean mFlag);
    public int select_card(int bank,String epc,boolean mFlag);


    //设置天线功率
    public int set_antenna_power(int power);

    //读取当前天线功率值
    public int get_antenna_power();

    //设置频率区域
    public int set_freq_region(int region);

    public int get_freq_region();

    //设置盘点的handler
    public void reg_handler(Handler hd);

    public int setlock(int type, int area, int passwd);
    //拿到最近一次详细内部错误信息
    public String GetLastDetailError();

    public int SetInvMode(int invm, int addr, int length);
    public int GetInvMode(int type);

    //设置频点
    public int setFrequency(double frequency);
    //载波测试接口
    public  int enableEngTest(int gain);
}
