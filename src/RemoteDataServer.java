
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import messages.Constants;

public class RemoteDataServer implements Runnable {
	
		// local settings
		private int PORT;
		private InetAddress ipAddress;
		
		
		//remote settings
		private int clientPort;
		private InetAddress listenerAddress;
		
		private ServerSocket server;
		private Socket client;
		private byte[] buf;
		private BufferedReader in;
		private PrintWriter out;
		
		private String message;
		private AutoBot bot;
				
		private ServerListener window;
		
		private ImageSender sender;
		private ImageReceiver receiver;

		
		public RemoteDataServer(int port) {
			PORT = port;
			buf = new byte[1000];
			bot = new AutoBot();
		}
		
		public RemoteDataServer() {
			buf = new byte[1000];
			bot = new AutoBot();
		}
		
		public void setPort(int port){ PORT = port; }
		
		public void setClientPort(int port){  clientPort = port; }
		
		public void setIP(InetAddress inet){ ipAddress = inet; }
		
		public void setServerListener(ServerListener listener){ window = listener; }
		
		public void shutdown() {
			try{ server.close(); }
			catch(Exception e){}
		}

		public void run() {
			boolean connected = false;
			try {
				server = new ServerSocket(PORT);
				client = server.accept();
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
				connected = true;
				receiveImage();	// Create server test
			}
			catch(BindException e) {
				setListenerMessage("Port "+PORT+" is already in use. Use a different Port");
				setConnectButtonEnabled(false);
			}
			catch(Exception e) {
				setListenerMessage("Unable to connect");
				setConnectButtonEnabled(false);
			}
			
			while(connected) {
				// get message from sender
				try {
					// store the packets address for sending images out
					listenerAddress = client.getInetAddress();
					clientPort = client.getPort();
					System.out.println("Client:" + listenerAddress);
					// translate and use the message to automate the desktop
					message = in.readLine();
					
					System.out.println(message);
					
					if (message.equals("Connectivity"))
					{
						setListenerMessage("Trying to Connect");
						out.println(message); //echo the message back
					}
					
					else if(message.equals("Connected"))
					{
						out.println(message); //echo the message back
					}
					
					else if(message.equals("Close"))
					{
						setListenerMessage("Controller has Disconnected. Trying to reconnect."); //echo the message back
					}
					
					else if(message.charAt(0) == Constants.REQUESTIMAGE)
					{
						
						String[] arr = message.split("" + Constants.DELIMITER);
						System.out.print("Request msg:" + arr[1]+" "+arr[2]+"\n");
						sendImage(Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
					}
					
					else if(message.charAt(0) == Constants.PROVIDEIMAGE)
					{
						System.out.println("Provide Image");
						receiveImage();
					}
					
					else
					{
						System.out.print("Touch:" + message);
						setListenerMessage("Connected to Controller");
						bot.handleMessage(message);
					}
				}catch(Exception e) {
					System.out.println("[Server]" + e);
					setListenerMessage("Disconnected");
					setConnectButtonEnabled(false);
					connected = false;
				}
			}
		}
		
		private void setListenerMessage(String msg) {
			if(window != null) {
				window.setMessage(msg);
			}
		}
		
		private void setConnectButtonEnabled(boolean enable) {
			if(window != null) {
				window.setConnectButtonEnabled(enable);
			}
		}
		
		public void sendImage(int width, int height) {
			if(sender == null && listenerAddress != null) {
				sender = new ImageSender(client);
			}
			
			if(sender != null) {
				if(width == 0 && height == 0) {
					System.out.println("Receive 0 rectangle");
					return;
				}
				
				float scale = 0.5f;
				if(width > height) {
					scale = ImageSender.SIZETHRESHOLD / width;
				}else{
					scale = ImageSender.SIZETHRESHOLD / height;
				}
				//width = height = 20;
				sender.setPort(clientPort);
				//sender.setImage(bot.getScreenCap((int)Math.round(width*scale), (int)Math.round(height * scale)) );
			
				sender.setImage(bot.getScreenCap(width, height));

				Thread send_image_thread = new Thread(sender);
				send_image_thread.start();
			}
		}

		public void receiveImage() {
			//if(receiver == null && listenerAddress != null) {
			if(receiver == null) {
				System.out.println("Call receive image, start receive server");
				receiver = new ImageReceiver();
				Thread receive_image_thread = new Thread(receiver);
				receive_image_thread.start();
			}
			/*
			if(receiver != null) {
				Thread receive_image_thread = new Thread(receiver);
				receive_image_thread.start();
			}
			*/
		}
}