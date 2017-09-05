package com.speedata.libuhf.utils;

/**
 * Description：
 * author：lei
 * date：2017/8/7 下午3:09
 */

public class StringUtils {

    public static String byteToHexString(byte[] b, int length) {
        String ret = "";
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

    public static String charToHexString(char[] data, int length) {
        byte[] bytes = new byte[2 * length];
        int i;
        for (i = 0; i < length; i++) {
            bytes[2 * i] = (byte) (data[i] >> 8);
            bytes[2 * i + 1] = (byte) (data[i]);
        }
        return byteToHexString(bytes, 2 * length);
    }

    public static String stringToHexString(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = (int) s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }

    public static char[] stringToChar(String value) {
        char[] WriteText = new char[(value.length() / 4)];
        byte[] btemp = new byte[(value.length())];

        for (int i = 0; i < btemp.length; i++) {
            btemp[i] = Byte.parseByte(value.substring(i, i + 1), 16);
        }
        for (int i = 0; i < WriteText.length; i++) {
            WriteText[i] = (char) (((btemp[i * 4 + 0] & 0x0f) << 12)
                    | ((btemp[i * 4 + 1] & 0x0f) << 8)
                    | ((btemp[i * 4 + 2] & 0x0f) << 4)
                    | (btemp[i * 4 + 3] & 0x0f));
        }
        return WriteText;
    }


    /**
     * 16进制的字符串表示转成字节数组 sl
     *
     * @param hexString 16进制格式的字符串
     * @return 转换后的字节数组
     **/
    public static byte[] stringToByte(String hexString) {
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
