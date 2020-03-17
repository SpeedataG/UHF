package com.speedata.libuhf;


import android.os.Handler;

import com.speedata.libuhf.bean.DynamicQParams;
import com.speedata.libuhf.bean.FixedQParams;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;

//import com.uhf.structures.DynamicQParams;
//import com.uhf.structures.FixedQParams;
import com.uhf.structures.InventoryData;
import com.uhf.structures.KrReadData;
import com.uhf.structures.RW_Params;
import com.uhf.structures.SelectCriteria;

/**
 * @author brxu
 * @date 2016/12/13
 */

public interface IUHFService {

    int REGION_CHINA_840_845 = 0;
    int REGION_CHINA_920_925 = 1;
    int REGION_CHINA_902_928 = 2;
    int REGION_EURO_865_868 = 3;
//    public static final int REGION_920_5_924_5 = 3;

    int RESERVED_A = 0;
    int EPC_A = 1;
    int TID_A = 2;
    int USER_A = 3;
    int FAST_MODE = 0;
    int SMART_MODE = 1;
    int LOW_POWER_MODE = 2;
    int USER_MODE = 3;
    String SERIALPORT0 = "/dev/ttyMT0";
    String SERIALPORT1 = "/dev/ttyMT1";
    String SERIALPORT = "/dev/ttyMT2";
    String SERIALPORT_SD100 = "/dev/ttyHSL2";
    String SERIALPORT_SD60 = "/dev/ttyMT0";
    String SERIALPORT7 = "/dev/ttyMT7";
    String SERIALPORT7_9600 = "/dev/ttyMT7:9600";
    String POWERCTL = "/sys/class/misc/mtgpio/pin";

    //*************************************************共用接口*********************************************************

    /**
     * 默认参数初始化模块上电
     *
     * @return
     */
    int openDev();

    /**
     * 释放模块下电
     */
    void closeDev();

    //*************************************************新版接口*********************************************************

    /**
     * 设置盘点数据监听
     *
     * @param onSpdInventoryListener
     */
    void setOnInventoryListener(OnSpdInventoryListener onSpdInventoryListener);

    /**
     * 开始盘点
     */
    void inventoryStart();

    /**
     * 停止盘点
     */
    void inventoryStop();

    /**
     * 设置读数据监听
     *
     * @param onSpdReadListener
     */
    void setOnReadListener(OnSpdReadListener onSpdReadListener);

    /**
     * 读卡
     *
     * @param area   区域
     * @param addr   起始地址
     * @param count  长度byte
     * @param passwd 密码
     * @return 0成功-1失败
     */
    int readArea(int area, int addr, int count, String passwd);

    /**
     * 设置写数据监听
     *
     * @param onSpdWriteListener
     */
    void setOnWriteListener(OnSpdWriteListener onSpdWriteListener);

    /**
     * 写卡
     *
     * @param area    区域
     * @param addr    起始地址
     * @param count   块数
     * @param passwd  密码
     * @param content 内容
     * @return 返回状态码
     */
    int writeArea(int area, int addr, int count, String passwd, byte[] content);

    /**
     * 设置密码
     *
     * @param which    区域
     * @param cur_pass 原密码
     * @param new_pass 新密码
     * @return 是否成功通过setOnWriteListener监听回调所得
     */
    int setPassword(int which, String cur_pass, String new_pass);

    /**
     * 锁卡
     *
     * @param type   权限类型
     * @param area   q区域
     * @param passwd 密码
     * @return
     */
    int setLock(int type, int area, String passwd);

    /**
     * 销毁标签
     *
     * @param accessPassword 访问密码
     * @param killPassword   销毁密码
     * @return 0成功
     */
    int setKill(String accessPassword, String killPassword);

    /**
     * 设置通话项
     *
     * @param selected 默认0
     * @param session  0-s0 1-s1 2-s2 3-s3
     * @param target   默认0
     * @return 返回0成功 -1失败
     */
    int setQueryTagGroup(int selected, int session, int target);

    /**
     * 获取通话项
     *
     * @return -
     */
    int getQueryTagGroup();

    /**
     * 选中要操作的标签（掩码操作）
     *
     * @param bank  -掩码区域
     * @param epc   -掩码内容
     * @param mFlag true-掩码  false-取消掩码
     * @return 0成功
     */
    int selectCard(int bank, byte[] epc, boolean mFlag);

    int selectCard(int bank, String epc, boolean mFlag);

