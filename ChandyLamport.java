package project2_2;

/**
 * Chandy Lamport Snapshot Algorithm implementation
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;


public class ChandyLamport implements Runnable{

	static int port;
	static Process process;
	static ServerProperties serverProperties = null;
	Scanner scan = new Scanner(System.in);
	int maxAmount = 100;
	static int serverId;
	boolean snapshotStarted = false;
	static int marker = 0;
	static String hostname;
	static ChandyLamport cl; 
	
	/**
	 * All initialization takes here.
	 * @throws UnknownHostException
	 */
	public ChandyLamport() throws UnknownHostException {
		port = 8400;
		hostname = InetAddress.getLocalHost().getHostName() + "";
		serverProperties = ServerProperties.getServerPropertiesObject();
		hostname += serverProperties.suffix;
		process = new Process(hostname);
		serverId = serverProperties.processId.get(hostname);

	}

	
	/**
	 * Defining rule for channels such that the same channel is used for communication between two processes every time.
	 * P0 -> P1 = 0
	 * P1 -> P2 = 1
	 * P2 -> P0 = 2
	 * P1 -> P0 = 3
	 * P0 -> P2 = 4
	 * P2 -> P1 = 5
	 * 
	 * @param sourceProcess - The process which is sending data.
	 * @param destProcess - The process which is going to receive the data.
	 * @return - The channel index that should be used.
	 */
	public int retrieveChannel(int sourceProcess, int destProcess){
		if(sourceProcess == 0){
			return (destProcess == 1) ? 0 : 4 ;
		}
		else if(sourceProcess == 1){
			return (destProcess == 0) ? 3 : 1 ;
		}
		else if(sourceProcess == 2){
			return (destProcess == 0) ? 2 : 5 ;
		}
		return -1;
	}

	/**
	 * Defining rule for channels such that the same channel is used for communication between two processes every time.
	 * P0 -> P1 = 0
	 * P1 -> P2 = 1
	 * P2 -> P0 = 2
	 * P1 -> P0 = 3
	 * P0 -> P2 = 4
	 * P2 -> P1 = 5
	 * 
	 * @param sourceProcess - The process which is sending data.
	 * @param destProcess - The process which is going to receive the data.
	 * @return - The channel index that should be used.
	 */
	public String retrieveHosts(int process){

		switch(process){
		case 0: 
			return "2,3";
		case 1: 
			return "0,5";
		case 2: 
			return "1,4";

		default:
			return "Invalid.";
		}
	}

	/**
	 * All the main execution happens here.
	 */
	public void run(){
		while(true){
			String threadName = Thread.currentThread().getName();

			if(!process.oneTime){
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				process.oneTime = true;
			}
			if(threadName.equals("local")){
				//Pick a random index of the child.
				Random rand = new Random();
				int randomAmount = rand.nextInt(maxAmount + 1);
				int currentAmount = process.balance;

				//				synchronized(process){
				Socket clientSocket = null; 
				int randomProcess = rand.nextInt(serverProperties.numberOfProcesses);

				try{
					Thread.sleep(1000); // Make the Thread sleep for one second.
				}
				catch(Exception e){
					errorMessage(e);
				}


				if(process.sendMarker){
					try{
						//							System.out.println("############################## Marker Sending Begins ############################## ");
						sendMarker(serverId, marker);
						//							System.out.println("############################## Marker Sending Ends ############################## ");
						process.sendMarker = false;
					}
					catch(Exception e){
						errorMessage(e);
						break;
					}
				}

				while(randomProcess == serverId){
//					System.out.println("Waiting...");
					randomProcess = rand.nextInt(serverProperties.numberOfProcesses);
				}


				String hostname = serverProperties.servers[randomProcess];

				if((currentAmount - randomAmount) > 0){

					process.balance -= randomAmount;
					try{
						/*while(process.sendMarker && process.sendingMarkers){

							}*/
//						System.out.println("Retreiving channel index for.. serverId: " + serverId + " randomProcess: " + randomProcess);
						int channelIndex = retrieveChannel(serverId, randomProcess);
						System.out.println("Sending data on "+ hostname +" data: " + randomAmount);
//						System.out.println("While Sending on Channel"+ channelIndex + " data: " + randomAmount);
						clientSocket = new Socket(hostname, port);
						DataOutputStream DO = new DataOutputStream(clientSocket.getOutputStream());
						DO.writeInt(channelIndex);

						DO.writeUTF("data");
						DO.writeInt(randomAmount);

						/*try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}*/

					}
					catch (Exception e){
						e.printStackTrace();
					}
					finally{
						try {
							clientSocket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
				else{
					System.err.println("Not enough Balance!");
//					break;
				}
				//				}
				if(serverId == 1 && !process.sendMarker ){
					try{
						/*long currentTime = (new Date()).getTime();

						if()
						process.startTime = currentTime;
						 */
						process.markerCompleted = false;
						Thread.sleep(2000);
						System.out.println("Taking Snapshot NOW!");
						marker++;
						process.processState = process.balance;
						process.sendMarker = true;
						process.oneTimeMarker = true;
						process.stateRecorded = true;
						System.out.println("############################## " + (marker) + " Marker Operation Begins ############################## ");
						System.out.println("Marker: " + marker + " Received, Process P" + serverId + " state: "+ process.processState);
						System.out.println("############################## " + (marker) + " Marker Operation Ends ############################## ");



					}
					catch (Exception e) {
						errorMessage(e);
					}
				}


				/*try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("2 seconds done.");*/
			}
			else if (threadName.equals("transferController")){

				ServerSocket serverSocket = null;
				Socket clientSocket = null;

				try{
					serverSocket = new ServerSocket(port);

					System.out.println("Listening on port: " + port);

					clientSocket = serverSocket.accept();
					System.out.println("Connected");
					//					synchronized(process){
					String data = "";
					//					synchronized(process){
					DataInputStream DI = new DataInputStream(clientSocket.getInputStream());
					int channelIndex = DI.readInt();
					System.out.println("Channel Index Received: |" + channelIndex + "|");
					data = DI.readUTF();
					System.out.println("Data received: |" + data + "|");


					if(data.equals("marker")){

						
						marker = DI.readInt();
						System.out.println("############################## " + (marker) + " Marker Operation Begins ############################## ");
						if(!process.stateRecorded){
							process.processState = process.balance;
							process.stateRecorded = true;
							
							process.sendingMarkers = true;
							System.out.println("Marker: " + marker + " Received, Process P" + serverId + " state: "+ process.processState);
							System.out.println("************ Channel" + channelIndex + " state recorded as:" + process.channels[channelIndex]);
							process.channelReserved[channelIndex] = true;
							process.sendMarker = true;
							/*,,,,,,,,*/
							//								Thread temp = new Thread(cl);
							//								temp.setName("sender");
							//								temp.run();
							/*ChandyLamport cl = new ChandyLamport(marker); 
								cl.sendMarker(serverId, marker);*/

						}
						else{
							System.out.println("************ Channel" + channelIndex + " state recorded as: " + process.channels[channelIndex]);
							process.channelReserved[channelIndex] = true;

							String[] sourceDest = retrieveHosts(serverId).split(",");
							int source = Integer.parseInt(sourceDest[0]);
							int dest = Integer.parseInt(sourceDest[1]);
							System.out.println("source: "+ source + " dest:" + dest );
							if(process.channelReserved[source] && process.channelReserved[dest]){
								System.out.println("Condition true!");
								process.markerCompleted = true;
								process.stateRecorded = false;
								process.balance += process.channels[source];
								process.balance += process.channels[dest];
								process.channels[source] = 0;
								process.channels[dest] = 0;
								process.channelReserved[source] = false;
								process.channelReserved[dest] = false;
								process.processState = 0; 
							}

						}
						System.out.println("############################## " + marker + " Marker Operation Ends ############################## ");

					}
					else if (data.equals("data")){

						int randomAmount = DI.readInt();
						System.out.println("Amount Received: |" + randomAmount + "|");
						if(!process.stateRecorded){
							System.out.println("After Receiving on Channel" + channelIndex + " data: " + 0);
							process.balance += randomAmount;
							System.out.println("Transfer of Amount: " + randomAmount +" Success, New Balance: " + process.balance);
						}
						else{
							process.channels[channelIndex] += randomAmount;
							System.out.println("Channel" + channelIndex + " Amount: " + process.channels[channelIndex]);
						}

					}
					else{
						System.err.println("Invalid data.");
					}
					//					}



				}
				catch(Exception e){
					errorMessage(e);
				}
				finally{
					try {
						serverSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						errorMessage(e);
					}
				}

			}
			/*try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			else{
				break;
			}
		}

	}

	private void sendMarker(int markerSource, int marker) throws IOException {
		Socket clientSocket = null;
		int number = 1000;
		for(int server=0; server<serverProperties.numberOfProcesses; server++){
			if(server!= markerSource){
				try{
					String hostname = serverProperties.servers[server];
					int channelIndex = retrieveChannel(markerSource, server);
					clientSocket = new Socket(hostname, port);
					DataOutputStream DO = new DataOutputStream(clientSocket.getOutputStream());
					System.out.println("################ Sending marker to " + hostname + " on Channel" + channelIndex);
					DO.writeInt(channelIndex);
					DO.writeUTF("marker");
					DO.writeInt(marker);
					Thread.sleep(number);
					number += 1000;
				}
				catch(Exception e){
					e.printStackTrace();
//					break;
				}
				finally{
					clientSocket.close();
				}

			}

		}
		process.sendingMarkers = false;
		/*System.out.println("Sleeping for 2 sec");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}

	private void errorMessage(Exception e) {
		e.printStackTrace();
	}

	public static void main(String[] args) throws UnknownHostException {
		cl = new ChandyLamport();
		Thread t1 = new Thread(cl);
		t1.setName("local");
		Thread t2 = new Thread(cl);
		t2.setName("transferController");
		t1.start();
		t2.start();
	}

}
