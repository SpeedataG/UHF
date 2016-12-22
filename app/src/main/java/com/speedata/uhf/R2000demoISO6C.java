package com.speedata.uhf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.INV_TIME;
import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.Tag_Data;
import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.utils.SharedXmlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.speedata.libuhf.UHFManager.mContext;

public class R2000demoISO6C extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    private static final String[] list = { "Reserved", "EPC", "TID", "USER" };
    private TextView Cur_Tag_Info;
    private TextView Status;
    private Spinner Area_Select;
    private ArrayAdapter<String> adapter;
    private Button Search_Tag;
    private Button Read_Tag;
    private Button Write_Tag;
    private Button Set_Tag;
    private Button Set_Password;
    private Button Set_EPC;
    private Button Lock_Tag;
    private EditText Tag_Content;

    private IUHFService iuhfService;

    private String current_tag_epc = null;
    private Button Speedt;
    private PowerManager pM = null;
    private WakeLock wK = null;
    private int init_progress = 0;
    private String modle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        iuhfService = UHFManager.getUHFService(R2000demoISO6C.this);
        if (iuhfService==null){
            return;
        }
        modle = SharedXmlUtil.getInstance(mContext).read("modle", "");
        Tag_Content = (EditText) findViewById(R.id.editText_content);
        Write_Tag = (Button) findViewById(R.id.btn_write);
        Write_Tag.setOnClickListener(this);
        Read_Tag = (Button) findViewById(R.id.btn_read);
        Read_Tag.setOnClickListener(this);
        Search_Tag = (Button) findViewById(R.id.btn_search);
        Search_Tag.setOnClickListener(this);
        Set_Tag = (Button) findViewById(R.id.btn_check);
        Set_Tag.setOnClickListener(this);
        Set_Password = (Button) findViewById(R.id.btn_setpasswd);
        Set_Password.setOnClickListener(this);
        Set_EPC = (Button) findViewById(R.id.btn_setepc);
        Set_EPC.setOnClickListener(this);
        Lock_Tag = (Button) findViewById(R.id.btn_lock);
        Lock_Tag.setOnClickListener(this);
        Speedt = (Button)findViewById(R.id.button_spt);
        Speedt.setOnClickListener(this);

        Cur_Tag_Info = (TextView) findViewById(R.id.textView_epc);
        Cur_Tag_Info.setText("");
        Status = (TextView) findViewById(R.id.textView_status);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Area_Select = (Spinner) findViewById(R.id.spinner_area);
        Area_Select.setAdapter(adapter);

        Set_Tag.setEnabled(false);
        Search_Tag.setEnabled(false);
        Read_Tag.setEnabled(false);
        Write_Tag.setEnabled(false);
        Set_EPC.setEnabled(false);
        Set_Password.setEnabled(false);
        Lock_Tag.setEnabled(false);
        Area_Select.setEnabled(false);

        if (iuhfService.OpenDev() != 0) {
            Cur_Tag_Info.setText("Open serialport failed");
            new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    finish();
                }
            }).show();
            return;
        }
        init_progress++;

        pM = (PowerManager) getSystemService(POWER_SERVICE);
        if (pM != null) {
            wK = pM.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, "lock3992");
            if (wK != null) {
                wK.acquire();
                init_progress++;
            }
        }

        if (init_progress == 1) {
            Log.w("3992_6C", "wake lock init failed");
        }

        Set_Tag.setEnabled(true);
        Search_Tag.setEnabled(true);
        Read_Tag.setEnabled(true);
        Write_Tag.setEnabled(true);
        Set_EPC.setEnabled(true);
        Set_Password.setEnabled(true);
        Lock_Tag.setEnabled(true);
        Area_Select.setEnabled(true);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("r2000_kt45", "called ondestory");
        switch (init_progress) {
            case 2:
                wK.release();
            case 1:
                iuhfService.CloseDev();
            case 0:
            default:
                init_progress = 0;
        }
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if (arg0 == Read_Tag) {
            if (current_tag_epc == null) {
                Status.setText(R.string.Status_No_Card_Select);
                return;
            }
            ReadTagDialog readTag = new ReadTagDialog(this);
            readTag.setTitle(R.string.Item_Read);
            readTag.show();
        } else if (arg0 == Write_Tag) {
            if (current_tag_epc == null) {
                Status.setText(R.string.Status_No_Card_Select);
                return;
            }
            WriteTagDialog writeTag = new WriteTagDialog(this);
            writeTag.setTitle(R.string.Item_Write);
            writeTag.show();
        } else if (arg0 == Search_Tag) {

            SearchTagDialog searchTag = new SearchTagDialog(this);
            searchTag.setTitle(R.string.Item_Choose);
            searchTag.show();

        } else if (arg0 == Set_Tag) {
            SetModuleDialog setDialog = new SetModuleDialog(this);
            setDialog.setTitle(R.string.Item_Set_Title);
            setDialog.show();

        } else if (arg0 == Set_Password) {
            if (current_tag_epc == null) {
                Status.setText(R.string.Status_No_Card_Select);
                return;
            }
            SetPasswordDialog setPasswordDialog = new SetPasswordDialog(this);
            setPasswordDialog.setTitle(R.string.SetPasswd_Btn);
            setPasswordDialog.show();
        } else if (arg0 == Set_EPC) {
            if (current_tag_epc == null) {
                Status.setText(R.string.Status_No_Card_Select);
                return;
            }
            SetEPCDialog setEPCDialog = new SetEPCDialog(this);
            setEPCDialog.setTitle(R.string.SetEPC_Btn);
            setEPCDialog.show();
        } else if (arg0 == Lock_Tag) {
            if (current_tag_epc == null) {
                Status.setText(R.string.Status_No_Card_Select);
                return;
            }
            LockTagDialog lockTagDialog = new LockTagDialog(this);
            lockTagDialog.setTitle(R.string.Lock_Btn);
            lockTagDialog.show();
        }
        else if(arg0 == Speedt)
        {
            SpeedTestDialog sptd = new SpeedTestDialog(this);
            sptd.setTitle("Speed Test");
            sptd.show();
        }
    }

    class SpeedTestDialog extends Dialog implements android.view.View.OnClickListener {
        private Button st;
        private TextView tv;
        private boolean inth = false;
        //private Thread thr;
        private Handler hd;
        private long total = 0;
        private long time0 = 0, time1 = 0;

        public SpeedTestDialog(Context context) {
            super(context);
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.spt);

            st = (Button)findViewById(R.id.button_spt_b);
            st.setOnClickListener(this);

            tv = (TextView)findViewById(R.id.textView_spt);

            hd = new Handler() {
                @Override
                public void handleMessage(Message msg)
                {
                    super.handleMessage(msg);
                    if (msg.what == 1) {
                        if(inth)
                        {
                            total++;
                            tv.setText("search " + total + " times");
                        }
                    }
                }
            };

            iuhfService.reg_handler(hd);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(v == st)
            {
                if(inth)
                {
                    iuhfService.inventory_stop();
                    time1 = SystemClock.uptimeMillis();
                    this.setCancelable(true);
                    st.setText("start");
                    inth = false;
                    long dv = time1 - time0;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    tv.setText("search cards " + total * 1000 / dv + " times / s");
                    Log.d("kt45", "in " + dv + "ms valid search " + total + " times average " + total * 1000 / dv + " times per ms");
                }
                else
                {
                    iuhfService.inventory_start();
                    time0 = SystemClock.uptimeMillis();
                    this.setCancelable(false);
                    total = 0;
                    st.setText("stop");
                    inth = true;
                }
            }
        }

    }

    class WriteTagDialog extends Dialog implements
            android.view.View.OnClickListener {

        private Button Ok;
        private Button Cancle;
        private TextView EPC;
        private TextView Status;
        private EditText Write_Addr;
        private EditText Write_Count;
        private EditText Write_Passwd;

        public WriteTagDialog(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.write);

            Ok = (Button) findViewById(R.id.btn_write_ok);
            Ok.setOnClickListener(this);
            Cancle = (Button) findViewById(R.id.btn_write_cancle);
            Cancle.setOnClickListener(this);

            EPC = (TextView) findViewById(R.id.textView_write_epc);
            EPC.setText(current_tag_epc);
            Status = (TextView) findViewById(R.id.textView_write_status);

            Write_Addr = (EditText) findViewById(R.id.editText_write_addr);
            Write_Count = (EditText) findViewById(R.id.editText_write_count);
            Write_Passwd = (EditText) findViewById(R.id.editText_write_passwd);
//			Write_Passwd.setText("00 00 00 00");
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == Ok) {
                String str_addr = Write_Addr.getText().toString();
                String str_count = Write_Count.getText().toString();
                String str_passwd = Write_Passwd.getText().toString();
                String str_content = Tag_Content.getText().toString();
                int num_addr;
                int num_count;
//				int passwd;
                long passwd;
                try {
                    num_addr = Integer.parseInt(str_addr, 16);
                    num_count = Integer.parseInt(str_count, 10);
//					passwd = Integer.parseInt(str_passwd, 16);
                    passwd = Long.parseLong(str_passwd, 16);

                } catch (NumberFormatException p) {
                    Status.setText(R.string.Status_InvalidNumber);
                    return;
                }
                int which_choose = Area_Select.getSelectedItemPosition();
                int rev = write_card(which_choose, num_addr, num_count * 2, (int)passwd, str_content);
                if (rev == 0) {
                    R2000demoISO6C.this.Status
                            .setText(R.string.Status_Write_Card_Ok);
                    dismiss();
                } else if (rev == -1) {
                    Status.setText(R.string.Status_Write_Error);
                } else if (rev == -2) {
                    Status.setText(R.string.Status_Passwd_Length_Error);
                } else if (rev == -3) {
                    Status.setText(R.string.Status_Content_Length_Error);
                } else if (rev == -4) {
                    Status.setText(R.string.Status_InvalidNumber);
                }

            } else if (v == Cancle) {
                dismiss();
            }
        }
    }

    private int write_card(int area, int addr, int count, int passwd, String cnt)
    {
        StringTokenizer cn = new StringTokenizer(cnt);
        if (cn.countTokens() < count) {
            return -3;
        }
        byte[] cf = new byte[count];
        int index = 0;
        while (cn.hasMoreTokens() && (index < count)) {
            try {
                int k = Integer.parseInt(cn.nextToken(), 16);
                if(k > 0xff)
                {
                    throw new NumberFormatException("can't bigger than 0xff");
                }
                cf[index++] = (byte)k;
//				cf[index++] = (byte) Integer.parseInt(cn.nextToken(), 16);
            } catch (NumberFormatException p) {
                return -4;
            }
        }
        return iuhfService.write_area(area, addr, passwd, cf);
    }

    class ReadTagDialog extends Dialog implements
            android.view.View.OnClickListener {

        private Button Ok;
        private Button Cancle;
        private TextView EPC;
        private TextView Status;
        private EditText Read_Addr;
        private EditText Read_Count;
        private EditText Password;

        public ReadTagDialog(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.read);

            Ok = (Button) findViewById(R.id.btn_read_ok);
            Ok.setOnClickListener(this);
            Cancle = (Button) findViewById(R.id.btn_read_cancle);
            Cancle.setOnClickListener(this);

            EPC = (TextView) findViewById(R.id.textView_read_epc);
            EPC.setText(current_tag_epc);
            Status = (TextView) findViewById(R.id.textView_read_status);

            Read_Addr = (EditText) findViewById(R.id.editText_read_addr);
            Read_Count = (EditText) findViewById(R.id.editText_read_count);
            Password = (EditText)findViewById(R.id.editText_rp);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == Ok) {
                String str_addr = Read_Addr.getText().toString();
                String str_count = Read_Count.getText().toString();
                String str_passwd = Password.getText().toString();
                int num_addr;
                int num_count;
                //int passwd;
                long passwd;
                try {
                    num_addr = Integer.parseInt(str_addr, 16);
                    num_count = Integer.parseInt(str_count, 10);
                    //passwd = Integer.parseInt(str_passwd, 16);
                    passwd = Long.parseLong(str_passwd, 16);
                } catch (NumberFormatException p) {
                    Status.setText(R.string.Status_InvalidNumber);
                    return;
                }
                int which_choose = Area_Select.getSelectedItemPosition();
                String res = read_card(which_choose, num_addr, num_count * 2, (int)passwd);
                if (res == null) {
                    Status.setText(R.string.Status_Read_Card_Faild);
                } else {
                    Tag_Content.setText(res);
                    R2000demoISO6C.this.Status
                            .setText(R.string.Status_Read_Card_Ok);
                    dismiss();
                }
            } else if (v == Cancle) {
                dismiss();
            }
        }
    }

    private String read_card(int area, int addr, int count, int passwd)
    {
        byte[] v = iuhfService.read_area(area, addr, count, passwd);
        if(v == null)
        {
            return null;
        }
        String j = new String();
        for(byte i : v)
        {
            j += String.format("%02x ", i);
        }
        return j;
    }

    class SearchTagDialog extends Dialog implements
            android.view.View.OnClickListener, OnItemClickListener {

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

        public SearchTagDialog(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
            cont = context;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.setreader);

            Cancle = (Button) findViewById(R.id.btn_search_cancle);
            Cancle.setOnClickListener(this);
            Action = (Button) findViewById(R.id.btn_search_action);
            Action.setOnClickListener(this);

            cbb = (CheckBox)findViewById(R.id.checkBox_beep);

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
                public void handleMessage(Message msg)
                {
                    super.handleMessage(msg);
                    if (msg.what == 1) {
                        scant++;
                        if(!cbb.isChecked())
                        {
                            if(scant % 50 == 0)
                            {
                                soundPool.play(soundId, 1, 1, 0, 0, 1);
                            }
                        }
                        ArrayList<Tag_Data> ks = (ArrayList<Tag_Data>) msg.obj;
                        String tmp[] = new String[ks.size()];
                        for(int i = 0; i < ks.size(); i++)
                        {
                            byte[] nq = ks.get(i).epc;
                            if(nq != null)
                            {
                                tmp[i] = new String();
                                for(int j = 0; j < nq.length; j++)
                                {
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
                                if(cbb.isChecked())
                                {
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
            current_tag_epc = firm.get(arg2).epc;
            Cur_Tag_Info.setText("  " + firm.get(arg2).epc);
            R2000demoISO6C.this.Status
                    .setText(R.string.Status_Select_Card_Ok);
            dismiss();
            int res = select_UHF(firm.get(arg2).epc);
            if (res == 0) {
                current_tag_epc = firm.get(arg2).epc;
                Cur_Tag_Info.setText("  " + firm.get(arg2).epc);
                R2000demoISO6C.this.Status
                        .setText(R.string.Status_Select_Card_Ok);
                dismiss();
            } else {
                Status.setText(R.string.Status_Select_Card_Faild);
            }
        }
    }

    class SetPasswordDialog extends Dialog implements
            android.view.View.OnClickListener {

        private String[] passwd_list = { "Kill Password", "Access Password" };
        private Button Ok;
        private Button Cancle;
        private TextView EPC;
        private TextView Status;
        private Spinner area_select;
        private EditText access_passwd;
        private EditText new_passwd;
        private ArrayAdapter<String> setadapter;

        public SetPasswordDialog(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.setpassword);

            Ok = (Button) findViewById(R.id.btn_setpawd_ok);
            Ok.setOnClickListener(this);
            Cancle = (Button) findViewById(R.id.btn_setpawd_cancle);
            Cancle.setOnClickListener(this);

            EPC = (TextView) findViewById(R.id.textView_setpawd_epc);
            EPC.setText(current_tag_epc);
            Status = (TextView) findViewById(R.id.textView_setpawd_status);

            access_passwd = (EditText) findViewById(R.id.editText_setpawd_accesspd);
            new_passwd = (EditText) findViewById(R.id.editText_setpawd_newpd);

            setadapter = new ArrayAdapter<String>(this.getContext(),
                    android.R.layout.simple_spinner_item, passwd_list);
            setadapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            area_select = (Spinner) findViewById(R.id.spinner_setpawd_paswd);
            area_select.setAdapter(setadapter);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == Ok) {
                String cur_pass = access_passwd.getText().toString();
                String new_pass = new_passwd.getText().toString();
                int which = area_select.getSelectedItemPosition();
                int reval = set_Password(which, cur_pass,
                        new_pass);
                if (reval == 0) {
                    dismiss();
                } else if (reval == -1) {
                    Status.setText(R.string.Status_Write_Error);
                } else if (reval == -2) {
                    Status.setText(R.string.Status_Passwd_Length_Error);
                } else if (reval == -3) {
                    Status.setText(R.string.Status_Content_Length_Error);
                } else if (reval == -4) {
                    Status.setText(R.string.Status_InvalidNumber);
                } else if (reval == -5) {
                    Status.setText(R.string.Status_Wrong_Password_Type);
                }
            } else if (v == Cancle) {
                dismiss();
            }
        }
    }

    class SetModuleDialog extends Dialog implements android.view.View.OnClickListener {

        private final String[] freq_area_item = {"840-845", "920-925", "902-928", "865-868", "..."};
        private final String[] inv_mode_item = {"fast", "smart", "low", "custom", "..."};

        private Button setf, setm, sett, back;
        private TextView status;
        private EditText iwt, irt;
        private Spinner lf, lm;
        private boolean seted = false;

        private Button setp;
        private EditText pv;

        public SetModuleDialog(Context context)
        {
            super(context);
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.sss);

            setf = (Button)findViewById(R.id.button_set_region);
            setf.setOnClickListener(this);

            setm = (Button)findViewById(R.id.button_invmode);
            setm.setOnClickListener(this);

            sett = (Button)findViewById(R.id.button_custom_time);
            sett.setOnClickListener(this);
            sett.setEnabled(false);

            back = (Button)findViewById(R.id.button_set_back);
            back.setOnClickListener(this);

            status = (TextView)findViewById(R.id.textView_set_status);

            iwt = (EditText)findViewById(R.id.editText_inv_wt);
            irt = (EditText)findViewById(R.id.editText_inv_rt);

            setp = (Button)findViewById(R.id.button_set_antenna);
            setp.setOnClickListener(this);
            setp.setEnabled(false);

            pv = (EditText)findViewById(R.id.editText_antenna);

            lf = (Spinner)findViewById(R.id.spinner_region);
            ArrayAdapter<String> tmp = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item, freq_area_item);
            tmp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            lf.setAdapter(tmp);

            lm = (Spinner)findViewById(R.id.spinner_invmode);
            tmp = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item, inv_mode_item);
            tmp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            lm.setAdapter(tmp);

            if (modle.equals("FEILIXIN")){
                setm.setEnabled(false);
            }
            if (modle.equals("XINLIAN")){
                setm.setEnabled(false);
            }
            lm.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    // TODO Auto-generated method stub
                    if(position == 3)
                    {
                        iwt.setEnabled(true);
                        irt.setEnabled(true);
                        sett.setEnabled(true);
                        INV_TIME ace = iuhfService.get_inventory_time();
                        if(ace == null)
                        {
                            iwt.setText("");
                            irt.setText("");
                            status.setText("read custom mode time failed");
                        }
                        else
                        {
                            iwt.setText("" + ace.work_t);
                            irt.setText("" + ace.rest_t);
                        }
                    }
                    else
                    {
                        iwt.setEnabled(false);
                        irt.setEnabled(false);
                        sett.setEnabled(false);
                        iwt.setText("");
                        irt.setText("");
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });

            int re = iuhfService.get_freq_region();
            if(re == iuhfService.REGION_CHINA_920_925)
            {
                lf.setSelection(1, true);
            }
            else if(re == iuhfService.REGION_CHINA_840_845)
            {
                lf.setSelection(0, true);
            }
            else if(re == iuhfService.REGION_CHINA_902_928)
            {
                lf.setSelection(2, true);
            }
            else if(re == iuhfService.REGION_EURO_865_868)
            {
                lf.setSelection(3, true);
            }
            else
            {
                lf.setSelection(4, true);
                status.setText("read region setting read failed");
                Log.e("r2000_kt45", "read region setting read failed");
            }

            re = iuhfService.get_inventory_mode();
            if(re == iuhfService.FAST_MODE)
            {
                lm.setSelection(0, true);
                Log.i("r2000_kt45", "fast");
            }
            else if(re == iuhfService.SMART_MODE)
            {
                lm.setSelection(1, true);
                Log.i("r2000_kt45", "smart");
            }
            else if(re == iuhfService.LOW_POWER_MODE)
            {
                lm.setSelection(2, true);
                Log.i("r2000_kt45", "low");
            }
            else if(re == iuhfService.USER_MODE)
            {
                Log.i("r2000_kt45", "custom");
                lm.setSelection(3, true);
            }
            else
            {
                lm.setSelection(4, true);
                status.setText("inv mode setting read failed");
                Log.e("r2000_kt45", "inv mode setting read failed");
            }

            int ivp = iuhfService.get_antenna_power();
            if(ivp > 0)
            {
                setp.setEnabled(true);
                pv.setText("" + ivp);
            }
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(v == setf)
            {
                int freq_region = lf.getSelectedItemPosition();
                if(freq_region >= 4)
                {
                    status.setText("Invalid select");
                }
                else
                {
                    if(iuhfService.set_freq_region(freq_region) < 0)
                    {
                        status.setText("set freq region failed");
                    }
                    else
                    {
                        status.setText("set freq region ok");
                        back.setText("update settings");
                        this.setCancelable(false);
                        if (modle.equals("R2000")){
                            seted = true;
                        }
                    }
                }

            }
            else if(v == setm)
            {
                int invm = lm.getSelectedItemPosition();
                if(invm >= 4)
                {
                    status.setText("Invalid select");
                }
                else
                {
                    if(iuhfService.set_inventory_mode(invm) < 0)
                    {
                        status.setText("set inventory mode failed");
                    }
                    else
                    {
                        status.setText("set invetory mode ok");
                        back.setText("update settings");
                        this.setCancelable(false);
                        if (modle.equals("R2000")){
                            seted = true;
                        }
                    }
                }
            }
            else if(v == sett)
            {
                String siwt = iwt.getText().toString();
                String sirt = irt.getText().toString();
                int isiwt, isirt;
                try
                {
                    isiwt = Integer.parseInt(siwt, 10);
                    isirt = Integer.parseInt(sirt, 10);
                }
                catch (NumberFormatException e)
                {
                    status.setText("Not a vaild number");
                    return;
                }
                if((isiwt < 50) || (isiwt > 2000))
                {
                    status.setText("work time value range is 50 ~ 2000");
                    return;
                }
                if(iuhfService.set_inventory_time(isiwt, isirt) < 0)
                {
                    status.setText("set inventory time failed");
                }
                else
                {
                    status.setText("set invetory time ok");
                    back.setText("update settings");
                    this.setCancelable(false);
                    if (modle.equals("R2000")){
                        seted = true;
                    }

                }
            }
            else if(v == back)
            {
                new toast_thread().setr("update settings now").start();
                if(seted == true)
                {
                    iuhfService.MakeSetValid();
                    iuhfService.CloseDev();
                    iuhfService.OpenDev();
                    seted=false;
                    dismiss();
                }
                else
                {
                    dismiss();
                }
            }
            else if(v == setp)
            {
                int ivp = Integer.parseInt(pv.getText().toString());
                if((ivp < 0) || (ivp > 30))
                {
                    status.setText("value range is 0 ~ 30");
                }
                else
                {
                    int rv = iuhfService.set_antenna_power(ivp);
                    if(rv < 0)
                    {
                        status.setText("set antenna power failed");
                    }
                    else
                    {
                        status.setText("set antenna power ok");
                        back.setText("update settings");
                        this.setCancelable(false);
                        if (modle.equals("R2000")){
                            seted = true;
                        }
                    }
                }
            }
        }

        private class toast_thread extends Thread {

            String a;

            public toast_thread setr(String m)
            {
                a = m;
                return this;
            }

            public void run() {
                super.run();
                Looper.prepare();
                Toast.makeText(getApplicationContext(), a, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }
    }

    class LockTagDialog extends Dialog implements
            android.view.View.OnClickListener {

        private String[] area_list = { "Kill password", "Access password",
                "EPC", "TID", "USER" };
        private String[] style_list = { "Unlock", "Lock", "Permaunlock",
                "Permalock" };
        private Button Ok;
        private Button Cancle;
        private TextView EPC;
        private TextView Status;
        private Spinner area;
        private Spinner style;
        private EditText passwd;
        private ArrayAdapter<String> setadapter;

        public LockTagDialog(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.locktag);

            Ok = (Button) findViewById(R.id.btn_lock_ok);
            Ok.setOnClickListener(this);
            Cancle = (Button) findViewById(R.id.btn_lock_cancle);
            Cancle.setOnClickListener(this);

            EPC = (TextView) findViewById(R.id.textView_lock_epc);
            EPC.setText(current_tag_epc);
            Status = (TextView) findViewById(R.id.textView_lock_status);

            passwd = (EditText) findViewById(R.id.editText_lock_passwd);

            setadapter = new ArrayAdapter<String>(this.getContext(),
                    android.R.layout.simple_spinner_item, area_list);
            setadapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            area = (Spinner) findViewById(R.id.spinner_lock_area);
            area.setAdapter(setadapter);

            setadapter = new ArrayAdapter<String>(this.getContext(),
                    android.R.layout.simple_spinner_item, style_list);
            setadapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            style = (Spinner) findViewById(R.id.spinner_lock_style);
            style.setAdapter(setadapter);

        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == Ok) {
                int area_nr = area.getSelectedItemPosition();
                int style_nr = style.getSelectedItemPosition();
                String ps = passwd.getText().toString();
                int reval = -1;
                try
                {
                    long passwd = Long.parseLong(ps, 16);
                    Log.i("as3992", "set lock to area " + area_nr + " type to "
                            + style_nr + " " + passwd);
                    reval = iuhfService.setlock(style_nr, area_nr, (int)passwd);
                }
                catch (NumberFormatException e)
                {
                    Status.setText("Not a vaild Hex number");
                    return;
                }
                if (reval == 0) {
                    dismiss();
                }
                else {
                    Status.setText(R.string.Status_Lock_Card_Error);
                }
            } else if (v == Cancle) {
                dismiss();
            }
        }
    }

    class SetEPCDialog extends Dialog implements
            android.view.View.OnClickListener {

        private Button Ok;
        private Button Cancle;
        private TextView EPC;
        private TextView Status;
        private EditText passwd;
        private EditText newepc;
        private EditText newepclength;

        public SetEPCDialog(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.setepc);

            Ok = (Button) findViewById(R.id.btn_epc_ok);
            Ok.setOnClickListener(this);
            Cancle = (Button) findViewById(R.id.btn_epc_cancle);
            Cancle.setOnClickListener(this);

            EPC = (TextView) findViewById(R.id.textView_epc_epc);
            EPC.setText(current_tag_epc);
            Status = (TextView) findViewById(R.id.textView_epc_status);

            passwd = (EditText) findViewById(R.id.editText_epc_passwd);
            newepc = (EditText) findViewById(R.id.editText_epc_newepc);
            newepclength = (EditText) findViewById(R.id.editText_epc_epclength);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (v == Ok) {
                String password = passwd.getText().toString();
                String epc_str = newepc.getText().toString();
                int epcl;
                try {
                    epcl = Integer.parseInt(newepclength.getText().toString(),
                            10);
                } catch (NumberFormatException e) {
                    Log.e("as3992", "parse int error in set epc");
                    return;
                }

                StringTokenizer sepc = new StringTokenizer(epc_str);
                if(epcl > sepc.countTokens())
                {
                    Status.setText(R.string.Status_Content_Length_Error);
                    return;
                }

                byte[] eepc = new byte[sepc.countTokens()];

                int index = 0;
                while (sepc.hasMoreTokens()) {
                    try {
                        int ak = Integer.parseInt(sepc.nextToken(), 16);
                        if(ak > 0xff)
                        {
                            throw new NumberFormatException("Can't bigger than 0xff");
                        }
                        eepc[index++] = (byte)ak;
                    } catch (NumberFormatException p) {
                        Log.e("as3992", "parse int error in set epc: " + p.getMessage());
                        Status.setText("Invalid epc str");
                        return;
                    }
                }

                int reval = set_EPC(epcl, password, eepc);

                if (reval == 0) {
                    dismiss();
                } else if (reval == -1) {
                    Status.setText(R.string.Status_Write_Error);
                } else if (reval == -2) {
                    Status.setText(R.string.Status_Passwd_Length_Error);
                } else if (reval == -3) {
                    Status.setText(R.string.Status_Content_Length_Error);
                } else if (reval == -4) {
                    Status.setText(R.string.Status_InvalidNumber);
                } else if (reval == -5) {
                    Status.setText(R.string.Status_Read_Pc_Error);
                }
            } else if (v == Cancle) {
                dismiss();
            }

        }

    }

    int select_UHF(String epc) {

        Log.i("r2000", "selec epc " + epc);
        StringTokenizer sepc = new StringTokenizer(epc);
        byte[] eepc = new byte[sepc.countTokens()];
        int index = 0;
        while (sepc.hasMoreTokens()) {
            try {
                eepc[index++] = (byte) Integer.parseInt(sepc.nextToken(), 16);
            } catch (NumberFormatException p) {
                return -1;
            }
        }
        int retry = 0;
        if(iuhfService.select_card(eepc) != 0)
        {
            return -1;
        }
        return 0;
    }

    int set_EPC(int epclength, String passwd, byte[] EPC) {
        byte[] res;
        int pa;
        int retry = 0;
        //int pss = 0;
        long pss = 0;
        if(epclength > 31)
        {
            return -3;
        }
        if(epclength * 2 < EPC.length)
        {
            return -3;
        }
        try
        {
            //pss = Integer.parseInt(passwd, 16);
            pss = Long.parseLong(passwd, 16);
        }
        catch (NumberFormatException e)
        {
            return -4;
        }
        res = iuhfService.read_area(iuhfService.EPC_A, 1, 2, 0);
        if(res == null)
        {
            return -5;
        }
        res[0] = (byte) ((res[0] & 0x7) | (epclength << 3));
        byte[] f = new byte[2 + epclength * 2];
        System.arraycopy(res, 0, f, 0, 2);
        System.arraycopy(EPC, 0, f, 2, epclength * 2);
        return iuhfService.write_area(iuhfService.EPC_A, 1, (int)pss, f);
    }

    int set_Password(int which, String cur_pass, String new_pass) {
        if (which > 1 || which < 0) {
            return -5;
        }
        try
        {
            long cp = Long.parseLong(cur_pass, 16);
            if((cp > 0xffffffffL ) || (cp < 0))
            {
                throw new NumberFormatException("can't bigger than 0xffffffff");
            }
            long np = Long.parseLong(new_pass, 16);
            if((np > 0xffffffffL ) || (np < 0))
            {
                throw new NumberFormatException("can't bigger than 0xffffffff");
            }

            byte[] nps = new byte[4];
            nps[3] = (byte)((np >> 0) & 0xff);
            nps[2] = (byte)((np >> 8) & 0xff);
            nps[1] = (byte)((np >> 16) & 0xff);
            nps[0] = (byte)((np >> 24) & 0xff);
            return iuhfService.write_area(iuhfService.RESERVED_A, which * 2, (int)cp, nps);
        }
        catch (NumberFormatException e)
        {
            return -5;
        }
    }
}
