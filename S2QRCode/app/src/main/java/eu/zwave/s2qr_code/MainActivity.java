package eu.zwave.s2qr_code;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_scan:
                    IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                    integrator.setCaptureActivity(ScanActivity.class);
                    integrator.setOrientationLocked(true);
                    integrator.setBeepEnabled(true);
                    integrator.setPrompt("Scan S2 Code");
                    integrator.initiateScan();
                    return true;
                case R.id.navigation_codes:

                    return true;
                case R.id.navigation_history:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_codes);
    }

    @Override
    protected void onResume() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setSelectedItemId(R.id.navigation_codes);
        String sender;

        try {
            sender= Objects.requireNonNull(this.getIntent().getExtras()).getString("SENDER_KEY");
        }catch (Exception e){
            e.printStackTrace();
            sender=null;
        }


        //IF ITS THE FRAGMENT THEN RECEIVE DATA
        if(sender != null)
        {
            this.receiveData();

        }

        super.onResume();
    }

    private void receiveData()
    {
        Intent i = getIntent();
        String Key = i.getStringExtra("BARCODE_RESULT_Text");
        TextView S2Key = findViewById(R.id.textView_s2_code);
        TextView LeadIn = findViewById(R.id.textView_lead_in_value);
        TextView Version = findViewById(R.id.textView_version_value);
        TextView Checksum = findViewById(R.id.textView_checksum_value);
        TextView ReqKeys = findViewById(R.id.textView_req_key_value);
        TextView DSK = findViewById(R.id.textView_dsk_value);
        TextView TypeCrit = findViewById(R.id.textView_critical_value);
        TextView Len = findViewById(R.id.textView_len_value);
        TextView DevType = findViewById(R.id.textView_prod_dev_type_value);
        TextView ProdIcon = findViewById(R.id.textView_prod_icon_type_value);
        TextView TypeCrit2 = findViewById(R.id.textView_critical2_value);
        TextView Len2 = findViewById(R.id.textView_len2_value);
        TextView ManuID = findViewById(R.id.textView_manufacturer_value);
        TextView ProdType= findViewById(R.id.textView_prod_type_value);
        TextView ProdID= findViewById(R.id.textView_prod_id_value);
        TextView AppVersion= findViewById(R.id.textView_app_version_value);
        TextView TypeCrit3= findViewById(R.id.textView_critical3_value);
        TextView Len3= findViewById(R.id.textView_len3_value);
        TextView UUID= findViewById(R.id.textView_uuid_value);
        //S2Key.setText(Key);

        if(Key.matches("[0-9]{70,}")){
            String helper;
            LeadIn.setText(Key.substring(0,2));
            Version.setText(Key.substring(2,4));
            Checksum.setText(Key.substring(4,9));
            ReqKeys.setText(Key.substring(9,12));
            DSK.setText(enterAfter_x(Key.substring(12,52),5));
            TypeCrit.setText(Key.substring(52,54));
            Len.setText(Key.substring(54,56));
            helper=Integer.toHexString(Integer.parseInt(Key.substring(56,61)));
            helper=("0x"+helper.substring(0,helper.length()-2)+" 0x"+helper.substring(helper.length()-2,helper.length()));
            DevType.setText(helper);
            helper="0x"+Integer.toHexString(Integer.parseInt(Key.substring(61,66)));
            ProdIcon.setText(helper);
            TypeCrit2.setText(Key.substring(66,68));
            Len2.setText(Key.substring(68,70));
            int manufacturerId = Integer.parseInt(Key.substring(70,75));
            String manu = "h0x0"+Integer.toHexString(manufacturerId);
            int identifier;
            identifier = getResources().getIdentifier(manu,"string", "eu.zwave.s2qr_code");
            if(identifier!=0){
                manu=getResources().getString(identifier);
                ManuID.setText(manu);
                helper="0x"+Integer.toHexString(Integer.parseInt(Key.substring(80,85)));
                helper= manu+"\nDev: "+helper;
                S2Key.setText(helper);
            }else{
                manu=manu.replace("h0x0","0x");
                ManuID.setText(manu);
                helper="0x"+Integer.toHexString(Integer.parseInt(Key.substring(80,85)));
                helper= manu+"\nDev: "+helper;
                S2Key.setText(helper);
            }
            helper="0x"+Integer.toHexString(Integer.parseInt(Key.substring(75,80)));
            ProdType.setText(helper);
            helper="0x"+Integer.toHexString(Integer.parseInt(Key.substring(80,85)));
            ProdID.setText(helper);
            helper=Integer.toHexString(Integer.parseInt(Key.substring(85,90)));
            helper=("0x"+helper.substring(0,helper.length()-2)+" 0x"+helper.substring(helper.length()-2,helper.length()));
            AppVersion.setText(helper);
            if(Key.length()>90){
                TypeCrit3.setText(Key.substring(90,92));
                Len3.setText(Key.substring(92,94));
                String Uuid= Key.substring(94,96)+"\n"+enterAfter_x(Key.substring(96,136),5);
                UUID.setText(Uuid);
            }



        }

    }

    private String enterAfter_x (String string, int x){

        StringBuilder sb = new StringBuilder();
        char[] stringArray =string.toCharArray();

        int i = 0;
        while (i!=string.length()) {
            sb.append(stringArray[i]);
            i++;
            if((i % x)==0 && i!=0){
                sb.append("\n");
            }

        }

        return sb.toString();
    }

}
