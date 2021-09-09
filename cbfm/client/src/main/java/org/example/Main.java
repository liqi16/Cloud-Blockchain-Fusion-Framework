package org.example;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.Channel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class Main {

    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost","true");
    }

    public static void helper(){
        System.out.println("Usage:");
        System.out.println("    java -Doption=[option] -Duser=[username] cbfm-client.jar");
        System.out.println("    -Doption=[option] - one of 'register' , 'query', 'upload', 'download', 'audit', 'update', 'check' and 'help'.");
        System.out.println("    -Duser=[username] - the name of current user.");
        System.out.println("Typically, one would first register cloud and then login to wait for requests. e.g.:");
        System.out.println("    java -Doption=register -Duser=alice -jar cbfm-client.jar [Attribute,CaPort]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar [QueryArgs]");
        System.out.println("    java -Doption=upload -Duser=alice -jar cbfm-client.jar [DataName, CloudMSP, CloudName, FilePath, SymmetricKey, AccessPolicy, Introduction]");
        System.out.println("    java -Doption=download -Duser=alice -jar cbfm-client.jar [Ciphertext/Plaintext,ShortRecord]");
        //System.out.println("    java -Doption=audit -Duser=alice -jar cbfm-client.jar [ShortRecord, AuditBlocksNum]");
        System.out.println("    java -Doption=audit -Duser=alice -jar cbfm-client.jar [ShortRecord]");
        System.out.println("    java -Doption=update -Duser=alice -jar cbfm-client.jar [ShortRecord, FilePath, SymmetricKey, Policy, Introduction]");
        System.out.println("    java -Doption=check -Duser=alice -jar cbfm-client.jar [ShortRecord]");
        queryHelper();
    }

    public static void updateHelper(){
        System.out.println("===============================================");
        System.out.println("若更新文件，请填写\"文件名\"。");
        System.out.println("若更新密钥，请填写\"加密密钥\"。");
        System.out.println("若更新访问策略，请填写\"文件访问属性\"。");
        System.out.println("若更新文件简介，请填写\"文件介绍\"。");
        System.out.println("无需更新的内容，请输入\"**\"。");
        System.out.println("===============================================");
    }

    public static void queryHelper(){
        System.out.println("Usage [Query]:");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar [QueryArgs]");
        System.out.println("E.g.");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar all user");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar all cloud");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar all data");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar all operation");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar all share");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar all audit");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar user [MSP] [Username]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar user [UserID]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar cloud [MSP] [Cloud]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar data record [ShortRecord]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar data history [ShortRecord]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar data all [UserID]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar operation [ShortRecord] [Timestamp]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar operation [ShortRecord]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar share [ShortRecord] [Timestamp]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar share [ShortRecord]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar audit [ShortRecord] [Timestamp]");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar audit [ShortRecord] ");
        System.out.println("    java -Doption=query -Duser=alice -jar cbfm-client.jar audit [ShortRecord] latest");
    }

    public static void main( String[] args ) throws Exception {
        //System.out.println( "Hello World!" );
        //EnrollAdmin.enrollAdmin();
        //RegisterUser.registerUser();

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("基于区块链的车联网数据安全框架 -- NGNLab 2021");
        System.out.println();

        try {
            //System.out.println("option:"+System.getProperty("option")+" org:"+System.getProperty("org"));
            String option = System.getProperty("option");
            String CurrentUser = System.getProperty("user");
            Network network;
            String ShortRecord;
            String DataName;
            String CloudMSP;
            String CloudName;
            String filePath;
            String SymKey;
            String Policy;
            String Introduction;
            if (option == null || CurrentUser == null) {
                helper();
                return;
            }
            switch (option){
                case "register":
                    System.out.println(" *** 注册用户 ***");
                    if (args.length != 2){
                        System.out.println("Error! Register need 2 args: [Attribute,CaPort]");
                        helper();
                        break;
                    }
                    String caIP="127.0.0.1";
                    String Attribute = args[0];
                    String caPort = args[1];
                    String Username = CurrentUser;
                    String userString  = userRegister(caIP,caPort,Username,Attribute);
                    TextOutput.jsonOutput("用户信息", userString);
                    break;
                case "query":
                    //queryHelper();
                    network = userLogin(CurrentUser);
                    if (args.length ==0 || args.length >3){
                        System.out.println("Error!");
                        queryHelper();
                        break;
                    }
                    try {
                        switch (args[0]){
                            case "all":
                                if (args.length != 2){
                                    queryHelper();
                                }else{
                                    if (args[1].equals("user")){
                                        System.out.println(" *** 查询全部用户 ***");
                                        try{
                                            User.queryAllUser(network);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if(args[1].equals("cloud")){
                                        System.out.println(" *** 查询全部云平台 ***");
                                        try{
                                            Cloud.queryAllCloud(network);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if (args[1].equals("data")){
                                        System.out.println(" *** 查询全部数据 ***");
                                        try{
                                            Data.queryAllData(network);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if (args[1].equals("operation")){
                                        System.out.println(" *** 查询全部操作记录 ***");
                                        try{
                                            Operation.queryAllOperation(network);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if (args[1].equals("share")){
                                        System.out.println(" *** 查询全部共享记录 ***");
                                        try{
                                            Share.queryAllShare(network);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if (args[1].equals("audit")){
                                        System.out.println(" *** 查询全部审计记录 ***");
                                        try{
                                            Audit.queryAllAudit(network);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else{
                                        queryHelper();
                                    }
                                }
                                break;
                            case "user":
                                if (args.length == 3){
                                    System.out.println(" *** 查询用户信息 ***");
                                    String QueryUserMsp = args[1];
                                    String QueryUsername = args[2];
                                    try{
                                        User.queryUser(network,QueryUserMsp,QueryUsername);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else if (args.length == 2){
                                    System.out.println(" *** 查询ID信息 ***");
                                    String QueryUserID = args[1];
                                    try{
                                        User.queryID(network,QueryUserID);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else {
                                    queryHelper();
                                }
                                break;
                            case "cloud":
                                if (args.length == 3){
                                    System.out.println(" *** 查询云服务器信息 ***");
                                    String QueryMSP = args[1];
                                    String QueryCloud= args[2];
                                    try{
                                        Cloud.queryCloud(network,QueryMSP,QueryCloud);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else {
                                    queryHelper();
                                }
                                break;
                            case "data":
                                if (args.length == 3){
                                    if (args[1].equals("record")){
                                        System.out.println(" *** 查询数据记录 ***");
                                        ShortRecord = args[2];
                                        try{
                                            String dataString = Data.queryData(network,ShortRecord);
                                            TextOutput.jsonOutput("数据长记录",dataString);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if (args[1].equals("history")){
                                        System.out.println(" *** 查询数据历史 ***");
                                        ShortRecord = args[2];
                                        try{
                                            Data.queryDataHistory(network,ShortRecord);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else if (args[1].equals("all")){
                                        System.out.println(" *** 查询某用户全部数据 ***");
                                        String OwnerId = args[2];
                                        try{
                                            Data.querySbData(network,OwnerId);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else {
                                        queryHelper();
                                    }
                                }else {
                                    queryHelper();
                                }
                                break;
                            case "operation":
                                if (args.length == 2){
                                    System.out.println(" *** 查询某数据操作记录 ***");
                                    ShortRecord = args[1];
                                    System.out.println("===============================");
                                    try{
                                        Operation.queryDataOperation(network,ShortRecord);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else if (args.length == 3){
                                    System.out.println(" *** 查询数据记录 ***");
                                    ShortRecord = args[1];
                                    String timeStamp = args[2];
                                    try{
                                        Operation.queryOperation(network,ShortRecord,timeStamp);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else {
                                    queryHelper();
                                }
                                break;
                            case "share":
                                if (args.length==2){
                                    System.out.println(" *** 查询某数据操作记录 ***");
                                    ShortRecord = args[1];
                                    try{
                                        Share.queryDataShare(network,ShortRecord);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else if (args.length ==3){
                                    System.out.println(" *** 查询数据记录 ***");
                                    ShortRecord = args[1];
                                    String timeStamp = args[2];
                                    System.out.println("===============================");
                                    try{
                                        Share.queryShare(network,ShortRecord,timeStamp);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else {
                                    queryHelper();
                                }
                                break;
                            case "audit":
                                if (args.length == 2){
                                    System.out.println(" *** 查询某数据全部审计记录 ***");
                                    ShortRecord = args[1];
                                    try{
                                        Audit.queryDataAudit(network,ShortRecord);
                                        System.out.println("查询成功!");
                                    }catch (Exception e){
                                        System.out.println("查询失败!");
                                        e.printStackTrace();
                                    }
                                }else if (args.length == 3){
                                    if (args[2].equals("latest")){
                                        System.out.println(" *** 查询某数据最新审计记录 ***");
                                        ShortRecord = args[1];
                                        System.out.println("===============================");
                                        try{
                                            Audit.queryDataLatestAudit(network,ShortRecord);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }else{
                                        System.out.println(" *** 查询审计记录 ***");
                                        ShortRecord = args[1];
                                        String time = args[2];
                                        System.out.println("===============================");
                                        try{
                                            Audit.queryAudit(network,ShortRecord,time);
                                            System.out.println("查询成功!");
                                        }catch (Exception e){
                                            System.out.println("查询失败!");
                                            e.printStackTrace();
                                        }
                                    }
                                }else {
                                    queryHelper();
                                }
                                break;
                            default:
                                queryHelper();
                                break;
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                        queryHelper();
                    }
                    break;
                case "upload":
                    if (args.length != 7){
                        System.out.println("Error! Upload need 7 args: [DataName, CloudMSP, CloudName, FilePath, SymmetricKey, AccessPolicy, Introduction]");
                        helper();
                        break;
                    }
                    DataName = args[0];
                    CloudMSP = args[1];
                    CloudName = args[2];
                    filePath = args[3];
                    SymKey = args[4];
                    Policy = args[5];
                    Introduction = args[6];
                    network = userLogin(CurrentUser);
                    String MspId = network.getGateway().getIdentity().getMspId();

                    try{
                        Data.uploadMetaData(network,MspId,DataName,CurrentUser,CloudMSP,CloudName,filePath,SymKey,Policy,Introduction);
                        System.out.println("上传数据成功!");
                    }catch (Exception e){
                        System.out.println("上传数据失败!");
                        e.printStackTrace();
                    }

                    break;
                case "download":
                    if (args.length != 2){
                        System.out.println("Error! Download need 2 args: [Ciphertext/Plaintext,ShortRecord]");
                        helper();
                        break;
                    }
                    String isEncrypted = args[0];
                    ShortRecord = args[1];
                    network = userLogin(CurrentUser);
                    if (isEncrypted.equals("Ciphertext")){
                        try{
                            Data.downloadEncryptedData(network,ShortRecord);
                            System.out.println("下载成功!");
                        }catch (Exception e){
                            System.out.println("下载失败!");
                            e.printStackTrace();
                        }
                    }else if (isEncrypted.equals("Plaintext")){
                        try{
                            Data.downloadDecryptedData(network,ShortRecord,CurrentUser);
                            System.out.println("下载成功!");
                        }catch (Exception e){
                            System.out.println("下载失败!");
                            e.printStackTrace();
                        }

                    }else{
                        System.out.println("Error! The first args should be Ciphertext/Plaintext.");
                        helper();
                    }
                    break;
                case "audit":
                    if (args.length != 1){
                        //System.out.println("Error! Audit need 2 args: [ShortRecord, AuditBlocksNum]");
                        System.out.println("Error! Audit need 1 args: [ShortRecord]");
                        helper();
                        break;
                    }
                    ShortRecord = args[0];
                    //String C = args[1];
                    network = userLogin(CurrentUser);
                    MspId = network.getGateway().getIdentity().getMspId();
                    try{
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
                        Audit.waitForChaincodeEvent(3000, channel, chaincodeEvents, chaincodeEventListenerHandle,MspId,CurrentUser,network);
                        System.out.println("审计成功!");
                    }catch (Exception e){
                        System.out.println("审计失败!");
                        e.printStackTrace();
                    }

                    break;
                case "update":
                    updateHelper();
                    if (args.length != 5){
                        System.out.println("Error! Update need 5 args: [ShortRecord, FilePath, SymmetricKey, Policy, Introduction]");
                        helper();
                        break;
                    }
                    ShortRecord = args[0];
                    filePath = args[1];
                    SymKey = args[2];
                    Policy = args[3];
                    Introduction = args[4];
                    network = userLogin(CurrentUser);
                    MspId = network.getGateway().getIdentity().getMspId();
                    try{
                        Data.updateData(network,MspId,CurrentUser,ShortRecord,filePath,SymKey,Policy,Introduction);
                        System.out.println("更新成功!");
                    }catch (Exception e){
                        System.out.println("更新失败!");
                        e.printStackTrace();
                    }


                    break;
                case "check":
                    if (args.length != 1){
                        System.out.println("Error! Check need 1 args: [ShortRecord]");
                        helper();
                        break;
                    }
                    ShortRecord = args[0];
                    network = userLogin(CurrentUser);
                    try {
                        Cloud.verifySignature(network,ShortRecord);
                        System.out.println("验证成功!");
                    }catch (Exception e){
                        System.out.println("验证失败!");
                        e.printStackTrace();
                    }

                    break;
                case "help":
                    helper();
                    break;
                default:
                    helper();
                    break;
            }

        }catch (Exception e){
            e.printStackTrace();
            helper();
        }
    }

    public static String userRegister(String caIP,String caPort, String Username,String Attribute) throws Exception {

        FileTransferClient client = new FileTransferClient(caIP,caPort);
        client.recvCert(Username);

        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..","first-network","connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, Username).networkConfig(networkConfigPath).discovery(true);
        Wallet.Identity userIdentity = wallet.get(Username);
        // create a gateway connection
        Gateway gateway = builder.connect();
        // get the network and contract
        Network network = gateway.getNetwork("mychannel");

        Boolean mk = false;

        try{
            mk = MasterKey.queryMasterKey(network);
        }catch (Exception e){
            mk = MasterKey.initLedger(network);
        }


        String userString = User.userRegister(network,userIdentity.getMspId(), Username,Attribute);


        return userString;
    }

    public static Network userLogin(String CurrentUsername) throws Exception{

        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..","first-network","connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, CurrentUsername).networkConfig(networkConfigPath).discovery(true);
        Wallet.Identity userIdentity = wallet.get(CurrentUsername);
        // create a gateway connection
        Gateway gateway = builder.connect();
        // get the network and contract
        Network network = gateway.getNetwork("mychannel");

        User.userLogin(network, userIdentity.getMspId(),CurrentUsername);
        System.out.println("登录成功!");

        return network;
    }




}
