package org.example;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;


public class Audit {

    public static String genChallenge(Network network,String shortDataRecord, int c)throws Exception{
        Contract contract = network.getContract("cbfm-java");
        byte[] result;
        
        System.out.println("\nSubmitTx: Generate Challenge...");
        result = contract.submitTransaction("challengeGen",shortDataRecord,String.valueOf(c));

        TextOutput.jsonOutput("审计记录",new String(result));

        return new String(result);

    }

    public static String genChallenge(Network network,String shortDataRecord)throws Exception{
        Contract contract = network.getContract("cbfm-java");
        byte[] result;

        System.out.println("\nSubmitTx: Generate Challenge...");
        result = contract.submitTransaction("auditAll",shortDataRecord);

        TextOutput.jsonOutput("审计记录",new String(result));

        return new String(result);

    }

    public static String setChaincodeEventListener(Channel channel, String expectedEventName, Vector<ChaincodeEventCapture> chaincodeEvents) throws Exception {

        ChaincodeEventListener chaincodeEventListener = new ChaincodeEventListener() {

            @Override
            public void received(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
                chaincodeEvents.add(new ChaincodeEventCapture(handle, blockEvent, chaincodeEvent));

                String eventHub = blockEvent.getPeer().toString();
                if(eventHub != null){
                    eventHub = blockEvent.getPeer().getName();
                } else {
                    eventHub = blockEvent.getEventHub().getName();
                }
                // Here put what you want to do when receive chaincode event
                TextOutput.jsonOutput("捕获事件","RECEIVED CHAINCODE EVENT with handle: " + handle + ", chaincodeId: " + chaincodeEvent.getChaincodeId() + ", chaincode event name: " + chaincodeEvent.getEventName() + ", transactionId: " + chaincodeEvent.getTxId() +", event Payload: " + new String(chaincodeEvent.getPayload()) + ", from eventHub: " + eventHub);
                //System.out.println("RECEIVED CHAINCODE EVENT with handle: " + handle + ", chaincodeId: " + chaincodeEvent.getChaincodeId() + ", chaincode event name: " + chaincodeEvent.getEventName() + ", transactionId: " + chaincodeEvent.getTxId() +", event Payload: " + new String(chaincodeEvent.getPayload()) + ", from eventHub: " + eventHub);
            }

        };
        // chaincode events.
        String eventListenerHandle = channel.registerChaincodeEventListener(Pattern.compile(".*"), Pattern.compile(Pattern.quote(expectedEventName)), chaincodeEventListener);
        return eventListenerHandle;
    }

