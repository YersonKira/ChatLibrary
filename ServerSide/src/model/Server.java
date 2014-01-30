package model;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import common.Client;
import common.JsonConverter;
import common.Message;
import common.MessageEvent;
import common.MessageListener;
import common.UserInformation;

public class Server implements Runnable, MessageListener {
	private ServerSocket serverSocket;
	private Thread serverThread;
	private HashMap<UserInformation, ClientServer> clients;
	
	public Server(int port) {
		try {
			serverSocket = new ServerSocket(port);
			clients = new HashMap<UserInformation, ClientServer>();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void StartServer() {
		serverThread = new Thread(this);
		serverThread.start();
		System.out.println("Server Running ...");
	}
	public void StopServer() {
		serverThread.interrupt();
		System.out.print("Server Stoped ...");
	}
	@Override
	public synchronized void run() {
		while (!Thread.interrupted()) {
			try {
				Socket clientSocket = serverSocket.accept();
				ClientServer client = new ClientServer(clientSocket);
				if (IsValidUser(client.getUserInformation())) {
					client.AddMessageListener(this);
					client.StartReadMessages();
					clients.put(client.getUserInformation(), client);
					// Actualizar la lista de de usuarios conectados a todos los clientes
					UpdateListClients();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private void UpdateListClients() {
		ArrayList<UserInformation> users = new ArrayList<UserInformation>(clients.keySet());
		String jsonusers = JsonConverter.ListToJsonString(users);
		for (UserInformation user : clients.keySet()) {
			clients.get(user).WriteOutputMessage(new Message("CLIENT", jsonusers));
		}
	}
	private boolean IsValidUser(UserInformation userinformation) {
		for (UserInformation user : clients.keySet()) {
			if (user.equals(userinformation)) {
				return false;
			}
		}
		return true;
	}
	@Override
	public void MessageReceived(MessageEvent e) {
		if (e.getMessage().getDestinationIP().equals("ALL")) {
			for (UserInformation user : clients.keySet()) {
				ClientServer client = clients.get(user);
				client.WriteOutputMessage(e.getMessage());
			}
		} else if (e.getMessage().getDestinationIP().equals("SERVER")) {
			if (e.getMessage().getText().equals("DISCONNECT")) {
				clients.remove(e.getMessage().getSourceIP());
			}
		} else {
			ClientServer client = clients.get(e.getMessage().getDestinationIP());
			client.WriteOutputMessage(e.getMessage());
		}
	}
}