    /**
     * 设置天线功率
     *
     * @param power -功率
     * @return 0成功 -1失败
     */
    int setAntennaPower(int power);

    /**
     * 读取当前天线功率值
     *
     * @return -1失败   成功 返回功率值
     */
    int getAntennaPower();

    /**
     * 设置频率区域
     *
     * @param region -
     * @return 0成功 -1失败
     */
    int setFreqRegion(int region);

    /**
     * 获取频率区域
     *
     * @return -1失败
     */
    int getFreqRegion();

    /**
     * 设置盘点模式（附加数据）
     *
     * @param invm   附加数据区
     * @param addr   起始地址
     * @param length 长度
     * @return 0成功
     */
    int setInvMode(int invm, int addr, int length);

    /**
     * 获取盘点模式
     *
     * @param type 0-附加区，1-起始地址，2-长度
     * @return -1失败 非-1成功
     */
    int getInvMode(int type);

    /**
     * 设置新的epc
     *
     * @param password 密码
     * @param len      新的epc长度
     * @param epc      新的epc
     * @return
     */
    int setNewEpc(String password, int len, byte[] epc);

    /**
     * 设置盘点模式
     *
     * @param mode 1-高性能盘点 2-低功耗盘点
     * @return 0成功 -1失败
     */
    int switchInvMode(int mode);

    /**
     * 设置低功耗时间调度计划
     *
     * @param invOnTime  持续工作时间
     * @param invOffTime 休息时间
     * @return -0成功 非0失败
     */
    int setLowpowerScheduler(int invOnTime, int invOffTime);

    /**
     * 获取调度计划
     *
     * @return -null失败
     * int[0] 持续时间
     * int[1] 休息时间
     */
    int[] getLowpowerScheduler();

    //***** 飞利信接口 *****************************************************************//

    /**
     * 设置驻留时间
     *
     * @param dwellTime 驻留时间
     * @return - 0成功
     */
    int setDwellTime(int dwellTime);

    /**
     * 获取驻留时间
     *
     * @return -1失败
     */
    int getDwellTime();

    /**
     * U8标签掩码
     *
     * @param area    区域
     * @param addr    起始地址 bit
     * @param length  长度 bit
     * @param content 掩码内容
     * @return
     */
    int mask(int area, int addr, int length, byte[] content);

    /**
     * 取消选卡
     *
     * @return -
     */
    int cancelMask();

    /**
     * 获取掩码信息
     *
     * @return -
     */
    SelectCriteria getMask();

    /**
     * 设置4QT标签的模式切换
     *
     * @param memMap:0       私有区域，1 公有区域
     * @param maskFlag       0-----禁用掩码，2----启用掩码
     * @param accessPassword 访问密码
     * @return 0--成功 非零--失败
     */
    int setMonzaQtTagMode(int memMap, int maskFlag, byte[] accessPassword);

