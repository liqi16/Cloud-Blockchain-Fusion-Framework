package org.example;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;

public class ECC {
	
		static {
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		}
		
		public static KeyPair generateKeyPair() throws Exception {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC","BC");
			keyPairGenerator.initialize(256, new SecureRandom());
	 
			KeyPair pair = keyPairGenerator.generateKeyPair();
	        return pair;
	    }

	    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
	    	Cipher encrypter = Cipher.getInstance("ECIES", "BC");
	    	encrypter.init(Cipher.ENCRYPT_MODE, publicKey);

	        byte[] cipherText = encrypter.doFinal(plainText.getBytes("UTF-8"));
	        return Base64.getEncoder().encodeToString(cipherText);
	    }

	    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception{
	    	byte[] bytes = null;
	    	try {
	    		bytes = Base64.getDecoder().decode(cipherText);
	    	}catch(Exception e)
	    	{
	    		System.out.println(e);
	    		//cipherText = cipherText.substring(0,cipherText.length() - 2);
	    		//System.out.println(cipherText);
	    		//cipherText = cipherText + "=";
	    		//bytes = Base64.getDecoder().decode(cipherText);
	    	}
	        

	        Cipher decriptCipher = Cipher.getInstance("ECIES", "BC");
	        decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

	        return new String(decriptCipher.doFinal(bytes), UTF_8);
	    }

	    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
	        Signature privateSignature = Signature.getInstance("SHA256withECDSA", "BC");
	        privateSignature.initSign(privateKey);
	        privateSignature.update(plainText.getBytes(UTF_8));

	        byte[] signature = privateSignature.sign();

	        return Base64.getEncoder().encodeToString(signature);
	    }

	    public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
	        Signature publicSignature = Signature.getInstance("SHA256withECDSA", "BC");
	        publicSignature.initVerify(publicKey);
	        publicSignature.update(plainText.getBytes(UTF_8));

	        byte[] signatureBytes = Base64.getDecoder().decode(signature);

	        return publicSignature.verify(signatureBytes);
	    }
	    
	    public static PublicKey importPK(String pkFileName)
	    {
	    	FileInputStream keyfis;
	    	byte[] encKey = null;
			try {
				keyfis = new FileInputStream(pkFileName);
				encKey = new byte[keyfis.available()];  
		    	keyfis.read(encKey);
		    	keyfis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			KeyFactory keyFactory;
			PublicKey pubKey = null;
			try {
				keyFactory = KeyFactory.getInstance("EC","BC");
				pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encKey));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return pubKey;
	    }
	    
	    public static PublicKey importPKFromString(String pkString)
	    {
	    	byte[] encKey = conver16HexToByte(pkString);
			
			KeyFactory keyFactory;
			PublicKey pubKey = null;
			try {
				keyFactory = KeyFactory.getInstance("EC","BC");
				pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encKey));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return pubKey;
	    }
	    
	    public static PrivateKey imporSK(String skFileName)
	    {
	    	FileInputStream keyfis;
	    	byte[] encKey = null;
			try {
				keyfis = new FileInputStream(skFileName);
				encKey = new byte[keyfis.available()];  
		    	keyfis.read(encKey);
		    	keyfis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			KeyFactory keyFactory;
			PrivateKey priKey = null;
			try {
				keyFactory = KeyFactory.getInstance("EC","BC");
				priKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encKey));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return priKey;		
	    }
	    
	    public static void exportPK(PublicKey pk, String pkFileName)
	    {
	    	byte[] key = pk.getEncoded();
	    	FileOutputStream keyfos;
			try {
				keyfos = new FileOutputStream(pkFileName);
				keyfos.write(key);
		    	keyfos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    public static void exportSK(PrivateKey sk, String skFileName)
	    {
	    	byte[] key = sk.getEncoded();
	    	FileOutputStream keyfos;
			try {
				keyfos = new FileOutputStream(skFileName);
				keyfos.write(key);
		    	keyfos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
		public static String conver16HexStr(byte[] b) {
	        StringBuffer result = new StringBuffer();
	        for (int i = 0; i < b.length; i++) {
	            if ((b[i] & 0xff) < 0x10)
	                result.append("0");
	            result.append(Long.toString(b[i] & 0xff, 16));
	        }
	        return result.toString().toUpperCase();
	    }
	
	    public static byte[] conver16HexToByte(String hex16Str) {
	        char[] c = hex16Str.toCharArray();
	        byte[] b = new byte[c.length / 2];
	        for (int i = 0; i < b.length; i++) {
	            int pos = i * 2;
	            b[i] = (byte) ("0123456789ABCDEF".indexOf(c[pos]) << 4 | "0123456789ABCDEF".indexOf(c[pos + 1]));
	        }
	        return b;
	    }
	    
	    public static String exportPKToString(PublicKey pk)
	    {
	    	byte[] key = pk.getEncoded();
	    	return conver16HexStr(key);
	    }

		
		public static String byteToHex(byte[] bytes){
	        String strHex = "";
	        StringBuilder sb = new StringBuilder("");
	        for (int n = 0; n < bytes.length; n++) {
	            strHex = Integer.toHexString(bytes[n] & 0xFF);
	            sb.append((strHex.length() == 1) ? "0" + strHex : strHex);
	        }
	        return sb.toString().trim();
	    }
		
		public static String exportSKToString(PrivateKey sk)
		{
			byte[] key = sk.getEncoded();
			return conver16HexStr(key);
		}
		

		//example
	/*
		 public static void main(String... argv) throws Exception {
		        //First generate a public/private key pair
		        KeyPair pair = generateKeyPair();
		        System.out.println(pair.getPrivate());
				System.out.println(pair.getPublic());

		        //Our secret message
		        String message = "ResponseHeader#1234567890#1234567890";
		        System.out.println("Message: "+message);

		        //Encrypt the message
		        String cipherText = encrypt(message, pair.getPublic());
		        System.out.println("CipherText: " + cipherText);

		        //Now decrypt it
		        String decipheredMessage = decrypt(cipherText, pair.getPrivate());
		        System.out.println("DecipheredMessage: "+decipheredMessage);

		        //Let's sign our message
		        String signature = sign(cipherText, pair.getPrivate());
		        System.out.println("Signature: "+signature);

		        //Let's check the signature
		        boolean isCorrect = verify(cipherText, signature, pair.getPublic());
		        System.out.println("Signature correct: " + isCorrect);
		        
		        exportPK(pair.getPublic(),"publicKey.dat");
		        exportSK(pair.getPrivate(),"privateKey.dat");
		        System.out.println("PrivateKey: "+exportSKToString(pair.getPrivate()));
		        
		        PublicKey importedPK = importPK("publicKey.dat");
		        PrivateKey importedSK = imporSK("privateKey.dat");
		        
		        //Encrypt the message
		        //message = "abcdefg";
		        System.out.println("Message: "+message);
		        
		        cipherText = encrypt(message, importedPK);
		        System.out.println("CipherText: " + cipherText);

		        //Now decrypt it
		        decipheredMessage = decrypt(cipherText, importedSK);
		        System.out.println("DecipheredMessage: "+decipheredMessage);

		        //Let's sign our message
		        signature = sign("foobar", importedSK);
		        System.out.println("Signature: "+signature);

		        //Let's check the signature
		        isCorrect = verify("foobar", signature, importedPK);
		        System.out.println("Signature correct: " + isCorrect);
		    }
	*/
}
