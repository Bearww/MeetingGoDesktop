// 	Copyright 2010 Justin Taylor
// 	This software can be distributed under the terms of the
// 	GNU General Public License. 

import javax.imageio.ImageIO;
import javax.swing.*;

import org.omg.CORBA.portable.ApplicationException;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;

public class ServerWindow extends ServerListener implements ActionListener {
	
	public RemoteDataServer server;
	public RemoteDataServer listener;
	
	private Thread sThread; //server thread
	private Thread lThread; //listen thread
	
	private static final boolean DISPLAY_DESKTOP_SHORCUT = true;
	
	private static final int WINDOW_HEIGHT = 275;
	private static final int WINDOW_WIDTH = 350;
	
	private String ipAddress;
	
	private JFrame window = new JFrame("Remote Desktop Server");
	private static JFrame shortcut = new JFrame("Dekstop Shortcut");
	private static JLabel shortcutLabel = new JLabel(new ImageIcon());
	
	private JLabel addressLabel = new JLabel("");
	private JLabel portLabel = new JLabel("PORT: ");
	private JLabel listenPortLabel = new JLabel("Listen PORT: ");
	
	private JTextArea[] buffers = new JTextArea[4];
	private JTextField portTxt = new JTextField(5);
	
	private JTextField listenPortTxt = new JTextField(5);
	private JTextField ipTxt = new JTextField(10);
	private JLabel serverMessages = new JLabel("Not Connected");
	
	private JButton connectButton = new JButton("Connect");
	private JButton disconnectButton = new JButton("Disconnect");
	private JButton shutdownButton = new JButton("Shutdown");
	
	
	public ServerWindow() {
		
		server = new RemoteDataServer();
		server.setServerListener(this);
		
		listener = new RemoteDataServer();
		listener.setServerListener(this);
		
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		if(DISPLAY_DESKTOP_SHORCUT) {
			shortcut.setSize(800, 600);
			shortcut.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JScrollPane scrollPane = new JScrollPane(shortcutLabel);//把Image放進label裡

			shortcut.getContentPane().add(scrollPane);
			shortcut.pack();
			shortcut.setLocationRelativeTo(null);
			shortcut.setVisible(true);
		}
		
		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		//shutdownButton.addActionListener(this);
		
		Container c = window.getContentPane();
		c.setLayout(new FlowLayout());
		
		try{
			InetAddress ip = getIpAddress();
			ipAddress = ip.getHostAddress();
			addressLabel.setText("IP Address: ");
			ipTxt.setText(ipAddress);
		}
		catch(Exception e){addressLabel.setText("IP Address Could Not be Resolved, Try typing in the IP address.");}
		
		int x;
		for(x = 0; x < 4; x++){
			buffers[x] = new JTextArea("", 1, 30);
			buffers[x].setEditable(false);
			buffers[x].setBackground(window.getBackground());
		}
		
		c.add(addressLabel);
		c.add(ipTxt);
		c.add(buffers[0]);
		c.add(portLabel);
		portTxt.setText("6060");
		c.add(portTxt);

		c.add(buffers[3]);
		
		c.add(listenPortLabel);
		listenPortTxt.setText("6080");
		c.add(listenPortTxt);
		
		c.add(buffers[1]);
		c.add(connectButton);
		c.add(disconnectButton);
		c.add(buffers[2]);
		c.add(serverMessages);
		
		//c.add(shutdownButton);
		ipTxt.setSize(100, 20);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		window.setResizable(false);
		
		int port = Integer.parseInt(portTxt.getText());
		int clientPort = Integer.parseInt(listenPortTxt.getText());

		try{
			InetAddress ip = InetAddress.getByName(ipTxt.getText());
			runServer(port, clientPort, ip);
		}catch(UnknownHostException err){
			serverMessages.setText("Error: Check that the ip you have entered is correct.");
		}
	}
	
	
	private InetAddress getIpAddress() throws Exception
	{
		// this first line generally works on mac and windows
		InetAddress ip = InetAddress.getLocalHost();
		
		// but on linux...
		if(ip.isLoopbackAddress())
		{
			//loop trough all network interfaces
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
            {
            	//loop through the ip address associated with the interface
            	Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                	
                	// if the address is no the loopback and is not ipv6
                	if(!inetAddress.isLoopbackAddress() && !inetAddress.toString().contains(":"))
                		return inetAddress;
                }
            }
		}
		
		return ip;
	}
	
	public void actionPerformed(ActionEvent e){
		Object src = e.getSource();
		
		if(src instanceof JButton){
			if((JButton)src == connectButton){
				int port = Integer.parseInt(portTxt.getText());
				int listenPort = Integer.parseInt(listenPortTxt.getText());
				try{
					InetAddress ip = InetAddress.getByName(ipTxt.getText());
					runServer(port, listenPort, ip);
				}catch(UnknownHostException err){
					serverMessages.setText("Error: Check that the ip you have entered is correct.");
				}
			}
				
			else if((JButton)src == disconnectButton){
				closeServer();
				setConnectButtonEnabled(true);
			}
			
			else if((JButton)src == shutdownButton){
				closeServer();
				shutdown();
				System.exit(0);
			}
		}
	}
	
	public void runServer(int port, int listenerPort, InetAddress ip){
		if(port < 65535){
			server.setPort(port);
			server.setIP(ip);
			sThread = new Thread(server);
			sThread.start();
			
			listener.setPort(listenerPort);
			lThread = new Thread(listener);
			lThread.start();
			serverMessages.setText("Waiting for connection on " + ip);
			connectButton.setEnabled(false);
		}else{
			serverMessages.setText("The port Number must be less than 65535");
			connectButton.setEnabled(true);
		}
	}
	
	public void closeServer(){
		server.shutdown();
		listener.shutdown();
		setMessage("Disconnected");
	}
	
	public void setMessage(String msg) {
		serverMessages.setText(msg);
	}
	
	public void setConnectButtonEnabled(boolean enable) {
		connectButton.setEnabled(enable);
	}
	
	public static void setImage(byte[] image) {
		if(DISPLAY_DESKTOP_SHORCUT) {
			ByteArrayInputStream in = new ByteArrayInputStream(image);
			int height, width;
			try {
				BufferedImage img = ImageIO.read(in);
				height = 500;
				width = 300;
				System.out.println("Img:" + height + " " + width);
				
				Image scaleImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				BufferedImage imageBuff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				imageBuff.getGraphics().drawImage(scaleImage, 0, 0, new Color(0, 0, 0), null);
				
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				
				ImageIO.write(imageBuff, "jpg", buffer);
				
				System.out.println("DrawImage:" + image.length);
				shortcutLabel.setIcon(new ImageIcon(buffer.toByteArray()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void shutdown() {
	    String shutdownCommand;
	    String operatingSystem = System.getProperty("os.name");

	    if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
	        shutdownCommand = "shutdown -h now";
	    }
	    else if ("Windows".equals(operatingSystem)) {
	        shutdownCommand = "shutdown.exe -s -t 0";
	    }
	    else {
	    	shutdownCommand = "null";
	    }

	    Runtime runtime = Runtime.getRuntime();
        try {
			Process proc = runtime.exec(shutdownCommand);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public static void main(String[] args){
		new ServerWindow();
	}
}
