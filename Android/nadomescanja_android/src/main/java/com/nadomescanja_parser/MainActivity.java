package com.nadomescanja_parser;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Primitive;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.common.collect.Ordering;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Map<String,String> dbContent;
    ConnectivityHelper internet = new ConnectivityHelper();


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.refresh:
                if((internet.isConnectedToNetwork(this))) {
                    File[] file_for_deletion = this.getFilesDir().listFiles();
                    for (int x = 0; x < file_for_deletion.length; x++) {
                        file_for_deletion[x].delete();
                    }
                    new DownloadLinksTask().execute();
                }
                else{
                    new DownloadLinksTask().execute();
                    Toast.makeText(this, "No internet, can't refresh", Toast.LENGTH_SHORT).show();
                }}
                return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("In sendVIČ veritas");

        if((internet.isConnectedToNetwork(this))){
            File[] file_for_deletion = this.getFilesDir().listFiles();
            for (int x = 0; x<file_for_deletion.length ;x++){
                file_for_deletion[x].delete();
            }
        }
        else{
            Toast.makeText(this, "No internet! Data may be old!", Toast.LENGTH_SHORT).show();
        }

        LinearLayoutManager linearVertical = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        List<String> arListView = new ArrayList<>();

        final ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,arListView);
        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(adapter);


        new DownloadLinksTask().execute();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String filename = adapter.getItem(i).toString();
                String url = dbContent.get(filename);
                File file = MainActivity.this.getFilesDir();

                if (new File(file,filename).exists()){
                    File file1 = new File(file, filename);
                    Intent intent = new Intent(MainActivity.this, PDFVIEW.class);
                    intent.putExtra("pdf", file1);
                    startActivity(intent);
                }
                else{
                    if(downloadFilesTask.getStatus() == AsyncTask.Status.RUNNING) {
                        Toast.makeText(MainActivity.this,"Download already running!", Toast.LENGTH_SHORT);
                        Log.e("ifstavk", "[E ENKRAT");
                    }
                    else if(internet.isConnectedToNetwork(MainActivity.this)){
                        downloadFilesTask = (DownloadFilesTask) new DownloadFilesTask().execute(url, filename);
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Connect to the internet.", Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.sort(Ordering.natural());

            }
        });




        //Dynamo dynamo = new Dynamo();
        //dynamo.parseFromDB(this);
        //PDFView pdfView1 = (PDFView) findViewById(R.id.pdfView);

    }

    void updateLinks(Map<String, String> downloaded_result){
        dbContent = downloaded_result;
    }

    void updatedbContent(String filename){
        //dbContent.put(filename, null);
    }


    private class DownloadFilesTask extends AsyncTask<String, Integer, String> implements DialogInterface.OnDismissListener {
        ProgressDialog progress = new ProgressDialog(MainActivity.this);
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setOnDismissListener(this);
            progress.setTitle("Downloading...");
            progress.show();
        }

        protected String doInBackground(String... strings) {
            Log.e("dyno", "starting");
            long i = 404;
            String filename = strings[1];

            try {
                        URL url = new URL(strings[0]);
                        URLConnection conection = url.openConnection();
                        conection.connect();
                        int lenghtOfFile = conection.getContentLength();
                        InputStream input = new BufferedInputStream(url.openStream(), 8192);
                        FileOutputStream outputStream;
                        byte data[] = new byte[1024];
                        long total = 0;

                        try {
                            outputStream = MainActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
                            int count = 0;
                            while ((count = input.read(data)) != -1) {
                                total += count;
                                outputStream.write(data, 0, count);
                            }
                            outputStream.flush();
                            outputStream.close();
                            input.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } catch (MalformedURLException e) {
                        Log.e("dyno", e.toString());
                    } catch (java.io.IOException e) {
                        Log.e("dyno", e.toString());
                    }

            return filename;
        }

        protected void onPostExecute(String result) {
            progress.hide();
            MainActivity.this.updatedbContent(result);
            File file = MainActivity.this.getFilesDir();
            if (new File(file,result).exists()){
                File file1 = new File(file, result);
                Intent intent = new Intent(MainActivity.this, PDFVIEW.class);
                intent.putExtra("pdf", file1);
                startActivity(intent);
            }

        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            Log.e("d2no", "CANCELED DOWNLOAD");
            this.cancel(true);
        }
    }
    private class DownloadLinksTask extends AsyncTask<List<String>, Integer, List<String>> {

        ProgressDialog progress = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setTitle("refreshing..");
            progress.show();
        }

        protected List<String> doInBackground(List<String>... params) {
            long i = 404;

            Map<String, String> dbContent = getURLs();
            List<String> adapter_names = new ArrayList<String>();
            if (dbContent != null){
                for (Map.Entry<String, String> entry : dbContent.entrySet()){
                    adapter_names.add(entry.getKey());
                }
            }


            return adapter_names;
        }

        protected void onPostExecute(List<String> result) {
            progress.hide();
            ListView myListView = (ListView) MainActivity.this.findViewById(R.id.listview);
            ArrayAdapter adapter = (ArrayAdapter) myListView.getAdapter();
            adapter.clear();
            adapter.addAll(result);
            adapter.sort(Ordering.natural());
            adapter.notifyDataSetChanged();

        }

        Map<String, String> getURLs() {
            Map<String, String> dbContent = new HashMap<String, String>();
            File file = MainActivity.this.getFilesDir();
            int a = file.listFiles().length;
            for (int y = 0; y < a; y++){
                Log.e("dyno1", file.list()[y]);
                dbContent.put(file.list()[y], null);
            }
            Log.e("d2no", "listed dir");

            try {
                boolean net = internet.isConnectedToNetwork(MainActivity.this);
                Log.e("d2no", String.valueOf(net));
                if(net) {
                    String COGNITO_POOL_ID = " .... ";
                    Regions COGNITO_REGION = Regions.EU_CENTRAL_1;
                    final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(getApplicationContext(), COGNITO_POOL_ID, COGNITO_REGION);
                    final AmazonDynamoDBClient dbClient = new AmazonDynamoDBClient(credentialsProvider);
                    dbClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));


                    Table dbTable = Table.loadTable(dbClient, "nadomescanja_links");
                    Map<String, AttributeValue> id = dbTable.getItem(new Primitive("0", true)).toAttributeMap();
                    String timestamp = id.get("current").getN();
                    Map<String, AttributeValue> data = dbTable.getItem(new Primitive(timestamp, true)).toAttributeMap().get("links").getM();


                    for (Map.Entry<String, AttributeValue> entry : data.entrySet()) {
                        String value = entry.getValue().getS();
                        value = value.substring(0, (value.length() - 1)) + "1";
                        dbContent.put(entry.getKey(), value);
                    }
                    Log.e("d2no", "tryed parsing");
                }

            }
            catch(Exception e){
                Log.e("d2no", e.getMessage());
            }

            MainActivity.this.updateLinks(dbContent);
            return dbContent;
        }


    }



}