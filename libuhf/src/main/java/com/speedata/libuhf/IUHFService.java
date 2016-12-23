package com.speedata.libuhf;


import android.os.Handler;

/**
 * Created by brxu on 2016/12/13.
 */

public interface IUHFService {

    public static final int REGION_CHINA_840_845 = 0;
    public static final int REGION_CHINA_920_925 = 1;
    public static final int REGION_CHINA_902_928 = 2;
    public static final int REGION_EURO_865_868 = 3;
    public static final int RESERVED_A = 0;
    public static final int EPC_A = 1;
    public static final int TID_A = 2;
    public static final int USER_A = 3;
    public static final int FAST_MODE = 0;
    public static final int SMART_MODE = 1;
    public static final int LOW_POWER_MODE = 2;
    public static final int USER_MODE = 3;

    //默认参数初始化模块
    public int OpenDev();

    //释放模块
    public void CloseDev();

    //开始盘点
    public void inventory_start();

    // Handler用于处理返回的盘点数据
    public void inventory_start(Handler hd);

     //停止盘点
    public void inventory_stop();

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
    public byte[] read_area(int area, int addr, int count, int passwd);

    //把 content 中的数据写到标签 area 区中 addr（以 word 计算）开始的位 置。
    public int write_area(int area, int addr, int passwd, byte[] content);

    //选中要进行操作的 epc 标签
    public int select_card(byte[] epc);

    //设置天线功率
    public int set_antenna_power(int power);

    //读取当前天线功率值
    public int get_antenna_power();

    //设置频率区域
    public int set_freq_region(int region);

    public int get_freq_region();

    //设置盘点的handler
    public void reg_handler(Handler hd);


    public INV_TIME get_inventory_time();
    public int set_inventory_time(int work_t, int rest_t);
    public int MakeSetValid();
    public int setlock(int type, int area, int passwd);
    public int get_inventory_mode();
    public int set_inventory_mode(int m);
}
