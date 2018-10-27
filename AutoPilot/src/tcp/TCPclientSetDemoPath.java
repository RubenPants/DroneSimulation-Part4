package tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class TCPclientSetDemoPath {

	static String ip = "10.46.213.13";
	
	private static boolean choosePath = false;
	
	public static void main(String argv[]) throws Exception {

		ArrayList<String> possibleModes = new ArrayList<String>() {{
			add("stack");
			add("taxi_to_other_side");
			add("taxi");
			add("default");
		}};

		ArrayList<String> loggingObject = new ArrayList<String>() {{
			add("airport");
			add("drone");
			add("off");
		}};

		ArrayList<String> possibleNumber = new ArrayList<String>() {{
			add("0");
			add("1");
			add("2");
			add("3");
			add("4");
			add("5");
			add("6");
			add("7");
			add("8");
			add("8");
			add("9");
			add("10");
			add("11");
			add("12");
			add("13");
			add("14");
			add("15");
			add("16");
			add("17");
		}};

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket;
        PrintWriter outToServer;
        
        if (choosePath) {
		    System.out.println("Choose your demo scenario, your options are: \n"
		    		+ "stack \n"
		    		+ "taxi_to_other_side (or 'taxi') \n"
		    		+ "default");
		    System.out.println("");
		    System.out.println("Please enter your choice:");
		    String receivedMode = inFromUser.readLine().toLowerCase();
		    while (!possibleModes.contains(receivedMode)) {
		    	System.out.println("");
		    	System.out.println("Invalid input!");
		        System.out.println("type [stack / taxi_to_other_side / default]");
		        receivedMode = inFromUser.readLine().toLowerCase();
		    }
		    
		    // TODO: Add extra functionality besides the default methods?
		    String dataToSend;
		    if (receivedMode.equals("taxi")) dataToSend = "taxi_to_other_side";
		    else dataToSend = receivedMode;
		    
		    System.out.println("");
		    System.out.println("DATA SENT: "+dataToSend);
		    
		    // Send data to the drone
		    clientSocket = new Socket(ip, 9876);
		    outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
		    outToServer.println(dataToSend);
		    clientSocket.close();
        }
        
        
        while (true) {
        	System.out.println("");
        	System.out.println("Enable logging to [airport/drone/off]: ");
            String receivedLogging = inFromUser.readLine().toLowerCase();
            
            while (!loggingObject.contains(receivedLogging)) {
            	System.out.println("");
            	System.out.println("Invalid input!");
            	System.out.println("Enable logging to [airport/drone/off]: ");
                receivedLogging = inFromUser.readLine().toLowerCase();	
            }
        	
            if (receivedLogging.contains("off")) {
            	System.out.println("TURN OFF ALL PRINTS");

                // Send data to the drone
                clientSocket = new Socket(ip, 9876);
                outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
                outToServer.println(receivedLogging+"/"+"0");
                clientSocket.close();
            } else {
	        	System.out.println("Choose number: ");
	            String receivedInt = inFromUser.readLine().toLowerCase();
	            
	            while (!possibleNumber.contains(receivedInt)) {
	            	System.out.println("");
	            	System.out.println("Invalid input!");
	            	System.out.println("Choose number: ");
	                receivedInt = inFromUser.readLine().toLowerCase();
	            	
	            }
	            
	            System.out.println("PRINT: "+receivedLogging+" number: "+receivedInt);
	            
	            // Send data to the drone
	            clientSocket = new Socket(ip, 9876);
	            outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
	            outToServer.println(receivedLogging+"/"+receivedInt);
	            clientSocket.close();
            }
        }
	}
}
