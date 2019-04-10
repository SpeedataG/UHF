package com.speedata.libuhf.bean;


import android.os.Parcel;
import android.os.Parcelable;

public class DynamicQParams implements Parcelable {
    public int startQValue;
    public int minQValue;
    public int maxQValue;
    public int retryCount;
    public int toggleTarget;
    public int thresholdMultiplier;

    public DynamicQParams() {
    }

    public DynamicQParams(int startQValue, int minQValue, int maxQValue, int retryCount, int toggleTarget, int thresholdMultiplier) {
        this.startQValue = startQValue;
        this.minQValue = minQValue;
        this.maxQValue = maxQValue;
        this.retryCount = retryCount;
        this.toggleTarget = toggleTarget;
        this.thresholdMultiplier = thresholdMultiplier;
    }

    protected DynamicQParams(Parcel in) {
        startQValue = in.readInt();
        minQValue = in.readInt();
        maxQValue = in.readInt();
        retryCount = in.readInt();
        toggleTarget = in.readInt();
        thresholdMultiplier = in.readInt();
    }

    public static final Creator<DynamicQParams> CREATOR = new Creator<DynamicQParams>() {
        @Override
        public DynamicQParams createFromParcel(Parcel in) {
            return new DynamicQParams(in);
        }

        @Override
        public DynamicQParams[] newArray(int size) {
            return new DynamicQParams[size];
        }
    };

    public void setValue(int startQValue, int minQValue, int maxQValue, int retryCount, int toggleTarget, int thresholdMultiplier) {
        this.startQValue = startQValue;
        this.minQValue = minQValue;
        this.maxQValue = maxQValue;
        this.retryCount = retryCount;
        this.toggleTarget = toggleTarget;
        this.thresholdMultiplier = thresholdMultiplier;
    }

    @Override
    public String toString() {
        return "DynamicQParams{startQValue=" + this.startQValue + ", minQValue=" + this.minQValue + ", maxQValue=" + this.maxQValue + ", retryCount=" + this.retryCount + ", toggleTarget=" + this.toggleTarget + ", thresholdMultiplier=" + this.thresholdMultiplier + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(startQValue);
        dest.writeInt(minQValue);
        dest.writeInt(maxQValue);
        dest.writeInt(retryCount);
        dest.writeInt(toggleTarget);
        dest.writeInt(thresholdMultiplier);
    }

    /**
     * 参数是一个Parcel,用它来存储与传输数据
     *
     * @param dest 手动添加read方法
     */
    public void readFromParcel(Parcel dest) {
        //注意，此处的读值顺序应当是和writeToParcel()方法中一致的
        startQValue = dest.readInt();
        minQValue = dest.readInt();
        maxQValue = dest.readInt();
        retryCount = dest.readInt();
        toggleTarget = dest.readInt();
        thresholdMultiplier = dest.readInt();
    }
}
