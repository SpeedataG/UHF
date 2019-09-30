package com.speedata.uhf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.interfaces.OnSpdBanMsgListener;
import com.speedata.libuhf.utils.CommonUtils;
import com.speedata.libuhf.utils.SharedXmlUtil;
import com.speedata.uhf.dialog.AlertDialogManager;
import com.speedata.uhf.dialog.DirectionalTagDialog;
import com.speedata.uhf.dialog.InvSetDialog;
import com.speedata.uhf.dialog.InventorySettingDialog;
import com.speedata.uhf.dialog.KillTagDialog;
import com.speedata.uhf.dialog.LockTagDialog;
import com.speedata.uhf.dialog.ReadTagDialog;
import com.speedata.uhf.dialog.SearchTagDialog;
import com.speedata.uhf.dialog.SetEPCDialog;
import com.speedata.uhf.dialog.SetModuleDialog;
import com.speedata.uhf.dialog.SetPasswordDialog;
import com.speedata.uhf.dialog.WriteTagDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author My_PC
 */
public class MainActivity extends Activity implements OnClickListener {
    private static final String[] LIST = {"Reserved", "EPC", "TID", "USER"};
    private TextView curTagInfo;
    private TextView status, version;
    private Spinner areaSelect;
    private Button searchTag;
    private Button readTag;
    private Button directionalTag;
    private Button writeTag;
    private Button setTag;
    private Button setPassword;
    private Button setEpc;
    private Button lockTag;
    private Button btnInvSet;
    private Button mButtonSetInv;
    private Button mButtonSetKill;
    private IUHFService iuhfService;
    private String currentTagEpc = null;
    private WakeLock wK = null;
    private int initProgress = 0;
    private String model;
    private String TAG = "TIME";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long currenTime = System.currentTimeMillis();
        try {
            iuhfService = UHFManager.getUHFService(MainActivity.this);
            Log.d(TAG, "currenTime 1:" + (System.currentTimeMillis() - currenTime));
        } catch (Exception e) {
            e.printStackTrace();
            boolean cn = "CN".equals(getApplicationContext().getResources().getConfiguration().locale.getCountry());
            if (cn) {
                Toast.makeText(getApplicationContext(), "模块不存在", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Module does not exist", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        model = SharedXmlUtil.getInstance(MainActivity.this).read("model", "");
        Log.d(TAG, "currenTime 2:" + (System.currentTimeMillis() - currenTime));
        initUI();
        Log.d(TAG, "currenTime 3:" + (System.currentTimeMillis() - currenTime));
        version.append("-" + model);
        newWakeLock();
        EventBus.getDefault().register(this);
        setTag.setEnabled(true);
        searchTag.setEnabled(true);
        readTag.setEnabled(true);
        writeTag.setEnabled(true);
        setEpc.setEnabled(true);
        setPassword.setEnabled(true);
        lockTag.setEnabled(true);
        areaSelect.setEnabled(true);
        btnInvSet.setVisibility(View.VISIBLE);
        btnInvSet.setEnabled(true);
        Log.d(TAG, "currenTime 4:" + (System.currentTimeMillis() - currenTime));

        //监听报警
        AlertDialogManager.getAlertInstance(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (iuhfService != null) {
                openDev();
                Log.e("zzc:", "上电");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        sendUpddateService();
        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            if (iuhfService != null) {
                iuhfService.closeDev();
                Log.e("zzc:", "下电");
                //断点后选卡操作会失效，需要重新选卡（掩码）
                currentTagEpc = null;
                curTagInfo.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("zzc:", "onStop()执行");
        super.onStop();
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MsgEvent mEvent) {
        String type = mEvent.getType();
        String msg = (String) mEvent.getMsg();
        if ("set_current_tag_epc".equals(type)) {
            currentTagEpc = msg;
            curTagInfo.setText(msg);
            MainActivity.this.status
                    .setText(R.string.Status_Select_Card_Ok);
        }
        if ("setPWD_Status".equals(type)) {
            MainActivity.this.status
                    .setText(R.string.Status_Write_Card_Ok);
        }
        if ("lock_Status".equals(type)) {
            MainActivity.this.status
                    .setText(R.string.set_success);
        }
        if ("SetEPC_Status".equals(type)) {
            MainActivity.this.status
                    .setText(R.string.Status_Write_Card_Ok);
        }
        if ("CancelSelectCard".equals(type)) {
            //断点后选卡操作会失效，需要重新选卡（掩码）
            currentTagEpc = null;
            curTagInfo.setText("");
        }

    }

    @SuppressLint("InvalidWakeLockTag")
    private void newWakeLock() {
        initProgress++;
        PowerManager pM = (PowerManager) getSystemService(POWER_SERVICE);
        if (pM != null) {
            wK = pM.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, "lock3992");
            if (wK != null) {
                wK.acquire();
                initProgress++;
            }
        }
        if (initProgress == 1) {
            Log.w("3992_6C", "wake lock init failed");
        }
    }

    /**
     * 上电开串口
     *
     * @return
     */
    @SuppressLint("SetTextI18n")
    private boolean openDev() {
        if (iuhfService.openDev() != 0) {
            Toast.makeText(this, "Open serialport failed", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).show();
            return true;
        }
        return false;
    }

    private void initUI() {
        setContentView(R.layout.main);
        writeTag = findViewById(R.id.btn_write);
        writeTag.setOnClickListener(this);
        readTag = findViewById(R.id.btn_read);
        readTag.setOnClickListener(this);
        directionalTag = findViewById(R.id.btn_direction);
        directionalTag.setOnClickListener(this);
        searchTag = findViewById(R.id.btn_search);
        searchTag.setOnClickListener(this);
        setTag = findViewById(R.id.btn_check);
        setTag.setOnClickListener(this);
        setPassword = findViewById(R.id.btn_setpasswd);
        setPassword.setOnClickListener(this);
        setEpc = findViewById(R.id.btn_setepc);
        setEpc.setOnClickListener(this);
        btnInvSet = findViewById(R.id.btn_inv_set);
        btnInvSet.setOnClickListener(this);
        lockTag = findViewById(R.id.btn_lock);
        lockTag.setOnClickListener(this);
        Button speedt = findViewById(R.id.button_spt);
        speedt.setOnClickListener(this);
        curTagInfo = findViewById(R.id.textView_epc);
        curTagInfo.setText("");
        status = findViewById(R.id.textView_status);
        version = findViewById(R.id.textView_version);
        version.setText(CommonUtils.getAppVersionName(this));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, LIST);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSelect = findViewById(R.id.spinner_area);
        areaSelect.setAdapter(adapter);
        setTag.setEnabled(false);
        searchTag.setEnabled(false);
        readTag.setEnabled(false);
        writeTag.setEnabled(false);
        setEpc.setEnabled(false);
        setPassword.setEnabled(false);
        lockTag.setEnabled(false);
        areaSelect.setEnabled(false);

        mButtonSetInv = findViewById(R.id.button_setInv);
        mButtonSetInv.setOnClickListener(this);
        mButtonSetKill = findViewById(R.id.button_setKill);
        mButtonSetKill.setOnClickListener(this);

    }

    private void sendUpddateService() {
        Intent intent = new Intent();
        intent.setAction("uhf.update");
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wK != null) {
            wK.release();
        }
        //注销广播、对象制空
//        UHFManager.closeUHFService();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if (arg0 == readTag) {
            if (currentTagEpc == null) {
                status.setText(R.string.Status_No_Card_Select);
                Toast.makeText(this, R.string.Status_No_Card_Select, Toast.LENGTH_SHORT).show();
                return;
            }
            //读卡
            ReadTagDialog readTag = new ReadTagDialog(this, iuhfService
                    , areaSelect.getSelectedItemPosition(), currentTagEpc, model);
            readTag.setTitle(R.string.Item_Read);
            readTag.show();

        } else if (arg0 == writeTag) {
            if (currentTagEpc == null) {
                status.setText(R.string.Status_No_Card_Select);
                Toast.makeText(this, R.string.Status_No_Card_Select, Toast.LENGTH_SHORT).show();
                return;
            }
            //写卡
            WriteTagDialog writeTag = new WriteTagDialog(this, iuhfService,
                    areaSelect.getSelectedItemPosition()
                    , currentTagEpc, model);
            writeTag.setTitle(R.string.Item_Write);
            writeTag.show();

        } else if (arg0 == searchTag) {
            //盘点选卡
            SearchTagDialog searchTag = new SearchTagDialog(this, iuhfService, model);
            searchTag.setTitle(R.string.Item_Choose);
            searchTag.show();

        } else if (arg0 == directionalTag) {
            //方向判断
            DirectionalTagDialog directionalTagDialog = new DirectionalTagDialog(this, iuhfService);
            directionalTagDialog.show();
        } else if (arg0 == setTag) {
            //设置频率频段
            SetModuleDialog setDialog = new SetModuleDialog(this, iuhfService, model);
            setDialog.setTitle(R.string.Item_Set_Title);
            setDialog.show();

        } else if (arg0 == setPassword) {
            if (currentTagEpc == null) {
                status.setText(R.string.Status_No_Card_Select);
                Toast.makeText(this, R.string.Status_No_Card_Select, Toast.LENGTH_SHORT).show();
                return;
            }
            //设置密码
            SetPasswordDialog setPasswordDialog = new SetPasswordDialog(this
                    , iuhfService, currentTagEpc, model);
            setPasswordDialog.setTitle(R.string.SetPasswd_Btn);
            setPasswordDialog.show();
        } else if (arg0 == setEpc) {
            if (currentTagEpc == null) {
                status.setText(R.string.Status_No_Card_Select);
                Toast.makeText(this, R.string.Status_No_Card_Select, Toast.LENGTH_SHORT).show();
                return;
            }
            //写EPC
            SetEPCDialog setEPCDialog = new SetEPCDialog(this, iuhfService, currentTagEpc, model);
            setEPCDialog.setTitle(R.string.SetEPC_Btn);
            setEPCDialog.show();
        } else if (arg0 == lockTag) {
            if (currentTagEpc == null) {
                status.setText(R.string.Status_No_Card_Select);
                Toast.makeText(this, R.string.Status_No_Card_Select, Toast.LENGTH_SHORT).show();
                return;
            }
            //锁
            LockTagDialog lockTagDialog = new LockTagDialog(this, iuhfService
                    , currentTagEpc, model);
            lockTagDialog.setTitle(R.string.Lock_Btn);
            lockTagDialog.show();
        } else if (arg0 == btnInvSet) {
            //盘点内容设置
            InvSetDialog invSetDialog = new InvSetDialog(this, iuhfService);
            invSetDialog.setTitle("Inv Set");
            invSetDialog.show();
        } else if (arg0 == mButtonSetInv) {
            InventorySettingDialog inventorySettingDialog = new InventorySettingDialog(this);
            inventorySettingDialog.show();
        } else if (arg0 == mButtonSetKill) {
            if (currentTagEpc == null) {
                status.setText(R.string.Status_No_Card_Select);
                Toast.makeText(this, R.string.Status_No_Card_Select, Toast.LENGTH_SHORT).show();
                return;
            }
            KillTagDialog killTagDialog = new KillTagDialog(this, iuhfService, currentTagEpc, model);
            killTagDialog.setTitle(getResources().getString(R.string.setKill));
            killTagDialog.show();
        }


    }

    private long mkeyTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.ACTION_DOWN:
                if ((System.currentTimeMillis() - mkeyTime) > 2000) {
                    mkeyTime = System.currentTimeMillis();
                    boolean cn = "CN".equals(getApplicationContext().getResources().getConfiguration().locale.getCountry());
                    if (cn) {
                        Toast.makeText(getApplicationContext(), "再按一次退出", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Click again to exit", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
}
