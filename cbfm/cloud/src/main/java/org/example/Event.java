package org.example;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.hyperledger.fabric.sdk.ChaincodeEventListener;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;

public class Event extends Thread{
	
	private Network network;
	private String eventName;
	
	public Event(Network NETWORK,String EventName) {
		this.network = NETWORK;
		this.eventName = EventName;
	}
	
	@Override
    public void run() {

		try {
            System.out.println("\n云平台$ 正在监听 ["+eventName+"]...");
			while(true) {
				// START CHAINCODE EVENT LISTENER HANDLER----------------------
		        //String expectedEventName = "AuditEvent";
		        Channel channel = network.getChannel();
		        Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture
		        String chaincodeEventListenerHandle = setChaincodeEventListener(channel, eventName, chaincodeEvents);
		        // END CHAINCODE EVENT LISTENER HANDLER------------------------

		        // START WAIT FOR THE EVENT-------------------------------------
		        boolean eventDone = false;
		        eventDone = waitForChaincodeEvent(600,channel, chaincodeEvents, chaincodeEventListenerHandle);
		        //System.out.println("eventDone: " + eventDone);
		        // END WAIT FOR THE EVENT---------------------------------------
		        Thread.sleep(100);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
    }
	
	public static String setChaincodeEventListener(Channel channel, String expectedEventName, Vector<ChaincodeEventCapture> chaincodeEvents) throws Exception {

        ChaincodeEventListener chaincodeEventListener = new ChaincodeEventListener() {

            @Override
            public void received(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
                chaincodeEvents.add(new ChaincodeEventCapture(handle, blockEvent, chaincodeEvent));

                String eventHub = blockEvent.getPeer().toString();
                if(eventHub != null){
                    eventHub = blockEvent.getPeer().getName();
                } else {
                    eventHub = blockEvent.getEventHub().getName();
                }
                // Here put what you want to do when receive chaincode event
                System.out.println("\n云平台$ 捕获事件");
                System.out.println("RECEIVED CHAINCODE EVENT with handle: " + handle + ", chaincodeId: " + chaincodeEvent.getChaincodeId() + ", chaincode event name: " + chaincodeEvent.getEventName() + ", transactionId: " + chaincodeEvent.getTxId() +", event Payload: " + new String(chaincodeEvent.getPayload()) + ", from eventHub: " + eventHub);
            }

        };
        // chaincode events.
        String eventListenerHandle = channel.registerChaincodeEventListener(Pattern.compile(".*"), Pattern.compile(Pattern.quote(expectedEventName)), chaincodeEventListener);
        return eventListenerHandle;
    }

    public boolean waitForChaincodeEvent(Integer timeout,Channel channel, Vector<ChaincodeEventCapture> chaincodeEvents, String chaincodeEventListenerHandle) throws Exception {
        
    	boolean eventDone = false;
        if (chaincodeEventListenerHandle != null) {

            int numberEventsExpected = 1;//channel.getEventHubs().size() + channel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).size();
            //System.out.println("numberEventsExpected: " + numberEventsExpected);
            //just make sure we get the notifications
            if (timeout.equals(0)) {
                // get event without timer
                while (chaincodeEvents.size() < numberEventsExpected) {
                    // do nothing
                }
                eventDone = true;
            } else {
                // get event with timer
                for (int i = 0; i < timeout; i++) {
                    if (chaincodeEvents.size() > numberEventsExpected) {
                        eventDone = true;
                        break;
                    } else {
                        try {
                            double j = i;
                            j = j / 10;
                            //System.out.println(j + " second");
                            Thread.sleep(100); // wait for the events for one tenth of second.
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            //System.out.println("chaincodeEvents.size(): " + chaincodeEvents.size());

            // unregister event listener
            channel.unregisterChaincodeEventListener(chaincodeEventListenerHandle);
            List<String> txList = new ArrayList<>();
            
            int i = 1;
            // arrived event handling
            for (ChaincodeEventCapture chaincodeEventCapture : chaincodeEvents) {
            	String tx = chaincodeEventCapture.getChaincodeEvent().getTxId();
            	if(!txList.contains(tx)) {
            		txList.add(tx);
            		String getEventName = chaincodeEventCapture.getChaincodeEvent().getEventName();
            		if(getEventName.equals(Main.MSP_Name +"_"+  Main.Cloud_Name+ "_" +"AuditEvent")) {
            			String auditString = new String(chaincodeEventCapture.getChaincodeEvent().getPayload());
                		Thread auditThread = new AuditThread(network,auditString);
                		auditThread.start();
            		} /* else if(getEventName.equals("DeleteEvent")) {
            			String deleteString = new String(chaincodeEventCapture.getChaincodeEvent().getPayload());
                		Thread deleteThread = new DeleteThread(network,deleteString);
                		deleteThread.start();
            		}*/else if(getEventName.equals(Main.MSP_Name +"_"+  Main.Cloud_Name+ "_" +"DataUploadEvent")) {
                        String uploadString = new String(chaincodeEventCapture.getChaincodeEvent().getPayload());
                        Thread uploadThread = new UploadThread(network,uploadString);
                        uploadThread.start();
                    }
            		//System.out.println("event capture object: " + chaincodeEventCapture.toString());
                    //System.out.println("Event Handle: " + chaincodeEventCapture.getHandle());
                    //System.out.println("Event TxId: " + chaincodeEventCapture.getChaincodeEvent().getTxId());
                    //System.out.println("Event Name: " + chaincodeEventCapture.getChaincodeEvent().getEventName());
                    //System.out.println("Event Payload: " + new String(chaincodeEventCapture.getChaincodeEvent().getPayload())); // byte
                    //System.out.println("Event ChaincodeId: " + chaincodeEventCapture.getChaincodeEvent().getChaincodeId());
            	}
            	/*
                System.out.println("Event number. " + i);
                System.out.println("event capture object: " + chaincodeEventCapture.toString());
                System.out.println("Event Handle: " + chaincodeEventCapture.getHandle());
                System.out.println("Event TxId: " + chaincodeEventCapture.getChaincodeEvent().getTxId());
                System.out.println("Event Name: " + chaincodeEventCapture.getChaincodeEvent().getEventName());
                System.out.println("Event Payload: " + new String(chaincodeEventCapture.getChaincodeEvent().getPayload())); // byte
                System.out.println("Event ChaincodeId: " + chaincodeEventCapture.getChaincodeEvent().getChaincodeId());
                
                BlockEvent blockEvent = chaincodeEventCapture.getBlockEvent();
                try {
                    System.out.println("Event Channel: " + blockEvent.getChannelId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Event Hub: " + blockEvent.getEventHub());
				*/
                i++;
            }

        } else {
            System.out.println("chaincodeEvents.isEmpty(): " + chaincodeEvents.isEmpty());
        }
        //System.out.println("eventDone: " + eventDone);
        return eventDone;
    }

	/*
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}*/

}
