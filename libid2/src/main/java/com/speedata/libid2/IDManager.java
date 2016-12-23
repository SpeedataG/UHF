package com.speedata.libid2;

/**
 * Created by brxu on 2016/12/15.
 * 二代证模块管理类
 */

public class IDManager {
    private static IID2Service iid2Service;

    public static IID2Service getInstance() {
        if (iid2Service == null) {
            iid2Service = new HuaXuID();
        }
        return iid2Service;
    }

}
