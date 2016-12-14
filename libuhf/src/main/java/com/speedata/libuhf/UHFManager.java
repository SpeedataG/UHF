package com.speedata.libuhf;

/**
 * Created by brxu on 2016/12/13.
 */

public class UHFManager {
    private static IUHFService iuhfService;
    //飞利信读取制造商指令
    private byte[] feilixin_cmd = {(byte) 0xbb, 0x00, 0x03, 0x03, 0x06, 0x7e};

    private final static String FACTORY_FEILIXIN = "1";
    private final static String FACTORY_XINLIAN = "2";
    private final static String FACTORY_R2000 = "3";

    public static IUHFService getUHFService() {
        //  判断模块   返回不同的模块接口对象
        if (iuhfService == null) {
            if (!judgeModle())
                return null;
        }
        return iuhfService;
    }

    private static boolean judgeModle() {
        String factory = getModle();
        boolean initResult = true;
        switch (factory) {
            case FACTORY_FEILIXIN:
                break;
            case FACTORY_XINLIAN:
                iuhfService = new XinLianQilian();
                break;
            case FACTORY_R2000:
                iuhfService = new R2K();
                break;
            default:
                initResult = false;
                break;
        }
        return initResult;
    }

    /**
     * @return 返回厂家信息
     */
    private static String getModle() {
        String factory = "2";
        return factory;
    }
}
