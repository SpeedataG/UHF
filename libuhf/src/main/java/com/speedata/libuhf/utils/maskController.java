package com.speedata.libuhf.utils;

import com.uhf.linkage.Linkage;
import com.uhf.structures.SingulationCriterion;

public class maskController
{
    private static String hexString = "0123456789ABCDEF";


    /*
         * 设置掩码
         */
    public static int SetMask(Linkage link, byte[] bytetemp, int length) {
        //刘工
        int status=0;
        int count = length * 4;
        SingulationCriterion pParms = new SingulationCriterion();
        pParms.countCriteria = 1;
        pParms.mask_count = count;
        pParms.mask_offset = 0;
        System.arraycopy(bytetemp, 0, pParms.mask_mask, 0, bytetemp.length);

        pParms.match = 1;
        status = link.Radio_SetPostMatchCriteria(pParms, 0);
        return status;
    }

    /**
     * 16进制的字符串表示转成字节数组 sl
     *
     * @param hexString 16进制格式的字符串
     * @return 转换后的字节数组
     **/
    public static byte[] toByteArray(String hexString) {
        hexString = hexString.toLowerCase();
        int length = hexString.length();
        if (length % 2 != 0) {
            length = length + 1;
        }
        final byte[] byteArray = new byte[length / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {// 因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte low;
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            if ((k + 1) == hexString.length()) {
                low = 0;
            } else {
                low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            }
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }


}
