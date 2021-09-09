package org.example;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;

import java.io.Console;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    static {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
    }

    public static Wallet.Identity adminIdentity;

    public static void helper(){
        System.out.println("Usage:");
        System.out.println("    java cbfm-client.jar -Doption=[option] -Dorg=[org] -Dport=[port]");
        System.out.println("    -Doption=[option] - one of 'enroll' , 'login', and 'help'.");
        System.out.println("    -Dorg=[org] - the name of organization.");
        System.out.println("    -Dport=[port] - the port that used to listen the register requests.");
        System.out.println("Typically, one would first enroll admin user and then login to wait for requests. e.g.:");
        System.out.println("    java -Doption=enroll -Dorg=Org1 -jar cbfm-ca.jar");
        System.out.println("    java -Doption=login -Dorg=Org1 -Dport=10001 -jar cbfm-ca.jar");
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println();
        System.out.println("基于云链融合的车联网数据安全框架 -- NGNLab 2021");
        System.out.println();
        try{
            //System.out.println("option:"+System.getProperty("option")+" org:"+System.getProperty("org"));
            String option = System.getProperty("option");
            String org = System.getProperty("org");
            String port = System.getProperty("port");
            if(option==null){
                helper();
                return;
            }
            switch(option){
                case "enroll":
                    if(org==null){
                        helper();
                        break;
                    }
                    try {
                        EnrollAdmin.enrollAdmin(org);
                        System.out.println(org+"admin 注册成功!");
                    } catch (Exception e) {
                        System.out.println("注册失败!");
                        e.printStackTrace();
                    }
                    break;
                case "login":
                    if(org==null || port==null){
                        helper();
                        break;
                    }
                    try{
                        RegisterUser.login(port);
                    }catch (Exception e){
                        System.out.println("登录失败!");
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

}
