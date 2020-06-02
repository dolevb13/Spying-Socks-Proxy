import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ConnectionHandler implements Runnable {

	Socket server;
	Socket client;
	InputStream serverInput;
	OutputStream serverOutput;
	InputStream clientInput;
	OutputStream clientOutput;
	byte[] packet;
	byte version;
	byte replyCD;
	String domainName;

	ConnectionHandler(Socket clientSocket) throws IOException {
		client = clientSocket;
		client.setSoTimeout(3000);
	}

	@Override
	public void run() {
		try {
			
			byte[] requestPacket = connectRequest();
			if (requestPacket != null) {
				connectReply(requestPacket);
			}
			if (replyCD == 90) {		// Connection Succeeded.
				DataThread serverTransfer = new DataThread(client, server, clientInput, serverOutput);
				serverTransfer.start();
				DataThread clientTransfer = new DataThread(server, client, serverInput, clientOutput);
				clientTransfer.start();
				serverTransfer.join();
				clientTransfer.join();
			}
			if (requestPacket != null) {
				closeConnections();
			}
		} catch (Exception e) {
			System.err.println("Exception in Proxy.run: " + e.getMessage());
		}
	}
	
	/**
	 * SOCKS CONNECT Request and TCP Connection Establishment to server
	 * @return requestPacket
	 * @throws IOException
	 */
	public byte[] connectRequest() throws IOException {
		byte[] requestPacket = new byte[8];
		clientInput = client.getInputStream();
		clientInput.read(requestPacket);
		ByteBuffer bb = ByteBuffer.wrap(requestPacket);

		version = bb.get();
		if (version != 4) {		// First byte of the packet represents the SOCKS protocol version.
			System.err.println("Connection error: while parsing request: Unsupported SOCKS protocol version"
					+ " (got " + version + ")");
			clientInput.close();
			client.close();
			System.err.println("Closing connection from " + client.getInetAddress().getHostAddress() + ":" + client.getPort());
			return null;
		}
		
		byte command = bb.get();		// Second byte of the packet represents the Command Code and should be 1 for CONNECT. 
		if (command != 1) {
			return null;
		}
		
		int destPort = bb.getShort();	 
		byte[] IPArr = new byte[4];
		for (int i = 0; i < 4; i++) {
			IPArr[i] = bb.get();
		}
		String IP = "";
		for (int i = 0; i < IPArr.length - 1; i++) {
			IP += (IPArr[i] & 0xff) + ".";
		}
		IP += (IPArr[3] & 0xff);
		byte b;
		while ((b = (byte) clientInput.read()) != 0) {
			// clean buffer
		}
		
		// BONUS PART - check the DSTIP in the request packet. If it represent address 0.0.0.x with nonzero x, 
		// the server must read in the domain name that the client sends in the packet.
		if (IPArr[0] == 0 && IPArr[1] == 0 && IPArr[2] == 0 && IPArr[3] != 0) {
			packet = new byte[4096];
			clientInput.read(packet);
			domainName = new String(packet);
			IP = domainName;
			clientOutput = client.getOutputStream();
			server = new Socket(IP, destPort);
			server.setSoTimeout(3000);
			serverInput = server.getInputStream();
			serverOutput = server.getOutputStream();
			replyCD = 90;
		} else {
			InetAddress inet = InetAddress.getByName(IP);
			clientOutput = client.getOutputStream();
			if (!inet.isReachable(3000)) {
				replyCD = 91;
			} else {
				server = new Socket(IP, destPort);
				server.setSoTimeout(3000);
				serverInput = server.getInputStream();
				serverOutput = server.getOutputStream();
				replyCD = 90;
			}
		}
		return requestPacket;
	}
	
	/**
	 * SOCKS CONNECT Reply
	 * @param requestPacket
	 * @throws IOException
	 */
	public void connectReply(byte[] requestPacket) {
		try {
			byte[] replyPacket = new byte[8];
			replyPacket[0] = 0;
			int clientPort = client.getPort();
			InetAddress clientAddress = client.getInetAddress();
			for (int i = 2; i < 8; i++) {
				replyPacket[i] = requestPacket[i];
			}
			
			replyPacket[1] = replyCD;
		
			if(replyCD == 90) {
				System.out.println("Successful connection from " + clientAddress.getHostAddress() + ":" + clientPort +  
						" to " + server.getRemoteSocketAddress().toString().split("/")[1]);
			}
			else if (replyCD == 91) {
				System.err.println("Connection error: while connecting to destination: connect timed out"); 
					
			}
			clientOutput.write(replyPacket);
		}
		catch(IOException e) {
			
		}
		
	}

	private void closeConnections() {
		try {
			if (replyCD == 90) {
				serverInput.close();
				serverOutput.close();

				System.out.println("Closing connection from " + client.getInetAddress().getHostAddress() + ":"
						+ client.getPort() + " to " + server.getRemoteSocketAddress().toString().split("/")[1]);
				server.close();

				clientInput.close();
				clientOutput.close();
				client.close();
			} else if (replyCD == 91) {
				clientInput.close();
				clientOutput.close();
				client.close();
				System.err.println("Closing connection from " + client.getInetAddress().getHostAddress() + ":"
						+ client.getPort());
			}

		} catch (IOException e) {
			System.err.println("close error" + e.getMessage());
		}
	}
}