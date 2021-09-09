package org.example;

import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

import java.io.File;
import java.security.PrivateKey;

import static org.example.ECC.imporSK;

public class Sign {

    public static String CalculateHash(String url) throws Exception{
        File dataFile = new File(url);
        if(!dataFile.exists()){
            System.out.println("本地文件不存在");
            throw new Exception("Error! Cannot find file " + url);
        }

        String md5Hashcode = Md5.getFileMD5(dataFile); //md5 hash
        return md5Hashcode;
    }

    public static String Sign(String hash) throws Exception{
        String skAddr = Main.path+"key/"+ Main.MSP_Name +"-"+ Main.Cloud_Name+"-sigSK.dat";
        PrivateKey importedSK = imporSK(skAddr);
        String signature = ECC.sign(hash, importedSK);
        return signature;
    }

    public static void cloudSign(Network network,String dataString)throws Exception{

        JSONObject dataObject = JSONObject.parseObject(dataString);
        String owner = dataObject.getString("Owner");
        String ownerMsp = dataObject.getString("OwnerMsp");
        String url = dataObject.getString("URL");
        String channel = dataObject.getString("Channel");
        String name = dataObject.getString("Name");
        String hash = CalculateHash(url);
        String sig = Sign(hash);

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        result = contract.submitTransaction("updateCloudSignature",channel,ownerMsp,owner,name,hash,sig);
        String resultString = new String(result);

        System.out.println();
        System.out.println("最新数据记录:");
        System.out.println(resultString);
    }

}