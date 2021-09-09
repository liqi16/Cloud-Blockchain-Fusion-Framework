package org.example;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;

public class FileOperation {
	//public static final int K=1024;
	public static final int blockSize = 1024*1024;

	public static byte[][] readData(List<Challenge> challenges, String filePath) throws IOException{

		int c = challenges.size();
		byte[][] data = new byte[c][];
		RandomAccessFile in = new RandomAccessFile(filePath, "r");
		in.seek(0);
		for(int i=0; i<c; i++){
			data[i] = new byte[blockSize];
			in.seek(challenges.get(i).num * blockSize);
			int len = in.read(data[i]);
			if(len!=-1) {
				Arrays.fill(data[i], len, blockSize, (byte)0);
			}
		}
		in.close();
		return data;
	}


	public static int blockNumbers(String filePath) throws IOException{
		File file = new File(filePath);
		long fileLength = file.length();
		long number = fileLength/(blockSize);
		long remain = 0;
		if(fileLength%blockSize > 0) {
			remain = 1;
		}
		int number2=(int)(number+remain);
		return number2;
	}
	
	/*public static int blockNumbers(String filePath,int blockSize) throws IOException{
		File file = new File(filePath);		
		long fileLength = file.length();
		//long number=fileLength/(K*blockSize);//modified by simba 20210407:
		//long remain=fileLength%(K*blockSize);	//modified by simba 20210407:
		//return (int)(remain==0?number:number+1);//modified by simba 20210407:
		long number=fileLength/(blockSize);//modified by simba 20210407:
		long remain = 0;//modified by simba 20210407:
		if(fileLength%blockSize>0){//modified by simba 20210407:
			remain = 1;//modified by simba 20210407:
		}//modified by simba 20210407:
		int number2 = (int)(number+remain);//modified by simba 20210407:
		return number2;//modified by simba 20210407:
	}*/

	public static byte[][] preProcessFile(String filePath) throws IOException{
		int fileBlocks = blockNumbers(filePath);
		byte[][] nSectors=new byte[fileBlocks][];

		RandomAccessFile in = new RandomAccessFile(filePath, "r");
		in.seek(0);
		for(int i=0; i<fileBlocks; i++){
			nSectors[i] = new byte[blockSize];
			int len = in.read(nSectors[i]);
			if(len!=-1) {
				Arrays.fill(nSectors[i], len, blockSize, (byte)0);
			}
		}
		in.close();
		return nSectors;
	}

	/*
	public static Element[][] preProcessFile(String filePath,int s,int sectorSize,Field r) throws IOException{
		//int blockSize=s*sectorSize/1000;//modified by simba 20210407:
		//int blockSizeK=(blockSize)*K;//modified by simba 20210407:
		int blockSize=s*sectorSize;//modified by simba 20210407:
		int blockSizeK=blockSize;//modified by simba 20210407:
		int fileBlocks=blockNumbers(filePath, blockSize);
		byte[] blockBuff;//����Ĵ�С�պ�����С���		
		Element[][] nSectors=new Element[fileBlocks][s];

		RandomAccessFile in = new RandomAccessFile(filePath, "r");		
		blockBuff=new byte[blockSizeK];//modified by simba 20210407:
		//for(int i=0;i<fileBlocks-1;i++){//modified by simba 20210407:
		//	blockBuff=new byte[blockSizeK];
		//	in.read(blockBuff,0,blockBuff.length);
		//	nSectors[i]=preProcessBlock(s, blockBuff, sectorSize, r);

		//}
		//blockBuff=new byte[blockSizeK];
		//int remainBytes=in.read(blockBuff);
		//if(remainBytes==blockSizeK){
		//	nSectors[fileBlocks-1]=(preProcessBlock(s, blockBuff, sectorSize, r));

		//}else{
		//	for(int k=remainBytes;k<blockBuff.length;k++)
		//		blockBuff[k]=0;
		//	nSectors[fileBlocks-1]=(preProcessBlock(s, blockBuff, sectorSize, r));
		//}//modified by simba 20210407:
		for(int i=0;i<fileBlocks;i++){//modified by simba 20210407:
			in.read(blockBuff);//modified by simba 20210407:
			nSectors[i]=preProcessBlock(s, blockBuff, sectorSize, r);//modified by simba 20210407:
		}//modified by simba 20210407:
		in.close();	
		return nSectors;
	}*/
	
	/*public static Element[] preProcessBlock(int s,byte[] blockData,int sectorSize,Field r) {
		Element[] sectorNums=new Element[s];		
		for(int i=0;i<s;i++){
			byte[] buff=subByteArray(blockData,i*sectorSize,sectorSize);			
			sectorNums[i]=(r.newElementFromBytes(buff));			
		}
		return sectorNums;
	}*/
	
	/*public static byte [] subByteArray(byte []a,int offset,int len){
		int aLength=a.length;
		//if(aLength-offset<len)//modified by simba 20210407:
		//	return null;	//modified by simba 20210407::
		byte [] result=new byte[len];
		for(int i=0;i<len;i++){
			result[i]=a[offset+i];
		}

		return result;
	}*/
	/*
	public static List<byte[]> readBlocks(String filePath,int blockSize,int[]blockNums) {
		int c=blockNums.length;
		byte[] data ;
		List<byte[]>cdata=new ArrayList<>();
		RandomAccessFile in;
		try {
			in = new RandomAccessFile(filePath, "r");
			in.seek(0);
			for(int i=0;i<c;i++){
				data=new byte[blockSize];
				in.seek(blockSize*(blockNums[i]-1));	
				in.read(data);
				cdata.add(data);
			}
			in.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return cdata;
	}*/

}
