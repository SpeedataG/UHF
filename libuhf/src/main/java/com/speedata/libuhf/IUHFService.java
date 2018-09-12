package com.speedata.libuhf;


import android.os.Handler;

import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;
import com.uhf.structures.SelectCriteria;

/**
 * Created by brxu on 2016/12/13.
 */

public interface IUHFService {

    public static final int REGION_CHINA_840_845 = 0;
    public static final int REGION_CHINA_920_925 = 1;
    public static final int REGION_CHINA_902_928 = 2;
    public static final int REGION_EURO_865_868 = 3;
//    public static final int REGION_920_5_924_5 = 3;

    public static final int RESERVED_A = 0;
    public static final int EPC_A = 1;
    public static final int TID_A = 2;
    public static final int USER_A = 3;
    public static final int FAST_MODE = 0;
    public static final int SMART_MODE = 1;
    public static final int LOW_POWER_MODE = 2;
    public static final int USER_MODE = 3;
    public static final String SERIALPORT = "/dev/ttyMT2";
    public static final String SERIALPORT_SD60 = "/dev/ttyMT0";
    public static final String POWERCTL = "/sys/class/misc/mtgpio/pin";

    //*************************************************共用接口*********************************************************

    //默认参数初始化模块上电
    public int openDev();

    //释放模块下电
    public void closeDev();

    //*************************************************新版接口*********************************************************

    /**
     * 设置盘点数据监听
     *
     * @param onSpdInventoryListener
     */
    public void setOnInventoryListener(OnSpdInventoryListener onSpdInventoryListener);

    /**
     * 开始盘点
     */
    public void inventoryStart();

    /**
     * 停止盘点
     */
    public void inventoryStop();

    /**
     * 设置读数据监听
     *
     * @param onSpdReadListener
     */
    public void setOnReadListener(OnSpdReadListener onSpdReadListener);

    /**
     * 读卡
     *
     * @param area   区域
     * @param addr   起始地址
     * @param count  长度byte
     * @param passwd 密码
     * @return 0成功-1失败
     */
    public int readArea(int area, int addr, int count, String passwd);

    /**
     * 设置写数据监听
     *
     * @param onSpdWriteListener
     */
    public void setOnWriteListener(OnSpdWriteListener onSpdWriteListener);

    /**
     * 写卡
     *
     * @param area    区域
     * @param addr    起始地址
     * @param passwd  密码
     * @param content 内容
     * @return
     */
    public int writeArea(int area, int addr, int count, String passwd, byte[] content);

    /**
     * 设置密码
     *
     * @param which    区域
     * @param cur_pass 原密码
     * @param new_pass 新密码
     * @return 是否成功通过setOnWriteListener监听回调所得
     */
    public int setPassword(int which, String cur_pass, String new_pass);

    /**
     * 锁卡
     *
     * @param type
     * @param area
     * @param passwd
     * @return
     */
    public int setLock(int type, int area, String passwd);


    public int setQueryTagGroup(int selected, int session, int target);

    public int getQueryTagGroup();


    /**
     * 掩码
     *
     * @param area    区域
     * @param addr    起始地址 bit
     * @param length  长度 bit
     * @param content 掩码内容
     * @return
     */
    public int mask(int area, int addr, int length, byte[] content);

    /**
     * 取消选卡
     *
     * @return
     */
    public int cancelMask();

    /**
     * 获取掩码信息
     *
     * @return
     */
    public SelectCriteria getMask();

    //********************************************老版接口（不再维护）***************************************************


    //开始盘点
    public void inventory_start();

    // Handler用于处理返回的盘点数据
    public void inventory_start(Handler hd);

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
    public int write_area(int area, int addr, int count, String passwd, byte[] content);

    public int write_area(int area, String addr, String pwd, String count, String content);


    //选中要进行操作的 epc 标签
    public int selectCard(int bank, byte[] epc, boolean mFlag);

    public int selectCard(int bank, String epc, boolean mFlag);


    //设置天线功率
    public int setAntennaPower(int power);

    //读取当前天线功率值
    public int getAntennaPower();

    //设置频率区域
    public int setFreqRegion(int region);

    public int getFreqRegion();

    //设置盘点的handler
    public void reg_handler(Handler hd);

    public int setlock(int type, int area, String passwd);

    //拿到最近一次详细内部错误信息
    public String GetLastDetailError();

    public int setInvMode(int invm, int addr, int length);

    public int getInvMode(int type);

    //设置频点
    public int setFrequency(double frequency);

    //载波测试接口
    public int enableEngTest(int gain);
}
