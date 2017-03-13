package com.idey.expofb4;

import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String MY_BUCKET = "expofb-store";
    private static final String TAG = "Expo Feedback:";
    private Timer timer = new Timer();
    private Set<Integer> processedIds = new HashSet<>();
    private static int cnt = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG,"Creating stuff");
        timer.schedule(new TimerTask(){

            @Override
            public void run() {{
                Uri uriSMSURI = Uri.parse("content://sms/inbox");
                Cursor cur = getContentResolver().query(uriSMSURI, null, null, null, null);
                String sms = "";
                boolean b = true;
                Log.i(TAG,"Starting pass :"+cnt++);
                while (cur.moveToNext()) {
            /*if (b) {
                for (int i = 0; i < cur.getColumnCount(); i++) {
                    sms += (cur.getColumnName(i) + ",i=" + i + "," + cur.getString(i) + "\n");
                }
                b = false;
            }
            sms += "From :" + cur.getString(2) +
                    " : Body:" + cur.getString(11) +
                    " : Read:" + cur.getString(6) + "\n";
                    */
                    //Log.i(TAG,"_id:"+cur.getString(0)+",value:"+cur.getString(12)+",read:"+cur.getString(7));
                    if("0".equals(cur.getString(7))){//!read
                        Log.i(TAG,"processedId?"+processedIds.contains(cur.getString(0)));
                        Log.i(TAG,"_id:"+cur.getString(0)+",value:"+cur.getString(12)+",read:"+cur.getString(7));
                        if(!processedIds.contains(cur.getString(0))){

                            File tmpFile = getTmpFile(cur.getString(0),cur.getString(12));
                            storeMsg(cur.getString(0),tmpFile);
                            processedIds.add(Integer.valueOf(cur.getString(0)));
                        }

                    }
                }

            }

            }
        },10000,120000);
    }
    public void storeMsg(String key, File contentFile){

        AmazonS3 s3 = new AmazonS3Client(getCredentialsProvider());
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());
        ObjectMetadata myObjectMetadata = new ObjectMetadata();

        TransferObserver observer = transferUtility.upload(
                MY_BUCKET,     /* The bucket to upload to */
                key,    /* The key for the uploaded object */
                contentFile        /* The file where the data to upload exists */
        );
        
        observer.setTransferListener(new TransferListener(){
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal){
                Log.i(TAG,"id:"+id+",bytesCurrent:"+bytesCurrent+",bytesTotal:"+bytesTotal);
            }
            public void onStateChanged(int id, TransferState state){
                Log.i(TAG, "id:"+id+",state:"+state);
            }
            public void onError(int id, Exception ex){
                Log.e(TAG, ex.getMessage());
                ex.printStackTrace();
            }
        });
        Log.i(TAG, "storeMsg: Successfully started transferring contentFile:"+contentFile.getAbsolutePath());
        // Create a record in a dataset and synchronize with the server
       /*
        Dataset dataset = syncClient.openOrCreateDataset("myDataset");
        dataset.put(key, value);
        dataset.synchronize(new DefaultSyncCallback() {
            @Override
            public void onSuccess(Dataset dataset, List newRecords) {
                //Your handler code here
            }
        });*/
    }

    private File getTmpFile(String key, String value) {
        File outputDir = getApplicationContext().getCacheDir(); // context being the Activity pointer
        File outputFile = null;
        try{
            outputFile = File.createTempFile(key, "tmp", outputDir);
            BufferedWriter bw = null;
            try{
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
                bw.write(value);
                Log.i(TAG, "getTmpFile: Successfully wrote aaa value:"+value+"to tmpFile:"+outputFile.getAbsolutePath());
            } finally {
                if(bw!=null)
                    bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile;
    }



    private CognitoCachingCredentialsProvider getCredentialsProvider(){
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:61e75f65-e9e7-4567-b915-xxxxxxxxxxxx", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        return credentialsProvider;
    }
/*
    public CognitoSyncManager getSyncClient(CognitoCachingCredentialsProvider credentialsProvider) {
        // Initialize the Cognito Sync client
        CognitoSyncManager syncClient = new CognitoSyncManager(
                getApplicationContext(),
                Regions.US_WEST_2, // Region
                credentialsProvider);
        return syncClient;
    }
    */


}