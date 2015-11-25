package ujaen.git.ppt;

import java.net.Socket;
/**
 * 
 * @author Adrián Cotes Ruiz
 * simple algorithm for taking the IP from a Socket class object
 */
public class ObtainIP
{
	Socket mSocket = null;
	String IP = null;
	Boolean fExec = true;
	
	public ObtainIP(Socket rSocket)
	{
		mSocket = rSocket;
	}
	
	/**
	 * it will identify if it is a IPv4 or a IPv6 and will split it for just taking the IP
	 * @return it will return the IP, without port
	 */
	public String getIP()
	{
		//take the IP
		String auxIP = mSocket.getRemoteSocketAddress().toString();
		// remove the slash "/" character from the begining
		String auxIP2 = auxIP.substring(1, auxIP.length());
		
		//splitting the auxIP2 in multiples strings
		String[] parts = auxIP2.split(":");
		//analyzing the number of parts, if it is a IPv6 IP direction type, it will have more than 2 parts
		//if more than 2 parts, it will remove the port number with a simple loop
		if(parts.length > 2)
		{
			for(int i = 0; i < parts.length - 1; i++)
			{
				if(fExec)
				{
					fExec = false;
					IP = parts[i];
				}
				else
				{
					IP += ":" + parts[i];
				}
			}
			fExec = true;
		}
		else
		{
			IP = parts[0];
		}
		
		return IP;
	}
}