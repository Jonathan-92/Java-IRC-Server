package client;

import java.util.Scanner;

/**
 * A chat client that can send and recieve messages to/from a server. The 
 * server address is specified in the argument list, default is the local host.
 * The port is also specified in the argument list, default is 2000.
 * 
 * @author Jonathan
 *
 */
public class Client  {
	public final String hostname;
	public int port;
	public Scanner sc;
	
	public Client(String[] args) {
		
		/* The user can enter server and port manually as arguments when running the program. 
		 * Default server is 127.0.0.1
		 */
		hostname = args.length > 0 ? args[0] : "127.0.0.1";
		
		// Default port is 2000
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
			port = 2000;
		}
		 
		sc = new Scanner(System.in);
		
		// Creates a thread that connects to the server and that continuously checks for incoming messages
		Connection conn = new Connection(this);
		conn.start();
		
		String input;
		
		do {
			input = sc.nextLine();
			conn.out.println(input);
		} while (!input.equals("/exit"));
		
		sc.close();
	}

	public static void main(String[] args) {
		new Client(args);
	}
	
}
