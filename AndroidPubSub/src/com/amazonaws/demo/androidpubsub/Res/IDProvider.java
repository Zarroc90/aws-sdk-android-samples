package com.amazonaws.demo.androidpubsub.Res;


import android.content.Context;
import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.demo.androidpubsub.LoginActivity;
import com.amazonaws.demo.androidpubsub.PubSubActivity;
import com.amazonaws.regions.Regions;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class IDProvider {

    private Context mContext;
    private LoginActivity logincontext;
    private Map<String, String> logins = new HashMap<String, String>();
    private static CognitoCachingCredentialsProvider credentialsProvider;
    private Boolean isReady = Boolean.FALSE;
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "acqx6akcdcn9n-ats.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-central-1:4b38968f-863d-4286-a967-8dfd62fb754c";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_CENTRAL_1;



    //Setter

    public void setLogins(Map<String, String> logins) {
        this.logins = logins;
    }

    public void setCredentialsProvider(CognitoCachingCredentialsProvider credentialsProvider){
        this.credentialsProvider = credentialsProvider;
    }

    public void setLogincontext(LoginActivity logincontext){
        this.logincontext = logincontext;
    }

    //Getter

    public Map<String, String> getLogins() {
        return logins;
    }

    public CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    public Boolean getReady() {
        return isReady;
    }

    //Additionals

    public IDProvider(Context context){
        this.mContext=context;
    }

    public void authenticateWithLogin (Context context) {
        this.mContext=context;
       credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );
        new AuthenticateTask(this,logincontext).execute();

    }

    private static class AuthenticateTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<IDProvider> idProviderReference;
        private WeakReference<LoginActivity> loginReference;

        AuthenticateTask(IDProvider idProvidercontext,LoginActivity logincontext){
            idProviderReference = new WeakReference<>(idProvidercontext);
            loginReference = new WeakReference<>(logincontext);
        }



        @Override
        protected Void doInBackground(Void... voids) {
            IDProvider idProvider = idProviderReference.get();
            idProvider.credentialsProvider.setLogins(idProvider.logins);
            idProvider.credentialsProvider.refresh();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            IDProvider idProvider = idProviderReference.get();
            idProvider.isReady = Boolean.TRUE;
            LoginActivity loginActivity = loginReference.get();
            loginActivity.loginSuccesful();
            idProvider.setCredentialsProvider(idProvider.credentialsProvider);
            super.onPostExecute(aVoid);
        }
    }

}
