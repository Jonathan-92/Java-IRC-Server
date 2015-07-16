package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Sets up a connection to a server and continuously checks for incoming messages.
 * 
 * @author Jonathan
 *
 */
public class Connection extends Thread {
	public PrintWriter out;

	private static final int TIMEOUT = 60 * 60 * 1000; // minutes / seconds / milliseconds

	private Socket socket = null;
	private BufferedReader in;
	private Client client;

	public Connection(Client client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
			initialize();
			checkForIncomingMessages();
		} catch (ConnectException e) {
			System.err.println("Connection lost to server");
		} catch (SocketException e) {
			System.err.println("Socket Exception: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (SocketException se) {
					System.out.println("Socket closed. Closing client.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.exit(0);
		}
	}

	private void initialize() throws UnknownHostException, IOException {
		socket = new Socket(client.hostname, client.port);
		in = new BufferedReader(
				new InputStreamReader(
						socket.getInputStream()));
		out = new PrintWriter(
				new OutputStreamWriter(
						socket.getOutputStream(), "ISO-8859-1"), true);

		socket.setSoTimeout(TIMEOUT);
	}

	private void checkForIncomingMessages() throws IOException {
		String msg;

		/* Continuously checks for messages coming to the server, and if there is any, 
		 * send it to the client to update the textArea
		 */
		while (true) {

			if ((msg = in.readLine()) != null) {
				System.out.println(msg);
			}

		}
	}
}
