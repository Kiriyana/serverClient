package serverClient;

public class SMTP {
	public SMTP () {
		
	}
	
	public String msg (int num, String [] arg) {
		String Response = "";
		
		switch (num) {
			case 220:
				Response = num + " Service Ready";
				break;
			case 250:
				Response = num + " OK";
				break;
			case 222:
				Response = num + " No such email";
				break;
			case 252:
				Response = num + " who would you like to view emails from?";
				break;
			case 251:
				Response = num + " do you want to open or delete email?";
				break;
			case 354:
				Response = num + " please enter your message, press enter for a new line and press . to end";
				break;
			case 501:
				Response = num + " Syntax error in parameters or arguments";
				break;
			default:
				if (arg.length > 0) {
					Response = "500 Syntax error, command not recognised: " + arg[0];
				} else {
					Response = "500 Syntax error, command not recognised: ";
				}
				break;
		}
		
		return Response;
	}	
}
