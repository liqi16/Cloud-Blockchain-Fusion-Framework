package org.example;

import org.hyperledger.fabric.gateway.Network;

public class UploadThread extends Thread{
	private static Network network;
	private String dataString;

	public UploadThread(Network NETWORK, String DATA) {
		this.network = NETWORK;
		this.dataString = DATA;
	}
	
	@Override
    public void run() {
		try {
			Sign.cloudSign(network, dataString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
