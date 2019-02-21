import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Proxy implements Runnable {

	Socket m_Server;
	Socket m_Client;
	InputStream m_ServerInput;
	OutputStream m_ServerOutput;
	InputStream m_ClientInput;
	OutputStream m_ClientOutput;
	byte[] packet;
	byte version;
	byte replyCD;
	String domainName;

	Proxy(Socket clientSocket) throws IOException {
		m_Client = clientSocket;
		m_Client.setSoTimeout(3000);
	}

	@Override
	public void run() {
		try {
			
			byte[] requestPacket = connectRequest();
			if (requestPacket != null) {
				connectReply(requestPacket);
			}
			if (replyCD == 90) {
				RunThread serverTransfer = new RunThread(m_Client, m_Server, m_ClientInput, m_ServerOutput);
				serverTransfer.start();
				RunThread clientTransfer = new RunThread(m_Server, m_Client, m_ServerInput, m_ClientOutput);
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
		m_ClientInput = m_Client.getInputStream();
		m_ClientInput.read(requestPacket);
		ByteBuffer bb = ByteBuffer.wrap(requestPacket);

		version = bb.get();
		if (version != 4) {
			System.err.println("Connection error: while parsing request: Unsupported SOCKS protocol version"
					+ " (got " + version + ")");
			m_ClientInput.close();
			m_Client.close();
			System.err.println("Closing connection from " + m_Client.getInetAddress().getHostAddress() + ":" + m_Client.getPort());
			return null;
		}
		byte CD = bb.get();
		if (CD != 1) {
			return null;
		}
		int DSTPORT = bb.getShort();
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
		while ((b = (byte) m_ClientInput.read()) != 0) {
			// clean buffer
		}
		if (IPArr[0] == 0 && IPArr[1] == 0 && IPArr[2] == 0 && IPArr[3] != 0) {
			packet = new byte[4096];
			m_ClientInput.read(packet);
			domainName = new String(packet);
			IP = domainName;
			m_ClientOutput = m_Client.getOutputStream();
			m_Server = new Socket(IP, DSTPORT);
			m_Server.setSoTimeout(3000);
			m_ServerInput = m_Server.getInputStream();
			m_ServerOutput = m_Server.getOutputStream();
			replyCD = 90;
		} else {
			InetAddress inet = InetAddress.getByName(IP);
			m_ClientOutput = m_Client.getOutputStream();
			if (!inet.isReachable(3000)) {
				replyCD = 91;
			} 
			else {
				m_Server = new Socket(IP, DSTPORT);
				m_Server.setSoTimeout(3000);
				m_ServerInput = m_Server.getInputStream();
				m_ServerOutput = m_Server.getOutputStream();
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
			int clientPort = m_Client.getPort();
			InetAddress clientAddress = m_Client.getInetAddress();
			for (int i = 2; i < 8; i++) {
				replyPacket[i] = requestPacket[i];
			}
			
			replyPacket[1] = replyCD;
		
			if(replyCD == 90) {
				System.out.println("Successful connection from " + clientAddress.getHostAddress() + ":" + clientPort +  
						" to " + m_Server.getRemoteSocketAddress().toString().split("/")[1]);
			}
			else if (replyCD == 91) {
				System.err.println("Connection error: while connecting to destination: connect timed out"); 
					
			}
			m_ClientOutput.write(replyPacket);
		}
		catch(IOException e) {
			
		}
		
	}

	private void closeConnections() {
		try {
			if (replyCD == 90) {
				m_ServerInput.close();
				m_ServerOutput.close();

				System.out.println("Closing connection from " + m_Client.getInetAddress().getHostAddress() + ":"
						+ m_Client.getPort() + " to " + m_Server.getRemoteSocketAddress().toString().split("/")[1]);
				m_Server.close();

				m_ClientInput.close();
				m_ClientOutput.close();
				m_Client.close();
			} else if (replyCD == 91) {
				m_ClientInput.close();
				m_ClientOutput.close();
				m_Client.close();
				System.err.println("Closing connection from " + m_Client.getInetAddress().getHostAddress() + ":"
						+ m_Client.getPort());
			}

		} catch (IOException e) {
			System.err.println("close error" + e.getMessage());
		}
	}
}