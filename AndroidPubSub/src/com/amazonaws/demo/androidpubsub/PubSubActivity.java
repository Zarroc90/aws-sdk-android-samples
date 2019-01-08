/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsub;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCognitoIdentityProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.IdentityChangedListener;
import com.amazonaws.demo.androidpubsub.Res.IDProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPolicyRequest;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.AttachThingPrincipalRequest;
import com.amazonaws.services.iot.model.AttachThingPrincipalResult;
import com.amazonaws.services.iot.model.AttributePayload;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.amazonaws.services.iot.model.CreateThingRequest;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iot.model.Tag;
import com.amazonaws.services.iot.model.TargetSelection;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.task.__IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.EspNetUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.MqttToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.amazonaws.demo.androidpubsub.Res.Statics.authRequest;

public class PubSubActivity extends AppCompatActivity implements WiFiPWDialogFragment.WiFiPWDialogListener {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();
    private static final String TAG = "PoppWifi";

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "acqx6akcdcn9n-ats.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-central-1:4b38968f-863d-4286-a967-8dfd62fb754c";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "IP_44_Policy";
    private static final String IDENTITY_POOL_ID = "eu-central-1:4b38968f-863d-4286-a967-8dfd62fb754c";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_CENTRAL_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "Keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "innovus2008!" ;
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "key0";

    CognitoCachingCredentialsProvider credentialsProvider;
    AWSCognitoIdentityProvider awsCognitoIdentityProvider;
    AWSIotDataClient iotDataClient;
    AWSIotClient mIotAndroidClient;
    static AWSIotMqttManager mqttManager;
    AWSIotClient mqttAwsIotClient;

    EditText txtSubcribe;
    EditText txtTopic;
    EditText txtMessage;

    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    Button btnSubscribe;
    Button btnPublish;
    Button btnDisconnect;


    String clientId;
    String mSsid;
    String bSsid;

    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;
    ListThingsResult listThingsResult;
    ListThingsRequest listThingsRequest;
    GetThingShadowRequest getThingShadowRequest;
    GetThingShadowResult getThingShadowResult;

    private ArrayList<String> mdeviceNames = new ArrayList<>();
    private ArrayList<String> mdeviceImages = new ArrayList<>();
    private ArrayList<String> mpowerText = new ArrayList<>();
    private ArrayList<Boolean> mbuttonState = new ArrayList<>();

