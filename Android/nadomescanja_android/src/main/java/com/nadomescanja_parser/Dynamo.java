package com.nadomescanja_parser;

import android.app.Application;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.UpdateItemOperationConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Document;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.DynamoDBEntry;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.DynamoDBList;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Primitive;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Dynamo extends AppCompatActivity {

    Boolean goturls = false;
    JSONObject dbContent;

    public void parseFromDB(Context context){
        String COGNITO_POOL_ID = "eu-central-1:40f7bbe7-18a3-45ae-aef2-383d9cb91124";
        Regions COGNITO_REGION = Regions.EU_CENTRAL_1;
        final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(context, COGNITO_POOL_ID, COGNITO_REGION);
        final AmazonDynamoDBClient dbClient = new AmazonDynamoDBClient(credentialsProvider);
        dbClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));


        new Thread(new Runnable() {
            @Override
            public void run() {
                Table dbTable = Table.loadTable(dbClient,"nadomescanja_links");
                Map<String, AttributeValue> id = dbTable.getItem(new Primitive("0", true)).toAttributeMap();
                String timestamp = id.get("current").getN();
                Map<String, AttributeValue> data = dbTable.getItem(new Primitive(timestamp, true)).toAttributeMap().get("links").getM();
                dbContent = new JSONObject();

                for (Map.Entry<String, AttributeValue> entry : data.entrySet()) {
                    try {
                        dbContent.accumulate(entry.getKey(), entry.getValue().getS());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                goturls = true;
                Log.e("dyno",dbContent.toString());
            }
        }).start();

    }

}
