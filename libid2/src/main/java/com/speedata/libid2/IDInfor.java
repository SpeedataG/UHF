package com.speedata.libid2;

import android.graphics.Bitmap;

/**
 * Created by brxu on 2016/12/15.
 * 身份信息
 */

public class IDInfor {
    private String name;
    private String sex;
    private String nation;//民族
    private String year;
    private String month;
    private String day;
    private String address;
    private String num;//TODO
    private String QianFa;
    private String startYear;
    private String startMonth;
    private String startDay;
    private String endYear;
    private String endMonth;
    private String endDay;
    private String deadLine;
    private Bitmap bmps;//照片
    private byte[] fingerprStringer;//指纹信息
    private boolean isSuccess;
    private String errorMsg = "";
    private boolean withFinger = false;

    public boolean isWithFinger() {
        return withFinger;
    }

    public void setWithFinger(boolean withFinger) {
        this.withFinger = withFinger;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Bitmap getBmps() {
        return bmps;
    }

    public void setBmps(Bitmap bmps) {
        this.bmps = bmps;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getDeadLine() {
        return deadLine;
    }

    public void setDeadLine(String deadLine) {
        this.deadLine = deadLine;
    }

    public String getEndDay() {
        return endDay;
    }

    public void setEndDay(String endDay) {
        this.endDay = endDay;
    }

    public String getEndMonth() {
        return endMonth;
    }

    public void setEndMonth(String endMonth) {
        this.endMonth = endMonth;
    }

    public String getEndYear() {
        return endYear;
    }

    public void setEndYear(String endYear) {
        this.endYear = endYear;
    }

    public byte[] getFingerprStringer() {
        return fingerprStringer;
    }

    public void setFingerprStringer(byte[] fingerprStringer) {
        this.fingerprStringer = fingerprStringer;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getQianFa() {
        return QianFa;
    }

    public void setQianFa(String qianFa) {
        QianFa = qianFa;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getStartMonth() {
        return startMonth;
    }

    public void setStartMonth(String startMonth) {
        this.startMonth = startMonth;
    }

    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
