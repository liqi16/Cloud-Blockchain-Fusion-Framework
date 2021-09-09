package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;

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
	
	public static void writeFile(String outputfile, String content) {
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

	public static String makeShortRecord(String channel, String index, String dataname){
		return channel+"-"+index+"-"+dataname;
	}
	/*
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
	}*/

}

