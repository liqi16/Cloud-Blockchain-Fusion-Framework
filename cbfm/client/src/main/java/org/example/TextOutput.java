package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dnl.utils.text.table.TextTable;
import org.hyperledger.fabric.gateway.Network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class TextOutput {

    /*
    //Tag
    public static Object[][] jsonObjectToData(String jsonString){
        JSONObject userObject = JSONObject.parseObject(jsonString);
        List<String> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for(String k:userObject.keySet()){
            keys.add(k);
            values.add(userObject.get(k));
        }

        Object[][] data = new Object[keys.size()][2];
        for (int i = 0; i < keys.size(); i++) {
            data[i][0] = keys.get(i);
        }
        for (int i = 0; i < keys.size(); i++) {
            data[i][1] = values.get(i);
        }
        return data;
    }
    public static String[] tagColumnNames(){
        return new String[] {"BLOCK_INDEX","TAG"};
    }


    //Cloud
    public static String[] cloudColumnNames(){
        return new String[] {"MSP","CLOUD_NAME","CLOUD_ID","IP","PORT"};
    }

    public static Object[][] cloudArrayToData(String allCloudJsonArrayString){
        JSONArray allUserJsonArray = JSON.parseArray(allCloudJsonArrayString);
        int size = allUserJsonArray.size();
        Object[][] data = new Object[size][cloudColumnNames().length];
        //System.out.println("用户信息:");
        for (int i = 0; i < size; i++){
            JSONObject jsonObject = allUserJsonArray.getJSONObject(i);
            data[i][0] = jsonObject.get("Msp");
            data[i][1] = jsonObject.get("Name");
            data[i][2] = jsonObject.get("CloudId");
            data[i][3] = jsonObject.get("IP");
            data[i][4] = jsonObject.get("Port");
        }
        return data;
    }

    //User
    public static String[] userColumnNames(){
        return new String[] {"MSP","USER_NAME","USER_ID","REPUTATION","ATTRIBUTE","PERIOD"};
    }

    public static Object[][] userArrayToData(String allUserJsonArrayString){

        JSONArray allUserJsonArray = JSON.parseArray(allUserJsonArrayString);
        int size = allUserJsonArray.size();
        Object[][] data = new Object[size][userColumnNames().length];
        //System.out.println("用户信息:");
        for (int i = 0; i < size; i++){
            JSONObject jsonObject = allUserJsonArray.getJSONObject(i);
            data[i][0] = jsonObject.get("Msp");
            data[i][1] = jsonObject.get("Username");
            data[i][2] = jsonObject.get("UserId");
            data[i][3] = jsonObject.get("Reputation");
            data[i][4] = jsonObject.get("Attribute");
            data[i][5] = jsonObject.get("Period");
        }
        return data;
    }

    //Operation
    public static String[] operationColumnNames(){
        return new String[] {
                "SHORT_RECORD",
                "OPERATOR_MSP",
                "OPERATOR",
                "OPERATION",
                "TIMESTAMP"};
    }

    public static Object[][] operationArrayToData(String allOperationJsonArrayString) throws Exception {

        JSONArray allDataJsonArray = JSON.parseArray(allOperationJsonArrayString);
        int size = allDataJsonArray.size();
        Object[][] data = new Object[size][operationColumnNames().length];
        //System.out.println("用户信息:");
        for (int i = 0; i < size; i++){
            JSONObject jsonObject = allDataJsonArray.getJSONObject(i);
            String datakey = jsonObject.getString("Datakey");
            //data[i][0] = datakey.split("\u0000")[2];
            //data[i][1] = datakey.split("\u0000")[3];
            //data[i][2] = datakey.split("\u0000")[4];
            data[i][0] = Utils.makeShortRecord(datakey.split("\u0000")[2],datakey.split("\u0000")[3],datakey.split("\u0000")[4]);
            data[i][1] = jsonObject.get("OperatorMsp");
            data[i][2] = jsonObject.get("Operator");
            data[i][3] = jsonObject.get("Operation");
            data[i][4] = jsonObject.get("Timestamp");
            //System.out.println(jsonObject.toString());
            //System.out.println();
        }
        return data;
    }

    //Data
    public static String[] dataColumnNames(){
        return new String[] {
                "SHORT_RECORD",
                "OWNER_MSP",
                "OWNER_NAME",
                "CLOUD_MSP",
                "CLOUD",
                "POLICY",
                "#BLOCKS",
                "UPDATE_TIME",
                "INTRODUCTION"};
    }


    public static Object[][] dataArrayToData(String allDataJsonArrayString) throws Exception {

        JSONArray allDataJsonArray = JSON.parseArray(allDataJsonArrayString);
        int size = allDataJsonArray.size();
        Object[][] data = new Object[size][dataColumnNames().length];
        //System.out.println("用户信息:");
        for (int i = 0; i < size; i++){
            JSONObject jsonObject = allDataJsonArray.getJSONObject(i);
            data[i][0] = Utils.makeShortRecord(jsonObject.getString("Channel"),jsonObject.getString("OwnerId"),jsonObject.getString("Name"));
            data[i][1] = jsonObject.get("OwnerMsp");
            data[i][2] = jsonObject.get("Owner");
            data[i][3] = jsonObject.get("CloudMsp");
            data[i][4] = jsonObject.get("Cloud");
            data[i][5] = jsonObject.get("Policy");
            data[i][6] = jsonObject.get("N");
            data[i][7] = jsonObject.get("UpdateTime");
            data[i][8] = jsonObject.get("Introduction");
        }
        return data;
    }

    //Share
    public static String[] auditColumnNames(){
        return new String[] {
                "SHORT_RECORD",
                "OWNER_MSP",
                "OWNER_NAME",
                "CLOUD_MSP",
                "CLOUD",
                "AUDITOR_MSP",
                "AUDITOR",
                "AUDIT_BLOCKS",
                "AUDIT_TIME",
                "RESULT"
        };
    }

    public static Object[][] auditArrayToData(String allAuditJsonArrayString){

        JSONArray allAuditJsonArray = JSON.parseArray(allAuditJsonArrayString);
        int size = allAuditJsonArray.size();
        Object[][] data = new Object[size][auditColumnNames().length];
        //System.out.println("用户信息:");
        for (int i = 0; i < size; i++){
            JSONObject jsonObject = allAuditJsonArray.getJSONObject(i);
            //System.out.println(jsonObject);
            String datakey = jsonObject.getString("datakey");
            //System.out.println(datakey);
            data[i][0] = Utils.makeShortRecord(datakey.split("\u0000")[2],datakey.split("\u0000")[3],datakey.split("\u0000")[4]);
            data[i][1] = jsonObject.get("ownerMsp");
            data[i][2] = jsonObject.get("owner");
            data[i][3] = jsonObject.get("cloudMsp");
            data[i][4] = jsonObject.get("cloud");
            data[i][5] = jsonObject.get("auditorMsp");
            data[i][6] = jsonObject.get("auditor");
            data[i][7] = jsonObject.get("c");
            data[i][8] = jsonObject.get("timestamp");
            data[i][9] = jsonObject.get("result");
            //System.out.println(jsonObject.toString());
            //System.out.println();
        }
        return data;
    }

    //Share
    public static String[] shareColumnNames(){
        return new String[] {
                "SHORT_RECORD",
                "DATA_OWNER_MSP",
                "DATA_OWNER",
                "DATA_USER_MSP",
                "DATA_USER",
                "TIME_STAMP"};
    }

    public static Object[][] shareArrayToData(String allJsonArrayString){

        JSONArray allJsonArray = JSON.parseArray(allJsonArrayString);
        int size = allJsonArray.size();
        Object[][] data = new Object[size][shareColumnNames().length];
        //System.out.println("用户信息:");
        for (int i = 0; i < size; i++){
            JSONObject jsonObject = allJsonArray.getJSONObject(i);
            String datakey = jsonObject.getString("Datakey");
            data[i][0] = Utils.makeShortRecord(datakey.split("\u0000")[2],datakey.split("\u0000")[3],datakey.split("\u0000")[4]);
            data[i][1] = jsonObject.get("OwnerMsp");
            data[i][2] = jsonObject.get("DataOwner");
            data[i][3] = jsonObject.get("UserMsp");
            data[i][4] = jsonObject.get("DataUser");
            data[i][5] = jsonObject.get("Timestamp");
            //System.out.println(jsonObject.toString());
            //System.out.println();
        }
        return data;
    }

    public static void tableOutput(String tableName, String[] columnNames, Object[][] data)
    {
        System.out.println();
        System.out.println(tableName);
        TextTable textTable = new TextTable(columnNames, data);
        textTable.printTable();
    }*/

    public static void jsonOutput(String jsonName,String jsonString){
        System.out.println();
        System.out.println(jsonName);
        System.out.println(jsonString);
    }

}
