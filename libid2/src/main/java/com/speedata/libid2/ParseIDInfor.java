package com.speedata.libid2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.cvr.device.CVRApi;
import com.speedata.libid2.utils.DataConversionUtils;
import com.speedata.libid2.utils.MyLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.speedata.libid2.Nation.codeAndMinzu;
import static com.speedata.libid2.utils.DataConversionUtils.byteArrayToAscii;

/**
 * Created by brxu on 2016/12/20.
 */

public class ParseIDInfor {

    private Context mContext;
    private IDReadCallBack callBack;

    public ParseIDInfor(IDReadCallBack callBack, Context mContext) {
        this.mContext = mContext;
        this.callBack = callBack;
    }

    private static MyLogger logger = MyLogger.jLog();
    public static final int STATUE_ERROR_HEAD = 1;
    public static final int STATUE_ERROR_CHECK = 2;
    public static final int STATUE_ERROR_LEN = 3;
    public static final int STATUE_ERROR_SELECT = 4;
    public static final int STATUE_OK = 5;
    public static final int STATUE_OK_SEARCH = 6;
    public static final int STATUE_ERROR_SEARCH = 7;
//    public static final int STATUE_READ_OK = 0;

    public static final int SELECT_CARD_OK = 8;//读卡成功
    public static final int READ_CARD_FAILED = 9;//读卡失败
    public static final int READ_CARD_NOT_SPOT = 10;//卡片类型不支持
    public static final int STATUE_READ_NULL = 11;//串口数据为null
    public static final int STATUE_UNSUPPORTEDENCODINGEXCEPTION = 12;//编码格式异常


    private static final int swtmp1[] = {0x00, 0x00, 0x90}; //操作成功
    private static final int swtmp2[] = {0x00, 0x00, 0x9F}; //寻找证/卡成功
    private static final int swtmp3[] = {0x00, 0x00, 0x80}; //寻找证/卡失败
    private static final int swtmp4[] = {0x00, 0x00, 0x81}; //选取证/卡失败
    private static final int swtmp5[] = {0x00, 0x00, 0x41}; //读证/卡操作失败
    private static final int swtmp6[] = {0x00, 0x00, 0x40}; //无法识别的卡类型

    public boolean isGet = false;

    public IDInfor parseIDInforWithFinger(byte[] revbuf) {
        IDInfor idInfor = new IDInfor();
        byte[] data_len = new byte[2];
        System.arraycopy(revbuf, 10, data_len, 0, 2);
        byte[] pic_len = new byte[2];
        System.arraycopy(revbuf, 5, pic_len, 0, 2);
        byte[] finger_len = new byte[2];
        System.arraycopy(revbuf, 7, finger_len, 0, 2);
        int dataLen = DataConversionUtils.byteArrayToInt(data_len);
        int picLen = DataConversionUtils.byteArrayToInt(pic_len);
        int fingerLen = DataConversionUtils.byteArrayToInt(finger_len);
        byte[] infor = new byte[dataLen];
        byte[] pic = new byte[picLen];
        byte[] finger = new byte[fingerLen];
//        System.arraycopy(revbuf);
        return idInfor;
    }

