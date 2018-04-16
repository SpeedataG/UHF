/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android.serialport;

import android.util.Log;

import com.speedata.libuhf.utils.DataConversionUtils;
import com.speedata.libuhf.utils.MyLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class SerialPortBackup {

    public static final String TAG = "SerialPortNative";
    //ttyMT0
    public static final String SERIAL_TTYMT0 = "/dev/ttyMT0";
    //ttyMT1
    public static final String SERIAL_TTYMT1 = "/dev/ttyMT1";
    //ttyMT2
    public static final String SERIAL_TTYMT2 = "/dev/ttyMT2";
    //ttyMT3
    public static final String SERIAL_TTYMT3 = "/dev/ttyMT3";
    //ttyG0
    public static final String SERIAL_TTYG0 = "/dev/ttyG0";
    //ttyG1
    public static final String SERIAL_TTYG1 = "/dev/ttyG1";
    //ttyG2
    public static final String SERIAL_TTYG2 = "/dev/ttyG2";
    //ttyG3
    public static final String SERIAL_TTYG3 = "/dev/ttyG3";

    private MyLogger logger = MyLogger.jLog();
    /*
     * Do not remove or rename the field mFd: it is used by native method
     * close();
     */
    private int fdx = -1;
    private int writelen;
    private String str;

    public SerialPortBackup() {
    }

    /**
     *
     * @param dev 串口路径 如：SERIAL_TTYMT0
     * @param brd 波特率
     * @throws SecurityException
     * @throws IOException
     */
    public void OpenSerial(String dev, int brd) throws SecurityException,
            IOException {
        // int result = 0;
        fdx = openport(dev, brd, 8, 1, 0);
        if (fdx < 0) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
    }

    /**
     *
     * @param device 串口路径 如：SERIAL_TTYMT0
     * @param baudrate 波特率
     * @param databit 数据位 一般位8
     * @param stopbit 停止位
     * @param crc 校验方式
     * @throws SecurityException
     * @throws IOException
     */
    public void OpenSerial(String device, int baudrate, int databit,
                           int stopbit, int crc) throws SecurityException, IOException {
        System.out.println("open");
        fdx = openport(device, baudrate, databit, stopbit, crc);
        if (fdx < 0) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
    }

    /**
     * 获取文件句柄
     * @return int
     */
    public int getFd() {
        return fdx;
    }

    /**
     *
     * @param fd 文件句柄
     * @param str 写入数据
     * @return writelen 写入长度
     */
    public int WriteSerialByte(int fd, byte[] str) {
        clearPortBuf(fd);
        logger.d("--WriteSerialByte---"
                + DataConversionUtils.byteArrayToString(str));
        writelen = writeport(fd, str);
        if (writelen >= 0) {
            logger.d("write success");
        } else {
            logger.e("write failed");
        }
        return writelen;
    }


    //读串口 最大延时
    private int timeout = 100;

    /**
     * 最大阻塞延时为100ms
     * @param fd 文件句柄
     * @param len 读取的最大长度
     * @return 读取的数据 无数据返回null
     * @throws UnsupportedEncodingException
     */
    public byte[] ReadSerial(int fd, int len)
            throws UnsupportedEncodingException {
        byte[] tmp = null;
        tmp = readport(fd, len, timeout);
        int count = 0;
        while (tmp == null && count < 10) {
            tmp = readport(fd, len, timeout);
            count++;
        }
        if (tmp != null) {
            logger.d("read---" + DataConversionUtils.byteArrayToStringLog(tmp, tmp.length));
        } else {
            logger.d("read---null");
        }
        return tmp;
    }

    /**
     *
     * @param fd 文件句柄
     * @param len 读取的最大长度
     * @param delay 最大阻塞延时
     * @return  byte[]
     * @throws UnsupportedEncodingException
     */
    public byte[] ReadSerial(int fd, int len,int delay)
            throws UnsupportedEncodingException {
        byte[] tmp = null;
        tmp = readport(fd, len, delay);
        int count = 0;
        while (tmp == null && count < 10) {
            tmp = readport(fd, len, delay);
            count++;
        }
        if (tmp != null) {
            logger.d("read---" + DataConversionUtils.byteArrayToStringLog(tmp, tmp.length));
        } else {
            logger.d("read---null");
        }
        return tmp;
    }

    /**
     * 读串口
     * @param fd 文件句柄
     * @param len 最大读取长度
     * @param isClear 读取后是否清空串口
     * @return byte[] 读取到的数据
     * @throws UnsupportedEncodingException
     */
    public byte[] ReadSerial(int fd, int len, boolean isClear)
            throws UnsupportedEncodingException {
        byte[] tmp = null;
        tmp = readport(fd, len, timeout);
        int count = 0;
        while (tmp == null && count < 10) {
            tmp = readport(fd, len, timeout);
            count++;
        }
        if (tmp != null) {
            logger.d("read---" + DataConversionUtils.byteArrayToStringLog(tmp, tmp.length));
            if (isClear)
                clearPortBuf(fd);
        } else {
            logger.d("read---null");
        }
        /*
         * for(byte x : tmp) { Log.w("xxxx", String.format("0x%x", x)); }
		 */

        return tmp;
    }

    /**
     * 读串口返回String 编码格式为utf8／gbk 阻塞延时为50ms
     * @param fd 文件句柄
     * @param len 读取最大长度
     * @return String 读到的数据
     * @throws UnsupportedEncodingException
     */
    public String ReadSerialString(int fd, int len)
            throws UnsupportedEncodingException {
        byte[] tmp;
        tmp = readport(fd, len, 50);
        if (tmp == null) {
            return null;
        }
        if (isUTF8(tmp)) {
            str = new String(tmp, "utf8");
            Log.d(TAG, "is a utf8 string");
        } else {
            str = new String(tmp, "gbk");
            Log.d(TAG, "is a gbk string");
        }
        return str;
    }

    /**
     * 关闭串口
     * @param fd  文件句柄
     */
    public void CloseSerial(int fd) {
        closeport(fd);
    }

    private boolean isUTF8(byte[] sx) {
        Log.d(TAG, "begian to set codeset");
        for (int i = 0; i < sx.length; ) {
            if (sx[i] < 0) {
                if ((sx[i] >>> 5) == 0x7FFFFFE) {
                    if (((i + 1) < sx.length)
                            && ((sx[i + 1] >>> 6) == 0x3FFFFFE)) {
                        i = i + 2;
                    } else {
                        return false;
                    }
                } else if ((sx[i] >>> 4) == 0xFFFFFFE) {
                    if (((i + 2) < sx.length)
                            && ((sx[i + 1] >>> 6) == 0x3FFFFFE)
                            && ((sx[i + 2] >>> 6) == 0x3FFFFFE)) {
                        i = i + 3;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                i++;
            }
        }
        return true;
    }

    public void clearPortBuf(int fd) {
        logger.d("clearPortBuf---");
        clearportbuf(fd);
    }

    // JNI

    private native int openport(String port, int brd, int bit, int stop, int crc);

    private native void closeport(int fd);

    private native byte[] readport(int fd, int count, int delay);

    private native int writeport(int fd, byte[] buf);

    public native void clearportbuf(int fd);


    static {
        System.loadLibrary("SP");
    }

}
