package org.example;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;

public class Operation {

    public static String queryAllOperation(Network network) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryAllOperation");
        //System.out.println(new String(result));

        String allOperationJsonArrayString = new String(result);

        /*String[] operationColumnNames = TextOutput.operationColumnNames();
        Object[][] data = TextOutput.operationArrayToData(allOperationJsonArrayString);
        TextOutput.tableOutput("操作记录",operationColumnNames,data);*/
        TextOutput.jsonOutput("操作记录",allOperationJsonArrayString);
        return allOperationJsonArrayString;
    }

    public static String queryOperation(Network network, String shortRecord, String timestamp) throws Exception {

        Contract contract = network.getContract("cbfm-go");
       
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];


        result = contract.evaluateTransaction("queryOperation",channel,ownerId,name,timestamp);
        //System.out.println(new String(result));

        String allOperationJsonArrayString = new String(result);
        TextOutput.jsonOutput("操作记录",allOperationJsonArrayString);

        return allOperationJsonArrayString;
    }

    public static String queryDataOperation(Network network,String shortRecord) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];

        result = contract.evaluateTransaction("queryDataOperation",channel,ownerId,name);
        //System.out.println(new String(result));

        String allOperationJsonArrayString = new String(result);

        /*String[] operationColumnNames = TextOutput.operationColumnNames();
        Object[][] data = TextOutput.operationArrayToData(allOperationJsonArrayString);
        TextOutput.tableOutput("操作记录",operationColumnNames,data);*/
        TextOutput.jsonOutput("操作记录",allOperationJsonArrayString);
        return allOperationJsonArrayString;
    }


}
