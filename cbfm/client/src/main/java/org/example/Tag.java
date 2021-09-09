package org.example;

import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import org.apache.commons.codec.digest.DigestUtils;

public class Tag {
	//private static final int sectors = 1;
	//private static final int sectorSize = 1024*1024;
	//private static final int K = 1024;
	//private static final int blockSize = sectors * sectorSize / 1000; //modefied by simba 20210407
	//private static final int blockSizeK = blockSize * K;//modefied by simba 20210407
	//private static final int blockSize = sectors * sectorSize;//modefied by simba 20210407
	//private static final int blockSizeK = blockSize;//modefied by simba 20210407
	
	public static String genTags(String filePath, String pubKeyString, String secKeyString) throws Exception {
		System.out.println(filePath+" " +pubKeyString+" "+secKeyString); 

		PublicParam pk = new PublicParam(pubKeyString);
		System.out.println("pk ok"); 
		SecretParam sk = new SecretParam(secKeyString);
		System.out.println("sk ok"); 

		Pairing pairing = pk.getPairing();
		Element g = pk.getG();
		Element u = pk.getU();
		Element v = pk.getV();
		Element w = pk.getW();
		Element x = sk.getX();

		PublicParam keys = new PublicParam(pairing, g, u, v, w);
		System.out.println("keys ok"); 

		byte[][] nSectors = FileOperation.preProcessFile(filePath);
		Element[] tags = metaGen(nSectors,keys,x);

		JSONObject tagsObject = new JSONObject();
		for (int i = 0; i < tags.length; i++) {
			String keyString = String.valueOf(i);
			tagsObject.put(keyString,Utils.elementToBase64(tags[i]));
		}

		/*Pubkey pk = new Pubkey(pubKeyString);
		SecKey sk = new SecKey(secKeyString);
		
		Element[][] nSectors = FileOperation.preProcessFile(filePath, sectors, sectorSize, sk.pairing.getZr());
		
		int n = FileOperation.blockNumbers(filePath, blockSize);
		
		Element[] tags = metaGen(nSectors, n, pk, sk);
		
		JSONObject tagsObject = new JSONObject();
		for (int i = 0; i < tags.length; i++) {
			String keyString = String.valueOf(i+1);
			String valueString = Utils.elementToBase64(tags[i]);
			tagsObject.put(keyString,valueString);
		}*/
		
		String tagString = tagsObject.toJSONString();		
		return tagString;
		
	}

	public static Element[] metaGen(byte[][] file,PublicParam keys,Element x) {
		int count = file.length;
		Element[] blockTags = new Element[count];
		for (int i = 0; i < count; i++) {
			String hashstr = DigestUtils.sha256Hex(file[i]);
			Element hash = keys.getPairing().getG1().newElement().setFromHash(hashstr.getBytes(), 0, hashstr.getBytes().length);
			Element mi = keys.getPairing().getZr().newElement().setFromHash(file[i], 0, file[i].length);
			blockTags[i] = keys.getU().duplicate().powZn(mi.duplicate()).mul(hash).powZn(x);
		}
		return blockTags;
	}
	
	/*private static Element[] metaGen(Element[][] mij, int count,Pubkey pk, SecKey sk) {
		Element[] blockTags = new Element[count];
		for (int i = 0; i < count; i++) {
			blockTags[i] = metaGen(i + 1, mij[i], pk, sk);
		}
		return blockTags;
	}*/
	/*
	private static Element metaGen(int blockNum, Element[] mij, Pubkey pk, SecKey sk) {
		int s = mij.length;
		// �����ļ����ǩ��t=(H(blockNum)*(g1^(aj*mij))^x
		Element aggSum = sk.pairing.getZr().newZeroElement();
		for (int j = 0; j < s; j++) {
			aggSum = aggSum.add(sk.ps[j].duplicate().mulZn(mij[j]));
		}
		//byte[] data = String.valueOf(blockNum).getBytes();
		//Element Hid = sk.pairing.getG1().newElementFromHash(data, 0, data.length);
		//Element t = (Hid.duplicate().mul(pk.g1.duplicate().powZn(aggSum))).powZn(sk.x);
        Element t = pk.g2.duplicate().powZn(aggSum).powZn(sk.x); 
		return t;
	}*/

}