    public static String waitForChaincodeEvent(Integer timeout, Channel channel, Vector<ChaincodeEventCapture> chaincodeEvents, String chaincodeEventListenerHandle,String currentUserMsp, String currentUsername,Network network) throws Exception {
        boolean eventDone = false;
        if (chaincodeEventListenerHandle != null) {


            int numberEventsExpected = 1;//channel.getEventHubs().size() + channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).size();
            //System.out.println("numberEventsExpected: " + numberEventsExpected);
            //just make sure we get the notifications
            if (timeout.equals(0)) {
                // get event without timer
                while (chaincodeEvents.size() < numberEventsExpected) {
                    // do nothing
                }
                eventDone = true;
            } else {
                // get event with timer
                for (int i = 0; i < timeout; i++) {
                    if (chaincodeEvents.size() > numberEventsExpected) {
                        eventDone = true;
                        break;
                    } else {
                        try {
                            double j = i;
                            j = j / 10;
                            //System.out.println(j + " second");
                            Thread.sleep(100); // wait for the events for one tenth of second.
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            //System.out.println("chaincodeEvents.size(): " + chaincodeEvents.size());

            // unregister event listener
            channel.unregisterChaincodeEventListener(chaincodeEventListenerHandle);
            List<String> txList = new ArrayList<>();
            int i = 1;
            // arrived event handling
            for (ChaincodeEventCapture chaincodeEventCapture : chaincodeEvents) {

                String tx = chaincodeEventCapture.getChaincodeEvent().getTxId();
                if(!txList.contains(tx)) {
                    txList.add(tx);
                    String eventString = new String(chaincodeEventCapture.getChaincodeEvent().getPayload());
                    String eventName = new String(chaincodeEventCapture.getChaincodeEvent().getEventName());
                    if (eventName.contains(currentUserMsp+"_"+currentUsername+"_VerifyEvent")){
                        //updateAuditResult(network,eventString);
                        TextOutput.jsonOutput("审计结果",new String(chaincodeEventCapture.getChaincodeEvent().getPayload()));
                        return new String(chaincodeEventCapture.getChaincodeEvent().getPayload());
                    }else{
                        System.out.println("EventName Error");
                    }

                }

                i++;
            }

        } else {
            System.out.println("chaincodeEvents.isEmpty(): " + chaincodeEvents.isEmpty());
        }
        //System.out.println("eventDone: " + eventDone);
        //return eventDone;
        return "{}";
    }

    /*public static String updateAuditResult(Network network, String auditString) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;


        JSONObject auditObject = JSONObject.parseObject(auditString);
        String Datakey = auditObject.getString("datakey");
        String channel = Datakey.split("\u0000")[2];
        String ownerId = Datakey.split("\u0000")[3];
        String name = Datakey.split("\u0000")[4];

        String auditorMsp = auditObject.getString("auditorMsp");
        String auditUser = auditObject.getString("auditor");
        String auditTime = auditObject.getString("timestamp");
        String auditResult = String.valueOf(auditObject.getBoolean("result"));

        result = contract.submitTransaction("updateAuditResult",channel,ownerId,name,auditTime,auditResult);

        String resultString = new String(result);
        TextOutput.jsonOutput("数据长记录",resultString);
        //System.out.println("数据信息:");
        //System.out.println(resultString);

        return resultString;
    }*/

    public static String queryAllAudit(Network network) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;
        result = contract.evaluateTransaction("queryAllAudit");
        //System.out.println(new String(result));

        String allAuditJsonArrayString = new String(result);
        TextOutput.jsonOutput("审计记录",allAuditJsonArrayString);

        /*String[] AuditColumnNames = TextOutput.auditColumnNames();
        Object[][] data = TextOutput.auditArrayToData(allAuditJsonArrayString);
        TextOutput.tableOutput("审计记录",AuditColumnNames,data);*/
        /*
        JSONArray allDataJsonArray = JSON.parseArray(allDataJsonArrayString);

        int size = allDataJsonArray.size();
        String dataString;
        System.out.println("数据信息:");
        for (int i = 0; i < size;i++){
            JSONObject jsonObject = allDataJsonArray.getJSONObject(i);
            dataString = jsonObject.toString();
            System.out.println(dataString);
            if(checkDataIsDeleted(dataString)){
                System.out.println("该数据已被删除!");
            }
            System.out.println();
        }
        */
        return allAuditJsonArrayString;
    }

    public static String queryAudit(Network network, String shortRecord, String timestamp) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];


        result = contract.evaluateTransaction("queryAudit",channel,ownerId,name,timestamp);
        //System.out.println(new String(result));

        TextOutput.jsonOutput("审计记录",new String(result));
        return new String(result);
    }

    public static String queryDataAudit(Network network, String shortRecord) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];

        result = contract.evaluateTransaction("queryDataAudit",channel,ownerId,name);
        //System.out.println(new String(result));

        String allAuditJsonArrayString = new String(result);

        /*String[] AuditColumnNames = TextOutput.auditColumnNames();
        Object[][] data = TextOutput.auditArrayToData(allAuditJsonArrayString);
        TextOutput.tableOutput("审计记录",AuditColumnNames,data);*/

        TextOutput.jsonOutput("审计记录",allAuditJsonArrayString);


        return allAuditJsonArrayString;
    }

    public static String queryDataLatestAudit(Network network, String shortRecord) throws Exception {

        Contract contract = network.getContract("cbfm-go");
        byte[] result;

        if (shortRecord.split("-").length<3){
            throw new Exception("short record formation error");
        }

        String channel = shortRecord.split("-",3)[0];
        String ownerId = shortRecord.split("-",3)[1];
        String name = shortRecord.split("-",3)[2];


        result = contract.evaluateTransaction("queryDataLatestAudit",channel,ownerId,name);
        //System.out.println(new String(result));

        TextOutput.jsonOutput("审计记录",new String(result));

        return new String(result);
    }


}
