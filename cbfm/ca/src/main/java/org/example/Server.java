package org.example;

import org.hyperledger.fabric.gateway.Network;

import java.io.*;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;

/**
 * 文件传输Server端<br>
 * 功能说明：
 *
 * @author 大智若愚的小懂
 * @Date 2016年09月01日
 * @version 1.0
 */
public class Server extends ServerSocket {
	private Network network;
 
    //private static final int SERVER_PORT = 8899; // 服务端端口
 
    private static DecimalFormat df = null;
 
    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }
 
    public Server(Network NETWORK, int PORT) throws Exception {
        super(PORT);
        this.network = NETWORK;
    }
 
    /**
     * 使用线程处理每个客户端传输的文件
     * @throws Exception
     */
    public void load() throws Exception {
        System.out.println("登录成功，等待用户注册...");
        while (true) {
            // server尝试接收其他Socket的连接请求，server的accept方法是阻塞式的
            Socket socket = this.accept();
            /**
             * 我们的服务端处理客户端的连接请求是同步进行的， 每次接收到来自客户端的连接请求后，
             * 都要先跟当前的客户端通信完之后才能再处理下一个连接请求。 这在并发比较多的情况下会严重影响程序的性能，
             * 为此，我们可以把它改为如下这种异步处理与客户端通信的方式
             */
            // 每接收到一个Socket就建立一个新的线程来处理它
            new Thread(new Task(socket)).start();
        }
    }
 
    /**
     * 处理客户端传输过来的文件线程类
     */
    class Task implements Runnable {
 
        private Socket socket;
 
        private DataInputStream dis;
        
        private DataOutputStream dos;
 
        private FileOutputStream fos;
        
        private FileInputStream fis;
 
        public Task(Socket socket) {
            this.socket = socket;
        }
 
        @Override
        public void run(){
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                String username = dis.readUTF();
                System.out.println("USERNAME:"+username);
                RegisterUser.registerUser(Main.adminIdentity,username);
                String userFilePath = "wallet/"+username+"/"+username;
                String userPrivFilePath = "wallet/"+username+"/"+username+"-priv";

                File userFile = new File(userFilePath);
                fis = new FileInputStream(userFile);
                if(!userFile.exists()) {
                    System.out.println("Error! File "+ userFilePath +" does not exsit. ");
                }else {
                    dos.writeLong(userFile.length());
                    dos.flush();

                    // 开始传输文件
                    System.out.println("开始传输文件");
                    byte[] bytes = new byte[1024];
                    int length = 0;
                    long progress = 0;
                    while((length = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();
                        progress += length;
                        System.out.print("| " + (100*progress/userFile.length()) + "% |");
                    }
                    System.out.println("\n文件传输成功 [File Name：" + userFilePath + "] [Size：" + getFormatFileSize(userFile.length()) + "]");
                }

                dis.readUTF();

                File userPrivFile = new File(userPrivFilePath);
                fis = new FileInputStream(userPrivFile);
                if(!userPrivFile.exists()) {
                    System.out.println("Error! File "+ userPrivFilePath +" does not exsit.");
                }else {
                    dos.writeLong(userPrivFile.length());
                    dos.flush();
                    // 开始传输文件
                    System.out.println("开始传输文件");
                    byte[] bytes = new byte[1024];
                    int length = 0;
                    long progress = 0;
                    while((length = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();
                        progress += length;
                        System.out.print("| " + (100*progress/userPrivFile.length()) + "% |");
                    }
                    System.out.println("\n文件传输成功 [File Name：" + userPrivFilePath + "] [Size：" + getFormatFileSize(userPrivFile.length()) + "]");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if(fos != null)
                        fos.close();
                    if(dis != null)
                        dis.close();
                    if(dos != null)
                        dos.close();
                    socket.close();
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
        }
    }
 
    /**
     * 格式化文件大小
     * @param length
     * @return
     */
    private String getFormatFileSize(long length) {
        double size = ((double) length) / (1 << 30);
        if(size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if(size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if(size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }

    /*
    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
          int number=random.nextInt(62);
          sb.append(str.charAt(number));
        }
        return sb.toString();
    }*/
 
    /**
     * 入口
     * @param args
     */
    /*
    public static void main(String[] args) {
        try {
            Server server = new Server(); // 启动服务端
            server.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
