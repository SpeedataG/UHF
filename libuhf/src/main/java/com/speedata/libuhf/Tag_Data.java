package com.speedata.libuhf;

/**
 * Created by 张明_ on 2016/12/16.
 */

public class Tag_Data {
    public Tag_Data(byte[] n_tid, byte[] n_epc) {
        tid = n_tid;
        epc = n_epc;
    }

    public byte[] tid;
    public byte[] epc;
}
