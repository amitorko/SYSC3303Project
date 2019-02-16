
/**
 * The Implementation of the Scheduler Class
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Scheduler {

	// Packets and Sockets
	DatagramPacket floorSendPacket, elevatorSendPacket, floorReceivePacket, elevatorReceivePacket;
	DatagramSocket sendSocket, floorReceiveSocket, elevatorReceiveSocket;

	// Lamps and Sensors
	private boolean floorLamps[];
	private ArrayList<Integer> reqFloors;
	private boolean arrivalSensors[];

	// Total number of floors
	private final int numFloors;

	// Elevator Data List
	private ElevatorData elevDataList[];
	private ArrayList<Integer> potentialRoutes;
	private int routedElevator;

	// Data Structures for relaying Data
	private SchedulerData scheDat;
	private FloorData floorDat;
	private ElevatorData elevDat;

	/**
	 * Create a new Scheduler with the corresponding number of floors
	 * 
	 * @param numFloors
	 */
	public Scheduler(int numFloors, int numElevators) {
		try {
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send UDP Datagram packets.
			sendSocket = new DatagramSocket();

			// Construct a datagram socket and bind it to port 4000
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			floorReceiveSocket = new DatagramSocket(3000);

			// Construct a datagram socket and bind it to port 4000
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			elevatorReceiveSocket = new DatagramSocket(4000);

			// to test socket timeout (2 seconds)
			// receiveSocket.setSoTimeout(2000);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		this.numFloors = numFloors;
		floorLamps = new boolean[numFloors];
		arrivalSensors = new boolean[numFloors];
		reqFloors = new ArrayList<Integer>();

		elevDataList = new ElevatorData[numElevators];
		for (int i = 0; i < numElevators; i++) {
			// Assume same starting position as set in elevator subsystem
			elevDataList[i] = new ElevatorData(i, 0, new ArrayList<Integer>(), false, false);
		}

	}

	/*
	 * public void receiveAndReply() { floorReceive(); elevatorSend(); wait5s();
	 * elevatorReceive(); floorSend(); }
	 */

	/**
	 * Close the sockets
	 */
	public void closeSockets() {
		// We're finished, so close the sockets.
		sendSocket.close();
		floorReceiveSocket.close();
		elevatorReceiveSocket.close();
	}

	/**
	 * Send the Floor subsystem a data packet
	 * 
	 * @param scheDat
	 *            the scheduler data
	 */
	public void floorSend(SchedulerData scheDat) {

		this.scheDat = scheDat;

		try {
			// Convert the FloorData object into a byte array
			ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
			ObjectOutputStream ooStream;
			ooStream = new ObjectOutputStream(new BufferedOutputStream(baoStream));
			ooStream.flush();
			ooStream.writeObject(scheDat);
			ooStream.flush();
			byte msg[] = baoStream.toByteArray();

			floorSendPacket = new DatagramPacket(msg, msg.length, floorReceivePacket.getAddress(),
					floorReceivePacket.getPort());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Send the datagram packet to the client via the send socket.
		try {
			sendSocket.send(floorSendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		processFloorSend();
		print("Scheduler: Packet sent to FloorSubsystem.\n");

	}

	/**
	 * Receive a packet from the Floor subsystem
	 */
	public void floorReceive() {
		// Construct a DatagramPacket for receiving packets up
		// to 100 bytes long (the length of the byte array).

		byte data[] = new byte[5000];
		floorReceivePacket = new DatagramPacket(data, data.length);
		print("Scheduler: Waiting for Packet.\n");

		// Block until a datagram packet is received from receiveSocket.
		try {
			print("Waiting..."); // so we know we're waiting
			floorReceiveSocket.receive(floorReceivePacket);
		} catch (IOException e) {
			print("IO Exception: likely:");
			print("Receive Socket Timed Out.\n" + e);
			e.printStackTrace();
			System.exit(1);
		}

		try {
			// Retrieve the ElevatorData object from the receive packet
			ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
			ObjectInputStream is;
			is = new ObjectInputStream(new BufferedInputStream(byteStream));
			Object o = is.readObject();
			is.close();

			floorDat = (FloorData) o;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		processFloorReceived();
	}

	/**
	 * Send the Elevator subsystem a data packet
	 * 
	 * @param scheDat
	 *            the scheduler data
	 */
	public void elevatorSend(SchedulerData scheDat) {

		this.scheDat = scheDat;
		try {
			// Convert the FloorData object into a byte array
			ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
			ObjectOutputStream ooStream;
			ooStream = new ObjectOutputStream(new BufferedOutputStream(baoStream));
			ooStream.flush();
			ooStream.writeObject(scheDat);
			ooStream.flush();
			byte msg[] = baoStream.toByteArray();

			elevatorSendPacket = new DatagramPacket(msg, msg.length, floorReceivePacket.getAddress(), 2000);// elevatorSubsystem
																											// server
			// port
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Send the datagram packet to the client via the send socket.
		try {
			sendSocket.send(elevatorSendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		processElevatorSend();

		print("Scheduler: Packet sent to ElevatorSubsystem.\n");
	}

	/**
	 * Receive a packet from the Elevator subsystem
	 */
	public void elevatorReceive() {
		// Construct a DatagramPacket for receiving packets up
		// to 100 bytes long (the length of the byte array).

		byte data[] = new byte[5000];
		elevatorReceivePacket = new DatagramPacket(data, data.length);
		print("Scheduler: Waiting for Packet.\n");

		// Block until a datagram packet is received from receiveSocket.
		try {
			print("Waiting..."); // so we know we're waiting
			elevatorReceiveSocket.receive(elevatorReceivePacket);
		} catch (IOException e) {
			print("IO Exception: likely:");
			print("Receive Socket Timed Out.\n" + e);
			e.printStackTrace();
			System.exit(1);
		}

		try {
			// Retrieve the ElevatorData object from the receive packet
			ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
			ObjectInputStream is;
			is = new ObjectInputStream(new BufferedInputStream(byteStream));
			Object o = is.readObject();
			is.close();

			elevDat = (ElevatorData) o;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		processElevatorReceived();
	}

	/**
	 * Process the received floor packet
	 */
	public void processFloorReceived() {
		print("Scheduler: Packet received.");
		print("From FloorSubsystem: " + floorReceivePacket.getAddress());
		print("Host port: " + floorReceivePacket.getPort());
		print("Packet Length: " + floorReceivePacket.getLength());
		print("Containing: \n" + floorDat.getStatus() + "\n");
	}

	/**
	 * Process the received elevator packet
	 */
	public void processElevatorReceived() {
		print("Scheduler: Packet received.");
		print("From ElevatorSubsystem: " + elevatorReceivePacket.getAddress());
		print("Host port: " + elevatorReceivePacket.getPort());
		print("Packet length: " + elevatorReceivePacket.getLength());
		print("Containing: \n" + elevDat.getStatus() + "\n");

		elevDataList[elevDat.getElevatorNumber()] = elevDat;
	}

	/**
	 * Process the scheduler packet sent to the Elevator subsystem
	 */
	public void processElevatorSend() {
		print("Scheduler: Sending packet to ElevatorSubsystem.");
		print("To host: " + elevatorSendPacket.getAddress());
		print("Destination host port: " + 2000);
		print("Length: " + elevatorSendPacket.getLength());
		print("Containing: \n" + scheDat.getStatus() + "\n");

	}

	/**
	 * Process the scheduler packet sent to the Floor subsystem
	 */
	public void processFloorSend() {
		print("Scheduler: Sending packet to FloorSubsystem.");
		print("To host: " + floorSendPacket.getAddress());
		print("Destination host port: " + floorSendPacket.getPort());
		print("Length: " + floorSendPacket.getLength());
		print("Containing: " + scheDat.getStatus() + "\n");

	}

	/**
	 * Wait 5 seconds
	 */
	public void wait5s() {
		// Slow things down (wait 5 seconds)
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Update the scheduler's floor requests
	 */
	public void updateRequests() {
		if (!reqFloors.contains(floorDat.getFloorNum()))
			reqFloors.add(floorDat.getFloorNum());
	}
	
	/**
	 * Clear the scheduler's floor requests
	 */
	public void clearRequest() {
		reqFloors.clear();
	}

	
	/**
	 * Update the scheduler's lamps
	 */
	public void updateLamps() {
		// Update the floor lamps
		floorLamps[elevDat.getCurrentFloor() - 1] = true;
	}

	/**
	 * Returns true if there is an elevator on the same floor
	 * 
	 * @return
	 */
	public boolean elevatorSameFloor() {
		boolean caseTrue = false;
		for (int i = 0; i < elevDataList.length; i++) {
			if (floorDat.getFloorNum() == elevDataList[i].getCurrentFloor()) {
				caseTrue = true;
				potentialRoutes.add(i);
			}
		}
		if (!caseTrue)
			potentialRoutes.clear();
		
		return false;
	}

	/**
	 * Returns true if there is an elevator above the requested floor
	 * 
	 * @return
	 */
	public boolean elevatorAboveFloor() {
		boolean caseTrue = false;
		for (int i = 0; i < elevDataList.length; i++) {
			if (elevDataList[i].getCurrentFloor() > floorDat.getFloorNum()) {
				caseTrue = true;
				potentialRoutes.add(i);
			}
		}
		if (!caseTrue)
			potentialRoutes.clear();
		
		return caseTrue;
	}

	/**
	 * Returns true if there is an elevator below the requested floor
	 * 
	 * @return
	 */
	public boolean elevatorBelowFloor() {
		boolean caseTrue = false;
		for (int i = 0; i < elevDataList.length; i++) {
			if (elevDataList[i].getCurrentFloor() < floorDat.getFloorNum()) {
				caseTrue = true;
				potentialRoutes.add(i);
			}
		}
		if (!caseTrue)
			potentialRoutes.clear();
		return caseTrue;
	}

	/**
	 * Determine which elevator should get the floor request
	 */
	public void routeElevator() {

		print("Routing an elevator.");
		potentialRoutes = new ArrayList<Integer>();
		// Case 1: There is already an elevator on the floor that the request came from
		if (elevatorSameFloor()) {
			print(potentialRoutes.size() + " potential routes.");
			routedElevator = potentialRoutes.get(0);
			// for now just pick the first elevator, choosing the most suitable (if idle)
			// can be implemented later
		}

		// Case 2: There are elevator(s) above request floor, floor request down
		else if (elevatorAboveFloor() && floorDat.downPressed()) {
			print(potentialRoutes.size() + " potential routes.");
			routedElevator = potentialRoutes.get(0);
			// Find the closest elevator that is moving down
			for (Integer i : potentialRoutes) {
				if ((Math.abs(elevDataList[i].getCurrentFloor() - floorDat.getFloorNum())) < 
						(Math.abs(elevDataList[routedElevator].getCurrentFloor() - floorDat.getFloorNum()))
						&& (elevDataList[i].isMovingDown() || elevDataList[i].isIdle()))
					routedElevator = i;
			}
		}

		// Case 3: There are elevator(s) above request floor, floor request down
		else if (elevatorAboveFloor() && floorDat.upPressed()) {
			print(potentialRoutes.size() + " potential routes.");
			routedElevator = potentialRoutes.get(0);
			// Find the closest elevator that is moving down
			for (Integer i : potentialRoutes) {
				if ((Math.abs(elevDataList[i].getCurrentFloor() - floorDat.getFloorNum())) < 
						(Math.abs(elevDataList[routedElevator].getCurrentFloor() - floorDat.getFloorNum()))
						&& (elevDataList[i].isMovingDown() || elevDataList[i].isIdle()))
					routedElevator = i;
			}
		}

		// Case 4: There are elevator(s) below request floor, floor request up
		else if (elevatorBelowFloor() && floorDat.upPressed()) {
			print(potentialRoutes.size() + " potential routes.");
			routedElevator = potentialRoutes.get(0);
			// Find the closest elevator that is moving down
			for (Integer i : potentialRoutes) {
				if ((Math.abs(elevDataList[i].getCurrentFloor() - floorDat.getFloorNum())) < 
						(Math.abs(elevDataList[routedElevator].getCurrentFloor() - floorDat.getFloorNum()))
						&& (elevDataList[i].isMovingUp() || elevDataList[i].isIdle()))
					routedElevator = i;
			}
		}

		// Case 5: There are elevator(s) below request floor, floor request up
		else if (elevatorBelowFloor() && floorDat.downPressed()) {
			print(potentialRoutes.size() + " potential routes.");
			routedElevator = potentialRoutes.get(0);
			// Find the closest elevator that is moving down
			for (Integer i : potentialRoutes) {
				if ((Math.abs(elevDataList[i].getCurrentFloor() - floorDat.getFloorNum())) < 
						(Math.abs(elevDataList[routedElevator].getCurrentFloor() - floorDat.getFloorNum()))
						&& (elevDataList[i].isMovingUp() || elevDataList[i].isIdle()))
					routedElevator = i;
			}
		}

		/*
		 * Case 6: No elevators are already on the path to floor req (ie elevators above
		 * are moving up & elevators below are moving dowm) wait for & send first
		 * elevator to become idle (ie no next destinatios) or send first elevator that
		 * switched directions (ie is now on the path to req floor)
		 */
		else {
			for (Integer i : potentialRoutes) {
				if ((Math.abs(elevDataList[i].getCurrentFloor() - floorDat.getFloorNum())) < 
						(Math.abs(elevDataList[routedElevator].getCurrentFloor() - floorDat.getFloorNum()))
						&& elevDataList[i].isIdle())
					routedElevator = i;
			}
		}

		print("Sending elevator " + routedElevator + ".\n");
		scheDat = new SchedulerData(routedElevator, floorLamps, reqFloors);
	}

	/**
	 * Return the last received elevator data
	 * 
	 * @return
	 */
	public ElevatorData getElevatorData() {
		return elevDat;
	}

	/**
	 * Return the scheduler's current data
	 * 
	 * @return the current scheduler data
	 */
	public SchedulerData getSchedulerData() {
		return scheDat;
	}

	/**
	 * Return the last received floor data
	 * 
	 * @return
	 */
	public FloorData getFloorData() {
		return floorDat;
	}

	/**
	 * Prints the message on the console
	 * 
	 * @param message
	 */
	public void print(String message) {
		System.out.println(message);
	}

	public static void main(String args[]) {
		Scheduler c = new Scheduler(5, 2);

		/**
		 * Scheduler Logic
		 */
		while (true) {
			// Receive a request from a floor
			c.floorReceive();
			// Update current data
			c.updateRequests();

			// Route appropriate elevator
			c.routeElevator();
			// Relay request to appropriate elevator
			c.elevatorSend(c.getSchedulerData());
			
			//Clear requests
			c.clearRequest();

			// Receive input data from elevator to light appropriate lamps
			c.elevatorReceive();
			// Light appropriate lamps
			c.updateLamps();
		}

	}
}
