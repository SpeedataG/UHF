package com.speedata.uhf.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kaopiz.kprogresshud.KProgressHUD;
import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.bean.SpdInventoryData;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.libuhf.utils.SharedXmlUtil;
import com.speedata.uhf.MainActivity;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;
import com.speedata.uhf.excel.EPCBean;
import com.speedata.uhf.libutils.excel.ExcelUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jxl.write.Colour;

/**
 * @author 张明_
 * @date 2016/12/28
 */

public class SearchTagDialog extends Dialog implements
        View.OnClickListener, AdapterView.OnItemClickListener {

    private Button cancel;
    private Button action;
    private TextView status;
    private boolean inSearch = false;
    private List<EpcDataBase> firm = new ArrayList<>();
    private ArrayAdapter<EpcDataBase> adapter;
    private Context cont;
    private SoundPool soundPool;
    private int soundId;
    private long scant = 0;
    private CheckBox cbb;
    private IUHFService iuhfService;
    private Button export;
    private KProgressHUD kProgressHUD;
    private TextView tagNumTv;
    private TextView speedTv;
    private TextView totalTv;
    private TextView totalTime;
    /**
     * 盘点命令下发后截取的系统时间
     */
    private long startCheckingTime;

    private static final String CHARGING_PATH = "/sys/class/misc/bq25601/regdump/";
    private File file;
    private BufferedWriter writer;

    public SearchTagDialog(Context context, IUHFService iuhfService, String model) {
        super(context);
        // TODO Auto-generated constructor stub
        cont = context;
        this.iuhfService = iuhfService;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setreader);

        initView();
        cancel = findViewById(R.id.btn_search_cancle);
        cancel.setOnClickListener(this);
        action = findViewById(R.id.btn_search_action);
        action.setOnClickListener(this);

        export = findViewById(R.id.btn_export);
        export.setOnClickListener(this);
        cbb = findViewById(R.id.checkBox_beep);

        status = findViewById(R.id.textView_search_status);
        ListView epcList = findViewById(R.id.listView_search_epclist);
        epcList.setOnItemClickListener(this);

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
        Log.w("as3992_6C", "id is " + soundId);


        //新的Listener回调参考代码
        adapter = new ArrayAdapter<>(
                cont, android.R.layout.simple_list_item_1, firm);
        epcList.setAdapter(adapter);


        iuhfService.setOnInventoryListener(new OnSpdInventoryListener() {
            @Override
            public void getInventoryData(SpdInventoryData var1) {
                Log.d("zzc:", "OnSpdInventoryListener 盘点回调");
                handler.sendMessage(handler.obtainMessage(1, var1));
            }

            @Override
            public void onInventoryStatus(int status) {

            }
        });

        file = new File(CHARGING_PATH);
    }

    //新的Listener回调参考代码

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            boolean cn = "CN".equals(cont.getApplicationContext().getResources().getConfiguration().locale.getCountry());
            switch (msg.what) {
                case 1:
                    scant++;
                    if (!cbb.isChecked()) {
                        soundPool.play(soundId, 1, 1, 0, 0, 1);
                    }
                    SpdInventoryData var1 = (SpdInventoryData) msg.obj;
                    int j;
                    for (j = 0; j < firm.size(); j++) {
                        if (var1.epc.equals(firm.get(j).epc)) {
                            firm.get(j).valid++;
                            firm.get(j).setRssi(var1.rssi);
                            break;
                        }
                    }
                    if (j == firm.size()) {
                        firm.add(new EpcDataBase(var1.epc, 1,
                                var1.rssi, var1.tid));
                        if (cbb.isChecked()) {
                            soundPool.play(soundId, 1, 1, 0, 0, 1);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    status.setText("Total: " + firm.size());
                    updateratecount();
                    break;

                case 2:
                    kProgressHUD.dismiss();
                    if (cn) {
                        Toast.makeText(cont, "导出完成", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(cont, "Export the complete", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case 3:
                    kProgressHUD.dismiss();
                    if (cn) {
                        Toast.makeText(cont, "导出过程中出现问题！请重试", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(cont, "There is a problem in exporting! Please try again", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    protected void onStop() {
        Log.w("stop", "im stopping");
        if (inSearch) {
            iuhfService.inventoryStop();
            inSearch = false;
        }
        soundPool.release();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v == cancel) {
            soundPool.release();
            dismiss();
        } else if (v == action) {
            if (inSearch) {
                inSearch = false;
                this.setCancelable(true);
                iuhfService.inventoryStop();
                try {
                    writer = new BufferedWriter(new FileWriter(file, false));
                    writer.write("otgoff");
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                action.setText(R.string.Start_Search_Btn);
                cancel.setEnabled(true);
                export.setEnabled(true);
            } else {
                inSearch = true;
                this.setCancelable(false);
                cancel.setEnabled(false);
                export.setEnabled(false);
                scant = 0;
                firm.clear();
                //取消掩码
                iuhfService.selectCard(1, "", false);
                EventBus.getDefault().post(new MsgEvent("CancelSelectCard", ""));
                try {
                    writer = new BufferedWriter(new FileWriter(file, false));
                    writer.write("otgon");
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SystemClock.sleep(10);
                iuhfService.inventoryStart();
                Log.d("zzc:", "inventoryStart 开始盘点");
                startCheckingTime = System.currentTimeMillis();
                action.setText(R.string.Stop_Search_Btn);

            }
        } else if (v == export) {
            kProgressHUD = KProgressHUD.create(cont)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setCancellable(false)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f)
                    .show();
            if (firm.size() > 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<EPCBean> epcBeanList = new ArrayList<EPCBean>();
                        for (EpcDataBase epcDataBase : firm) {
                            EPCBean epcBean = new EPCBean();
                            epcBean.setEPC(epcDataBase.epc);
                            epcBean.setTID_USER(epcDataBase.tidUser);
                            epcBeanList.add(epcBean);
                        }
                        if (epcBeanList.size() > 0) {
                            try {
                                ExcelUtils.getInstance()
                                        .setSHEET_NAME("UHFMsg")//设置表格名称
                                        .setFONT_COLOR(Colour.BLUE)//设置标题字体颜色
                                        .setFONT_TIMES(8)//设置标题字体大小
                                        .setFONT_BOLD(true)//设置标题字体是否斜体
                                        .setBACKGROND_COLOR(Colour.GRAY_25)//设置标题背景颜色
                                        .setContent_list_Strings(epcBeanList)//设置excel内容
                                        .setWirteExcelPath(Environment.getExternalStorageDirectory() + File.separator + "UHFMsg.xls")
                                        .createExcel(cont);
                                handler.sendMessage(handler.obtainMessage(2));
                            } catch (Exception e) {
                                e.printStackTrace();
                                handler.sendMessage(handler.obtainMessage(3));
                            }
                        } else {
                            handler.sendMessage(handler.obtainMessage(3));
                        }


                    }
                }).start();
            } else {
                kProgressHUD.dismiss();
                boolean cn = "CN".equals(cont.getApplicationContext().getResources().getConfiguration().locale.getCountry());
                if (cn) {
                    Toast.makeText(cont, "没有数据，请先盘点", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(cont, "No data, please take stock", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void initView() {
        tagNumTv = findViewById(R.id.tagNum_tv);
        speedTv = findViewById(R.id.speed_tv);
        totalTv = findViewById(R.id.total_tv);
        totalTime = findViewById(R.id.totalTime);
    }

    class EpcDataBase {
        String epc;
        int valid;
        String rssi;
        String tidUser;

        EpcDataBase(String e, int v, String rssi, String tidUser) {
            // TODO Auto-generated constructor stub
            epc = e;
            valid = v;
            this.rssi = rssi;
            this.tidUser = tidUser;
        }

        public String getRssi() {
            return rssi;
        }

        void setRssi(String rssi) {
            this.rssi = rssi;
        }

        @NonNull
        @Override
        public String toString() {
            if (TextUtils.isEmpty(tidUser)) {
                return "EPC:" + epc + "\n"
                        + "(" + "COUNT:" + valid + ")" + " RSSI:" + rssi + "\n";
            } else {
                return "EPC:" + epc + "\n"
                        + "T/U:" + tidUser + "\n"
                        + "(" + "COUNT:" + valid + ")" + " RSSI:" + rssi + "\n";
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                            long arg3) {
        // TODO Auto-generated method stub
        if (inSearch) {
            return;
        }

        String epcStr = firm.get(arg2).epc;
        boolean u8 = SharedXmlUtil.getInstance(cont).read("U8", false);
        if (u8) {
            epcStr = epcStr.substring(0, 24);
        }
        int res = iuhfService.selectCard(1, epcStr, true);
        if (res == 0) {
            EventBus.getDefault().post(new MsgEvent("set_current_tag_epc", epcStr));
            dismiss();
        } else {
            status.setText(R.string.Status_Select_Card_Faild);
        }
    }


    private void updateratecount() {

        long mLendtime = System.currentTimeMillis();

        double rate = Math.ceil((scant * 1.0) * 1000 / (mLendtime - startCheckingTime));

        long totalTimeCount = mLendtime - startCheckingTime;

        speedTv.setText(String.format("%s次/秒", String.valueOf(rate)));

        tagNumTv.setText(String.format("已盘%s个", String.valueOf(firm.size())));

        totalTv.setText(String.format("总数%s次", String.valueOf(scant)));

        totalTime.setText(String.valueOf(getTimeFromMillisecond(totalTimeCount)));


    }

    /**
     * 从时间(毫秒)中提取出时间(时:分:秒)
     * 时间格式:  时:分
     *
     * @param millisecond 毫秒
     * @return 时间字符串
     */
    private static String getTimeFromMillisecond(Long millisecond) {
        String milli;
        long hours = millisecond / (60 * 60 * 1000);
        //根据时间差来计算小时数
        long minutes = (millisecond - hours * (60 * 60 * 1000)) / (60 * 1000);
        //根据时间差来计算分钟数
        long second = (millisecond - hours * (60 * 60 * 1000) - minutes * (60 * 1000)) / 1000;
        //根据时间差来计算秒数
        long milliSecond = millisecond - hours * (60 * 60 * 1000) - minutes * (60 * 1000) - second * 1000;
        //根据时间差来计算秒数
        if (milliSecond < 100) {
            milli = "0" + milliSecond;
        } else {
            milli = "" + milliSecond;
        }

        return hours + ": " + minutes + ": " + second + ":" + milli;
    }
}
