package com.speedata.libuhf.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by 张明_ on 2016/12/16.
 */

public class SpdInventoryData implements Parcelable{
    public SpdInventoryData(String n_tid, String n_epc, String rssi) {
        tid = n_tid;
        epc = n_epc;
        this.rssi=rssi;
    }

    public String tid;
    public String epc;
    public String rssi;


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
}
