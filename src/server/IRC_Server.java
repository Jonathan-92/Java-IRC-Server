package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * An IRC server which accepts clients connecting via a socket. The server 
 * can handle multiple channels and users can whisper to other users. The
 * server's address is the local host and the port can be specified in the
 * argument list (default is 2000).
 * 
 * @author Jonathan
 *
 */
public class IRC_Server {

	private static final int DEFAULT_PORT = 2000;
	private TreeSet<ClientThread> clients = new TreeSet<ClientThread>();
	private Map<String, TreeSet<ClientThread>> channels = new HashMap<String, 
			TreeSet<ClientThread>>();
	private Socket socket;

	public static void main(String[] args) {
		new IRC_Server(args);
	}

	public IRC_Server(String[] args) {
		int port;

		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
			port = DEFAULT_PORT;
		}

		/*
		 * Waits for clients to connect, and when a client connects, a new
		 * ClientThread is spawned.
		 */
		try (ServerSocket servSock = new ServerSocket(port)) {
			while (true) {
				try {
					socket = servSock.accept();
					new ClientThread(this, socket);
				} catch (IOException ex) {}
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}

	public void clientConnected(ClientThread ct) {
		forAll(clients, String.format(">> User connected: %s", ct.getUserName()));
		clients.add(ct);
	}

	private void forAll(Collection<ClientThread> coll, String message) {
		synchronized (coll) {
			for (ClientThread c : coll) {
				c.getOut().println(message);
			}
		}
	}

	public void onlineUsers(ClientThread ct) {
		PrintWriter out = ct.getOut();
		boolean inChannel = ct.getChannel() != null;
		TreeSet<ClientThread> users = inChannel ? 
				channels.get(ct.getChannel()) : clients;

		String where = inChannel ? "in channel" : "on server";
		out.format(">> Users online %s:\n", where);

		synchronized (users) {
			for (ClientThread c : users) {
				out.println(">> " + c);
			}
		}
	}

	/**
	 * Takes a message from a client in a channel and sends that message to all
	 * other clients in that channel.
	 * 
	 * @param ct		The client which is sending the message.
	 * @param message	The message which is to be sent to the other clients
	 * 					in the channel.
	 */
	public void broadcast(ClientThread ct, String message) {
		TreeSet<ClientThread> clientsInChannel = 
				new TreeSet<ClientThread>(channels.get(ct.getChannel()));

		clientsInChannel.remove(ct);

		for (ClientThread c : clientsInChannel) {
			c.getOut().println(message);
		}
	}

	public void joinChannel(ClientThread ct, String channelName) {
		if (channels.containsKey(channelName)) {
			channels.get(channelName).add(ct);
			broadcast(ct, String.format(">> %s joined the channel", ct));
		} else {
			TreeSet<ClientThread> clientsInChannel = new TreeSet<ClientThread>();
			clientsInChannel.add(ct);
			channels.put(channelName, clientsInChannel);
		}
	}

	public void leaveChannel(ClientThread ct) {
		broadcast(ct, String.format(">> %s left the channel", ct));
		channels.get(ct.getChannel()).remove(ct);
	}

	/**
	 * Takes a message from a client and sends it to another client.
	 * 
	 * @param senderName	The client who wishes to send a message.
	 * @param receiverName	The client who is supposed to receive the message.
	 * @param message		The message which is to be sent.
	 * @return				<code>false</code> if the receiver doesn't exist;
	 * 						<code>true</code> if everything succeeds. 
	 */
	public boolean whisper(String senderName, String receiverName, String message) {
		synchronized (clients) {
			for (ClientThread c : clients) {
				if (c.getUserName().equals(receiverName)) {
					c.getOut().println(senderName + ":> " + message);
					return true;
				}
			}
		}

		return false;
	}

	public void disconnect(ClientThread ct) {
		synchronized (clients) {
			clients.remove(ct);
		}
		
		Set<ClientThread> clientsInChannel;
		if ((clientsInChannel = channels.get(ct.getChannel())) != null)
				clientsInChannel.remove(ct);
		
		forAll(clients, String.format(">> User disconnected: %s", ct.getUserName()));
		ct.interrupt();
	}

	public Set<ClientThread> getClients() {
		return new HashSet<ClientThread>(clients);
	}
}
