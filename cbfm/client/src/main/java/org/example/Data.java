package org.example;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.gateway.*;

import java.io.File;
import java.security.PrivateKey;
import java.util.ArrayList;

import static org.example.ECC.imporSK;

public class Data {

    public static boolean checkIsOwner(String dataJSONString, String CurrentUser){
        JSONObject dataObject = JSONObject.parseObject(dataJSONString);
        String Owner = dataObject.getString("Owner");
        return Owner.equals(CurrentUser);
    }

    public static String generateTag(Network network, String userMsp, String username, String filePath) throws Exception {
        Contract contract = network.getContract("cbfm-go");

        String resultString = User.queryUser(network,userMsp,username);//contract.evaluateTransaction("queryUser", username);

        JSONObject userJsonObject = JSONObject.parseObject(resultString);

        JSONObject pkObject = userJsonObject.getJSONObject("AuditPk");

        String pkString = pkObject.toString();

        String userSecKeyFileName = User.getSecKeyFile(username);

        String skString = Utils.readFile(userSecKeyFileName);

        String tagString = Tag.genTags(filePath,pkString,skString);

        return tagString;

    }

    public static String uploadMetaData(Network network,String CurrentUserMsp, String dataname, String username, String cloudMsp, String cloud,  String fileName, String symKey, String policy,String introduction) throws Exception {
        //policy = "((0 AND 1) OR (2 AND 3)) AND 5"

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (symKey.length()!=8){
            System.out.println("Error! Symmetric key need 8 characters.");
            throw new Exception("Symmetric Key Error");
        }

        //String md5Hashcode = Md5.getFileMD5(new File(filePath+File.separatorChar+fileName)); //md5 hash
        File localFile = new File(fileName);
        if(!localFile.exists()){
            System.out.println("本地文件不存在");
            throw new Exception("Error! Cannot find file " + fileName);
        }

        String total_filename = localFile.getCanonicalPath();
        String only_filename = localFile.getName();
        String filePath = total_filename.substring(0,total_filename.length()-only_filename.length());
        System.out.println(filePath);

        String cmd = "go run " + ClientApp.path + "go/des.go " + "enc" +" " + symKey + " " + total_filename;
        System.out.println(cmd);
        String cmdResult = Command.exeCmd(cmd);
        //System.out.println(cmdResult);
        if(cmdResult.contains("Error")){
            throw new Exception("Error! File encrypted failed. ");
        }

        String cipherTextName = only_filename + "_cipherText";


        String md5Hashcode = Md5.getFileMD5(new File(filePath+cipherTextName)); //md5 hash
        TextOutput.jsonOutput("MD5_Hash",md5Hashcode);

        PrivateKey importedSK = imporSK("./key/"+username+"-sigSK.dat");

        //Let's sign our message
        String signature = ECC.sign(md5Hashcode, importedSK);
        System.out.println("Signature: "+signature);

        String cloudInfo = Cloud.queryCloud(network,cloudMsp,cloud);
        JSONObject cloudObject = JSONObject.parseObject(cloudInfo);
        String IP = cloudObject.getString("IP");
        String Port = cloudObject.getString("Port");

        FileTransferClient client = new FileTransferClient(IP,Port); // 启动客户端连接
        String URL = client.sendFile(username,filePath,cipherTextName); // 传输文件
        if(URL.length()==0){
            throw new Exception("Error! Cannot get cloud url.");
        }
        TextOutput.jsonOutput("URL",URL);
        String uniqueFilePath = URL.split("/")[URL.split("/").length-1];

        String tagString = generateTag(network,CurrentUserMsp,username,filePath  + cipherTextName);
        /*String[] tagColumnNames = TextOutput.tagColumnNames();
        Object[][] data = TextOutput.jsonObjectToData(tagString);
        TextOutput.tableOutput("数据标签",tagColumnNames,data);*/
        TextOutput.jsonOutput("数据标签",tagString);

        JSONObject tagJsonObject = JSONObject.parseObject(tagString);
        int n = tagJsonObject.size();


        MasterKey.queryMasterKey(network);
        Utils.writeFile(ClientApp.path+"go/policy",policy);
        Utils.writeFile(ClientApp.path+"go/encryptedSymKey","");
        cmd = "go run " + ClientApp.path + "go/main.go " +"enc" + " "+symKey;
        System.out.println(cmd);
        cmdResult = Command.exeCmd(cmd);

        if(cmdResult.contains("Error") || cmdResult.contains("Fail")){
            throw new Exception("Error! Key encrypted failed. ");
        }
        String cipherKey = Utils.readFile(ClientApp.path + "go/encryptedSymKey");
        if (cipherKey.length()==0){
            throw new Exception("Error! File encryptedSymKey is empty. ");
        }

        TextOutput.jsonOutput("密钥密文",cipherKey);

        //System.out.println(uniqueFilePath);

        System.out.println("\nSubmitTx: Upload Metadata...");
        result = contract.submitTransaction("uploadMetadata", dataname, cloudMsp,cloud,URL, md5Hashcode, signature, cipherKey, policy, tagString, String.valueOf(n),introduction);



        String resultString = new String(result);

        TextOutput.jsonOutput("数据长记录",resultString);

        JSONObject dataObject = JSONObject.parseObject(resultString);

        TextOutput.jsonOutput("数据短记录",Utils.makeShortRecord(dataObject.getString("Channel"),dataObject.getString("OwnerId"),dataObject.getString("Name")));
        Utils.writeSymkey(username,Utils.makeShortRecord(dataObject.getString("Channel"),dataObject.getString("OwnerId"),dataObject.getString("Name")),symKey);

        //return  resultString;
        return Utils.makeShortRecord(dataObject.getString("Channel"),dataObject.getString("OwnerId"),dataObject.getString("Name"));
    }

