package com.speedata.libuhf.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class FixedQParams implements Parcelable {
    public int qValue;
    public int retryCount;
    public int toggleTarget;
    public int repeatUntiNoTags;

    public FixedQParams() {
    }

    public FixedQParams(int qValue, int retryCount, int toggleTarget, int repeatUntiNoTags) {
        this.qValue = qValue;
        this.retryCount = retryCount;
        this.toggleTarget = toggleTarget;
        this.repeatUntiNoTags = repeatUntiNoTags;
    }

    protected FixedQParams(Parcel in) {
        qValue = in.readInt();
        retryCount = in.readInt();
        toggleTarget = in.readInt();
        repeatUntiNoTags = in.readInt();
    }

    public static final Creator<FixedQParams> CREATOR = new Creator<FixedQParams>() {
        @Override
        public FixedQParams createFromParcel(Parcel in) {
            return new FixedQParams(in);
        }

        @Override
        public FixedQParams[] newArray(int size) {
            return new FixedQParams[size];
        }
    };

    public void setValue(int qValue, int retryCount, int toggleTarget, int repeatUntiNoTags) {
        this.qValue = qValue;
        this.retryCount = retryCount;
        this.toggleTarget = toggleTarget;
        this.repeatUntiNoTags = repeatUntiNoTags;
    }

    @Override
    public String toString() {
        return "FixedQParams{qValue=" + this.qValue + ", retryCount=" + this.retryCount + ", toggleTarget=" + this.toggleTarget + ", repeatUntiNoTags=" + this.repeatUntiNoTags + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(qValue);
        dest.writeInt(retryCount);
        dest.writeInt(toggleTarget);
        dest.writeInt(repeatUntiNoTags);
    }

    /**
     * 参数是一个Parcel,用它来存储与传输数据
     *
     * @param dest 手动添加read方法
     */
    public void readFromParcel(Parcel dest) {
        //注意，此处的读值顺序应当是和writeToParcel()方法中一致的
        qValue = dest.readInt();
        retryCount = dest.readInt();
        toggleTarget = dest.readInt();
        repeatUntiNoTags = dest.readInt();
    }
}
