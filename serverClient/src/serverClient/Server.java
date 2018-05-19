package serverClient;

import javax.crypto.spec.RC2ParameterSpec;
import java.io.*;
import java.io.File;
import java.net.*;
import java.nio.file.*;
import java.sql.Struct;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.lang.*;



public class Server {
    int portNumber = 15872;
    ServerSocket serverSoc = null;
    ArrayList<socketManager> clients = null;
    ArrayList<Message> messageQueue = null;
    
	public Server (int port) {
		if (port > 2048) {
			portNumber = port;
		} else {
			System.err.println("Port number too low, defaulting to 15882");
		}
        
        try{
            //Setup the socket for communication 
            serverSoc = new ServerSocket(portNumber);
            clients = new ArrayList<socketManager>();
            
            while (true){
                
                //accept incoming communication
                System.out.println("Waiting for client");
                Socket soc = serverSoc.accept();
                
                socketManager temp = new socketManager(soc);
                clients.add(temp);
                
                //create a new thread for the connection and start it.
                ServerConnetionHandler sch = new ServerConnetionHandler(clients, temp);
                Thread schThread = new Thread(sch);
                schThread.start();
            }
            
        }
        catch (Exception except){
            //Exception thrown (except) when something went wrong, pushing message to the console
            System.err.println("Error --> " + except.getMessage());
        }
    }   
	
    //Main Method:- called when running the class file.
    public static void main(String[] args){
    	Scanner scan = new Scanner(System.in);
    	int port = scan.nextInt();
    	Server server = new Server(port);
    }
}
    
class ServerConnetionHandler implements Runnable {
	socketManager selfs = null;
	ArrayList<socketManager> clients = null;
	boolean verbose = false;
	State currentState = State.CONNECTED;
	State previousState = State.NONE;
	String from = "";
	String rcpt = "";
	String subject = "";
	String userName = "";
	String userInput = "";
	String messages = "";


	public ServerConnetionHandler(ArrayList<socketManager> l, socketManager inSoc) {
		selfs = inSoc;
		clients = l;
	}

	public ServerConnetionHandler(ArrayList<socketManager> l, socketManager inSoc, boolean v) {
		selfs = inSoc;
		clients = l;
		verbose = v;
	}

	public void run() {
		try {
			//Catch the incoming data in a data stream, read a line and output it to the console

			System.out.println("Client Connected");
			SMTP smtp = new SMTP();
			clientSend(selfs, smtp.msg(220, null));
			clientSend(selfs, "Enter ' Create', 'Inbox' or 'Trash' to view respective ");

			while (true) {
				//Print out message
				String message = selfs.input.readUTF();

				if (verbose) {
					System.out.println("--> " + message);
				}

				parse(message, selfs);


				//for (socketManager sm : clients) {
				//	sm.output.writeUTF(selfs.soc.getInetAddress().getHostAddress() + ":" + selfs.soc.getPort() + " wrote: " + message);
				//}

			}
			//close the stream once we are done with it
		} catch (Exception except) {
			//Exception thrown (except) when something went wrong, pushing message to the console
			System.out.println("Error in ServerHandler--> " + except.getMessage());
		}
	}

	private boolean validateDomain(String domain) {
		// validate as valid domain	Makes sure that the sting entered matches a valid domain string 	abc.co, abc.co.uk, 123.bob
		String pattern = "^(([a-zA-Z]{1})|([a-zA-Z]{1}[a-zA-Z]{1})|([a-zA-Z]{1}[0-9]{1})|([0-9]{1}[a-zA-Z]{1})|([a-zA-Z0-9][a-zA-Z0-9-_]{1,61}[a-zA-Z0-9]))\\.([a-zA-Z]{2,6}|[a-zA-Z0-9-]{2,30}\\.[a-zA-Z]{2,3})$";
		return domain.matches(pattern);
	}

	private boolean validateEmail(String email) {
		// validate as valid email		Makes sure that the sting entered matches a valid email string test@email.com
		String pattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
		return email.matches(pattern);
	}

	private void clientSend(socketManager sms, String message) throws IOException {
		sms.output.writeUTF(message);
		sms.output.flush();
	}

