package org.hyperledger.fabric.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Utils {

    //public static String dir = "../../resources/";

    public static String intArrayToString(int[] intArray){
        if (intArray.length==0){
            return "";
        }

        String seperateChar = ",";
        String result = "";
        for (int i=0;i<intArray.length-1;i++){
            result = result + String.valueOf(intArray[i]) + seperateChar;
        }
        result = result + intArray[intArray.length-1];
        return result;
    }

    public static int[] GenRandom(int len, int total) {
        int[] set = new int[len];
        Random ran = new Random();
        HashSet<Integer> hs = new HashSet<Integer>();
        while(hs.size() != len){
            int tmp = ran.nextInt(total);
            hs.add(tmp);
        }
        int i = 0;
        for(int num: hs) {
            set[i] = num;
            i++;
        }
        return set;
    }

    /*public static Map<String ,byte[]> getPublicInfor(String ownerPkString){

        //String userKey = "user" + username;
        //byte[] result ;

        //result = stub.getState(userKey);
        //String resultString = new String(result);
        //System.out.println(resultString);

        //JSONObject userObject = JSONObject.parseObject(resultString);

        JSONObject pubkeyObject = JSON.parseObject(ownerPkString);

        Map<String ,byte[]> tempMap = new HashMap<String, byte[]>();

        tempMap.put("g1", Utils.base64StringToElementBytes(pubkeyObject.getString("g1")));
        tempMap.put("g2", Utils.base64StringToElementBytes(pubkeyObject.getString("g2")));
        tempMap.put("v", Utils.base64StringToElementBytes(pubkeyObject.getString("v")));
        tempMap.put("us1", Utils.base64StringToElementBytes(pubkeyObject.getString("us")));

        return tempMap;

    }*/

    public static String elementToBase64(Element e) {
        Base64.Encoder encoder = Base64.getEncoder();
        String elementBase64String = encoder.encodeToString(e.toBytes());
        return elementBase64String;
    }

    //base64 to elementByte[]
    public static byte[] base64StringToElementBytes(String elementBase64String){
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] elementByte = decoder.decode(elementBase64String);
        return elementByte;
    }

    public static void writeFile(String outputfile, String content) {
        FileWriter writer;
        try {
            writer = new FileWriter(outputfile);
            writer.write("");
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(String filename) {
        String data = "";

        try {
            File myObj = new File(filename);
            Scanner myReader = new Scanner(myObj);

            while (myReader.hasNextLine()) {
                data += myReader.nextLine();
            }

            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return data;
    }

    public static String storeChallenges(List<Challenge> challenges) {
        JSONObject challengesObject = new JSONObject();

        for (int i = 0; i < challenges.size(); i++) {
            Challenge challenge = challenges.get(i);
            String keyString = String.valueOf(challenge.num);
            String valueString = Utils.elementToBase64(challenge.random);
            challengesObject.put(keyString, valueString);
        }

        String chalString = challengesObject.toJSONString();
        //String challengePath = verifier.getClass().getClassLoader().getResource(dir + "main/challenges").getPath();
        //String challengePath = "challenges";
        //Utils.writeFile(challengePath, chalString);
        return chalString;
    }

    /*public static List<Challenge> getChallenges(Verifier verifier,String chalString){
        //String challengePath = verifier.getClass().getClassLoader().getResource(dir + "main/challenges").getPath();
        //String challengePath = "challenges";
        //String chalString = Utils.readFile(challengePath);
        JSONObject challengesObject = JSONObject.parseObject(chalString);
        List<Challenge> challenges = new ArrayList<Challenge>();
        Iterator iter = challengesObject.entrySet().iterator();
        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();
            String keyString = entry.getKey().toString();
            int key = Integer.valueOf(keyString);

            String valueString = challengesObject.getString(keyString);
            byte[] valueBytes = Utils.base64StringToElementBytes(valueString);
            Element valuElement = verifier.pubInfor.pairing.getZr().newElementFromBytes(valueBytes);

            challenges.add(new Challenge(key, valuElement));
            //System.out.println(entry.getKey().toString());
            //System.out.println(entry.getValue().toString());
        }

        return challenges;

    }*/

    /*public static Map<String,Element> getProof(Verifier verifier,String proofString) {
        //String proofPath =  verifier.getClass().getClassLoader().getResource(dir + "main/proof").getPath();
        //String proofPath = "proof";
        //String proofString = Utils.readFile(proofPath);
        JSONObject proofObject = JSONObject.parseObject(proofString);

        Map<String,Element> tempProof = new HashMap<String, Element>();

        String aggreDMulTempString = proofObject.getString("aggreDMulTemp");
        Element aggreDMulTemp = verifier.pubInfor.pairing.getG1().newElementFromBytes(Utils.base64StringToElementBytes(aggreDMulTempString));

        Element aggreDMul = verifier.pubInfor.pairing.pairing(aggreDMulTemp,verifier.pubInfor.v);

        String aggreTMulString = proofObject.getString("aggreTMul");
        Element aggreTMul = verifier.pubInfor.pairing.getG1().newElementFromBytes(Utils.base64StringToElementBytes(aggreTMulString));

        tempProof.put("aggreDMul", aggreDMul);
        tempProof.put("aggreTMul", aggreTMul);

        return tempProof;

    }*/

    public static List<Challenge> getChallenges(Verifier verifier,String chalString){

        JSONObject challengesObject = JSONObject.parseObject(chalString);
        List<Challenge> challenges = new ArrayList<Challenge>();
        Iterator iter = challengesObject.entrySet().iterator();
        while (iter.hasNext()) {

            Map.Entry entry = (Map.Entry) iter.next();
            String keyString = entry.getKey().toString();
            int key = Integer.valueOf(keyString);

            String valueString = challengesObject.getString(keyString);
            byte[] valueBytes = Utils.base64StringToElementBytes(valueString);
            Element valuElement = verifier.getKeys().getPairing().getZr().newElementFromBytes(valueBytes);

            challenges.add(new Challenge(key, valuElement));
        }

        return challenges;

    }

    public static Map<String,Element> getProof(Verifier verifier,String proofString) {

        JSONObject proofObject = JSONObject.parseObject(proofString);

        Map<String,Element> tempProof = new HashMap<String, Element>();

        String miuString = proofObject.getString("miu");
        Element miu = verifier.getKeys().getPairing().getZr().newElementFromBytes(Utils.base64StringToElementBytes(miuString));

        String wString = proofObject.getString("R");
        Element R = verifier.getKeys().getPairing().getG2().newElementFromBytes(Utils.base64StringToElementBytes(wString));

        String hashMulString = proofObject.getString("hashMul");
        Element hashMul = verifier.getKeys().getPairing().getG1().newElementFromBytes(Utils.base64StringToElementBytes(hashMulString));

        tempProof.put("miu", miu);
        tempProof.put("R", R);
        tempProof.put("hashMul", hashMul);

        return tempProof;

    }

    public static String SHA256(byte[] text) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        // 传入要加密的字符串
        messageDigest.update(text);
        // 得到 byte 類型结果
        byte[] hash = messageDigest.digest();
        Base64.Encoder encoder = Base64.getEncoder();
        String base64 = encoder.encodeToString(hash);
        return base64;
    }




}
