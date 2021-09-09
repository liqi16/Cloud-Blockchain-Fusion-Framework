package org.example;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

public class MasterKey {

    public static boolean queryMasterKey(Network network) throws Exception {
        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryMasterKey");
        //System.out.println(new String(result));

        //String path = "/home/simba/gopath/src/github.com/hyperledger/fabric/scripts/fabric-samples/fabsystem/fabsystem-golang/";

        Utils.writeFile(ClientApp.path +"go/masterKey",new String(result));

        return true;
    }

    public static boolean initLedger(Network network){

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        try{
            result = contract.submitTransaction("Init");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("系统初始化失败");
            return false;
        }
        System.out.println("系统初始化成功");
        return true;
    }

}
