package com.speedata.libuhf.interfaces;

import com.speedata.libuhf.bean.SpdInventoryData;

/**
 * @author 张明_
 * @date 2017/11/15
 */
public interface OnSpdInventoryListener {
    /**
     * 盘点成功
     *
     * @param var1 标签信息
     */
    void getInventoryData(SpdInventoryData var1);

    /**
     * 盘点状态
     *
     * @param status 状态码
     */
    void onInventoryStatus(int status);
}
