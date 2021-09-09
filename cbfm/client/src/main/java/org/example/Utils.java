package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;

public class Utils {

    //element to base64String
    public static String elementToBase64(Element e) {
        Base64.Encoder encoder = Base64.getEncoder();
        String elementBase64String = encoder.encodeToString(e.toBytes());
        return elementBase64String;
    }

    //base64 to elementByte[]
    public static byte[] base64StringToElementBytes(String elementBase64String){
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] elementByte = decoder.decode(elementBase64String);
        return elementByte;
    }

    public static void writeFile(String outputfile, String content) throws Exception {
        File outputFile = new File(outputfile);
        if (!outputFile.exists()){
            outputFile.createNewFile();
        }
        FileWriter writer;
        try {
            writer = new FileWriter(outputfile);
            writer.write("");
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(String filename) {
        String data = "";

        try {
            File myObj = new File(filename);
            Scanner myReader = new Scanner(myObj);

            while (myReader.hasNextLine()) {
                data += myReader.nextLine();
            }

            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return data;
    }

    public static void writeSymkey(String username,String shortRecord,String symkey)throws Exception{
        String symkeyFilename = ClientApp.path+"key/"+username + "SymkeyDict";
        File symkeyDictFile = new File(symkeyFilename);
        JSONObject symkeyObject;
        if (!symkeyDictFile.exists()){
            symkeyDictFile.createNewFile();
            symkeyObject = new JSONObject();
        }else{
            symkeyObject = JSON.parseObject(readFile(symkeyFilename));
        }
        symkeyObject.put(shortRecord,symkey);
        writeFile(symkeyFilename,symkeyObject.toJSONString());
    }

    public static String readSymkey(String username,String shortRecord)throws Exception{
        String symkeyFilename = ClientApp.path+"key/"+username + "SymkeyDict";
        File symkeyDictFile = new File(symkeyFilename);
        if (!symkeyDictFile.exists()){
            throw new Exception("Error! The SymKey dictionary is empty.");
        }
        String symkeyString = readFile(symkeyFilename);
        JSONObject symkeyObject = JSONObject.parseObject(symkeyString);
        return symkeyObject.getString(shortRecord);
    }

    public static String makeShortRecord(String channel, String index, String dataname){
        return channel+"-"+index+"-"+dataname;
    }

}