	private void parse(String message, socketManager sm) throws IOException {
		String[] components = message.split(" ");

		FileReader fReader = new FileReader("misc\\users.txt");
		BufferedReader bReader = new BufferedReader(fReader);
		String username = bReader.readLine();
		String password = bReader.readLine();
		bReader.close();

		SMTP smtp = new SMTP();

			if (components.length > 0) {
				if (components[0].toUpperCase().equals("INBOX")) {
					if (currentState == State.CONNECTED) {
						{

							File file = new File("emails\\");
							File[] files = file.listFiles();
								for (File f : files) {
									clientSend(selfs, f.getName());
								}

								if (files.length == 0) {
									clientSend(selfs, "No email");
								}


							previousState = currentState;
							currentState = State.R_INBOX;

							clientSend(selfs, smtp.msg(252, null));
						}

					} else {
						clientSend(selfs, smtp.msg(503, new String[]{"INBOX"}));
					}

				}if (components[0].toUpperCase().equals("MAIL")) {

					if (currentState == State.R_INBOX) {
						boolean validEmail = true;
						if (components.length >= 1) {
							validEmail = validateEmail(components[1]);
							from = components[1];
							File file = new File("emails\\" + from);
							File[] files = file.listFiles();
							for (File f : files) {
								clientSend(selfs, f.getName());
							}

							if (files.length == 0) {
								clientSend(selfs, "244 No email");
							}
							
						}
						if (!validEmail) {
							clientSend(selfs, smtp.msg(553, new String[]{components[1]}));
						} else {
							
							previousState = currentState;
							currentState = State.R_EMAIL;

							from = components[1];
							clientSend(selfs, smtp.msg(250, null));
						}
					}

				}if (components[0].toUpperCase().equals("EMAIL")) {
					if (currentState == State.R_EMAIL) {


						subject = components[1];
						clientSend(selfs, smtp.msg(251, null));
						previousState = currentState;
						currentState = State.R_OPEN;
					}

				}if (components[0].toUpperCase().equals("OPEN")) {
					if (currentState == State.R_OPEN) {

							BufferedReader bufferedReader = null;
							String line;

							try{
								bufferedReader = new BufferedReader(new FileReader("emails\\" + from + "\\" + subject + "\\" + subject));
							} catch (FileNotFoundException fnfex){
								clientSend(selfs, smtp.msg(222, null));
							}

							while ((line = bufferedReader.readLine()) != null) {
								clientSend(selfs, line);
							}

							clientSend(selfs, smtp.msg(250, null));
							previousState = currentState;
							currentState = State.CONNECTED;
						}
				}if (components[0].toUpperCase().equals("DELETE")){
					if (currentState == State.R_OPEN) {
						
						File destinationFile = new File("misc\\deleted\\" + from + "\\" + subject);
					    File sourceFile = new File("emails\\" + from + "\\" + subject);

					    if (!destinationFile.exists()){
					    	
					        destinationFile.mkdirs();
					    }

					    if (sourceFile.exists() && sourceFile.isDirectory()){
					    	
					        File[] listOfFiles = sourceFile.listFiles();

					        if (listOfFiles != null){
					        	
					            for (File child : listOfFiles ){
					            	
					                child.renameTo(new File(destinationFile + "\\" + child.getName()));
					            }

					            sourceFile.delete();
								previousState = currentState;
								currentState = State.CONNECTED;
					            clientSend(selfs, "Email deleted!");
					        }
					    }else{
							clientSend(selfs, smtp.msg(222, null));
					    }

					}
				}if (components[0].toUpperCase().equals("TRASH")) {
					if (currentState == State.CONNECTED) {
						File file = new File("misc\\deleted");
						File[] files = file.listFiles();
							for (File f : files) {
								clientSend(selfs, f.getName());
							}
	
							if (files.length == 0) {
								clientSend(selfs, "No email");
							}

						clientSend(selfs, smtp.msg(250, null));
						previousState = currentState;
						currentState = State.R_TRASH;
					}

				}if (components[0].toUpperCase().equals("MAIL")) {

					if (currentState == State.R_TRASH) {
						boolean validEmail = true;
						if (components.length >= 1) {
							validEmail = validateEmail(components[1]);
							from = components[1];
							File file = new File("misc\\deleted\\" + from);
							File[] files = file.listFiles();
							for (File f : files) {
								clientSend(selfs, f.getName());
							}

							if (files.length == 0) {
								clientSend(selfs, "No email");
							}
							
						}
						if (!validEmail) {
							clientSend(selfs, smtp.msg(553, new String[]{components[1]}));
						} else {
							
							previousState = currentState;
							currentState = State.R_UNDELETE;

							from = components[1];
							clientSend(selfs, smtp.msg(250, null));
							clientSend(selfs, "Select email");
						}
					}

				}if (components[0].toUpperCase().equals("RECOVER")){
					if (currentState == State.R_UNDELETE) {
						subject = components[1];

						File sourceFile = new File("misc\\deleted\\" + from + "\\" + subject);
					    File destinationFile = new File("emails\\" + from + "\\" + subject);

					    if (!destinationFile.exists()){
					    	
					        destinationFile.mkdirs();
					    }

					    if (sourceFile.exists() && sourceFile.isDirectory()){
					    	
					        File[] listOfFiles = sourceFile.listFiles();

					        if (listOfFiles != null){
					        	
					            for (File child : listOfFiles ){
					            	
					                child.renameTo(new File(destinationFile + "\\" + child.getName()));
					            }

					            sourceFile.delete();
					            previousState = currentState;
					            currentState = State.CONNECTED;
					            clientSend(selfs, "Email recovered!");
					            clientSend(selfs, smtp.msg(250, null));
					        }
					    }else{
							clientSend(selfs, smtp.msg(222, null));
					    }

					}
				}if (components[0].toUpperCase().equals("HELP")){
					clientSend(selfs, "No help currently avaliable");
					clientSend(selfs, "Please try again later");
					clientSend(selfs, "Returning back to previous state");
					previousState = currentState;
					currentState = previousState;
				}if (components[0].toUpperCase().equals("CREATE")){
					if (currentState == State.CONNECTED){

						previousState = currentState;
						currentState = State.R_CREATE ;
						clientSend(selfs, smtp.msg(250, null));
						clientSend(selfs, "Who would you like to message?");
					}
				}if (components[0].toUpperCase().equals("RCPT")){
					if (currentState == State.R_CREATE) {
						boolean validEmail = true;
						if (components.length >= 1) {						// if a domain exists
							validEmail = validateEmail(components[1]);
						}
						if (!validEmail) {
							clientSend(selfs, smtp.msg(250, new String[] {components[1]}));	//
						} else {
							previousState = currentState;
							currentState = State.R_RCPT;

							rcpt = components[1];
							clientSend(selfs, smtp.msg(250, null));
							clientSend(selfs, "Enter a subject");

						}
					}
				}if (components[0].toUpperCase().equals("SUBJECT")){
					if (currentState == State.R_RCPT){
						subject = components[1];

						previousState = currentState;
						currentState = State.R_DATA;
						clientSend(selfs, smtp.msg(250, null));

					}
				}if (components[0].toUpperCase().equals("DATA")) {
					if (currentState == State.R_DATA) {
						clientSend(selfs, smtp.msg(354, null));
						boolean fullstop = false;
						messages = "";
						while (fullstop == false) {
							String userInput;
							userInput = sm.input.readUTF();
							messages += "\n" + userInput;
							if (userInput.equals(".")) {
								fullstop = true;
							}

							clientSend(selfs, smtp.msg(250, null));

							Date date = new Date();
							long time = date.getTime();
							Timestamp timeSent = new Timestamp(time);
							clientSend(selfs, "Date: " + timeSent);
							//clientSend(selfs, "From: " + from);
							clientSend(selfs, "To: " + rcpt);
							clientSend(selfs, "Subject: " + subject);
							clientSend(selfs, messages);

							File file = new File("emails\\" + rcpt);
							if (!file.exists()) {
								if (file.mkdir()) {
									System.out.println("Directory is created!");
								} else {
									System.out.println("Failed to create directory!");
								}
							}

							File files = new File("emails\\" + rcpt);
							if (!files.exists()) {
								if (files.mkdirs()) {
									System.out.println("Multiple directories are created!");
								} else {
									System.out.println("Failed to create multiple directories!");
								}
							}
							File file2 = new File("emails\\" + rcpt + "\\" + subject + ".txt");
							if (!file2.exists()) {
								if (file2.mkdir()) {
									System.out.println("Directory is created!");
								} else {
									System.out.println("Failed to create directory!");
								}
							}

							File files2 = new File("emails\\" + rcpt + "\\" + subject + ".txt");
							if (!files2.exists()) {
								if (files2.mkdirs()) {
									System.out.println("Multiple directories are created!");
								} else {
									System.out.println("Failed to create multiple directories!");
								}
							}

							FileWriter csWriter = new FileWriter("emails\\" + rcpt + "\\" + subject + ".txt" + "\\" + subject + ".txt" , true);
							BufferedWriter csBufferWriter = new BufferedWriter(csWriter);

							csBufferWriter.newLine();
							csBufferWriter.write("Date: " + timeSent);
							csBufferWriter.newLine();
							//csBufferWriter.write("From: " + from);
							//csBufferWriter.newLine();
							csBufferWriter.write("To: " + rcpt);
							csBufferWriter.newLine();
							csBufferWriter.write("Subject: " + subject);
							csBufferWriter.newLine();
							csBufferWriter.write(messages);
							//csBufferWriter.newLine();

							csWriter.close();
							csBufferWriter.close();

						}
					}
				}
			}
		}
	}




