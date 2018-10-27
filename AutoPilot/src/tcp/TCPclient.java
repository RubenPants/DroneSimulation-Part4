package tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class TCPclient {
	
	/*
	 * READ ME
	 * 
	 * De CLI helpt u normaal gezien wel goed genoeg met het besturen van de drone, maar toch eerst
	 *  een paar opmerkingen!
	 *  
	 *  0) Pas eerst je ip-adres aan (zie string 'ip') vooralleer je kunt vliegen!
	 *  1) Om te vliegen moeten er meer dan twee targets zijn, bij het starten van de drone zijn pad (i.e. 
	 *  	een kubus toe te voegen) wordt er een extra kubus toegevoegd, verwijder deze indien nodig
	 *  2) Indien het 'remall' commando wordt toegepast op een drone, dan zal de drone crashen!
	 *  3) Het is het beste om twee consoles open te doen, zodat ge in het oog kunt houden wat het pad van de
	 *  	drone is (enkel voor testen! Niet wanneer meerdere drones vliegen --> Print = traag)
	 *  4) Om te test raad ik het aan om slechts 1 drone te gebruiken, zet dan ook NOOIT de drone integer op een 
	 *  	getal boven de 0! Het is echter wel mogelijk om tot met tien drones tegelijkertijd te vliegen, 
	 *  	just because that shit is lit af
	 *  5) Voor alles overzichtelijk te houden, zet alle prints af buiten die van de Module en de
	 *  	droneAutopilot zelf (laatste print target-lijst)
	 *  6) De methode van de drone veranderen naar "free" zal hem doen crashen. Free is bedoeld om eerst een
	 *  	volledige lijst in te kunnen voegen vooraleer de drone zal vertrekken
	 *  7) Indien alle targets verwijderd worden adhv remfir of remlas dan zal de drone terug naar (0,0,0) keren.
	 */
	
	static String ip = "10.46.213.13";
	
	public static void main(String argv[]) throws Exception {

		ArrayList<String> possibleModes = new ArrayList<String>() {{
			add("change");
			add("addfir");
			add("addlas");
			add("remfir");
			add("remlas");
			add("remall");
			add("addran");
		}};

		ArrayList<String> possibleInt = new ArrayList<String>() {{
			add("0");
			add("1");
			add("2");
			add("3");
			add("4");
			add("5");
			add("6");
			add("7");
			add("8");
			add("9");
		}};

		while (true) {
	        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            
            System.out.println("_______________________________________________________________");
            System.out.println("Choose to add (add---) or remove (rem---) cube in front (---fir) or \n "
            		+ "at the end (---las) of the list. It also possible to change the \n"
            		+ " method, remove all cubes, or add quickly a random cube (addran).");
            System.out.println("");
            System.out.println("type [change/addfir/addlas/remfir/remlas/remall/addran]");

            String receivedMode = inFromUser.readLine().toLowerCase();
            while (!possibleModes.contains(receivedMode)) {
            	System.out.println("");
            	System.out.println("Invalid input");
                System.out.println("type [change/addfir/addlas/remfir/remlas/remall/addran]");
                receivedMode = inFromUser.readLine().toLowerCase();
            }
            
            System.out.println("");
            System.out.println("Choose which drone you want to control (0-9)");

            String receivedDroneInt = inFromUser.readLine().toLowerCase();
            while (!possibleInt.contains(receivedDroneInt)) {
            	System.out.println("");
            	System.out.println("Invalid input");
                System.out.println("type [0/1/.../8/9]");
                receivedDroneInt = inFromUser.readLine().toLowerCase();
            }
            
            String dataToSend = null;
            String method;
            String cubeCoor;
            switch(receivedMode) {
            case "addfir": // Add cube to front of list
            	method = getMethod(inFromUser);
            	cubeCoor = getCubeCoor(inFromUser);
            	dataToSend = "addfir/"+receivedDroneInt+"/"+method+"/"+cubeCoor;
            	break;
            case "addlas": // Add cube to end of list
            	method = getMethod(inFromUser);
            	cubeCoor = getCubeCoor(inFromUser);
            	dataToSend = "addlas/"+receivedDroneInt+"/"+method+"/"+cubeCoor;
            	break;
            case "remfir": // Remove first element from list
            	dataToSend = "remfir/"+receivedDroneInt;
            	break;
            case "remlas": // Remove last element from list
            	dataToSend = "remlas/"+receivedDroneInt;
            	break;
            case "remall":	// Remove all elements from list
            	dataToSend = "remall/"+receivedDroneInt;
            	break;
            case "addran":	// Add quickly a cube
            	String randomCube = getRandomCoor();
            	dataToSend = "addlas/"+receivedDroneInt+"/"+"one cube"+"/"+randomCube;
            	break;
            default: // Only change method
            	dataToSend = "change/"+receivedDroneInt;
            	method = getMethod(inFromUser);
            	dataToSend = "change/"+receivedDroneInt+"/"+method;
                break;
            }
            
            System.out.println("");
            System.out.println("DATA SENT: "+dataToSend);
            
            // Send data to the drone
            Socket clientSocket = new Socket(ip, 9876);
            PrintWriter outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
            outToServer.println(dataToSend);
            clientSocket.close();
		}
	}
	
	static String getMethod(BufferedReader inFromUser) {
		
		System.out.println("");
		System.out.println("Which method do you want to use?");
        System.out.println("type [dubins/stack/one cube/free]");
		
        String method;
		try {
			method = inFromUser.readLine().toLowerCase();
	        while (!method.equals("dubins") && !method.equals("stack") && !method.equals("one cube") && !method.equals("free")) {
	        	System.out.println("");
	        	System.out.println("Invalid input");
	            System.out.println("type [dubins/stack/one cube/free]");
	            method = inFromUser.readLine().toLowerCase();
	        }
	        return method;
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return null;
	}
	
	static String getCubeCoor(BufferedReader inFromUser) {
		
		String x;
		String y;
		String z;
		
		System.out.println("");
		System.out.println("Please enter your cube coordinates in the following form:");
		System.out.println("+0000 or -0000, where the '0' can be any integer");

		System.out.print("---CubeX: ");
		x = getCoor(inFromUser);

		System.out.print("---CubeY: ");
		y = getCoor(inFromUser);

		System.out.print("---CubeZ: ");
		z = getCoor(inFromUser);
		
		return 	x+","+y+","+z;
	}
	
	static String getCoor(BufferedReader inFromUser) {
        String coor;
		try {
			coor = inFromUser.readLine().toLowerCase();
	        while ((coor.length() != 5) || !(coor.substring(0, 1).equals("+") || coor.substring(0, 1).equals("-"))) {
	        	System.out.println("");
	        	System.out.println("Invalid input");
	            System.out.println("type something in the following form '+0000'");
	            System.out.print("---Coordinate: ");
	            coor = inFromUser.readLine().toLowerCase();
	        }
	        return coor;
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return null;	
	}
	
	static String getRandomCoor() {
		String x = null;
		String y = null;
		String z = null;

		// Add signs
		double signX = Math.random();
		if (signX > 0.5) x = "+";
		else x = "-";
		
		y = "+";
		
		double signZ = Math.random();
		if (signZ > 0.5) z = "+";
		else z = "-";
		
		// Add random integers
		for (int i=0; i<4; i++) {
			x += getRandomInt();
			y += getRandomInt();
			z += getRandomInt();
		}
		
		return x+","+y+","+z;
	}
	
	static String getRandomInt() {
		int random = (int) (Math.random()*10);
		return Integer.toString(random);
	}
}
