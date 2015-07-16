package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * A thread listens for incoming messages to the server from a client and
 * communicates back to the client.
 * 
 * @author Jonathan
 *
 */
public class ClientThread extends Thread implements Comparable<ClientThread> {
	private Socket socket;
	private IRC_Server server;
	private PrintWriter out;
	private String channel;
	private String username;

	private static final int TIMEOUT = 60 * 60 * 1000; // minutes / seconds / milliseconds

	public ClientThread(IRC_Server server, Socket socket) {
		this.server = server;
		this.socket = socket;

		start();
	}
	
	private boolean usernameTaken(String username) {
		synchronized (server) {
			for (ClientThread ct : server.getClients()) {
				if (username.equals(ct.getUserName()) && this != ct) {
					out.println("Username already taken. Please enter a new one:");
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Sets up an input stream and output stream and then starts to listen
	 * for incoming messages from the client which are handled in different
	 * ways.
	 */
	@Override
	public void run() {
		BufferedReader in = null;
		final String HELP = ">> Following is a list of available commands:\n"
				+ ">> To join a channel, type: /join (e.g. /join MikesRoom)\n"
				+ ">> To leave a channel, type: /leave\n"
				+ ">> To whisper to another user, type: /whisper, followed by the\n"
				+ ">> user and the message (e.g. /whisper joel get on skype!)\n"
				+ ">> To display a list of online users, type /users. If you are\n"
				+ ">> in a channel, users in the channel will be displayed.\n"
				+ ">> Otherwise, all online users on the server will be displayed.\n"
				+ ">> To exit from the server, type /exit\n"
				+ ">> To display this list again, type /help";

		try {
			socket.setSoTimeout(TIMEOUT);

			in = new BufferedReader(
					new InputStreamReader(
							socket.getInputStream()));
			out = new PrintWriter(
					new OutputStreamWriter(
							socket.getOutputStream(), "ISO-8859-1"), true);

			out.println(">> Connected to: " + socket.getLocalAddress() + 
					".\n>> Please enter your username:");

			String msg;
			
			do {
				if ((msg = in.readLine()) != null) username = msg;
			} while (usernameTaken(username));
			
			server.clientConnected(this);
			out.println(">> Welcome, " + username + ".");
			out.println(HELP);

			while ((msg = in.readLine()) != null) {
				String[] args = msg.split(" ", 3);
				String command = args[0];

				if (command.equals("/join")) {
					if (args.length < 2) {
						out.println(">> Please include channel name");
						continue;
					}

					channel = args[1];
					server.joinChannel(this, channel);
					out.println(">> Joined channel: " + channel);
				} else if (command.equals("/whisper")) {
					if (args.length < 3) {
						out.println(">> Please include receipent and message");
						continue;
					}

					String reciepent = args[1];

					if (reciepent.equals(username)) {
						out.println(">> Unfortunately you cannot whisper to yourself.");
						continue;
					}

					String whisper = args[2];
					String response;

					if (server.whisper(username, reciepent, whisper)) {
						response = ">> To " + reciepent + ": " + whisper;
					} else {
						response = ">> There is no user named: " + reciepent;
					}

					out.println(response);
				} else if (command.equals("/users")) {
					server.onlineUsers(this);
				} else if (command.equals("/leave")) {
					if (channel == null) {
						out.println(">> You are not in a channel.");
						continue;
					}
					
					server.leaveChannel(this);
					out.println(">> Left channel: " + channel);
					channel = null;
				} else if (command.equals("/exit")) {
					out.println(">> Disconnecting from server.");
					break;
				} else if (command.equals("/help")) {
					out.println(HELP);
				} else if (channel != null) {
					server.broadcast(this, username + ": " + msg);
				}
			}
		} catch (SocketTimeoutException e) {
			System.out.println(String.format("Client timed out: %s", this));
		} catch (SocketException e) {
			System.out.println(String.format("Socket exception for client: %s", 
					this));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Client disconnected: " + this);
			server.disconnect(this);

			if (socket != null) {
				try {
					in.close();
					out.close();
					socket.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public PrintWriter getOut() {
		return out;
	}

	public String getUserName() {
		return username;
	}

	public String getChannel() {
		return channel;
	}

	public String toString() {
		return username;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		
		if (other == null || this.getClass() != other.getClass())
			return false;
		
		ClientThread ct = (ClientThread) other;
		return username.equals(ct.getUserName());
	}
	
	@Override
	public int compareTo(ClientThread ct) {
		return username.compareTo(ct.getUserName());
	}
}