    public static ArrayList<String> queryAllData(Network network) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryAllData");
        //System.out.println(new String(result));

        String allDataJsonArrayString = new String(result);

        /*String[] dataColumnNames = TextOutput.dataColumnNames();
        Object[][] data = TextOutput.dataArrayToData(allDataJsonArrayString);
        TextOutput.tableOutput("数据记录",dataColumnNames,data);*/
        TextOutput.jsonOutput("数据记录",allDataJsonArrayString);
        ArrayList<String> shortRecordList = new ArrayList<>();
        JSONArray allDataJsonArray = JSON.parseArray(allDataJsonArrayString);
        int size = allDataJsonArray.size();
        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = allDataJsonArray.getJSONObject(i);
            String shortRecord = Utils.makeShortRecord(jsonObject.getString("Channel"),jsonObject.getString("OwnerId"),jsonObject.getString("Name"));
            shortRecordList.add(shortRecord);
        }
        return shortRecordList;
    }

    public static String queryData(Network network,String ShortRecord) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        if (ShortRecord.split("-").length<3){
            throw new Exception("Short Record formation error");
        }
        String channel = ShortRecord.split("-",3)[0];
        String ownerId = ShortRecord.split("-",3)[1];
        String dataname = ShortRecord.split("-",3)[2];
        result = contract.evaluateTransaction("queryData",channel,ownerId,dataname);
        //System.out.println(new String(result));

        String dataString = new String(result);
        return dataString;
    }


    public static String querySbData(Network network, String ownerId) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        String channel = network.getChannel().getName();
        result = contract.evaluateTransaction("querySbData", channel,ownerId);
        //System.out.println(new String(result));

        String allUploadJsonArrayString = new String(result);

        /*String[] dataColumnNames = TextOutput.dataColumnNames();
        Object[][] data = TextOutput.dataArrayToData(allUploadJsonArrayString);
        TextOutput.tableOutput(ownerId+"的数据",dataColumnNames,data);*/
        TextOutput.jsonOutput(ownerId+"的数据",allUploadJsonArrayString);
        return allUploadJsonArrayString;
    }

    public static String queryDataHistory(Network network,String ShortRecord) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        //result = contract.evaluateTransaction("queryDataHistory");
        //System.out.println(new String(result));

        if (ShortRecord.split("-").length<3){
            throw new Exception("Short Record formation error");
        }
        String channel = ShortRecord.split("-",3)[0];
        String ownerId = ShortRecord.split("-",3)[1];
        String dataname = ShortRecord.split("-",3)[2];
        result = contract.evaluateTransaction("queryDataHistory",channel,ownerId,dataname);

        String allDataJsonArrayString = new String(result);


        //JSONArray allDataJsonArray = JSONObject.parseArray(allDataJsonArrayString);
        //System.out.println("数据历史信息:");
        TextOutput.jsonOutput("数据历史信息",allDataJsonArrayString);

        /*int size = allDataJsonArray.size();
        String dataString;
        System.out.println("数据历史信息:");
        for (int i = 0; i < size;i++){
            JSONObject jsonObject = allDataJsonArray.getJSONObject(i);
            dataString = jsonObject.toString();
            System.out.println(dataString);
            System.out.println();
        }*/
        return allDataJsonArrayString;
    }

    public static String downloadDataFromCloud(String url,String ip, String port) throws Exception {
        FileTransferClient client = new FileTransferClient(ip,port);
        String localURL = client.recvFile(url);
        return localURL;
    }

    public static String preDec(Network network,String channel, String ownerId, String name) throws Exception {
        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        System.out.println("\nSubmitTx: Pre Decryption...");
        result = contract.submitTransaction("PreDecryption", channel,ownerId,name);

        String resultString = new String(result);
        JSONObject resultObject = JSONObject.parseObject(resultString);
        if (resultObject.containsKey("Reputation")){
            System.out.println(resultString);
            throw new Exception("Error! Policy mismatch. PreDec failed.");
        }

        TextOutput.jsonOutput("中间密钥",resultString);

        return resultString;

    }

    public static String downloadEncryptedData(Network network,String ShortDataRecord) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        String dataString = queryData(network,ShortDataRecord);

        JSONObject dataObject = JSONObject.parseObject(dataString);

        String url = dataObject.getString("URL");
        String msp = dataObject.getString("CloudMsp");
        String cloud = dataObject.getString("Cloud");

        String cloudString = Cloud.queryCloud(network,msp,cloud);
        JSONObject cloudObject = JSONObject.parseObject(cloudString);

        String ip = cloudObject.getString("IP");
        String port = cloudObject.getString("Port");

        String localUrl = downloadDataFromCloud(url,ip,port);
        return localUrl;
    }

    public static String downloadDecryptedData(Network network,String ShortRecord, String CurrentUsername) throws Exception {
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        String dataString = queryData(network,ShortRecord);
        TextOutput.jsonOutput("数据长记录",dataString);

        JSONObject dataObject = JSONObject.parseObject(dataString);


        String url = dataObject.getString("URL");
        String msp = dataObject.getString("CloudMsp");
        String cloud = dataObject.getString("Cloud");
        String channel = dataObject.getString("Channel");
        String ownerId = dataObject.getString("OwnerId");
        String name = dataObject.getString("Name");

        String cloudString = Cloud.queryCloud(network,msp,cloud);
        JSONObject cloudObject = JSONObject.parseObject(cloudString);

        String ip = cloudObject.getString("IP");
        String port = cloudObject.getString("Port");

        downloadDataFromCloud(url,ip,port);

        String folder = url.split("/")[url.split("/").length-2];
        String uniqueName = url.split("/")[url.split("/").length-1];

        String symKey = "";

        if (checkIsOwner(dataString,CurrentUsername)){//isOwner == True
            symKey = Utils.readSymkey(CurrentUsername,ShortRecord);
        }else{//isOwner == False
            /*for(int i=0;i<11;i++)
            {
                long t1 = System.currentTimeMillis();
                String midResult = testPreDec(network,username,ownername,filename,cloud);
                long t2 = System.currentTimeMillis();
                System.out.println("**************PreDec"+(t2-t1)+"ms**********************");
                Utils.writeFile(path+"midResult",midResult);
            }*/

            String midResult = preDec(network,channel,ownerId,name);
            //TextOutput.jsonOutput("中间密钥",midResult);
            Utils.writeFile(ClientApp.path+"go/midResult",midResult);

            Utils.writeFile(ClientApp.path+"go/data",dataString);

            String cmd = "go run " + ClientApp.path+"go/main.go " +"dec";
            //System.out.println(cmd);
            String cmdResult = Command.exeCmd(cmd);

            if(cmdResult.contains("Error")){
                System.out.println("Error! Decrypt symKey fail. ");
                throw new Exception("Error! Decrypt symKey fail. ");
                //return;
            }

            symKey = Utils.readFile(ClientApp.path+"go/symKey");
        }

        if (symKey.length()==0){
            throw new Exception("Error! The symkey is null. ");
        }

        TextOutput.jsonOutput("数据密钥",symKey);

        String cmd = "go run " + ClientApp.path+"go/des.go " + "dec" +" " + symKey + " " + ClientApp.path+"data/" + folder + File.separatorChar + uniqueName;
        //System.out.println(cmd);

        String cmdResult = Command.exeCmd(cmd);
        System.out.println("PlainText: "+ ClientApp.path+"data/" + folder + File.separatorChar + uniqueName+"_plainText");
        //System.out.println(cmdResult);

        if(cmdResult.contains("Error")){
            System.out.println("Error! File encrypted failed. ");
            throw new Exception("Error! File encrypted failed. ");
        }
        return  ClientApp.path+"data/" + folder + File.separatorChar + uniqueName+"_plainText";

        //String plaintext = Utils.readFile(path+"plaintext.txt");
    }

    public static String updateData(Network network,String currentUserMsp,String username,String ShortRecord, String filename, String SymKey, String Policy, String Introduction) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        String oldDataString = queryData(network,ShortRecord);
        JSONObject oldDataObject = JSONObject.parseObject(oldDataString);
        String cloud = oldDataObject.getString("Cloud");
        String msp = oldDataObject.getString("CloudMsp");
        String channel = oldDataObject.getString("Channel");
        String ownerId = oldDataObject.getString("OwnerId");
        String name = oldDataObject.getString("Name");
        String url = oldDataObject.getString("URL");


        String tagString ="";
        String cipherKey ="";
        String isNewFile = "";
        String userHash = "";
        String userSig = "";
        int n = 0;

        if (SymKey.equals("**")){
            System.out.println("不更新密钥");
            SymKey = Utils.readSymkey(username,ShortRecord);
            if (Policy.equals("**")) {
                Policy = oldDataObject.getString("Policy");
                System.out.println("不更新访问策略");
                cipherKey = oldDataObject.getString("CipherKey");
            }else{
                MasterKey.queryMasterKey(network);
                Utils.writeFile(ClientApp.path+"go/policy",Policy);
                Utils.writeFile(ClientApp.path+"go/encryptedSymKey","");
                String cmd = "go run " + ClientApp.path + "go/main.go " +"enc" + " "+SymKey;
                System.out.println(cmd);
                String cmdResult = Command.exeCmd(cmd);

                if(cmdResult.contains("Error") || cmdResult.contains("Fail")){
                    throw new Exception("Error! Key encrypted failed. ");
                }
                cipherKey = Utils.readFile(ClientApp.path + "go/encryptedSymKey");
                if (cipherKey.length()==0){
                    throw new Exception("Error! File encryptedSymKey is empty. ");
                }

                TextOutput.jsonOutput("密钥密文",cipherKey);
            }

        }else{
            Utils.writeSymkey(username,ShortRecord,SymKey);
            if (Policy.equals("**")){
                Policy = oldDataObject.getString("Policy");
                System.out.println("不更新访问策略");
            }
            MasterKey.queryMasterKey(network);
            Utils.writeFile(ClientApp.path+"go/policy",Policy);
            Utils.writeFile(ClientApp.path+"go/encryptedSymKey","");
            String cmd = "go run " + ClientApp.path + "go/main.go " +"enc" + " "+SymKey;
            System.out.println(cmd);
            String cmdResult = Command.exeCmd(cmd);

            if(cmdResult.contains("Error") || cmdResult.contains("Fail")){
                throw new Exception("Error! Key encrypted failed. ");
            }
            cipherKey = Utils.readFile(ClientApp.path + "go/encryptedSymKey");
            if (cipherKey.length()==0){
                throw new Exception("Error! File encryptedSymKey is empty. ");
            }

            TextOutput.jsonOutput("密钥密文",cipherKey);
        }



        if (filename.equals("**")){
            System.out.println("不更新文件内容");
            tagString = oldDataObject.getString("Tag");
            n = oldDataObject.getInteger("N");
            userHash = oldDataObject.getString("UserHash");
            userSig = oldDataObject.getString("UserSig");
            isNewFile = "F";

        }else{
            isNewFile = "T";

            //String md5Hashcode = Md5.getFileMD5(new File(filePath+File.separatorChar+fileName)); //md5 hash
            File localFile = new File(filename);
            if(!localFile.exists()){
                System.out.println("本地文件不存在");
                throw new Exception("Error! Cannot find file " + filename);
            }

            String total_filename = localFile.getCanonicalPath();
            String only_filename = localFile.getName();
            String filePath = total_filename.substring(0,total_filename.length()-only_filename.length());
            System.out.println(filePath);

            String cmd = "go run " + ClientApp.path + "go/des.go " + "enc" +" " + SymKey + " " + total_filename;
            System.out.println(cmd);
            String cmdResult = Command.exeCmd(cmd);
            //System.out.println(cmdResult);
            if(cmdResult.contains("Error")){
                throw new Exception("Error! File encrypted failed. ");
            }

            String cipherTextName = only_filename + "_cipherText";

            userHash = Md5.getFileMD5(new File(filePath+cipherTextName)); //md5 hash
            TextOutput.jsonOutput("MD5_Hash",userHash);

            PrivateKey importedSK = imporSK("./key/"+username+"-sigSK.dat");

            //Let's sign our message
            userSig = ECC.sign(userHash, importedSK);
            TextOutput.jsonOutput("Signature",userSig);

            String cloudInfo = Cloud.queryCloud(network,msp,cloud);
            JSONObject cloudObject = JSONObject.parseObject(cloudInfo);
            String IP = cloudObject.getString("IP");
            String Port = cloudObject.getString("Port");

            FileTransferClient client = new FileTransferClient(IP,Port); // 启动客户端连接
            client.updateFile(filePath,cipherTextName,url); // 传输文件


            tagString = generateTag(network,currentUserMsp,username,filePath  + cipherTextName);
            /*String[] tagColumnNames = TextOutput.tagColumnNames();
            Object[][] data = TextOutput.jsonObjectToData(tagString);
            TextOutput.tableOutput("数据标签",tagColumnNames,data);*/
            TextOutput.jsonOutput("数据标签",tagString);

            JSONObject tagJsonObject = JSONObject.parseObject(tagString);
            n = tagJsonObject.size();

        }

        if (Introduction.equals("**")){
            Introduction = oldDataObject.getString("Introduction");
            System.out.println("不更新文件简介");
        }

        System.out.println("\nSubmitTx: Updata Data...");
        //System.out.println(channel);
        //System.out.println(index);
        //System.out.println(name);
        //System.out.println(userHash);
        //System.out.println(userSig);
        //System.out.println(cipherKey);
        //System.out.println(Policy);
        //System.out.println(tagString);
        //System.out.println(String.valueOf(n));
        //System.out.println(Introduction);
        //System.out.println(isNewFile);
        result = contract.submitTransaction("updateData", channel, ownerId,name,userHash, userSig,cipherKey,Policy,tagString,String.valueOf(n),Introduction,isNewFile);

        String resultString = new String(result);

        TextOutput.jsonOutput("数据长标识",resultString);

        return resultString;

    }

}
