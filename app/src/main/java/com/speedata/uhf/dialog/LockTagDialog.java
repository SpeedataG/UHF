package com.speedata.uhf.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.uhf.MsgEvent;
import com.speedata.uhf.R;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class LockTagDialog extends Dialog implements
        android.view.View.OnClickListener {

    private String[] area_list = {"Kill password", "Access password",
            "EPC", "TID", "USER"};
    private String[] style_list = {"Unlock", "Lock", "Permaunlock",
            "Permalock"};
    private Button Ok;
    private Button Cancle;
    private TextView EPC;
    private TextView Status;
    private Spinner area;
    private Spinner style;
    private EditText passwd;
    private ArrayAdapter<String> setadapter;
    private IUHFService iuhfService;
    private String current_tag_epc;
    private String model;

    public LockTagDialog(Context context,IUHFService iuhfService
            ,String current_tag_epc,String model) {
        super(context);
        // TODO Auto-generated constructor stub
        this.iuhfService=iuhfService;
        this.current_tag_epc=current_tag_epc;
        this.model=model;
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
            try {
                long passwd;
                if (model.equals("FEILIXIN")) {
                    passwd = Long.parseLong(ps);
                } else {
                    passwd = Long.parseLong(ps, 16);
                }
                Log.i("as3992", "set lock to area " + area_nr + " type to "
                        + style_nr + " " + passwd);
                reval = iuhfService.setlock(style_nr, area_nr, (int) passwd);
            } catch (NumberFormatException e) {
                Status.setText("Not a vaild Hex number");
                return;
            }
            if (reval == 0) {
                EventBus.getDefault().post(new MsgEvent("lock_Status",""));
                dismiss();
            } else {
                Status.setText(R.string.Status_Lock_Card_Error);
            }
        } else if (v == Cancle) {
            dismiss();
        }
    }
}
