package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Random;

import org.hyperledger.fabric.gateway.Network;
 
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
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
 
                // username and filename and length
                String cmd = dis.readUTF();
                if(cmd.equals("upload")) {
                	System.out.println("\n云平台$ UPLOAD FILE...\n");
                	String username = dis.readUTF();
                    String fileName = dis.readUTF();
                    long fileLength = dis.readLong();
                    long process = 0;
                    File directory = new File(Main.MSP_Name+"_"+Main.Cloud_Name);
                    if(!directory.exists()) {
                        directory.mkdir();
                    }
                    File userdic = new File(Main.MSP_Name+"_"+Main.Cloud_Name + File.separatorChar + username);
                    if(!userdic.exists()) {
                    	userdic.mkdir();
                    }
                    
                    
                    String url = userdic.getAbsolutePath() + File.separatorChar + fileName;
                    File file = new File(url);
                    
                    int fileIndex = 1;
                    String cloudname = url;
                    while(file.exists()) {
                    	//url = url + getRandomString(1);
                    	url = cloudname + "("+String.valueOf(fileIndex)+")";
                    	//System.out.println(url);
                    	file = new File(url);
                    	fileIndex ++;
                    }
                    
                    fos = new FileOutputStream(file);
     
                    // 开始接收文件
                    System.out.println("开始接收文件");
                    byte[] bytes = new byte[1024];
                    int length = 0;
                    while((length = dis.read(bytes, 0, bytes.length)) != -1) {
                        fos.write(bytes, 0, length);
                        fos.flush();
                        process += length;
                        System.out.print("| " + (100*process/fileLength) + "% |");
                        //System.out.println(process);
                        if(process >= fileLength)break;
                    }
                    //String result = dis.readUTF();
                    //System.out.println(1);
                    dos.writeUTF(url);
                    //System.out.println(2);
                    dos.flush();
                    System.out.println("\n文件接收成功 [File Name：" + fileName + "] [Size：" + getFormatFileSize(fileLength) + "]");

                    //


                }else if(cmd.equals("download")) {
                	System.out.println("\n云平台$ DOWNLOAD FILE...\n");
                	String url = dis.readUTF();
                	File downloadFile = new File(url);
                	fis = new FileInputStream(downloadFile);
                	if(!downloadFile.exists()) {
                		System.out.println("Error! File "+ url +" does not exsit. ");
                	}else {
                		dos.writeLong(downloadFile.length());
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
                            System.out.print("| " + (100*progress/downloadFile.length()) + "% |");
                        }
                        System.out.println("\n文件传输成功 [File Name：" + url + "] [Size：" + getFormatFileSize(downloadFile.length()) + "]");
                        //System.out.println("文件传输成功");
                	}
                /*}else if(cmd.equals("audit")) {
                	//String audit = dis.readUTF();
                	
                	//String filePath =  dis.readUTF();
                	//String chalString = dis.readUTF();
                	//String ownerPkString = dis.readUTF();
                	//String tagString = dis.readUTF();
                	
                	//File auditFile = new File(filePath);
                	//if(auditFile.exists()) {
                	//	String proofString = Cloud.proofGen(filePath,chalString,ownerPkString,tagString);
                	//	dos.writeUTF(proofString);
                	//}else {
                	//	System.out.println("Error! File does not exsit. ");
                	//}
                	String auditString = dis.readUTF();
                	String updatedAuditString = Audit.testGetProofAndVerify(network, auditString);
                	if(updatedAuditString.length()>0) {
                		dos.writeUTF(updatedAuditString);
                	}
                	String updatedDataString = Audit.testUpdateDataAuditResult(network, updatedAuditString);
                	if(updatedDataString.length()>0) {
                		dos.writeUTF(updatedDataString);
                	}*/
                /*}else if(cmd.equals("delete")) {
                	
                	String url =  dis.readUTF();
                	File deleteFile = new File(url);
                	fis = new FileInputStream(deleteFile);
                	if(!deleteFile.exists()) {
                		System.out.println("Error! File "+ url +" does not exsit. ");
                		dos.writeBoolean(false);
                	}else {
                		if (deleteFile.isFile()) {
                			 dos.writeBoolean(deleteFile.delete());
                		}else {
                			dos.writeBoolean(false);
                		}
                	}*/
                }else if(cmd.equals("update")) {
                	System.out.println("\n云平台$ UPDATE FILE...\n");
                	String url = dis.readUTF();
                    long fileLength = dis.readLong();
                    long process = 0;
                    File updateFile = new File(url);
                    if(updateFile.exists()) {
                    	Utils.writeFile(url,""); // clear file
                    	
                    	fos = new FileOutputStream(updateFile);
                        
                        // 开始接收文件
                    	System.out.println("开始接收文件");
                        byte[] bytes = new byte[1024];
                        int length = 0;
                        while((length = dis.read(bytes, 0, bytes.length)) != -1) {
                            fos.write(bytes, 0, length);
                            fos.flush();
                            process += length;
                            System.out.print("| " + (100*process/fileLength) + "% |");
                            //System.out.println(process);
                            if(process >= fileLength)break;
                        }
                        dos.writeBoolean(true);
                        dos.flush();
                        System.out.println("\n文件接收成功 [File Name：" + url + "] [Size：" + getFormatFileSize(fileLength) + "]");
                    }else {
                    	dos.writeBoolean(false);
                    	System.out.println("Error! File does not exsit.");
                    }
                }
                
                else {
                	System.out.println("Command Error!");
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