    public IDInfor parseIDInfor(byte[] revbuf, boolean isNeedFinger) {
        IDInfor idInfor = new IDInfor();
        idInfor.setSuccess(true);
        byte[] idname = new byte[30];
        byte[] idsex = new byte[2];
        byte[] idminzu = new byte[4];
        byte[] idyear = new byte[8];
        byte[] idmouth = new byte[4];
        byte[] idday = new byte[4];
        byte[] idaddr = new byte[70];
        byte[] idnum = new byte[36];
        byte[] idqianfa = new byte[30];
        byte[] idqishiyear = new byte[8];
        byte[] idjiezhiyear = new byte[8];
        byte[] idqishimouth = new byte[4];
        byte[] idjiezhimouth = new byte[4];
        byte[] idqishiday = new byte[4];
        byte[] idjiezhiday = new byte[4];
        byte[] idjiezhiall = new byte[16];
        byte[] idimg = new byte[1024];
        byte[] finger = new byte[1024];
        int start_parse;
        if (isNeedFinger) {
            start_parse = 14 + 2;
            byte[] finger_len = new byte[2];
            System.arraycopy(revbuf, 14, finger_len, 0, 2);
            int fingerLen = DataConversionUtils.byteArrayToInt(finger_len);
            if (fingerLen > 0 && revbuf.length >= start_parse + 255 + 2048) {
                System.arraycopy(revbuf, start_parse + 255 + 1024, finger, 0, 1024);
                idInfor.setWithFinger(true);
                idInfor.setFingerprStringer(finger);
            } else {
                logger.e("no finger");
            }
        } else {
            start_parse = 14;
        }
        System.arraycopy(revbuf, start_parse, idname, 0, 30);
        System.arraycopy(revbuf, start_parse + 30, idsex, 0, 2);
        System.arraycopy(revbuf, start_parse + 32, idminzu, 0, 4);
        System.arraycopy(revbuf, start_parse + 36, idyear, 0, 8);
        System.arraycopy(revbuf, start_parse + 44, idmouth, 0, 4);
        System.arraycopy(revbuf, start_parse + 48, idday, 0, 4);
        System.arraycopy(revbuf, start_parse + 52, idaddr, 0, 70);
        System.arraycopy(revbuf, start_parse + 52 + 70, idnum, 0, 36);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36, idqianfa, 0, 30);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30, idqishiyear, 0, 8);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30 + 8, idjiezhiyear, 0, 8);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30 + 8 + 8, idqishimouth, 0, 4);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30 + 8 + 8 + 4, idjiezhimouth, 0, 4);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30 + 8 + 8 + 4 + 4, idqishiday, 0, 4);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30 + 8 + 8 + 4 + 4 + 4,
                idjiezhiday, 0, 4);
        System.arraycopy(revbuf, start_parse + 52 + 70 + 36 + 30 + 8 + 8 + 4 + 4 + 4 + 4,
                idjiezhiall, 0, 16);
        byte a[] = byteArrayToAscii(idsex).getBytes();
        if (btoi(a[0]) == '0') {
            idInfor.setSex("未知");
        } else if (btoi(a[0]) == '1') {
            idInfor.setSex("男");
        } else if (btoi(a[0]) == '2') {
            idInfor.setSex("女");
        } else if (btoi(a[0]) == '9') {
            idInfor.setSex("未说明");
        }

        try {
            String mssg = new String(idname, "UTF-16LE");
            idInfor.setName(mssg.substring(0, 4));
        } catch (UnsupportedEncodingException e) {
            idInfor.setName("EncodingException");
            e.printStackTrace();
        }

        byte b[] = new byte[2];
        byte c[] = byteArrayToAscii(idminzu).getBytes();
        b[0] = c[0];
        b[1] = c[2];
        for (int i = 0; i < 6; i++) {
            if (byteArrayToAscii(b).substring(0, 2).equals
                    (codeAndMinzu[i][0])) {
                idInfor.setNation(codeAndMinzu[i][1]);
                break;
            }
        }
        try {
            String mssgA = new String(idaddr, "UTF-16LE");
            idInfor.setAddress(mssgA);
        } catch (UnsupportedEncodingException e) {
            idInfor.setAddress("EncodingException");
            e.printStackTrace();
        }

        try {
            String mssgb = new String(idqianfa, "UTF-16LE");
            idInfor.setQianFa(mssgb);
        } catch (UnsupportedEncodingException e) {
            idInfor.setQianFa("EncodingException");
            e.printStackTrace();
        }
        idInfor.setYear(byteArrayToAscii(idyear));
        idInfor.setMonth(byteArrayToAscii(idmouth));
        idInfor.setNum(byteArrayToAscii(idnum));
        if (idjiezhiyear[0] >= '0' && idjiezhiyear[0] <= '9') {
            idInfor.setStartYear(byteArrayToAscii(idqishiyear));
            idInfor.setStartMonth(byteArrayToAscii(idqishimouth));
            idInfor.setStartDay(byteArrayToAscii(idqishiday));
            idInfor.setEndYear(byteArrayToAscii(idjiezhiyear));
            idInfor.setEndMonth(byteArrayToAscii(idjiezhimouth));
            idInfor.setEndDay(byteArrayToAscii(idjiezhiday));
            idInfor.setDeadLine(byteArrayToAscii(idqishiyear) + "." +
                    byteArrayToAscii(idqishimouth) + "." +
                    byteArrayToAscii(idqishiday) + "-" + byteArrayToAscii
                    (idjiezhiyear) + "." + byteArrayToAscii(idjiezhimouth)
                    + "." + byteArrayToAscii(idjiezhiday));
        } else {
            try {
                String sjiezhi = new String(idjiezhiall, "UTF-16LE");
                idInfor.setDeadLine(byteArrayToAscii(idqishiyear) + "." +
                        byteArrayToAscii(idqishimouth) + "." +
                        byteArrayToAscii(idqishiday) + "-" + sjiezhi);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.arraycopy(revbuf, 256 + start_parse, idimg, 0, 1024);
        byte[] bmp = new byte[14 + 40 + 308 * 126];
        //判断SD卡中是否有解码库文件 不存在需要cp过去
        if (!IDFileUtils.isExit()) {
            IDFileUtils.copyfile("/sdcard/wltlib", "base.dat", R.raw.base, mContext);
            IDFileUtils.copyfile("/sdcard/wltlib", "license.lic", R.raw.license, mContext);
        }
        //非主线程  需要执行prepare
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            Looper.prepare();
//        }
        CVRApi api;// = new CVRApi(new Handler());
        try {
            api = new CVRApi(new Handler());
        } catch (Exception e) {
            Looper.prepare();
            api = new CVRApi(new Handler());
        }
        //授权目录
        String absolutePath = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/wltlib";

        int ret = api.Unpack(absolutePath, idimg, bmp);// 照片解码
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(absolutePath
                    + "/zp.bmp");
            Bitmap bmps = BitmapFactory.decodeStream(fis);
            idInfor.setBmps(bmps);
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            IDFileUtils.copyfile("/sdcard/wltlib", "base.dat", R.raw.base, mContext);
            IDFileUtils.copyfile("/sdcard/wltlib", "license.lic", R.raw.license, mContext);
        }
//        currentStatue = STATUE_READ_OK;
//        callBack.callBack(idInfor);
//        logger.d("callback OK");
        return idInfor;
    }

    public int currentStatue;

    /**
     * /**
     * 校验指令
     *
     * @param buf len
     * @return 1.数据头错误    2.校验错误   3.长度错误   4.选取证/卡失败  5.操作成功 6.寻找证/卡成功  7.寻找证/卡失败
     */
    public int checkPackage(byte[] buf, int len, boolean isCheck) {

        int res = 100;
        byte[] xorbuf = new byte[len - 6];

        if ((btoi(buf[0]) != 0xaa) || (btoi(buf[1]) != 0xaa) || (btoi(buf[2]) != 0xaa) || (btoi
                (buf[3]) != 0x96) || (btoi(buf[4]) != 0x69)) {
            res = STATUE_ERROR_HEAD;
            return res;
        }
        logger.d("checkPackage len=" + buf.length);
        System.arraycopy(buf, 5, xorbuf, 0, len - 6);
        if (isCheck) {
            byte xorflag = xor(xorbuf, xorbuf.length);
            byte check = buf[len - 1];
            if (xorflag != check) {
                res = STATUE_ERROR_CHECK;
                logger.d("xorflag failed  =" + check + "  " + xorflag);
                return res;
            }
        }
        if (isCheck) {
            int datelen = (int) btoi(buf[5]);
            datelen <<= 8;
            datelen += btoi(buf[6]);
            if (datelen != (len - 7)) {
                res = STATUE_ERROR_LEN;
                return res;
            }
        }

        if (swtmp4[2] == btoi(buf[9])) {
            res = STATUE_ERROR_SELECT;
        } else if ((swtmp1[0] == btoi(buf[7])) && (swtmp1[1] == btoi(buf[8])) && (swtmp1[2] ==
                btoi(buf[9]))) {
            res = STATUE_OK;
        } else if ((swtmp2[0] == btoi(buf[7])) && (swtmp2[1] == btoi(buf[8])) && (swtmp2[2] ==
                btoi(buf[9]))) {
            res = STATUE_OK_SEARCH;
        } else if ((swtmp3[0] == btoi(buf[7])) && (swtmp3[1] == btoi(buf[8])) && (swtmp3[2] ==
                btoi(buf[9]))) {
            res = STATUE_ERROR_SEARCH;
        } else if ((swtmp4[0] == btoi(buf[7])) && (swtmp4[1] == btoi(buf[8])) && (swtmp4[2] ==
                btoi(buf[9]))) {
            res = READ_CARD_FAILED;

        } else if ((swtmp5[0] == btoi(buf[7])) && (swtmp5[1] == btoi(buf[8])) && (swtmp5[2] ==
                btoi(buf[9]))) {
            res = READ_CARD_NOT_SPOT;
        }

        //带指纹的字节要多
        if (len >= 1295) {
//            res = STATUE_READ_OK;
            res = SELECT_CARD_OK;
            logger.d("---read ok,goto parse");
//            isGet = true;
//            parseIDInfor(buf);
        }
        logger.d("---staues=" + res);
        currentStatue = res;
        return res;
    }

    private static int btoi(byte a) {
        return (a < 0 ? a + 256 : a);
    }

    /**
     * 异或指令
     *
     * @param buf
     * @return
     */
    private static byte xor(byte[] buf, int len) {
        int i;
        byte a, b;

        a = buf[0];
        for (i = 1; i < len; i++) {
            a = (byte) (a ^ buf[i]);
        }
        logger.d("xor =" + String.format("%02x", a) + "\n");
        return a;
    }
}
