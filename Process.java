package project2_2;

public class Process {
	
	int id;
	int balance = 1000;
	static ServerProperties serverProperties;
	boolean stateRecorded = false, sendMarker = false, oneTime = false, oneTimeMarker = false, markerCompleted = true;
	boolean sendingMarkers = false;
	int processState = 0;
	int channels[];
	boolean channelReserved[];
	long startTime = 0;
	
	Process(String hostname){
		serverProperties = ServerProperties.getServerPropertiesObject();
		this.id = serverProperties.processId.get(hostname);
		this.channels = new int[serverProperties.numberOfProcesses * (serverProperties.numberOfProcesses - 1)];
		this.channelReserved = new boolean[serverProperties.numberOfProcesses * (serverProperties.numberOfProcesses - 1)];
	}
}
