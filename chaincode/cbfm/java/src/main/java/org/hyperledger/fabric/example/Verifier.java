package org.hyperledger.fabric.example;

import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Verifier {

    private PublicParam keys;

    public Verifier(String ownerPkString) {
        // TODO Auto-generated constructor stub
        keys = new PublicParam(ownerPkString);
    }

    public PublicParam getKeys() {
        return keys;
    }

    public int[] generateIdx(int c, int allBlocks) {
        int[] idx = Utils.GenRandom(c, allBlocks);
        return idx;
    }

    public List<Challenge> challengeGen(int c, int[] idx){

        List<Challenge>challenges=new ArrayList<>(c);

        for(int i=0;i<c;i++){
            Element e = keys.getPairing().getZr().newRandomElement().getImmutable();
            challenges.add(new Challenge(idx[i],e));
        }
        return challenges;
    }

    public static Element[] readTags(PublicParam keys, List<Challenge>challenges,String tagsJsonString){

        JSONObject tagsObject = JSONObject.parseObject(tagsJsonString);

        int count = challenges.size();
        Element[] tags = new Element[count];

        for(int i=0;i<count;i++) {
            String jsonStr = tagsObject.getString(String.valueOf(challenges.get(i).num));
            tags[i] = keys.getPairing().getG2().newElement();
            tags[i].setFromBytes(Utils.base64StringToElementBytes(jsonStr));
        }

        return tags;
    }

    public boolean VeriProof(List<Challenge>challenges, Map<String,Element> proof, String tagsJsonString){

        Element miu = proof.get("miu");
        Element R = proof.get("R");
        Element hashMul = proof.get("hashMul");

        Element[] tags = readTags(keys, challenges, tagsJsonString);
        Element sigma = keys.getPairing().getG1().newOneElement();
        int c = challenges.size();
        for(int i=0;i<c;i++) {
            sigma.mul(tags[i].duplicate().powZn(challenges.get(i).random));
        }
        sigma.mul(R);
        sigma.getImmutable();

        Element powMiu = keys.getU().duplicate().powZn(miu).mul(hashMul);
        //e(Hchal,v)
        Element temp1 = keys.getPairing().pairing(powMiu, keys.getV());
        //e(Tp,g1)
        Element temp2 = keys.getPairing().pairing(sigma, keys.getG());
        return (temp1.isEqual(temp2))? true :false;

    }

    /*
    public static int[] randomChooseBlocks(int c, int allBlocks){
        int[] ran=new int[c];
        ran=GenerateRandom.random(1,allBlocks,c);
        SortAlg.sort(ran, 0, c-1);
        return ran;
    }*/

    /*
    public List<Challenge> challengeGen(int c, int[] ran){


        List<Challenge>challenges=new ArrayList<>(c);

        for(int i=0;i<c;i++){

            int r = ran[i];
            Element e = pubInfor.pairing.getZr().newRandomElement();

            //System.out.println(r+" "+ e);

            challenges.add(new Challenge(r,e));
        }
        return challenges;
    }*/

    /*
    public boolean proofVerify(List<Challenge>challenges, Map<String,Element> proof){
        Element aggreTMul=proof.get("aggreTMul");
        Element aggreDMul=proof.get("aggreDMul");
        int c=challenges.size();
        Element aggreBlock=pubInfor.pairing.getG1().newOneElement();
        //modified by simba 20210407
        //for(int i=0;i<c;i++){
        //    byte[] data=String.valueOf(challenges.get(i).num).getBytes();
        //    Element Hid=pubInfor.pairing.getG1().newElementFromHash(data,0,data.length);
        //    Element tmp=Hid.duplicate().powZn(challenges.get(i).random);
        //    aggreBlock=aggreBlock.mul(tmp);
        //} //modified by simba 20210407
        //e(Hchal,v)
        //Element temp1 =pubInfor.pairing.pairing(aggreBlock,pubInfor.v); //modified by simba 20210407
        //e(Tp,g2)
        //Element temp2 = pubInfor.pairing.pairing(aggreTMul, pubInfor.g2); //modified by simba 20210407
        //return (aggreDMul.mul(temp1)).equals(temp2)? true :false; //modified by simba 20210407
        Element temp2 = pubInfor.pairing.pairing(aggreTMul, pubInfor.g2); //modified by simba 20210407
        boolean isTrue = aggreDMul.equals(temp2); //modified by simba 20210407
        return isTrue; ////modified by simba 20210407
    }*/

}
