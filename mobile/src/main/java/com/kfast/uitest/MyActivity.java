package com.kfast.uitest;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import com.google.android.gms.location.ActivityRecognitionClient;


public class MyActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 1;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private PendingIntent mActivityRecognitionPendingIntent;

    private PendingResult<DataApi.DataItemResult> wearPendingResult;

    private GoogleApiClient wearClient;
    private GoogleApiClient recognitionClient;
    private ActivityRecognitionApi recognitionApi;

    private boolean mInProgress;

    private enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;

    private Button btnStop;
    private Button btnSend;

    private ImageView ivTestImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        btnStop = (Button) findViewById(R.id.btnStop);
        btnSend = (Button) findViewById(R.id.btnSend);

        ivTestImage = (ImageView) findViewById(R.id.ivTestImage);

        ivTestImage.setImageBitmap(getBitmapFromAssets());

        mInProgress = false;

        wearClient = new GoogleApiClient.Builder(this)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.d("wear api  mobile", "connected");
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Log.d("wear api mobile", "connection suspended");
                            }
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(ConnectionResult connectionResult) {
                                Log.d("wear api mobile", "connection failed");
                            }
                        })
                        .build();

        wearClient.connect();

        recognitionClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        recognitionApi = ActivityRecognition.ActivityRecognitionApi;

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopUpdates();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSendDataToWear();
            }
        });

        startUpdates();
    }

    private void startSendDataToWear(){
        PutDataMapRequest dataMap = PutDataMapRequest.create("/test");
        dataMap.getDataMap().putString("message", "this is test message");
        dataMap.getDataMap().putAsset("img", createAssetFromBitmap(getBitmapFromAssets()));
        dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
        PutDataRequest request = dataMap.asPutDataRequest();
        wearPendingResult = Wearable.DataApi.putDataItem(wearClient,  request);

        wearPendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d("wear pending result", "status: " + dataItemResult.getStatus().getStatus() + "result: " + dataItemResult.getDataItem().getUri());
                Toast.makeText(MyActivity.this, "status: " + dataItemResult.getStatus().getStatus() + "result: " + dataItemResult.getDataItem().getUri(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap getBitmapFromAssets(){
        AssetManager assetManager = getAssets();
        InputStream iStream = null;

        try{
            iStream = assetManager.open("chrome_icon.png");
        }catch (IOException e){
            e.printStackTrace();
        }

        return BitmapFactory.decodeStream(iStream);
    }

    private Asset createAssetFromBitmap(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);

        return Asset.createFromBytes(baos.toByteArray());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                //connect again

                switch (resultCode){
                    case RESULT_OK:
                        //request again
                        break;
                }
                break;
        }
    }

    private boolean servicesConnected(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if(resultCode == ConnectionResult.SUCCESS){
            Log.d("Play services", "Google Play Services available");

            return true;
        }else{
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if(errorDialog != null){
                ErrorDialogFragment errorFragment =  new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Google Play Services");
            }

            return false;
        }
    }

    public void startUpdates(){
        mRequestType = REQUEST_TYPE.START;

        if(!servicesConnected()){
            return;
        }

        if(!mInProgress){
            recognitionClient.connect();
        }else {

        }
    }

    public void stopUpdates(){
        mRequestType = REQUEST_TYPE.STOP;

        if(!servicesConnected()){
            return;
        }

        if(!mInProgress){
            mInProgress = true;
            recognitionClient.connect();
        }else{

        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        switch (mRequestType){
            case START:
//                recognitionApi.requestActivityUpdates(recognitionClient, DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);
                break;
            case STOP:
//                recognitionApi.removeActivityUpdates(recognitionClient, mActivityRecognitionPendingIntent);
                break;
        }

        mInProgress = false;
        recognitionClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        if(connectionResult.hasResolution()){
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }else{
            int errorCode = connectionResult.getErrorCode();
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if(errorDialog != null){
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Connection Failed");
            }
        }
    }

    public static class ErrorDialogFragment extends DialogFragment{
        private Dialog mDialog;

        public ErrorDialogFragment(){
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog){
            mDialog = dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
