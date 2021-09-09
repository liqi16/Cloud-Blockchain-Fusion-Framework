package org.example;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;
import java.util.HashMap;

import it.unisa.dia.gas.jpbc.Pairing;
import org.apache.commons.codec.digest.DigestUtils;
import org.hyperledger.fabric.gateway.Wallet;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

import java.security.KeyPair;

public class Cloud {
    public PublicParam keys;

    public Cloud(String jsonStringPub){
        keys = new PublicParam(jsonStringPub);
    }

    public static String cloudRegister(Network network,String mspId, String cloud, String IP, String Port) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        String sigPK = "",sigSK;

        try{
            KeyPair pair = ECC.generateKeyPair();

            ECC.exportPK(pair.getPublic(), Main.path+"key/"+ mspId +"-"+cloud+"-sigPK.dat");
            ECC.exportSK(pair.getPrivate(),Main.path+"key/"+mspId +"-"+cloud+"-sigSK.dat");
            sigPK = ECC.exportPKToString(pair.getPublic());
            sigSK = ECC.exportSKToString(pair.getPrivate());
            TextOutput.jsonOutput("签名公钥",sigPK);
            TextOutput.jsonOutput("签名私钥",sigSK);
        }catch (Exception e){
            e.printStackTrace();
        }

        result = contract.submitTransaction("cloudRegister",mspId,cloud,IP,Port,sigPK);

        String resultString = new String(result);

        //TextOutput.jsonOutput("云信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }

    public static String cloudLogin(Network network,String mspId, String cloud) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.evaluateTransaction("cloudLogin", mspId,cloud);

        String resultString = new String(result);

        //TextOutput.jsonOutput("云信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }

    public static String cloudUpdate(Network network,String mspId, String cloud, String IP,String Port) throws Exception{
        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.submitTransaction("cloudUpdate",mspId,cloud,IP,Port);

        String resultString = new String(result);

        //TextOutput.jsonOutput("云信息",resultString);
        //System.out.println("用户信息:");
        //System.out.println(resultString);

        return resultString;
    }

    public String proofGen(String filePath,String chalString) {

        //System.out.print("Cloud computes proof......\t\t\t");
        List<Challenge> challenges = getChallenges(chalString);
        Map<String,Element> proof = genProof(challenges,filePath);
        String proofString = storeProof(proof);
        //System.out.println("[ok].");
        return proofString;
    }

    public List<Challenge> getChallenges(String chalString){
        JSONObject challengesObject = JSONObject.parseObject(chalString);
        List<Challenge> challenges = new ArrayList<Challenge>();
        Iterator iter = challengesObject.entrySet().iterator();
        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();
            String keyString = entry.getKey().toString();
            int key = Integer.valueOf(keyString);

            String valueString = challengesObject.getString(keyString);
            byte[] valueBytes = Utils.base64StringToElementBytes(valueString);
            Element valuElement = keys.getPairing().getZr().newElementFromBytes(valueBytes);

            challenges.add(new Challenge(key, valuElement));
        }

        return challenges;

    }

    public String storeProof(Map<String,Element> proof) {

        JSONObject proofObject = new JSONObject();

        proofObject.put("miu",Utils.elementToBase64(proof.get("miu")));
        proofObject.put("R", Utils.elementToBase64(proof.get("R")));
        proofObject.put("hashMul", Utils.elementToBase64(proof.get("hashMul")));

        return proofObject.toString();
    }

    public Map<String,Element> genProof(List<Challenge> challenges, String filePath){

        Map<String,Element> proof=new HashMap<>();
        try {

            byte[][] cdata = FileOperation.readData(challenges,filePath);
            Pairing pairing = keys.getPairing();
            int c = challenges.size();
            Element miu = pairing.getZr().newZeroElement();
            Element r = pairing.getZr().newRandomElement().getImmutable();
            Element R = keys.getW().duplicate().powZn(r);
            Element hashMul = pairing.getG2().newOneElement();
            for(int i=0;i<c;i++){
                //add(mi*ri)
                String hashstr = DigestUtils.sha256Hex(cdata[i]);
                Element hash = keys.getPairing().getG1().newElement().setFromHash(hashstr.getBytes(), 0, hashstr.getBytes().length);
                hashMul.mul(hash.powZn(challenges.get(i).random));
                Element mi = pairing.getZr().newElement().setFromHash(cdata[i], 0, cdata[i].length);
                miu.add(mi.mulZn(challenges.get(i).random));
            }
            miu.add(r);
            R.getImmutable();
            miu.getImmutable();
            hashMul.getImmutable();

            proof.put("miu", miu);
            proof.put("R", R);
            proof.put("hashMul", hashMul);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return proof;
    }

    /*
    public static String proofGen(String filePath,String chalString,String ownerPkString, String tagString) {

        //System.out.print("Cloud computes proof......\t\t\t");
        CloudServiceProvider csp=new CloudServiceProvider(ownerPkString);
        List<Challenge> challenges = getChallenges(csp,chalString);
        Map<String,Element> proof=csp.genProof(challenges,filePath,tagString);
        String proofString = storeProof(proof);
        //System.out.println("[ok].");
        return proofString;
    }*/

    /*
    public static List<Challenge> getChallenges(CloudServiceProvider csp, String chalString){

        //String chalString = Utils.readFile("config/challenges");
        JSONObject challengesObject = JSONObject.parseObject(chalString);
        List<Challenge> challenges = new ArrayList<Challenge>();
        Iterator iter = challengesObject.entrySet().iterator();
        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();
            String keyString = entry.getKey().toString();
            int key = Integer.valueOf(keyString);

            String valueString = challengesObject.getString(keyString);
            byte[] valueBytes = Utils.base64StringToElementBytes(valueString);
            Element valuElement = csp.pubInfor.pairing.getZr().newElementFromBytes(valueBytes);

            challenges.add(new Challenge(key, valuElement));
            //System.out.println(entry.getKey().toString());
            //System.out.println(entry.getValue().toString());
        }

        return challenges;

    }*/

    /*
    public static String storeProof(Map<String,Element> proof) {

        JSONObject proofObject = new JSONObject();

        proofObject.put("aggreDMulTemp",Utils.elementToBase64(proof.get("aggreDMulTemp")));
        proofObject.put("aggreTMul", Utils.elementToBase64(proof.get("aggreTMul")));

        //Utils.writeFile("config/proof", proofObject.toString());
        return proofObject.toString();

    }*/

}
