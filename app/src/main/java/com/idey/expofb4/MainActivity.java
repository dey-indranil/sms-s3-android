package com.idey.expofb4;

import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final String MY_BUCKET = "expofb-store";
    private static final String TAG = "Expo Feedback:";
    private Timer timer = new Timer();
    private Set<Integer> processedIds = new HashSet<>();
    private Set<String> processedSenders = new HashSet<>();
    private static int cnt = 0;
    private List<Integer> processedVals = new ArrayList<>();
    private AtomicInteger processedCnt = new AtomicInteger(0);

    TextView txtView3;

    private final String NEW_LINE = System.getProperty("line.separator");
    private final String NEW_LINE2 = "\\r\\n";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG,"Creating stuff");
        setContentView(R.layout.activity_main);

        txtView3 = (TextView) findViewById(R.id.textView3);

        timer.schedule(new TimerTask(){

            @Override
            public void run() {{
                Uri uriSMSURI = Uri.parse("content://sms/inbox");
                Cursor cur = getContentResolver().query(uriSMSURI, null, null, null, null);
                String sms = "";
                boolean b = true;
                int procSize = processedVals.size();
                Log.i(TAG,"Starting pass :"+cnt+++",procSize:"+procSize);
                StringBuilder sbDtl = new StringBuilder();
                while (cur.moveToNext()) {
                    String smsBody = cur.getString(12);
                    if(smsBody==null || smsBody.trim().length()==0) continue;
                    //Log.i(TAG,"_id:"+cur.getString(0)+",value:"+cur.getString(12)+",read:"+cur.getString(7));
                    if(isExpoFb(smsBody)){//!read
                        Log.i(TAG,"processedId?"+processedIds.contains(cur.getString(0)));
                        Log.i(TAG,"_id:"+cur.getString(0)+",value:"+cur.getString(12)+",read:"+cur.getString(7));

                        if(!processedIds.contains(Integer.valueOf(cur.getString(0)))){
                            if(!processedSenders.contains(cur.getString(2))) {
                                processedVals.add(sanitizedInput(smsBody));
                                processedIds.add(Integer.valueOf(cur.getString(0)));
                                processedSenders.add(cur.getString(2));
                            }
                            sbDtl.append(cur.getString(2)).append(",").append(smsBody).append(NEW_LINE);
                        }
                        processedCnt.set(processedVals.size());
                    }
                }
                Log.i(TAG,"Ending pass :"+cnt+++",procSize:"+procSize+",procVals.size():"+processedVals.size());
                if(procSize!=processedVals.size()) {
                    File tmpFile = getTmpFile("expofb", createCsv(processedVals));
                    storeMsg("expofb",tmpFile);
                    File tmpFileDtl = getTmpFile("expofbdtl", sbDtl.toString());
                    storeMsg("expofbdtl",tmpFileDtl);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTextView();
                    }
                });
            }

            }
        },10000,120000);
    }

    private void updateTextView() {
        txtView3.setText(Integer.toString(processedCnt.get()));
    }

    private String createCsv(List<Integer> processedVals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Votes").append(NEW_LINE);
        for(int i:processedVals){
            sb.append(i).append(NEW_LINE);
        }
        return sb.toString();
    }

    private Integer sanitizedInput(String smsBody) {
        String remBody = smsBody.replaceAll("(?i)wmexpofb","");
        Integer ret = 0;
        try {
            ret = Integer.valueOf(remBody.trim());
        } catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return ret;
    }

    private boolean isExpoFb(String smsBody) {
        return smsBody!=null && smsBody.toLowerCase().contains("wmexpofb");
    }

    public void storeMsg(String key,File contentFile){

        AmazonS3 s3 = new AmazonS3Client(getCredentialsProvider());
        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());

        TransferObserver observer = transferUtility.upload(
                MY_BUCKET,     /* The bucket to upload to */
                key+".csv",    /* The key for the uploaded object */
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
            outputFile = File.createTempFile(key, ".tmp", outputDir);
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
                "us-east-1:61e75f65-e9e7-4567-b915-xxxxxxxxxxx", // Identity Pool ID
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