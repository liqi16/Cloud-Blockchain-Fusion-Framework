package org.example;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.sdk.Channel;
import java.util.ArrayList;
import java.util.Vector;

public class JarInterface {

    public static String uploadData(String CurrentUser,String DataName, String CloudMSP, String CloudName, String filePath, String SymKey, String Policy, String Introduction) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String MspId = network.getGateway().getIdentity().getMspId();

        String shortRecord = Data.uploadMetaData(network, MspId, DataName, CurrentUser, CloudMSP, CloudName, filePath, SymKey, Policy, Introduction);
        return shortRecord;
    }

    public static String auditData(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String MspId = network.getGateway().getIdentity().getMspId();
        //int c = Integer.valueOf(C);
        System.out.println("\n正在发起审计...");
        //String auditString = Audit.genChallenge(network,ShortRecord,c);
        String auditString = Audit.genChallenge(network,ShortRecord);
        System.out.println("\n等待审计结果...");
        // START CHAINCODE EVENT LISTENER HANDLER----------------------
        String expectedEventName = MspId+"_"+CurrentUser+"_VerifyEvent";
        Channel channel = network.getChannel();
        Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture
        String chaincodeEventListenerHandle = Audit.setChaincodeEventListener(channel, expectedEventName, chaincodeEvents);
        // END CHAINCODE EVENT LISTENER HANDLER------------------------

        // START WAIT FOR THE EVENT-------------------------------------
        String auditRecord = Audit.waitForChaincodeEvent(3000, channel, chaincodeEvents, chaincodeEventListenerHandle,MspId,CurrentUser,network);
        return auditRecord;
    }

    public static String updateData(String CurrentUser,String ShortRecord, String filePath, String SymKey, String Policy,String Introduction) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String MspId = network.getGateway().getIdentity().getMspId();
        String dataString = Data.updateData(network,MspId,CurrentUser,ShortRecord,filePath,SymKey,Policy,Introduction);
        return dataString;
    }

    public static Boolean verifyData(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        Boolean isTrue = Cloud.verifySignature(network,ShortRecord);
        return  isTrue;
    }

    public static String downloadData(String CurrentUser, String isEncrypted, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String filePath = "";
        if (isEncrypted.equals("Ciphertext")){
            filePath = Data.downloadEncryptedData(network,ShortRecord);
        }else if (isEncrypted.equals("Plaintext")) {
            filePath = Data.downloadDecryptedData(network, ShortRecord, CurrentUser);
        }
        return  filePath;
    }

    public static String queryAllUser(String CurrentUser) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = User.queryAllUser(network);
        return resultString;
    }

    public static String queryAllCloud(String CurrentUser) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Cloud.queryAllCloud(network);
        return resultString;
    }

    public static ArrayList<String> queryAllData(String CurrentUser) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        ArrayList<String> resultString = Data.queryAllData(network);
        return resultString;
    }

    public static String queryAllOperation(String CurrentUser) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Operation.queryAllOperation(network);
        return resultString;
    }

    public static String queryAllShare(String CurrentUser) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Share.queryAllShare(network);
        return resultString;
    }

    public static String queryAllAudit(String CurrentUser) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Audit.queryAllAudit(network);
        return resultString;
    }

    public static String queryUser(String CurrentUser, String QueryUserMsp, String QueryUsername) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = User.queryUser(network,QueryUserMsp,QueryUsername);
        return resultString;
    }

    public static String queryUserID(String CurrentUser,String QueryUserID) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = User.queryID(network,QueryUserID);
        return resultString;
    }

    public static String queryCloud(String CurrentUser, String QueryMSP, String QueryCloud) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Cloud.queryCloud(network,QueryMSP,QueryCloud);
        return resultString;
    }

    public static String queryData(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Data.queryData(network,ShortRecord);
        return resultString;
    }

    public static String queryDataHistroy(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Data.queryDataHistory(network,ShortRecord);
        return resultString;
    }

    public static String querySbData(String CurrentUser, String OwnerId) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Data.querySbData(network,OwnerId);
        return resultString;
    }

    public static String queryDataOperation(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Operation.queryDataOperation(network,ShortRecord);
        return resultString;
    }

    public static String queryOperation(String CurrentUser,String ShortRecord,String timeStamp) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Operation.queryOperation(network,ShortRecord,timeStamp);
        return resultString;
    }

    public static String queryDataShare(String CurrentUser,String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Share.queryDataShare(network,ShortRecord);
        return resultString;
    }

    public static String queryShare(String CurrentUser,String ShortRecord,String timeStamp) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Share.queryShare(network,ShortRecord,timeStamp);
        return resultString;
    }

    public static String queryDataAudit(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Audit.queryDataAudit(network,ShortRecord);
        return resultString;
    }

    public static String queryDataLatestAudit(String CurrentUser, String ShortRecord) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Audit.queryDataLatestAudit(network,ShortRecord);
        return resultString;
    }

    public static String queryAudit(String CurrentUser,String ShortRecord, String time) throws Exception {
        Network network = Main.userLogin(CurrentUser);
        String resultString = Audit.queryAudit(network,ShortRecord,time);
        return resultString;
    }

}
