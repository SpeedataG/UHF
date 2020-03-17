package com.speedata.libuhf;

import android.os.Handler;

import com.speedata.libuhf.bean.DynamicQParams;
import com.speedata.libuhf.bean.FixedQParams;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.interfaces.OnSpdReadListener;
import com.speedata.libuhf.interfaces.OnSpdWriteListener;
//import com.uhf.structures.DynamicQParams;
//import com.uhf.structures.FixedQParams;
import com.uhf.structures.InventoryData;
import com.uhf.structures.KrReadData;
import com.uhf.structures.RW_Params;
import com.uhf.structures.SelectCriteria;

/**
 * 适配器类
 *
 * @author zzc
 * @date 2019/10/18
 */
public class IUHFServiceAdapter implements IUHFService {

    @Override
    public int openDev() {
        return -1;
    }

    @Override
    public void closeDev() {

    }

    @Override
    public void setOnInventoryListener(OnSpdInventoryListener onSpdInventoryListener) {

    }

    @Override
    public void inventoryStart() {

    }

    @Override
    public void inventoryStop() {

    }

    @Override
    public void setOnReadListener(OnSpdReadListener onSpdReadListener) {

    }

    @Override
    public int readArea(int area, int addr, int count, String passwd) {
        return -1;
    }

    @Override
    public void setOnWriteListener(OnSpdWriteListener onSpdWriteListener) {

    }

    @Override
    public int writeArea(int area, int addr, int count, String passwd, byte[] content) {
        return -1;
    }

    @Override
    public int setPassword(int which, String cur_pass, String new_pass) {
        return -1;
    }

    @Override
    public int setLock(int type, int area, String passwd) {
        return -1;
    }

    @Override
    public int setKill(String accessPassword, String killPassword) {
        return -1;
    }

    @Override
    public int setQueryTagGroup(int selected, int session, int target) {
        return -1;
    }

    @Override
    public int getQueryTagGroup() {
        return -1;
    }

    @Override
    public int selectCard(int bank, byte[] epc, boolean mFlag) {
        return -1;
    }

    @Override
    public int selectCard(int bank, String epc, boolean mFlag) {
        return -1;
    }

    @Override
    public int setAntennaPower(int power) {
        return -1;
    }

    @Override
    public int getAntennaPower() {
        return -1;
    }

    @Override
    public int setFreqRegion(int region) {
        return -1;
    }

    @Override
    public int getFreqRegion() {
        return -1;
    }

    @Override
    public int setInvMode(int invm, int addr, int length) {
        return -1;
    }

    @Override
    public int getInvMode(int type) {
        return -1;
    }

    @Override
    public int setNewEpc(String password, int len, byte[] epc) {
        return -1;
    }

    @Override
    public int switchInvMode(int mode) {
        return -1;
    }

    @Override
    public int setLowpowerScheduler(int invOnTime, int invOffTime) {
        return -1;
    }

    @Override
    public int[] getLowpowerScheduler() {
        return null;
    }

    @Override
    public int setDwellTime(int dwellTime) {
        return -1;
    }

    @Override
    public int getDwellTime() {
        return -1;
    }

    @Override
    public int mask(int area, int addr, int length, byte[] content) {
        return -1;
    }

    @Override
    public int cancelMask() {
        return -1;
    }

    @Override
    public SelectCriteria getMask() {
        return null;
    }

    @Override
    public int setMonzaQtTagMode(int memMap, int maskFlag, byte[] accessPassword) {
        return -1;
    }

    @Override
    public int readMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length) {
        return -1;
    }

    @Override
    public int readMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, int timeOutMs, RW_Params rw_params) {
        return -1;
    }

    @Override
    public int writeMonzaQtTag(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData) {
        return -1;
    }

    @Override
    public int writeMonzaQtTagSync(int memMap, byte[] pwd, int bank, int address, int length, byte[] writeData, int timeOutMs, RW_Params rw_params) {
        return -1;
    }

    @Override
    public int setDynamicAlgorithm(int startQ, int minQ, int maxQ, int tryCount, int target, int threshold) {
        return -1;
    }

    @Override
    public int setFixedAlgorithm(int qValue, int tryCount, int target, int repeat) {
        return -1;
    }

    @Override
    public int getDynamicAlgorithm(DynamicQParams dynamicQParams) {
        return -1;
    }

    @Override
    public int getFixedAlgorithm(FixedQParams fixedQParams) {
        return -1;
    }

    @Override
    public int setFrequency(double frequency) {
        return -1;
    }

    @Override
    public int enableEngTest(int gain) {
        return -1;
    }

    @Override
    public int setDynamicAlgorithm() {
        return -1;
    }

    @Override
    public int setQT(byte[] rpaswd, int cmdType, int memType, int persistType, int rangeType) {
        return -1;
    }

    @Override
    public int setGen2QValue(int qValue) {
        return -1;
    }

    @Override
    public int setGen2Target(int target) {
        return -1;
    }

    @Override
    public int[] getGen2AllValue() {
        return null;
    }

    @Override
    public int setGen2WriteMode(int wMode) {
        return -1;
    }

    @Override
    public int setGen2Blf(int blf) {
        return -1;
    }

    @Override
    public int setGen2MaxLen(int maxLen) {
        return -1;
    }

    @Override
    public int setGen2Code(int code) {
        return -1;
    }

    @Override
    public int setGen2Tari(int tari) {
        return -1;
    }

    @Override
    public int setNxpu8(int mode) {
        return 0;
    }

    @Override
    public int yixinFilterEpc(int bank, int ads, int len, String data, Boolean save) {
        return -1;
    }

    @Override
    public int krSm7Inventory(InventoryData inventoryData) {
        return -1;
    }

    @Override
    public int krSm7Blockwrite(int length, int addr, int area, byte[] pwd, byte[] content) {
        return -1;
    }

    @Override
    public int krSm7Write(int length, int addr, int area, byte[] pwd, byte[] content) {
        return -1;
    }

    @Override
    public int krSm7Read(int length, int addr, int area, byte[] pwd, KrReadData krSm7Data) {
        return -1;
    }

    @Override
    public int krSm7End() {
        return -1;
    }

    @Override
    public void inventory_start() {

    }

    @Override
    public void inventory_start(Handler hd) {

    }

    @Override
    public int inventory_stop() {
        return -1;
    }

    @Override
    public byte[] read_area(int area, int addr, int count, String passwd) {
        return null;
    }

    @Override
    public String read_area(int area, String str_addr, String str_count, String str_passwd) {
        return null;
    }

    @Override
    public int write_area(int area, int addr, int count, String passwd, byte[] content) {
        return -1;
    }

    @Override
    public int write_area(int area, String addr, String pwd, String count, String content) {
        return -1;
    }

    @Override
    public void reg_handler(Handler hd) {

    }

    @Override
    public int setlock(int type, int area, String passwd) {
        return -1;
    }

    @Override
    public int set_Password(int which, String cur_pass, String new_pass) {
        return -1;
    }

    @Override
    public String GetLastDetailError() {
        return null;
    }
}
