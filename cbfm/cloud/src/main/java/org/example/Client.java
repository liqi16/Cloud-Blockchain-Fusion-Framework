package org.example;

import java.io.*;
import java.math.RoundingMode;
import java.net.Socket;
import java.text.DecimalFormat;


public class Client extends Socket {

    //private static final String SERVER_IP = "127.0.0.1";//= "192.168.154.136"; // 服务端IP
    //private static final int SERVER_PORT = 8899; // 服务端端口

    private Socket client;

    private FileInputStream fis;

    private FileOutputStream fos;

    private DataOutputStream dos;

    private DataInputStream dis;

    private static DecimalFormat df = null;

    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }


    /**
     * 构造函数<br/>
     * 与服务器建立连接
     * @throws Exception
     */
    public Client(String Client_IP, String Client_Port) throws Exception {
        super(Client_IP, Integer.valueOf(Client_Port));
        this.client = this;
        System.out.println("Cliect[port:" + client.getLocalPort() + "] 成功连接服务端");
    }

    /*
    public String chooseFunction(String cmd, List<String> args) throws Exception {
        if (cmd.equals("upload")){
            String username = args.get(0);
            String filePath = args.get(1);
            String fileName = args.get(2);
            return sendFile(username,filePath,fileName);
        }else if (cmd.equals("download")){
            String url = args.get(0);
            return recvFile(url);
        }else{
            System.out.println("Error! cmd = { upload, download }.");
            return null;
        }
    }*/

    public void recvCert(String username) throws Exception {

        try{
            dos = new DataOutputStream(client.getOutputStream());
            dis = new DataInputStream(client.getInputStream());

            dos.writeUTF(username);
            dos.flush();

            Long fileLength = dis.readLong();

            String basePath = Main.path+"wallet/"+username;
            String userFilePath = Main.path+"wallet/"+username+"/"+username;
            String userPrivFilePath = userFilePath+"-priv";

            File baseDictionary = new File(basePath);
            if(!baseDictionary.exists()){
                baseDictionary.mkdir();
            }

            File userFile = new File(userFilePath);
            if(!userFile.exists()){
                userFile.createNewFile();
            }

            fos = new FileOutputStream(userFile);

            // 开始接收文件
            byte[] bytes = new byte[1024];
            int length = 0;
            long process = 0;

            while((length = dis.read(bytes, 0, bytes.length)) != -1) {
                fos.write(bytes, 0, length);
                fos.flush();
                process += length;
                //System.out.println(process);
                if(process >= fileLength)break;
            }
            System.out.println("文件接收成功 [File Name：" + userFilePath + "] [Size：" + getFormatFileSize(fileLength) + "]");

            dos.writeUTF(username);
            dos.flush();

            fileLength = dis.readLong();

            File userPrivFile = new File(userPrivFilePath);
            if(!userPrivFile.exists()){
                userPrivFile.createNewFile();
            }

            fos = new FileOutputStream(userPrivFile);

            // 开始接收文件
            bytes = new byte[1024];
            length = 0;
            process = 0;

            while((length = dis.read(bytes, 0, bytes.length)) != -1) {
                fos.write(bytes, 0, length);
                fos.flush();
                process += length;
                //System.out.println(process);
                if(process >= fileLength)break;
            }
            System.out.println("文件接收成功 [File Name：" + userPrivFilePath + "] [Size：" + getFormatFileSize(fileLength) + "]");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        if(fis != null)
            fis.close();
        if(dos != null)
            dos.close();
        client.close();
        }
    }
    /*
    public String deleteFile(String url) throws Exception {
        try{
            dos = new DataOutputStream(client.getOutputStream());
            dis = new DataInputStream(client.getInputStream());

            dos.writeUTF("delete");
            dos.flush();
            dos.writeUTF(url);
            dos.flush();

            Boolean result = dis.readBoolean();
            if (!result){
                return "Error!";
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fis != null)
                fis.close();
            if(dos != null)
                dos.close();
            client.close();
        }
        return "Success!";
    }*/
    /*
    public String raiseAudit(String url,String chalString,String ownerPkString,String tagString) throws Exception {
        String proofString = "";
        try{
            dos = new DataOutputStream(client.getOutputStream());
            dis = new DataInputStream(client.getInputStream());

            dos.writeUTF("audit");
            dos.flush();
            dos.writeUTF(url);
            dos.flush();
            dos.writeUTF(chalString);
            dos.flush();
            dos.writeUTF(ownerPkString);
            dos.flush();
            dos.writeUTF(tagString);
            dos.flush();

            proofString = dis.readUTF();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fis != null)
                fis.close();
            if(dos != null)
                dos.close();
            client.close();
        }
        System.out.println(proofString);
        return proofString;
    }*/
    /*
    public String raiseAudit(String auditRecordString) throws Exception {

        String proofString = "";
        String dataString = "";
        try{
            dos = new DataOutputStream(client.getOutputStream());
            dis = new DataInputStream(client.getInputStream());

            dos.writeUTF("audit");
            dos.flush();
            dos.writeUTF(auditRecordString);
            dos.flush();

            proofString = dis.readUTF();
            dataString = dis.readUTF();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fis != null)
                fis.close();
            if(dos != null)
                dos.close();
            client.close();
        }

        if (proofString.length()==0 || dataString.length()==0){
            return "";
        }
        System.out.println("审计结果:");
        System.out.println(proofString);
        System.out.println("最新数据:");
        System.out.println(dataString);
        return proofString + "\n" + dataString;
    }*/

    /**
     * 向服务端传输文件
     * @throws Exception
     */
    public String sendFile(String username, String filePath, String fileName) throws Exception {
        String URL = "";
        try {
            File file = new File(filePath + File.separatorChar + fileName);
            if(file.exists()) {
                fis = new FileInputStream(file);
                dos = new DataOutputStream(client.getOutputStream());
                dis = new DataInputStream(client.getInputStream());

                //用户名 文件名 长度
                dos.writeUTF("upload");
                dos.flush();
                //System.out.println(username);
                dos.writeUTF(username);
                dos.flush();
                //System.out.println(file.getName());
                dos.writeUTF(file.getName());
                dos.flush();
                dos.writeLong(file.length());
                dos.flush();


                // 开始传输文件
                //System.out.println("======== 开始传输文件 ========");
                byte[] bytes = new byte[1024];
                int length = 0;
                long progress = 0;
                while((length = fis.read(bytes, 0, bytes.length)) != -1) {
                    dos.write(bytes, 0, length);
                    dos.flush();
                    progress += length;
                    //System.out.print("| " + (100*progress/file.length()) + "% |");
                }
                //dos.write(new byte[0], 0, length);
                //dos.flush();
                //System.out.println();
                //dos.writeUTF("ok");
                //dos.flush();
                URL = dis.readUTF();
                //System.out.println(URL);
                System.out.println("文件传输成功 [File Name：" + file.getName() + "] [Size：" + getFormatFileSize(file.length()) + "]");
            }else{
                System.out.println("File does not exsit. "+ filePath + File.separatorChar + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fis != null)
                fis.close();
            if(dos != null)
                dos.close();
            client.close();
        }
        return URL;
    }

    public Boolean updateFile(String filePath, String fileName, String CloudURL) throws Exception {
        Boolean updateResult = false;
        try {
            File file = new File(filePath + File.separatorChar + fileName);
            if(file.exists()) {
                fis = new FileInputStream(file);
                dos = new DataOutputStream(client.getOutputStream());
                dis = new DataInputStream(client.getInputStream());

                //用户名 文件名 长度
                dos.writeUTF("update");
                dos.flush();
                dos.writeUTF(CloudURL);
                dos.flush();
                dos.writeLong(file.length());
                dos.flush();


                // 开始传输文件
                //System.out.println("======== 开始传输文件 ========");
                byte[] bytes = new byte[1024];
                int length = 0;
                long progress = 0;
                while((length = fis.read(bytes, 0, bytes.length)) != -1) {
                    dos.write(bytes, 0, length);
                    dos.flush();
                    progress += length;
                    //System.out.print("| " + (100*progress/file.length()) + "% |");
                }
                //System.out.println();
                updateResult = dis.readBoolean();
                if(updateResult){
                    //System.out.println("======== 文件传输成功 ========");
                    System.out.println("文件传输成功 [File Name：" + file.getName() + "] [Size：" + getFormatFileSize(file.length()) + "]");
                }else{
                    System.out.println("文件传输失败!");
                }
            }else{
                System.out.println("File does not exsit. "+ filePath + File.separatorChar + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fis != null)
                fis.close();
            if(dos != null)
                dos.close();
            client.close();
        }
        return updateResult;
    }

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

    /**
     * 入口
     * @param args
     */
    /*
    public static void main(String[] args) {
        try {
            FileTransferClient client = new FileTransferClient(); // 启动客户端连接
            client.sendFile("Simba","D:","test.txt"); // 传输文件
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}

