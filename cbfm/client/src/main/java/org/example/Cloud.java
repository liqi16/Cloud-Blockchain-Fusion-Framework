package org.example;

import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

import java.io.File;
import java.security.PublicKey;

public class Cloud {

    public static String queryCloud(Network network,String msp, String cloud) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.evaluateTransaction("queryCloud", msp,cloud);

        String resultString = new String(result);

        TextOutput.jsonOutput("云信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }

    public static String queryAllCloud(Network network) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryAllCloud");
        //System.out.println(new String(result));

        String cspJsonArrayString = new String(result);

        /*String[] cloudColumnNames = TextOutput.cloudColumnNames();
        Object[][] data = TextOutput.cloudArrayToData(cspJsonArrayString);
        TextOutput.tableOutput("云信息",cloudColumnNames,data);*/
        TextOutput.jsonOutput("云信息",cspJsonArrayString);
        return cspJsonArrayString;
    }

    public static boolean verifySignature(Network network, String shortRecord) throws Exception{

        String dataString = Data.queryData(network,shortRecord);

        JSONObject dataObject = JSONObject.parseObject(dataString);

        String url = dataObject.getString("URL");
        String msp = dataObject.getString("CloudMsp");
        String cloud = dataObject.getString("Cloud");
        String cloudHash = dataObject.getString("CloudHash");
        String cloudSig = dataObject.getString("CloudSig");

        String cloudString = Cloud.queryCloud(network,msp,cloud);
        JSONObject cloudObject = JSONObject.parseObject(cloudString);

        String ip = cloudObject.getString("IP");
        String port = cloudObject.getString("Port");
        String cloudPk = cloudObject.getString("Pk");

        String localURL = Data.downloadDataFromCloud(url,ip,port);
        PublicKey pk = ECC.importPKFromString(cloudPk);
        String hashcode = Md5.getFileMD5(new File(localURL));
        if (hashcode.equals(cloudHash)) {
            System.out.println("Hash is correct! Hash = "+hashcode+".");
        }else {
            System.out.println("Hash is conflicting ! DataHash = "+hashcode+" and CloudHash = "+cloudHash+".");
        }
        Boolean isTure = ECC.verify(hashcode,cloudSig,pk);
        //System.out.println("The verify result is "+isTure+".");
        return isTure;
    }

}
