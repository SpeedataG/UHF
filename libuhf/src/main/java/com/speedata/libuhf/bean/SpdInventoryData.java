package com.speedata.libuhf.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 张明_ on 2016/12/16.
 */

public class SpdInventoryData implements Parcelable {

    public SpdInventoryData(String n_tid, String n_epc, String rssi) {
        tid = n_tid;
        epc = n_epc;
        this.rssi = rssi;
        this.bid = "";
        this.u8Tid = "";
    }
    public SpdInventoryData(String n_tid, String n_epc, String rssi, String bid, String u8Tid) {
        tid = n_tid;
        epc = n_epc;
        this.rssi = rssi;
        this.bid = bid;
        this.u8Tid = u8Tid;
    }

    public String tid;
    public String epc;
    public String rssi;
    public String bid;
    public String u8Tid;

    public int frequency;
    public byte[] pc = new byte[2];
    public int readCnt;



    protected SpdInventoryData(Parcel in) {
        tid = in.readString();
        epc = in.readString();
        rssi = in.readString();
    }

    public static final Creator<SpdInventoryData> CREATOR = new Creator<SpdInventoryData>() {
        @Override
        public SpdInventoryData createFromParcel(Parcel in) {
            return new SpdInventoryData(in);
        }

        @Override
        public SpdInventoryData[] newArray(int size) {
            return new SpdInventoryData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tid);
        dest.writeString(epc);
        dest.writeString(rssi);
    }

    /**
     * 参数是一个Parcel,用它来存储与传输数据
     *
     * @param dest 手动添加read方法
     */
    public void readFromParcel(Parcel dest) {
        //注意，此处的读值顺序应当是和writeToParcel()方法中一致的
        tid = dest.readString();
        epc = dest.readString();
        rssi = dest.readString();
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getU8Tid() {
        return u8Tid;
    }

    public void setU8Tid(String u8Tid) {
        this.u8Tid = u8Tid;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public byte[] getPc() {
        return pc;
    }

    public void setPc(byte[] pc) {
        this.pc = pc;
    }

    public int getReadCnt() {
        return readCnt;
    }

    public void setReadCnt(int readCnt) {
        this.readCnt = readCnt;
    }
}
