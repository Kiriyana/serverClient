package serverClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class socketManager {
	public Socket soc = null;
	public DataInputStream input = null;
	public DataOutputStream output = null;
	String name = null;
	public boolean auth = false;
	
	public socketManager(Socket socket) throws IOException {
		soc = socket;
		input = new DataInputStream(soc.getInputStream());
		output = new DataOutputStream(soc.getOutputStream());
		name = soc.getLocalAddress().getHostName();
	}
	
	synchronized public DataInputStream getInput() {
		return input;
	}
	
	synchronized public DataOutputStream getOutput() {
		return output;
	}
	
	public void close() {
		try {
			input.close();
			output.close();
			soc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	synchronized public void setName(String val) {
		name = val;
	}
	
	synchronized public String getName() {
		return name;
	}
	
	synchronized public String ip() {
		return soc.getInetAddress().getHostAddress();
	}
	
	synchronized public String port() {
		return Integer.toString(soc.getPort());
	}
}