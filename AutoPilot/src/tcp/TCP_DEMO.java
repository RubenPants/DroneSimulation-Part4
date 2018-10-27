package tcp;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TCP_DEMO {
	
	public static void main(String argv[]) throws Exception {

		ArrayList<String> possibleRequests = new ArrayList<String>() {{
			add("tab");
			add("right_arrow");
		}};

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	    
		while (true) {
			System.out.println("____________________________________________________");
		    System.out.println("Please choose your action: ");
	        String receivedMode = inFromUser.readLine().toLowerCase();
			while (!possibleRequests.contains(receivedMode)) {
				System.out.println("Invalid input, please try again: ");
		        receivedMode = inFromUser.readLine().toLowerCase();
			}
		    
			switch(receivedMode) {
			case "tab":
				try {
					Robot robot = new Robot();
					
				    robot.keyPress(KeyEvent.VK_ALT);
				    robot.keyPress(KeyEvent.VK_TAB);
				    robot.delay(100);
				    robot.keyRelease(KeyEvent.VK_TAB);
				    robot.keyRelease(KeyEvent.VK_ALT);
				    
				    System.out.println("'tab' pressed");
				} catch (AWTException e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
		
	}
}
