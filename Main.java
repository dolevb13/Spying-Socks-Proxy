import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		ServerSocket welcomeSocket;
		try {
			welcomeSocket = new ServerSocket(8080);
			ExecutorService executor = Executors.newFixedThreadPool(20);

			while (true) {
				Socket clientSocket = welcomeSocket.accept();		// Listens for a connection to be made to this socket and accepts it.
				Runnable worker = new ConnectionHandler(clientSocket);
				executor.execute(worker);
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}

