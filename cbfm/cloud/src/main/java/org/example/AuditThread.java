package org.example;

import org.hyperledger.fabric.gateway.Network;

public class AuditThread extends Thread{
	private static Network network;
	private String auditString;
	
	public AuditThread(Network NETWORK,String AUDIT) {
		this.network = NETWORK;
		this.auditString = AUDIT;
	}
	
	@Override
    public void run() {
		try {
			Audit.getProofAndVerify(network, auditString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
