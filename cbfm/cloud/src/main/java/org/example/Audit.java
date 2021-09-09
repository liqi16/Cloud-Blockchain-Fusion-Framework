package org.example;

import java.io.File;

import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.Channel;

import com.alibaba.fastjson.JSONObject;

//import javax.rmi.CORBA.Util;

public class Audit {

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


    public static String queryUser(Network network,String mspId, String name) throws ContractException {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.evaluateTransaction("queryUser",mspId, name);

        String resultString = new String(result);

        TextOutput.jsonOutput("用户信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }

	public static String getProofAndVerify(Network network,String AuditString) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        JSONObject auditObject = JSONObject.parseObject(AuditString);
        String Datakey = auditObject.getString("Datakey");
        String Channel = Datakey.split("\u0000")[2];
        String OwnerId = Datakey.split("\u0000")[3];
        String Name = Datakey.split("\u0000")[4];
        String Timestamp = auditObject.getString("Timestamp");


        String dataString = queryData(network, Utils.makeShortRecord(Channel,OwnerId,Name));
        JSONObject dataObject = JSONObject.parseObject(dataString);
        String URL = dataObject.getString("URL");
        //String tagString = dataObject.getJSONObject("Tag").toString();
        String owner = dataObject.getString("Owner");
        String ownerMsp = dataObject.getString("OwnerMsp");

        String ownerString = queryUser(network,ownerMsp,owner);
        JSONObject ownerObject = JSONObject.parseObject(ownerString);
        String ownerPkString = ownerObject.getJSONObject("AuditPk").toString();

        String chalString = auditObject.getJSONObject("Challenge").toString();


        File auditFile = new File(URL);
    	if(auditFile.exists()) {
    		//Long t1 = System.currentTimeMillis();
            Cloud cloud = new Cloud(ownerPkString);
    		String proofString = cloud.proofGen(URL,chalString);
    		System.out.println("\n云平台$ 数据证明");
    		System.out.println(proofString);
    		//Long t2 = System.currentTimeMillis();
    		//System.out.println("*********************************proofGen : "+(t2-t1)+"ms***********************************************");
    		if (!(proofString.length()>0)){
                System.out.println("Error! Cannot get proof. ");
                throw new Exception("Error! Cannot get proof. ");
            }
    		contract = network.getContract("cbfm-java");
    		//t1 = System.currentTimeMillis();
            result = contract.submitTransaction("verify",Channel,OwnerId,Name,Timestamp,proofString);
            //t2 = System.currentTimeMillis();
            //System.out.println("*********************************verify : "+(t2-t1)+"ms***********************************************");
            System.out.println("\n云平台$ 审计记录");
            System.out.println(new String(result));
            return new String(result);
    	}else {
    		System.out.println("Error! File does not exsit. ");
    		return null;
    	}
    }
	
	
	
}
