/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.example;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import it.unisa.dia.gas.jpbc.Element;

public class SimpleChaincode extends ChaincodeBase {

    private static String MAX_UNICODE_RUNE = "\udbff\udfff";

    @Override
    public Response init(ChaincodeStub stub) {
        return newSuccessResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        try {
            String func = stub.getFunction();

            List<String> params = stub.getParameters();
            if (func.equals("challengeGen")) {
                return challengeGen(stub, params);
            }else if (func.equals("verify")) {
                return verify(stub, params);
	    }else if (func.equals("auditAll")) {
                return auditAll(stub, params);
            }else {
                return newErrorResponse("Invalid invoke function name.");
            }
        } catch (Throwable e) {
            return newErrorResponse(e);
        }
    }

    private Response challengeGen(ChaincodeStub stub, List<String> args) throws NoSuchAlgorithmException {

        if (args.size() != 2) {
            return newErrorResponse("Error! Incorrect number of arguments. Expecting 2");
        }
        String shortRecord = args.get(0);
        int c = Integer.parseInt(args.get(1));
        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];
        List<byte[]> sub_args = new ArrayList<>();
        sub_args.add("queryData".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        String responseJson = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	
        System.out.println("$$$$$$$$$$$$$$$$$data"+responseJson);

        JSONObject dataObject = JSONObject.parseObject(responseJson);

        int n = dataObject.getInteger("N");
        String ownername = dataObject.getString("Owner");
        String ownerMsp = dataObject.getString("OwnerMsp");
        String cloud = dataObject.getString("Cloud");
        String cloudMsp = dataObject.getString("CloudMsp");

        sub_args.clear();
        sub_args.add("queryUser".getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownername.getBytes(StandardCharsets.UTF_8));
        String ownerString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
        System.out.println("$$$$$$$$$$$$$$$$$owner"+ownerString);

        JSONObject ownerObject = JSONObject.parseObject(ownerString);

        String ownerPkString = ownerObject.getString("AuditPk");

        if (c > n ){
            System.out.println( "Error! Expecting c <= "+n);
            return newErrorResponse("Error! Expecting c <= "+n);
        }

        byte[] creator = stub.getCreator();
        String userId = Utils.SHA256(creator);
        sub_args.clear();
        sub_args.add("queryPeer".getBytes(StandardCharsets.UTF_8));
        sub_args.add(userId.getBytes(StandardCharsets.UTF_8));
        String peerString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	System.out.println("$$$$$$$$$$$$$$$$$peer"+peerString);
        JSONObject peerObject = JSONObject.parseObject(peerString);
        String auditorMsp = peerObject.getString("Msp");
        String auditor = peerObject.getString("Name");
        //String datakey = "\u0000"+"DATA"+"\u0000"+channel+"\u0000"+ownerId+"\u0000"+name+"\u0000";

        System.out.print("Blockchain computes challenges......\t\t");
        Verifier verifier=new Verifier(ownerPkString);

        //byte[] result = stub.getState("data"+ownername+filePath);
        //String dataString = new String(result);
        //System.out.println(dataString);

        //JSONObject dataObject = JSON.parseObject(dataString);

        //int n = Integer.valueOf(dataObject.getString("N"));

        if( c > n){
            return newErrorResponse("Error! c > n. ");
        }
        int ran[] = verifier.generateIdx(c,n);
        List<Challenge> challenges = verifier.challengeGen(c, ran);
        String chalString = Utils.storeChallenges(challenges);

        Instant txTime = stub.getTxTimestamp();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp= sdf.format(Date.from(txTime));

        sub_args.clear();
        sub_args.add("auditData".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownername.getBytes(StandardCharsets.UTF_8));
        sub_args.add(auditorMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(auditor.getBytes(StandardCharsets.UTF_8));
        sub_args.add(cloudMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(cloud.getBytes(StandardCharsets.UTF_8));
        sub_args.add(Utils.intArrayToString(ran).getBytes(StandardCharsets.UTF_8));
        sub_args.add(timestamp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(chalString.getBytes(StandardCharsets.UTF_8));
        sub_args.add(stub.getTxId().getBytes(StandardCharsets.UTF_8));
        String auditString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
        byte[] auditBytes = auditString.getBytes(StandardCharsets.UTF_8);

        stub.setEvent(cloudMsp+"_"+cloud+"_AuditEvent",auditBytes);

        return newSuccessResponse(auditBytes);
    }

    // Deletes an entity from state
    private Response verify(ChaincodeStub stub, List<String> args) throws NoSuchAlgorithmException {

        if (args.size() != 5) {
            return newErrorResponse("Incorrect number of arguments. Expecting 5");
        }

        String channel = args.get(0);
        String ownerId = args.get(1);
        String name = args.get(2);
        String timestamp = args.get(3);
        String proofString = args.get(4);

        List<byte[]> sub_args = new ArrayList<>();
        sub_args.add("queryPeer".getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        String peerString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	System.out.println("$$$$$$$$$$$$$$$$$owner"+peerString);
        JSONObject peerObject = JSONObject.parseObject(peerString);
        String ownerMsp = peerObject.getString("Msp");
        String owner = peerObject.getString("Name");

        sub_args.clear();
        sub_args.add("queryUser".getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(owner.getBytes(StandardCharsets.UTF_8));
        String userString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	System.out.println("$$$$$$$$$$$$$$$$$user"+userString);
        JSONObject userObject = JSONObject.parseObject(userString);
        String ownerPkString = userObject.getString("AuditPk");


        sub_args.clear();
        sub_args.add("queryData".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        String dataString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	System.out.println("$$$$$$$$$$$$$$$$$data"+dataString);
        JSONObject dataObject = JSONObject.parseObject(dataString);
        String tagString = dataObject.getString("Tag");
        String cloudString = dataObject.getString("Cloud");
        String cloudMspString = dataObject.getString("CloudMsp");

	byte[] creator = stub.getCreator();
        String cloudid = Utils.SHA256(creator);
        sub_args.clear();
        sub_args.add("queryPeer".getBytes(StandardCharsets.UTF_8));
        sub_args.add(cloudid.getBytes(StandardCharsets.UTF_8));
        peerString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	System.out.println("$$$$$$$$$$$$$$$$$peer"+peerString);
        peerObject = JSONObject.parseObject(peerString);
        if (!peerObject.getString("Msp").equals(cloudMspString)) return newErrorResponse("Error! Permission deny.");
        if (!peerObject.getString("Name").equals(cloudString)) return newErrorResponse("Error! Permission deny.");

        sub_args.clear();
        sub_args.add("queryAudit".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        sub_args.add(timestamp.getBytes(StandardCharsets.UTF_8));
        String resultString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();

        if (resultString.length()==0){
            return newErrorResponse("Error! Cannot find this audit.");
        }

        JSONObject resultObject = JSONObject.parseObject(resultString);
        String chalString = resultObject.getString("Challenge");
        String auditor = resultObject.getString("Auditor");
        String auditorMsp = resultObject.getString("AuditorMsp");


        System.out.print("Blockchain verifies proof......\t\t\t");
        Verifier verifier=new Verifier(ownerPkString);
        Map<String,Element> proof = Utils.getProof(verifier,proofString);
        List<Challenge> challenges = Utils.getChallenges(verifier,chalString);
        boolean isTrue=verifier.VeriProof(challenges,proof,tagString);

        sub_args.clear();
        sub_args.add("updateAuditResult".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        sub_args.add(timestamp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(proofString.getBytes(StandardCharsets.UTF_8));
        sub_args.add(String.valueOf(isTrue).getBytes(StandardCharsets.UTF_8));
        resultString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();

        stub.setEvent(auditorMsp+"_"+auditor+"_VerifyEvent",resultString.getBytes(StandardCharsets.UTF_8));

        return newSuccessResponse(resultObject.toString().getBytes());
    }

    private Response auditAll(ChaincodeStub stub, List<String> args) throws NoSuchAlgorithmException {

        if (args.size() != 1) {
            return newErrorResponse("Error! Incorrect number of arguments. Expecting 1");
        }
        String shortRecord = args.get(0);
        //int c = Integer.parseInt(args.get(1));
        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];
        List<byte[]> sub_args = new ArrayList<>();
        sub_args.add("queryData".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        String responseJson = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	
        System.out.println("$$$$$$$$$$$$$$$$$data"+responseJson);

        JSONObject dataObject = JSONObject.parseObject(responseJson);

        int n = dataObject.getInteger("N");
	int c = n;
        String ownername = dataObject.getString("Owner");
        String ownerMsp = dataObject.getString("OwnerMsp");
        String cloud = dataObject.getString("Cloud");
        String cloudMsp = dataObject.getString("CloudMsp");

        sub_args.clear();
        sub_args.add("queryUser".getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownername.getBytes(StandardCharsets.UTF_8));
        String ownerString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
        System.out.println("$$$$$$$$$$$$$$$$$owner"+ownerString);

        JSONObject ownerObject = JSONObject.parseObject(ownerString);

        String ownerPkString = ownerObject.getString("AuditPk");

        if (c > n ){
            System.out.println( "Error! Expecting c <= "+n);
            return newErrorResponse("Error! Expecting c <= "+n);
        }

        byte[] creator = stub.getCreator();
        String userId = Utils.SHA256(creator);
        sub_args.clear();
        sub_args.add("queryPeer".getBytes(StandardCharsets.UTF_8));
        sub_args.add(userId.getBytes(StandardCharsets.UTF_8));
        String peerString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
	System.out.println("$$$$$$$$$$$$$$$$$peer"+peerString);
        JSONObject peerObject = JSONObject.parseObject(peerString);
        String auditorMsp = peerObject.getString("Msp");
        String auditor = peerObject.getString("Name");
        //String datakey = "\u0000"+"DATA"+"\u0000"+channel+"\u0000"+ownerId+"\u0000"+name+"\u0000";

        System.out.print("Blockchain computes challenges......\t\t");
        Verifier verifier=new Verifier(ownerPkString);
        
        int ran[] = verifier.generateIdx(c,n);
        List<Challenge> challenges = verifier.challengeGen(c, ran);
        String chalString = Utils.storeChallenges(challenges);

        Instant txTime = stub.getTxTimestamp();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp= sdf.format(Date.from(txTime));

        sub_args.clear();
        sub_args.add("auditData".getBytes(StandardCharsets.UTF_8));
        sub_args.add(channel.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerId.getBytes(StandardCharsets.UTF_8));
        sub_args.add(name.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownerMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(ownername.getBytes(StandardCharsets.UTF_8));
        sub_args.add(auditorMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(auditor.getBytes(StandardCharsets.UTF_8));
        sub_args.add(cloudMsp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(cloud.getBytes(StandardCharsets.UTF_8));
        sub_args.add(Utils.intArrayToString(ran).getBytes(StandardCharsets.UTF_8));
        sub_args.add(timestamp.getBytes(StandardCharsets.UTF_8));
        sub_args.add(chalString.getBytes(StandardCharsets.UTF_8));
        sub_args.add(stub.getTxId().getBytes(StandardCharsets.UTF_8));
        String auditString = stub.invokeChaincode("cbfm-go", sub_args,channel).getStringPayload();
        byte[] auditBytes = auditString.getBytes(StandardCharsets.UTF_8);

        stub.setEvent(cloudMsp+"_"+cloud+"_AuditEvent",auditBytes);

        return newSuccessResponse(auditBytes);
    }



    public static void main(String[] args) {
        //System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());
        new SimpleChaincode().start(args);
    }
}
