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

import com.speedata.libid2.utils.DataConversionUtils;
import com.speedata.libid2.utils.MyLogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class SerialPort {

    public static final String TAG = "SerialPortNative";
    public static final String SERIAL_TTYMT0 = "/dev/ttyMT0";
    public static final String SERIAL_TTYMT1 = "/dev/ttyMT1";
    public static final String SERIAL_TTYMT2 = "/dev/ttyMT2";
    public static final String SERIAL_TTYMT3 = "/dev/ttyMT3";
    public static final String SERIAL_TTYG0 = "/dev/ttyG0";
    public static final String SERIAL_TTYG1 = "/dev/ttyG1";
    public static final String SERIAL_TTYG2 = "/dev/ttyG2";
    public static final String SERIAL_TTYG3 = "/dev/ttyG3";

    private MyLogger logger = MyLogger.jLog();
    /*
     * Do not remove or rename the field mFd: it is used by native method
     * close();
     */
    private int fdx = -1;
    private int writelen;
    private String str;

    public SerialPort() {
    }

    public void OpenSerial(String dev, int brd) throws SecurityException,
            IOException {
        // int result = 0;
        fdx = openport(dev, brd, 8, 1, 0);
        if (fdx < 0) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
    }

    public void OpenSerial(String device, int baudrate, int databit,
                           int stopbit, int crc) throws SecurityException, IOException {
        System.out.println("open");
        fdx = openport(device, baudrate, databit, stopbit, crc);
        if (fdx < 0) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
    }

    public int getFd() {
        return fdx;
    }

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


    private int timeout = 100;

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
//            clearPortBuf(fd);
        } else {
            logger.d("read---null");
        }
        /*
         * for(byte x : tmp) { Log.w("xxxx", String.format("0x%x", x)); }
		 */
        return tmp;
    }

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
    // private native int openport_easy(String port, int brd);

    private native int openport(String port, int brd, int bit, int stop, int crc);

    private native void closeport(int fd);

    private native byte[] readport(int fd, int count, int delay);

    private native int writeport(int fd, byte[] buf);

    public native void clearportbuf(int fd);

    // public native static int writestring(int fd, String wb, int len);

    static {
        System.loadLibrary("serial_port");
    }

}
