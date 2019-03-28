package com.speedata.libuhf.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 张明_ on 2017/11/15.
 */

public class SpdWriteData implements Parcelable {
    private byte[] EPCData;
    private int EPCLen;
    private int RSS;
    private int status;

    public SpdWriteData(){

    }

    protected SpdWriteData(Parcel in) {
        EPCData = in.createByteArray();
        EPCLen = in.readInt();
        RSS = in.readInt();
        status = in.readInt();
    }

    public static final Creator<SpdWriteData> CREATOR = new Creator<SpdWriteData>() {
        @Override
        public SpdWriteData createFromParcel(Parcel in) {
            return new SpdWriteData(in);
        }

        @Override
        public SpdWriteData[] newArray(int size) {
            return new SpdWriteData[size];
        }
    };

    public byte[] getEPCData() {
        return EPCData;
    }

    public void setEPCData(byte[] EPCData) {
        this.EPCData = EPCData;
    }

    public int getEPCLen() {
        return EPCLen;
    }

    public void setEPCLen(int EPCLen) {
        this.EPCLen = EPCLen;
    }

    public int getRSS() {
        return RSS;
    }

    public void setRSS(int RSS) {
        this.RSS = RSS;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(EPCData);
        dest.writeInt(EPCLen);
        dest.writeInt(RSS);
        dest.writeInt(status);
    }

    /**
     * 参数是一个Parcel,用它来存储与传输数据
     *
     * @param dest 手动添加read方法
     */
    public void readFromParcel(Parcel dest) {
        //注意，此处的读值顺序应当是和writeToParcel()方法中一致的
        dest.readByteArray(EPCData);
        EPCLen = dest.readInt();
        RSS = dest.readInt();
        status = dest.readInt();
    }
}