    private EsptouchAsyncTask4 mTask;
    private boolean mDestroyed = false;
    private static final int REQUEST_PERMISSION = 0x01;
    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(PubSubActivity.this, text,
                        Toast.LENGTH_LONG).show();
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDestroyed = true;
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
        }
    }


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        listThingsResult= new ListThingsResult();
        listThingsRequest = new ListThingsRequest();
        getThingShadowRequest =new GetThingShadowRequest();
        getThingShadowResult = new GetThingShadowResult();
        TextView wifiSSID = findViewById(R.id.wifissid);
        EditText editText = findViewById(R.id.password);



        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(intent, authRequest);
        setContentView(R.layout.recycler_view);


        if (Build.VERSION.SDK_INT >= 28) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };

                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
            } else {
                registerBroadcastReceiver();
            }

        } else {
            registerBroadcastReceiver();
        }

        initRecyclerView();
        final FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("ssid",mSsid);
                bundle.putString("bssid",bSsid);
                WiFiPWDialogFragment wiFiPWDialogFragment = new WiFiPWDialogFragment();
                wiFiPWDialogFragment.show(getSupportFragmentManager(),"Test");
                wiFiPWDialogFragment.setArguments(bundle);


                /*
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CreateThingRequest createThingRequest = new CreateThingRequest();
                        createThingRequest.setThingName("IP44_Plug_80:7D:3A:66:3A:6C");
                        createThingRequest.setThingTypeName("PowerPlug");
                        AttributePayload attributePayload = new AttributePayload();
                        attributePayload.addattributesEntry("ProductionNr","061220180100001");
                        attributePayload.addattributesEntry("model","ip44");
                        attributePayload.addattributesEntry("user",clientId);
                        createThingRequest.setAttributePayload(attributePayload);
                        mIotAndroidClient.createThing(createThingRequest);
                        AttachThingPrincipalRequest attachThingPrincipalRequest = new AttachThingPrincipalRequest();
                        attachThingPrincipalRequest.setThingName("IP44_Plug_80:7D:3A:66:3A:6C");
                        attachThingPrincipalRequest.setPrincipal("arn:aws:iot:eu-central-1:370334120503:cert/f05bd2c0e7f41f1368016f4d9bd4491c66ccc62a8c390f4dd161b345fb195625");
                        mIotAndroidClient.attachThingPrincipal(attachThingPrincipalRequest);
                        listThingsRequest.setAttributeName("user");
                        listThingsRequest.setAttributeValue(clientId);
                        listThingsResult=mIotAndroidClient.listThings(listThingsRequest);
                        System.out.println("Things: " + listThingsResult.toString());

                        int numberofThings = listThingsResult.getThings().size();

                        if(numberofThings>0){
                            for(int i=0;i<=numberofThings;i++){
                                mdeviceNames.add(listThingsResult.getThings().get(i).getThingName());
                                mdeviceImages.add(listThingsResult.getThings().get(i).getAttributes().get("model"));
                                mpowerText.add("18.5 W");
                                mbuttonState.add(Boolean.TRUE);
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        int j = recyclerViewAdapter.getItemCount();
                                        recyclerViewAdapter.notifyItemInserted(j+1);
                                    }
                                });

                            }
                        }

                    }
                }).start(); */


            }
        });
        super.onCreate(savedInstanceState);
    }

    private void initRecyclerView(){
        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new RecyclerViewAdapter(mdeviceNames,mdeviceImages,mpowerText,mbuttonState,this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case authRequest:{
                switch (resultCode){
                    case Activity.RESULT_OK:{
                        String result = data.getStringExtra("result");
                        System.out.println("Result: "+result);


                        IDProvider idProvider = new IDProvider(getApplicationContext());
                        credentialsProvider = idProvider.getCredentialsProvider();
                        /*iotDataClient = new AWSIotDataClient(credentialsProvider);
                        String iotDataEndpoint = CUSTOMER_SPECIFIC_ENDPOINT;
                        iotDataClient.setEndpoint(iotDataEndpoint);*/
                        // IoT Client (for creation of certificate if needed)
                        Region region = Region.getRegion(MY_REGION);
                        mIotAndroidClient = new AWSIotClient(credentialsProvider);
                        iotDataClient = new AWSIotDataClient(credentialsProvider);
                        iotDataClient.setEndpoint(CUSTOMER_SPECIFIC_ENDPOINT);
                        iotDataClient.setRegion(region);
                        mIotAndroidClient.setRegion(region);
                        clientId = credentialsProvider.getIdentityId();
                        Log.d(LOG_TAG, "clientId = " + clientId);
                        mqttManager = new AWSIotMqttManager("AndroidApp", CUSTOMER_SPECIFIC_ENDPOINT);
                        mqttManager.connect(credentialsProvider,new AWSIotMqttClientStatusCallback() {
                            @Override
                            public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                        final Throwable throwable) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (status == AWSIotMqttClientStatus.Connecting) {
                                            Toast.makeText(getApplicationContext(), "Connecting...",Toast.LENGTH_SHORT).show();
                                            //tvStatus.setText("Connecting...");

                                        } else if (status == AWSIotMqttClientStatus.Connected) {
                                            Toast.makeText(getApplicationContext(), "Connected",Toast.LENGTH_SHORT).show();
                                            //tvStatus.setText("Connected");

                                        } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                            if (throwable != null) {
                                                Log.e(LOG_TAG, "Connection error.", throwable);
                                                Toast.makeText(getApplicationContext(), "Connection error.",Toast.LENGTH_SHORT).show();
                                            }
                                            //tvStatus.setText("Reconnecting");
                                        } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                            if (throwable != null) {
                                                Log.e(LOG_TAG, "Connection error.", throwable);
                                                Toast.makeText(getApplicationContext(), "Connection error.",Toast.LENGTH_SHORT).show();
                                            }
                                            //tvStatus.setText("Disconnected");
                                            Toast.makeText(getApplicationContext(), "Disconnected",Toast.LENGTH_SHORT).show();
                                        } else {
                                            //tvStatus.setText("Disconnected");
                                            Toast.makeText(getApplicationContext(), "Disconnected",Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                });

                            }
                        });
                        mqttManager.setAutoReconnect(true);


                        //tvClientId.setText(clientId);
                        //btnConnect.setEnabled(true);

                        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider = new CognitoCachingCredentialsProvider(getApplicationContext(), IDENTITY_POOL_ID, MY_REGION);
                        Map<String, String> logins = new HashMap<String, String>();
                        logins.put("cognito-idp.eu-central-1.amazonaws.com/eu-central-1:4b38968f-863d-4286-a967-8dfd62fb754c", "Test-User");
                        cognitoCachingCredentialsProvider.setLogins(logins);




                        initRecyclerView();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                AttachPolicyRequest attachPolicyRequest = new AttachPolicyRequest();
                                attachPolicyRequest.setPolicyName("AllowAll");
                                attachPolicyRequest.setTarget(clientId);
                                mIotAndroidClient.attachPolicy(attachPolicyRequest);

                                listThingsRequest.setAttributeName("user");
                                listThingsRequest.setAttributeValue(clientId);
                                listThingsResult=mIotAndroidClient.listThings(listThingsRequest);

                                System.out.println(listThingsResult.toString());

                                int numberofThings = listThingsResult.getThings().size();

                                if(numberofThings>0){
                                    for(int i=0;i<numberofThings;i++){
                                        mdeviceNames.add(listThingsResult.getThings().get(i).getThingName());
                                        mdeviceImages.add(listThingsResult.getThings().get(i).getAttributes().get("model"));
                                        mpowerText.add("18.5 W");
                                        getThingShadowRequest.setThingName(listThingsResult.getThings().get(i).getThingName());
                                        String ServiceName= iotDataClient.getEndpoint();
                                        getThingShadowRequest.setThingName(listThingsResult.getThings().get(i).getThingName());
                                        getThingShadowResult = iotDataClient.getThingShadow(getThingShadowRequest);
                                        String jsonShadow = new String(getThingShadowResult.getPayload().array(),StandardCharsets.UTF_8);
                                        JSONObject object,report;
                                        JSONArray reported;
                                        String status="";
                                        try {
                                            object = new JSONObject(jsonShadow);
                                            status=object.getJSONObject("state").getJSONObject("reported").getString("switch");
                                            reported = object.getJSONArray("state");
                                            report = reported.getJSONObject(0);
                                            //status=report.getString("status");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        Boolean plugstatus;
                                        if(status.contains("on")){
                                            plugstatus=true;
                                        }else{
                                            plugstatus=false;
                                        }

                                        mbuttonState.add(plugstatus);


                                        String mytopic ="";
                                        if(mytopic.isEmpty()||mytopic==null){
                                            mytopic="$aws/things/"+ listThingsResult.getThings().get(i).getThingName()+"/shadow/update";
                                        }
                                        final String topic = mytopic;


                                        Log.d(LOG_TAG, "topic = " + topic);

                                        try {
                                            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                                                    new AWSIotMqttNewMessageCallback() {
                                                        @Override
                                                        public void onMessageArrived(final String topic, final byte[] data) {
                                                            try {
                                                                String message = new String(data, "UTF-8");
                                                                JSONObject object,report;
                                                                JSONArray reported;
                                                                String status="";
                                                                try {
                                                                    object = new JSONObject(message);
                                                                    status=object.getJSONObject("state").getJSONObject("reported").getString("switch");
                                                                } catch (JSONException e) {
                                                                    e.printStackTrace();
                                                                }

                                                                if(status.contains("on")){
                                                                    for(int i=0;i<mdeviceNames.size();i++){
                                                                        final int itemNr =i;
                                                                        if(topic.contains(mdeviceNames.get(i))){
                                                                            mbuttonState.set(i,true);
                                                                            runOnUiThread(new Runnable() {

                                                                                @Override
                                                                                public void run() {

                                                                                    recyclerViewAdapter.notifyItemChanged(itemNr);

                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                }
                                                                if(status.contains("off")){
                                                                    for(int i=0;i<mdeviceNames.size();i++){
                                                                        final int itemNr =i;
                                                                        if(topic.contains(mdeviceNames.get(i))){
                                                                            mbuttonState.set(i,false);
                                                                            runOnUiThread(new Runnable() {

                                                                                @Override
                                                                                public void run() {

                                                                                    recyclerViewAdapter.notifyItemChanged(itemNr);

                                                                                }
                                                                            });
                                                                        }
                                                                    }
                                                                }
                                                            } catch (UnsupportedEncodingException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            Log.e(LOG_TAG, "Subscription error.", e);
                                        }


                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {

                                                int j = recyclerViewAdapter.getItemCount();
                                                recyclerViewAdapter.notifyItemInserted(j+1);

                                            }
                                        });
                                    }
                                }


                            }
                        }).start();




                        break;
                    }
                    case Activity.RESULT_CANCELED:{
                        //Not logged in
                        break;
                    }
                }
                break;
            }
            default:{
                break;
            }
        }
    }



    public void publishMqttMessage (String msg,String topic, AWSIotMqttQos quality){

        mqttManager.publishString(msg,topic,quality);

    }

     @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        /*byte[] ssid = ByteUtil.getBytesByString("Z-Wave");
        byte[] password = ByteUtil.getBytesByString("ZWESMHT2018!");
        byte [] bssid = ByteUtil.getBytesByString("78:8a:20:f4:55:2a");
        byte[] deviceCount = ByteUtil.getBytesByString("1");
        byte[] broadcast = ByteUtil.getBytesByString("1");
        if(mTask != null) {
            mTask.cancelEsptouch();
        }
        mTask = new EsptouchAsyncTask4(this);
        mTask.execute(ssid, bssid, password, deviceCount, broadcast);*/
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    public static class EsptouchAsyncTask4 extends AsyncTask<byte[], Void, List<IEsptouchResult>> {
        private WeakReference<PubSubActivity> mActivity;

        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private final Object mLock = new Object();
        private ProgressDialog mProgressDialog;
        private android.support.v7.app.AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;

        EsptouchAsyncTask4(PubSubActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        void cancelEsptouch() {
            cancel(true);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = mActivity.get();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage("Your Wifi Device is configuring, please wait for a moment...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (__IEsptouchTask.DEBUG) {
                            Log.i(TAG, "progress dialog back pressed canceled");
                        }
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getText(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            synchronized (mLock) {
                                if (__IEsptouchTask.DEBUG) {
                                    Log.i(TAG, "progress dialog cancel button canceled");
                                }
                                if (mEsptouchTask != null) {
                                    mEsptouchTask.interrupt();
                                }
                            }
                        }
                    });
            mProgressDialog.show();
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            PubSubActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(activity.myListener);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            PubSubActivity activity = mActivity.get();
            mProgressDialog.dismiss();
            mResultDialog = new android.support.v7.app.AlertDialog.Builder(activity)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            mResultDialog.setCanceledOnTouchOutside(false);
            if (result == null) {
                mResultDialog.setMessage("Create task failed, the port could be used by other thread");
                mResultDialog.show();
                return;
            }

            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {

                    //StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        activity.addDevice(resultInList.getBssid());
                        /*sb.append("Esptouch success, bssid = ")
                                .append(resultInList.getBssid())
                                .append(", InetAddress = ")
                                .append(resultInList.getInetAddress().getHostAddress())
                                .append("\n");*/
                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
                    if (count < result.size()) {
                        /*sb.append("\nthere's ")
                                .append(result.size() - count)
                                .append(" more result(s) without showing\n");*/
                    }
                    //mResultDialog.setMessage(sb.toString());
                } else {
                    mResultDialog.setMessage("Esptouch fail");
                    mResultDialog.show();
                }

                //
            }

            activity.mTask = null;
        }
    }

    void addDevice(final String macAddress){

        new Thread(new Runnable() {
            @Override
            public void run() {
                char divisionChar = ':';
                String mac = macAddress.replaceAll("(.{2})", "$1"+divisionChar).substring(0,17);
                String thingname= "IP44_Plug_"+mac.toUpperCase();
                CreateThingRequest createThingRequest = new CreateThingRequest();
                createThingRequest.setThingName(thingname);
                createThingRequest.setThingTypeName("PowerPlug");
                AttributePayload attributePayload = new AttributePayload();
                attributePayload.addattributesEntry("ProductionNr","061220180100001");
                attributePayload.addattributesEntry("model","ip44");
                attributePayload.addattributesEntry("user",clientId);
                createThingRequest.setAttributePayload(attributePayload);
                mIotAndroidClient.createThing(createThingRequest);
                AttachThingPrincipalRequest attachThingPrincipalRequest = new AttachThingPrincipalRequest();
                attachThingPrincipalRequest.setThingName(thingname);
                attachThingPrincipalRequest.setPrincipal("arn:aws:iot:eu-central-1:370334120503:cert/f05bd2c0e7f41f1368016f4d9bd4491c66ccc62a8c390f4dd161b345fb195625");
                mIotAndroidClient.attachThingPrincipal(attachThingPrincipalRequest);
                String topic = "$aws/things/"+thingname+"/shadow/update";
                String msg = "{\"state\":{\"desired\":{\"switch\":\""+"off"+"\"}}}";
                publishMqttMessage(msg,topic,AWSIotMqttQos.QOS0);
                /*listThingsRequest.setAttributeName("user");
                listThingsRequest.setAttributeValue(clientId);
                listThingsResult=mIotAndroidClient.listThings(listThingsRequest);
                System.out.println("Things: " + listThingsResult.toString());*/

                String mytopic ="";
                if(mytopic.isEmpty()||mytopic==null){
                    mytopic="$aws/things/"+ thingname+"/shadow/update";
                }
                final String topic1 = mytopic;


                Log.d(LOG_TAG, "topic = " + topic1);

                try {
                    mqttManager.subscribeToTopic(topic1, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic1, final byte[] data) {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        JSONObject object,report;
                                        JSONArray reported;
                                        String status="";
                                        try {
                                            object = new JSONObject(message);
                                            status=object.getJSONObject("state").getJSONObject("reported").getString("switch");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        if(status.contains("on")){
                                            for(int i=0;i<mdeviceNames.size();i++){
                                                final int itemNr =i;
                                                if(topic1.contains(mdeviceNames.get(i))){
                                                    mbuttonState.set(i,true);
                                                    runOnUiThread(new Runnable() {

                                                        @Override
                                                        public void run() {

                                                            recyclerViewAdapter.notifyItemChanged(itemNr);

                                                        }
                                                    });
                                                }
                                            }
                                        }
                                        if(status.contains("off")){
                                            for(int i=0;i<mdeviceNames.size();i++){
                                                final int itemNr =i;
                                                if(topic1.contains(mdeviceNames.get(i))){
                                                    mbuttonState.set(i,false);
                                                    runOnUiThread(new Runnable() {

                                                        @Override
                                                        public void run() {

                                                            recyclerViewAdapter.notifyItemChanged(itemNr);

                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Subscription error.", e);
                }

                mdeviceNames.add(thingname);
                mdeviceImages.add("ip44");
                mpowerText.add("0.0 W");
                mbuttonState.add(Boolean.TRUE);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        int j = recyclerViewAdapter.getItemCount();
                        recyclerViewAdapter.notifyItemInserted(j+1);
                    }
                });




            }
        }).start();

    }


    private boolean mReceiverRegistered = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    onWifiChanged(wifiInfo);
                    break;
            }
        }
    };


    private void onWifiChanged(WifiInfo info) {
        if (info == null) {


            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
                new android.support.v7.app.AlertDialog.Builder(PubSubActivity.this)
                        .setMessage("Wifi disconnected or changed")
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        } else {
            String ssid = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            mSsid=ssid;
            String bssid = info.getBSSID();
            bSsid =bssid;
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        mReceiverRegistered = true;
    }
}
