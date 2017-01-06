package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.Tag_Data;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class SearchTagDialog extends Dialog implements
        android.view.View.OnClickListener, AdapterView.OnItemClickListener {

    private Button Cancle;
    private Button Action;
    private TextView Status;
    private ListView EpcList;
    private boolean inSearch = false;
    private List<EpcDataBase> firm = new ArrayList<EpcDataBase>();
    private Handler handler = null;
    private ArrayAdapter<EpcDataBase> adapter;
    private Context cont;
    private SoundPool soundPool;
    private int soundId;
    private long scant = 0;
    private CheckBox cbb;
    private IUHFService iuhfService;
    private String model;

    public SearchTagDialog(Context context,IUHFService iuhfService,String model) {
        super(context);
        // TODO Auto-generated constructor stub
        cont = context;
        this.iuhfService=iuhfService;
        this.model=model;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setreader);

        Cancle = (Button) findViewById(R.id.btn_search_cancle);
        Cancle.setOnClickListener(this);
        Action = (Button) findViewById(R.id.btn_search_action);
        Action.setOnClickListener(this);

        cbb = (CheckBox) findViewById(R.id.checkBox_beep);

        Status = (TextView) findViewById(R.id.textView_search_status);
        EpcList = (ListView) findViewById(R.id.listView_search_epclist);
        EpcList.setOnItemClickListener(this);

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        if (soundPool == null) {
            Log.e("as3992", "Open sound failed");
        }
        soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
        Log.w("as3992_6C", "id is " + soundId);


        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    scant++;
                    if (!cbb.isChecked()) {
                        if (scant % 50 == 0) {
                            soundPool.play(soundId, 1, 1, 0, 0, 1);
                        }
                    }
                    ArrayList<Tag_Data> ks = (ArrayList<Tag_Data>) msg.obj;
                    String tmp[] = new String[ks.size()];
                    for (int i = 0; i < ks.size(); i++) {
                        byte[] nq = ks.get(i).epc;
                        if (nq != null) {
                            tmp[i] = new String();
                            for (int j = 0; j < nq.length; j++) {
                                tmp[i] += String.format("%02x ", nq[j]);
                            }
                        }
                    }
                    int i, j;
                    for (i = 0; i < tmp.length; i++) {
                        for (j = 0; j < firm.size(); j++) {
                            if (tmp[i].equals(firm.get(j).epc)) {
                                firm.get(j).valid++;
                                break;
                            }
                        }
                        if (j == firm.size()) {
                            firm.add(new EpcDataBase(tmp[i], 1));
                            if (cbb.isChecked()) {
                                soundPool.play(soundId, 1, 1, 0, 0, 1);
                            }
                        }
                    }
                }
                adapter = new ArrayAdapter<EpcDataBase>(
                        cont, android.R.layout.simple_list_item_1, firm);
                EpcList.setAdapter(adapter);
                Status.setText("Total: " + firm.size());
            }
        };

        iuhfService.reg_handler(handler);
    }


    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        Log.w("stop", "im stopping");
        if (inSearch) {
            iuhfService.inventory_stop();
            inSearch = false;
        }
        soundPool.release();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == Cancle) {
            soundPool.release();
            dismiss();
        } else if (v == Action) {
            if (inSearch) {
                inSearch = false;
                this.setCancelable(true);
                iuhfService.inventory_stop();
                Action.setText(R.string.Start_Search_Btn);
                Cancle.setEnabled(true);
            } else {
                inSearch = true;
                this.setCancelable(false);
                scant = 0;
                iuhfService.inventory_start();

                Action.setText(R.string.Stop_Search_Btn);
                Cancle.setEnabled(false);
            }
        }
    }

    class EpcDataBase {
        String epc;
        int valid;

        public EpcDataBase(String e, int v) {
            // TODO Auto-generated constructor stub
            epc = e;
            valid = v;
        }

        @Override
        public String toString() {
            return epc + "  ( " + valid + " )";
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                            long arg3) {
        // TODO Auto-generated method stub
        if (inSearch) {
            return;
        }
//        int res = select_UHF(firm.get(arg2).epc);
        int res =iuhfService.select_card(firm.get(arg2).epc);
        if (res == 0) {
            EventBus.getDefault().post(new MsgEvent("set_current_tag_epc",firm.get(arg2).epc));
            dismiss();
        } else {
            Status.setText(R.string.Status_Select_Card_Faild);
        }
    }


//    int select_UHF(String epc) {
//
//        Log.i("r2000", "selec epc " + epc);
//        byte[] eepc;
//        if (model.equals("FEILIXIN")) {
//            eepc = getBytes(epc);
//        } else {
//            StringTokenizer sepc = new StringTokenizer(epc);
//            eepc = new byte[sepc.countTokens()];
//            int index = 0;
//            while (sepc.hasMoreTokens()) {
//                try {
//                    eepc[index++] = (byte) Integer.parseInt(sepc.nextToken(), 16);
//                } catch (NumberFormatException p) {
//                    return -1;
//                }
//            }
//        }
//
//        if (iuhfService.select_card(eepc) != 0) {
//            return -1;
//        }
//        return 0;
//    }

    /**
     * 将一个可能带空格的字符串，以Byte.parseByte的方法转化为byte数组
     */
//    private byte[] getBytes(String data) {
//        String newData = data.trim().replace(" ", "");
//        byte[] datas = new byte[newData.length()];
//        int i;
//        for (i = 0; i < datas.length; ++i) {
//            datas[i] = Byte.parseByte(newData.substring(i, i + 1), 16);
//        }
//        return datas;
//    }
}
