package com.speedata.libid2;

import android.content.Context;
import android.serialport.DeviceControl;

import java.io.IOException;

/**
 * Created by brxu on 2016/12/15.
 */

public interface IID2Service {
    public static final int power_type_mian = 0;
    public static final int power_type_expand = 1;



    /**
     * @param callBack
     * @param serialport 串口
     * @param braut      波特率
     * @param power_type 上电类型
     * @param gpio       上电GPIO
     */
    public void initDev(Context mContext, IDReadCallBack callBack, String serialport, int braut,
                        DeviceControl.PowerType power_type,
                        int... gpio) throws IOException;

    public void releaseDev() throws IOException;

    public int searchCard();

    public int selectCard();

    /**
     * @param isNeedFingerprinter 是否需要指纹
     * @return 返回身份信息实体
     */
    public IDInfor readCard(boolean isNeedFingerprinter);

    public IDInfor getIDInfor(boolean isNeedFingerprinter);

//    public void startCirculationRead(int interval);
    public String parseReturnState(int state);
}
