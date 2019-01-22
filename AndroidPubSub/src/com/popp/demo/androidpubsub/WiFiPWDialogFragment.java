package com.popp.demo.androidpubsub;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.popp.demo.androidpubsub.R;
import com.espressif.iot.esptouch.util.ByteUtil;

import java.lang.ref.WeakReference;

public class WiFiPWDialogFragment extends AppCompatDialogFragment {

    private TextView wifiSSID;
    private WeakReference<PubSubActivity> mActivity;
    private EditText epassword;
    private PubSubActivity.EsptouchAsyncTask4 mTask;
    String mssid = "";
    String mbssid = "";


    public interface WiFiPWDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    WiFiPWDialogListener mListener;



    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder;

        try {
            mssid=getArguments().getString("ssid");
            mbssid=getArguments().getString("bssid");
        }catch (Exception e){
            e.printStackTrace();
        }

        final PubSubActivity activity = (PubSubActivity) getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.wifi_pw,null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.signin, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        String pass = epassword.getText().toString();
                        byte[] ssid = ByteUtil.getBytesByString(mssid);
                        byte[] password = ByteUtil.getBytesByString(pass);
                        byte [] bssid = ByteUtil.getBytesByString(mbssid);
                        byte[] deviceCount = ByteUtil.getBytesByString("1");
                        byte[] broadcast = ByteUtil.getBytesByString("1");
                        if(mTask != null) {
                            mTask.cancelEsptouch();
                        }
                        mTask = new PubSubActivity.EsptouchAsyncTask4(activity);
                        mTask.execute(ssid, bssid, password, deviceCount, broadcast);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        WiFiPWDialogFragment.this.getDialog().cancel();
                    }
                });



        wifiSSID = view.findViewById(R.id.wifissid);
        wifiSSID.setText("SSID: "+mssid);
        epassword = view.findViewById(R.id.password);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (WiFiPWDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement NoticeDialogListener");
        }

    }


}
