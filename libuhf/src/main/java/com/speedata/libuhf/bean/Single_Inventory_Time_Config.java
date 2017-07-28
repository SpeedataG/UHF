package com.speedata.libuhf.bean;

/**
 * Created by 张明_ on 2016/12/16.
 */

public class Single_Inventory_Time_Config {
    public int iWorkTime;
    public int iRestTime;

    public Single_Inventory_Time_Config() {
    }

    public Single_Inventory_Time_Config(int iWorkTime, int iRestTime) {
        this.iWorkTime = iWorkTime;
        this.iRestTime = iRestTime;
    }

    public void setValue(int iWorkTime, int iRestTime) {
        this.iWorkTime = iWorkTime;
        this.iRestTime = iRestTime;
    }
}
