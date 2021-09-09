package org.example;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

public class Share {

    public static String queryAllShare(Network network) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryAllShare");
        //System.out.println(new String(result));

        String allShareJsonArrayString = new String(result);

        /*String[] ShareColumnNames = TextOutput.shareColumnNames();
        Object[][] data = TextOutput.shareArrayToData(allShareJsonArrayString);
        TextOutput.tableOutput("共享记录",ShareColumnNames,data);*/
        TextOutput.jsonOutput("共享记录",allShareJsonArrayString);

        return allShareJsonArrayString;
    }

    public static String queryShare(Network network, String shortRecord, String timestamp) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];


        result = contract.evaluateTransaction("queryShare",channel,ownerId,name,timestamp);
        //System.out.println(new String(result));

        TextOutput.jsonOutput("共享记录",new String(result));
        return new String(result);

    }

    public static String queryDataShare(Network network, String shortRecord) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];

        result = contract.evaluateTransaction("queryDataShare",channel,ownerId,name);
        //System.out.println(new String(result));

        String allShareJsonArrayString = new String(result);

        /*String[] shareColumnNames = TextOutput.shareColumnNames();
        Object[][] data = TextOutput.shareArrayToData(allShareJsonArrayString);
        TextOutput.tableOutput("共享记录",shareColumnNames,data);*/
        TextOutput.jsonOutput("共享记录",allShareJsonArrayString);

        return allShareJsonArrayString;
    }
}