    /**
     * @param memMap  0私有区域，1 公有区域
     * @param pwd     访问密码
     * @param bank    区域
     * @param address 起始地址
     * @param length  读取长度
     * @return 0--成功 非0失败
     */
    int readMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length);

    /**
     * @param memMap    0私有区域，1 公有区域
     * @param pwd       访问密码
     * @param bank      区域
     * @param address   起始地址
     * @param length    读取长度
     * @param timeOutMs 超时时间
     * @param rw_params 读取的信息
     * @return 0--成功 非0失败
     */
    int readMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, int timeOutMs, RW_Params rw_params);

    /**
     * @param memMap    0私有区域，1 公有区域
     * @param pwd       访问密码
     * @param bank      区域
     * @param address   起始地址
     * @param length    长度
     * @param writeData 写入的数据
     * @return 0--成功 非0失败
     */
    int writeMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData);

    /**
     * @param memMap    0私有区域，1 公有区域
     * @param pwd       访问密码
     * @param bank      区域
     * @param address   起始地址
     * @param length    长度
     * @param writeData 写入的数据
     * @param timeOutMs 超时时间
     * @param rw_params 信息
     * @return 0--成功 非0失败
     */
    int writeMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData, int timeOutMs, RW_Params rw_params);

    /**
     * @param startQ    起始Q值
     * @param minQ      最小Q值 0～15
     * @param maxQ      最大Q值 0～15
     * @param tryCount  尝试次数
     * @param target    是否翻转 0启用 1禁止
     * @param threshold 阀值 0~255
     * @return <p> 0设置成功
     * -1 startQ  minQ  maxQ 取值范围不对
     * -2 Q值范围设置错误 最小不能大于最大
     * -3 不在阀值0～255的取值范围内，无效阀值
     * -4 不在重试次数0～10的取值范围内，无效重试次数
     * 其他值 查询厂商错误码</p>
     */
    int setDynamicAlgorithm(int startQ, int minQ, int maxQ, int tryCount, int target, int threshold);

    /**
     * @param qValue   起始Q值  0～15
     * @param tryCount 尝试次数
     * @param target   是否翻转 0启用 1禁止
     * @param repeat   是否重复 0启用 1禁止
     * @return 0设置成功
     * -1 qValue取值范围不对
     * -4 不在重试次数0～10的取值范围内，无效重试次数
     * 其他值 查询厂商错误码
     */
    int setFixedAlgorithm(int qValue, int tryCount, int target, int repeat);


    int getDynamicAlgorithm(DynamicQParams dynamicQParams);

    int getFixedAlgorithm(FixedQParams fixedQParams);

    //设置频点
    int setFrequency(double frequency);

    //载波测试接口
    int enableEngTest(int gain);

    //***** 芯联接口 ******************************************************************//

    /**
     * @param rpaswd      密码 16进制byte
     * @param cmdType     命令类型 0为读 1为写
     * @param memType     内存视图 0为私密 1为公共
     * @param persistType 识别距离 0为远场 1为近场
     * @param rangeType   状态类型 0为临时 1为永久
     * @return
     */
    int setQT(byte[] rpaswd, int cmdType, int memType, int persistType, int rangeType);

    int setGen2QValue(int qValue);

    int setGen2Target(int target);

    int[] getGen2AllValue();

    int setGen2WriteMode(int wMode);

    int setGen2Blf(int blf);

    int setGen2MaxLen(int maxLen);

    int setGen2Code(int code);

    int setGen2Tari(int tari);

    int setNxpu8(int mode);

    //***** 一芯接口 ******************************************************************//

    /**
     * 一芯寻标签过滤
     * 取消过滤 ads=0 len=0
     *
     * @param bank 区域
     * @param ads  起始地址
     * @param len  长度
     * @param data 数据 epc
     * @param save 是否要掉电保存 	True：要；False：不要
     * @return 0成功 失败 -1
     */
    int yixinFilterEpc(int bank, int ads, int len, String data, Boolean save);


    //*****************坤瑞sm7认证接口************

    /**
     * 坤锐sm7盘点
     *
     * @param inventoryData 判断数据
     * @return 默认 返回-1
     */
    int krSm7Inventory(InventoryData inventoryData);


    /**
     * 坤锐sm7blocwrite
     *
     * @param length  长度
     * @param addr    起始地址
     * @param area    要操作的区
     * @param pwd     标签密码
     * @param content 写入的内容
     * @return 转态码      return -1;
     */
    int krSm7Blockwrite(int length, int addr, int area, byte[] pwd, byte[] content);

    /**
     * 坤锐正常写
     *
     * @param length  长度
     * @param addr    起始地址
     * @param area    要操作的区
     * @param pwd     标签密码
     * @param content 写入的内容
     * @return 默认返回-1
     */
    int krSm7Write(int length, int addr, int area, byte[] pwd, byte[] content);

    /**
     * 坤锐读取
     *
     * @param length    长度
     * @param addr      起始地址
     * @param area      要操作的区
     * @param pwd       标签密码
     * @param krSm7Data 读取内容
     * @return 默认返回-1
     */
    int krSm7Read(int length, int addr, int area, byte[] pwd, KrReadData krSm7Data);

    /**
     * 坤锐关闭
     *
     * @return 默认返回-1
     */
    int krSm7End();


    //********************************************老版接口（不再维护）***************************************************

    void inventory_start();

    void inventory_start(Handler hd);

    int inventory_stop();

    /**
     * @param area
     * @param addr
     * @param count
     * @param passwd
     * @return
     * @deprecated Please use {@link #readArea(int, int, int, String)} instead.
     */
    byte[] read_area(int area, int addr, int count, String passwd);

    String read_area(int area, String str_addr, String str_count, String str_passwd);

    int write_area(int area, int addr, int count, String passwd, byte[] content);

    int write_area(int area, String addr, String pwd, String count, String content);

    void reg_handler(Handler hd);

    int setlock(int type, int area, String passwd);

    int set_Password(int which, String cur_pass, String new_pass);

    String GetLastDetailError();

    int setDynamicAlgorithm();

}
