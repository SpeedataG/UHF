package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.bean.INV_TIME;
import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.R;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class SetModuleDialog extends Dialog implements android.view.View.OnClickListener {

    private final String[] freq_area_item = {"840-845", "920-925", "902-928", "865-868", "..."};
    private final String[] inv_mode_item = {"fast", "smart", "low", "custom", "..."};
    private Button setf, setm, sett, back;
    private TextView status;
    private EditText iwt, irt;
    private Spinner lf, lm;
    private boolean seted = false;
    private Button setp;
    private EditText pv;
    private IUHFService iuhfService;
    private String model;
    private Context mContext;


    public SetModuleDialog(Context context,IUHFService iuhfService,String model) {
        super(context);
        this.iuhfService=iuhfService;
        this.model=model;
        this.mContext=context;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sss);

        setf = (Button) findViewById(R.id.button_set_region);
        setf.setOnClickListener(this);

        setm = (Button) findViewById(R.id.button_invmode);
        setm.setOnClickListener(this);

        sett = (Button) findViewById(R.id.button_custom_time);
        sett.setOnClickListener(this);
        sett.setEnabled(false);

        back = (Button) findViewById(R.id.button_set_back);
        back.setOnClickListener(this);

        status = (TextView) findViewById(R.id.textView_set_status);

        iwt = (EditText) findViewById(R.id.editText_inv_wt);
        irt = (EditText) findViewById(R.id.editText_inv_rt);

        setp = (Button) findViewById(R.id.button_set_antenna);
        setp.setOnClickListener(this);
        setp.setEnabled(false);

        pv = (EditText) findViewById(R.id.editText_antenna);

        lf = (Spinner) findViewById(R.id.spinner_region);
        ArrayAdapter<String> tmp = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item, freq_area_item);
        tmp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lf.setAdapter(tmp);

        lm = (Spinner) findViewById(R.id.spinner_invmode);
        tmp = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item, inv_mode_item);
        tmp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lm.setAdapter(tmp);

        if (model.equals("feilixin")) {
            setm.setEnabled(false);
        }
        if (model.equals("xinlian")) {
            setm.setEnabled(false);
        }
        if (model.equals("as3992")){
            setm.setEnabled(false);
        }
        lm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // TODO Auto-generated method stub
                if (position == 3) {
                    iwt.setEnabled(true);
                    irt.setEnabled(true);
                    sett.setEnabled(true);
                    INV_TIME ace = iuhfService.get_inventory_time();
                    if (ace == null) {
                        iwt.setText("");
                        irt.setText("");
                        status.setText("read custom mode time failed");
                    } else {
                        iwt.setText("" + ace.work_t);
                        irt.setText("" + ace.rest_t);
                    }
                } else {
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
        if (re == iuhfService.REGION_CHINA_920_925) {
            lf.setSelection(1, true);
        } else if (re == iuhfService.REGION_CHINA_840_845) {
            lf.setSelection(0, true);
        } else if (re == iuhfService.REGION_CHINA_902_928) {
            lf.setSelection(2, true);
        } else if (re == iuhfService.REGION_EURO_865_868) {
            lf.setSelection(3, true);
        } else {
            lf.setSelection(4, true);
            status.setText("read region setting read failed");
            Log.e("r2000_kt45", "read region setting read failed");
        }

//        re = iuhfService.get_inventory_mode();
//        if (re == iuhfService.FAST_MODE) {
//            lm.setSelection(0, true);
//            Log.i("r2000_kt45", "fast");
//        } else if (re == iuhfService.SMART_MODE) {
//            lm.setSelection(1, true);
//            Log.i("r2000_kt45", "smart");
//        } else if (re == iuhfService.LOW_POWER_MODE) {
//            lm.setSelection(2, true);
//            Log.i("r2000_kt45", "low");
//        } else if (re == iuhfService.USER_MODE) {
//            Log.i("r2000_kt45", "custom");
//            lm.setSelection(3, true);
//        } else {
//            lm.setSelection(4, true);
//            status.setText("inv mode setting read failed");
//            Log.e("r2000_kt45", "inv mode setting read failed");
//        }

        int ivp = iuhfService.get_antenna_power();
        if (ivp > 0) {
            setp.setEnabled(true);
            pv.setText("" + ivp);
        }
        if (model.equals("as3992")){
            pv.setHint("0关天线1开天线");
            setp.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == setf) {
            int freq_region = lf.getSelectedItemPosition();
            if (freq_region >= 4) {
                status.setText("Invalid select");
            } else {
                if (iuhfService.set_freq_region(freq_region) < 0) {
                    status.setText("set freq region failed");
                } else {
                    status.setText("set freq region ok");
                    back.setText("update settings");
                    this.setCancelable(false);
//                    if (model.equals("r2k")) {
//                        seted = true;
//                    }
                }
            }

        } else if (v == setm) {
            int invm = lm.getSelectedItemPosition();
            if (invm >= 4) {
                status.setText("Invalid select");
            } else {
                if (iuhfService.set_inventory_mode(invm) < 0) {
                    status.setText("set inventory mode failed");
                } else {
                    status.setText("set invetory mode ok");
                    back.setText("update settings");
                    this.setCancelable(false);
//                    if (model.equals("R2000")) {
//                        seted = true;
//                    }
                }
            }
        } else if (v == sett) {
            String siwt = iwt.getText().toString();
            String sirt = irt.getText().toString();
            int isiwt, isirt;
            try {
                isiwt = Integer.parseInt(siwt, 10);
                isirt = Integer.parseInt(sirt, 10);
            } catch (NumberFormatException e) {
                status.setText("Not a vaild number");
                return;
            }
            if ((isiwt < 50) || (isiwt > 2000)) {
                status.setText("work time value range is 50 ~ 2000");
                return;
            }
            if (iuhfService.set_inventory_time(isiwt, isirt) < 0) {
                status.setText("set inventory time failed");
            } else {
                status.setText("set invetory time ok");
                back.setText("update settings");
                this.setCancelable(false);
//                if (model.equals("R2000")) {
//                    seted = true;
//                }

            }
        } else if (v == back) {
            new toast_thread().setr("update settings now").start();
            if (seted == true) {
                iuhfService.MakeSetValid();
                iuhfService.CloseDev();
                iuhfService.OpenDev();
                seted = false;
                dismiss();
            } else {
                dismiss();
            }
        } else if (v == setp) {
            int ivp = Integer.parseInt(pv.getText().toString());
            if ((ivp < 0) || (ivp > 30)) {
                status.setText("value range is 0 ~ 30");
            } else {
                int rv = iuhfService.set_antenna_power(ivp);
                if (rv < 0) {
                    status.setText("set antenna power failed");
                } else {
                    status.setText("set antenna power ok");
                    back.setText("update settings");
                    this.setCancelable(false);
//                    if (model.equals("R2000")) {
//                        seted = true;
//                    }
                }
            }
        }
    }

    private class toast_thread extends Thread {

        String a;

        public toast_thread setr(String m) {
            a = m;
            return this;
        }

        public void run() {
            super.run();
            Looper.prepare();
            Toast.makeText(mContext, a, Toast.LENGTH_LONG).show();
            Looper.loop();
        }
    }
}
