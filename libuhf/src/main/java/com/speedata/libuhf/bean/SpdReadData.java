package com.speedata.libuhf.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 张明_ on 2017/11/15.
 */

public class SpdReadData implements Parcelable {
    private byte[] ReadData;
    private byte[] EPCData;
    private int EPCLen;
    private int DataLen;
    private int RSS;
    private int status;

    public SpdReadData(){

    }

    protected SpdReadData(Parcel in) {
        ReadData = in.createByteArray();
        EPCData = in.createByteArray();
        EPCLen = in.readInt();
        DataLen = in.readInt();
        RSS = in.readInt();
        status = in.readInt();
    }

    public static final Creator<SpdReadData> CREATOR = new Creator<SpdReadData>() {
        @Override
        public SpdReadData createFromParcel(Parcel in) {
            return new SpdReadData(in);
        }

        @Override
        public SpdReadData[] newArray(int size) {
            return new SpdReadData[size];
        }
    };

    public byte[] getReadData() {
        return ReadData;
    }

    public void setReadData(byte[] readData) {
        ReadData = readData;
    }

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

    public int getDataLen() {
        return DataLen;
    }

    public void setDataLen(int dataLen) {
        DataLen = dataLen;
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
        dest.writeByteArray(ReadData);
        dest.writeByteArray(EPCData);
        dest.writeInt(EPCLen);
        dest.writeInt(DataLen);
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
        dest.readByteArray(ReadData);
        dest.readByteArray(EPCData);
        EPCLen = dest.readInt();
        DataLen = dest.readInt();
        RSS = dest.readInt();
        status = dest.readInt();
    }
}
