package com.speedata.libuhf.utils;

import android.util.Log;

/**
 * Created by 张明_ on 2017/6/26.
 */

public class ByteCharStrUtils {
    public static String b2hexs(byte[] b, int length) {
        String ret = "";

        for (int i = 0; i < length; ++i) {
            String hex = Integer.toHexString(b[i] & 255);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            ret = ret + hex.toUpperCase();
        }

        return ret;
    }

    public static String c2hexs(char[] data, int length) {
        byte[] bytes = new byte[2 * length];

        for (int i = 0; i < length; ++i) {
            bytes[2 * i] = (byte) (data[i] >> 8);
            bytes[2 * i + 1] = (byte) data[i];
        }

        return b2hexs(bytes, 2 * length);
    }

    public static String toHexString(String s) {
        String str = "";

        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }

        return str;
    }

    public static char[] s2char(String value) {
        char[] WriteText = new char[value.length() / 4];
        byte[] btemp = new byte[value.length()];

        int i;
        for (i = 0; i < btemp.length; ++i) {
            btemp[i] = Byte.parseByte(value.substring(i, i + 1), 16);
        }

        for (i = 0; i < WriteText.length; ++i) {
            WriteText[i] = (char) ((btemp[i * 4 + 0] & 15) << 12 | (btemp[i * 4 + 1] & 15) << 8 | (btemp[i * 4 + 2] & 15) << 4 | btemp[i * 4 + 3] & 15);
        }

        return WriteText;
    }

    /**
     * 把16进制字符串转换成字节数组
     */
    public static byte[] toByteArray(String hex) {
        if (hex == null) {
            return null;
        }
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }

        Log.i("lei", "----bytes--------" + result[0]);
        Log.i("lei", "----bytes--------" + result[1]);

        return result;

    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /**
     * 判断十六进制
     */
    public static boolean IsHex(String str) {
        boolean b = false;

        char[] c = str.toUpperCase().toCharArray();
        for (char aC : c) {
            if ((aC >= '0' && aC <= '9') || (aC >= 'A' && aC <= 'F')) {
                b = true;
            } else {
                b = false;
                break;
            }
        }
        return b;
    }
}
