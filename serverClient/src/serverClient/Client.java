package serverClient;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;


public class Client {

    public static void main(String[] args) throws IOException {

        boolean authentication;
        //int portNumber = getPortNumber();
        //String serverIP = getServerIP();
        int portNumber = 12345;
        String serverIP = "localhost";
        do {
            authentication = getUser();
            if (authentication == false) {
                System.out.print("Invalid username or password.\n");
            }
        } while (authentication == false);
        setUpConnection(portNumber, serverIP);
    }


    public static boolean getUser() throws IOException {
        String usernameCheck = "";
        String inputUsername = "";
        String inputPassword = "";
        Scanner userNameScanner;
        String[] components;

        System.out.print("Enter username: ");
        userNameScanner = new Scanner(System.in);
        inputUsername = userNameScanner.nextLine();

        System.out.print("Enter password: ");
        inputPassword = userNameScanner.nextLine();

        BufferedReader reader;
        reader = new BufferedReader(new FileReader("misc\\users.txt"));
        usernameCheck = reader.readLine();

        while (usernameCheck != null) {
            components = usernameCheck.split(":");

            if (inputUsername.equals(components[0]) && inputPassword.equals(components[1])) {
                System.out.print("Welcome.\n");
                reader.close();
                return true;
            }

            usernameCheck = reader.readLine();
        }

        reader.close();
        return false;
    }


    public static void setUpConnection(int portNumber, String serverIP) {
        try {
            // Create a new socket for communication
            Socket socket = new Socket(serverIP, portNumber);

            // create new instance of the client reader thread, initialise it and start it running
            ClientReader clientRead = new ClientReader(socket);
            Thread clientReadThread = new Thread(clientRead);
            clientReadThread.start();

            // create new instance of the client writer thread, initialise it and start it running
            ClientWriter clientWrite = new ClientWriter(socket);
            Thread clientWriteThread = new Thread(clientWrite);
            clientWriteThread.start();

        } catch (Exception except) {
            System.out.println("Error in client");
        }
    }
}

//This thread is responcible for writing messages
class ClientWriter implements Runnable
{
    Socket cwSocket = null;
    
    public ClientWriter (Socket outputSoc){
        cwSocket = outputSoc;
    }
    
    public void run(){
    	Random rand = new Random();
    	int start = rand.nextInt(cwSocket.getLocalPort());
        try{
            //Create the outputstream to send data through
            DataOutputStream dataOut = new DataOutputStream(cwSocket.getOutputStream());
            
            //System.out.println("Client writer running");
            Scanner scan = new Scanner(System.in);
            
            //Write message to output stream and send through socket
            while (true) {
            	String temp = scan.nextLine();
            dataOut.writeUTF(temp);
            dataOut.flush();
            }
            
        }
        catch (Exception except){
            //Exception thrown (except) when something went wrong, pushing message to the console
            System.out.println("Error in Writer--> " + except.getMessage());
        }
    }
}


//This thread is responcible for writing messages
class ClientReader implements Runnable
{
  Socket cwSocket = null;
  
  public ClientReader (Socket inputSoc){
      cwSocket = inputSoc;
  }
  
  public void run(){
      try{
          //Create the outputstream to send data through
          DataInputStream dataOut = new DataInputStream(cwSocket.getInputStream());

          System.out.println("Client writer running");

          while (true) {

          //Write message to output stream and send through socket
        	  String msg = dataOut.readUTF();
        	  String[] parts = msg.split(":");
        	  if (parts[0].toUpperCase().equals("CLIENTSEND")) {
        		  // print nothing
        	  } else
        	  {

                  System.out.println(msg);

        	  }
          // Split string on :
          // if first split is CLIENTSEND ignore
          // else print message
          }
          //close the stream once we are done with it
      }
      catch (Exception except){
          //Exception thrown (except) when something went wrong, pushing message to the console
          System.out.println("Error in Writer--> " + except.getMessage());
      }

  }
}