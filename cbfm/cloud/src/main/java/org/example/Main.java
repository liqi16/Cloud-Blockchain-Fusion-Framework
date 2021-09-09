package org.example;

import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static String MSP_Name;
    public static String Cloud_Name;

    public static String path = "/home/simba/gopath/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/cloud/";

    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
    }

    public static void helper(){
        System.out.println("Usage:");
        System.out.println("    java cbfm-cloud.jar -Doption=[option] -Dcs=[cs] -Dcsport=[csport] -Dcaport=[caport]");
        System.out.println("    -Doption=[option] - one of 'register' , 'login', 'update' and help.");
        System.out.println("    -Dcs=[cs] - the name of cloud service.");
        System.out.println("    -Dcsport=[csport] - the port that Cloud used to transfer data.");
        System.out.println("    -Dcaport=[caport] - the port that CA used to listen the register requests.");
        System.out.println("Typically, one would first register cloud and then login to wait for requests. e.g.:");
        System.out.println("    java -Doption=register -Dcs=aliyun01 -Dcsport=10002 -Dcaport=10001 -jar cbfm-cloud.jar");
        System.out.println("    java -Doption=login -Dcs=aliyun01 -jar cbfm-cloud.jar");
        System.out.println("    java -Doption=update -Dcs=aliyun01 -Dcsport=10003 -jar cbfm-cloud.jar");
    }

    public static void main(String[] args) throws Exception{

        //System.out.println( "Hello World!" );
        //EnrollAdmin.enrollAdmin();
        //RegisterUser.registerUser();
        //AppCloud.cloudApp();

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("基于区块链的车联网数据安全框架 -- NGNLab 2021");
        System.out.println();
        try {
            //System.out.println("option:"+System.getProperty("option")+" org:"+System.getProperty("org"));
            String option = System.getProperty("option");
            String cs = System.getProperty("cs");
            String csport = System.getProperty("csport");
            String caport = System.getProperty("caport");
            if (option == null || cs == null) {
                helper();
                return;
            }
            String CloudName = cs;
            String caIP = "127.0.0.1";
            String IP = "127.0.0.1";
            switch (option){
                case "register":
                    if (csport == null || caport == null) {
                        helper();
                        return;
                    }
                    try {
                        String cloudString = registerCloud(caIP,caport,CloudName,IP,csport);
                        TextOutput.jsonOutput("云信息", cloudString);
                    } catch (Exception e) {
                        System.out.println("注册失败!");
                        e.printStackTrace();
                    }
                    break;
                case "login":
                    try {
                        loginCloud(CloudName);
                        /*
                         *Cloud Function End
                         *
                         */
                    } catch (Exception e) {
                        System.out.println("登录失败!");
                        e.printStackTrace();
                    }
                    break;
                case "update":
                    if (csport == null) {
                        helper();
                        return;
                    }
                    try {
                        String cloudString = updateCloud(CloudName,IP,csport);
                        TextOutput.jsonOutput("云信息", cloudString);
                        System.out.println("更新成功!");
                    } catch (Exception e) {
                        System.out.println("更新失败!");
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
        }catch(Exception e){
            e.printStackTrace();
            helper();
        }

    }

    public static String registerCloud(String caIP,String caport,String CloudName,String IP, String csport) throws Exception {
        Client client = new Client(caIP,caport);
        client.recvCert(CloudName);
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..","first-network","connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, CloudName).networkConfig(networkConfigPath).discovery(true);
        Wallet.Identity cloudIdentity = wallet.get(CloudName);
        // create a gateway connection
        Gateway gateway = builder.connect();
        Network network = gateway.getNetwork("mychannel");

        String cloudString = Cloud.cloudRegister(network,cloudIdentity.getMspId(),CloudName, IP,csport);
        return cloudString;
    }

    public static void loginCloud(String CloudName) throws Exception {
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..","first-network","connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, CloudName).networkConfig(networkConfigPath).discovery(true);

        boolean adminExists = wallet.exists(CloudName);
        if (!adminExists) {
            throw new Exception("An identity for the user \""+CloudName+"\" not exists in the wallet");
        }

        Wallet.Identity cloudIdentity = wallet.get(CloudName);
        // create a gateway connection
        Gateway gateway = builder.connect();
        Network network = gateway.getNetwork("mychannel");

        String cloudString = Cloud.cloudLogin(network,cloudIdentity.getMspId(), CloudName);
        System.out.println("登录成功!");
        MSP_Name = cloudIdentity.getMspId();
        Cloud_Name = CloudName;
        System.out.println("MSP_Name : "+MSP_Name +"  "+ "Cloud_Name : " + Cloud_Name);
        JSONObject cloudObject = JSONObject.parseObject(cloudString);
        int port = Integer.valueOf(cloudObject.getString("Port"));
        /*
         *Cloud Function Start
         *
         */

        Thread auditEventThread = new Event(network,MSP_Name +"_"+  Cloud_Name+ "_" +"AuditEvent");
        auditEventThread.start();

        Thread uploadEventThread = new Event(network,MSP_Name +"_"+  Cloud_Name+ "_" +"DataUploadEvent");
        uploadEventThread.start();

        try {
            Server server = new Server(network,port); // 启动服务端
            System.out.println("\n云平台$ 正在监听 [Socket]...");
            server.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String updateCloud(String CloudName, String IP, String csport) throws Exception {
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallet.createFileSystemWallet(walletPath);

        // load a CCP
        Path networkConfigPath = Paths.get("..", "..","first-network","connection-org1.yaml");

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, CloudName).networkConfig(networkConfigPath).discovery(true);
        Wallet.Identity cloudIdentity = wallet.get(CloudName);
        // create a gateway connection
        Gateway gateway = builder.connect();
        Network network = gateway.getNetwork("mychannel");

        String cloudString = Cloud.cloudUpdate(network,cloudIdentity.getMspId(),CloudName,IP,csport);
        return cloudString;
    }
}
