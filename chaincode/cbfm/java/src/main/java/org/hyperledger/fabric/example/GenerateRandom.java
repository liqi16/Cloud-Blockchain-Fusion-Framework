package org.hyperledger.fabric.example;

import java.util.Arrays;
import java.util.Random;

public class GenerateRandom {
	
	public static  int[] random(int start,int end,int len){
		int [] rst=new int[len];
		Arrays.fill(rst,start-1);
		Random r=new Random();
		for (int i = 0; i < rst.length; ) {
			int ran=r.nextInt(end-start+1)+start;
			if(!isDup(rst, ran)){
				rst[i++]=ran;
			}

		}
		return rst;
	}
	
	public static boolean  isDup(int []random,int ran){
		for (int i = 0; i < random.length; i++) {
			if(random[i]==ran)
				return true;
		}
		return false;
	}

}
