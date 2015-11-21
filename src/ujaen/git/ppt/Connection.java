package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

import ujaen.git.ppt.mail.Mailbox;
import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;

public class Connection implements Runnable, RFC5322
{

	public static final int S_NOCOMMAND = -1;
	public static final int S_HELO = 0;
	public static final int S_EHLO = 1;
	public static final int S_MAIL = 2;
	public static final int S_RCPT = 3;
	public static final int S_DATA = 4;
	public static final int S_RSET = 5;
	public static final int S_QUIT = 6;
	

	protected Socket mSocket;
	protected int mEstado = S_HELO;;
	private boolean mFin = false;
	//new variables added from here
	protected boolean isHELO = false;
	protected boolean isMAIL = false;
	protected boolean isRCPT = false;
	protected String mFrom = "", mTo = "";
	protected String mArguments = "";

	public Connection(Socket s)
	{
		mSocket = s;
		mEstado = 0;
		mFin = false;
	}

	@Override
	public void run()
	{

		String inputData = null;
		String outputData = "";
		

		if (mSocket != null)
		{
			try
			{
				// Inicialización de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(
						mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				// Envío del mensaje de bienvenida
				String response = RFC5321.getReply(RFC5321.R_220) + SP + RFC5321.MSG_WELCOME
						+ RFC5322.CRLF;
				output.write(response.getBytes());
				output.flush();

				while (!mFin && ((inputData = input.readLine()) != null))
				{
					
					System.out.println("Servidor [Recibido]> " + inputData);
					
					// Todo análisis del comando recibido
					SMTPMessage m = new SMTPMessage(inputData);
					mEstado = m.getCommandId();
					mArguments = m.getArguments();
					System.out.println(mEstado + " " + mArguments);
					
					// TODO: Máquina de estados del protocolo
					switch (mEstado)
					{
						case S_HELO:
							isHELO = true;
							break;
						case S_EHLO:
							isHELO = true;
							break;
						case S_MAIL:
							if(isHELO)
							{
								mFrom = mArguments;
								isMAIL = true;
							}
							break;
						case S_RCPT:
							if(isHELO && isMAIL)
							{
								//check if the user exists
								if(Mailbox.checkRecipient(mArguments))
								{
									isRCPT = true;
								}
								else
								{
									isRCPT = false;
								}
								System.out.println(isRCPT);
							}
							break;
						case S_DATA:
							break;
						case S_RSET:
							isMAIL = false;
							isRCPT = false;
							break;
						case S_QUIT:
							mFin = true;
							break;
						default:
							break;
					}

					// TODO montar la respuesta
					// El servidor responde con lo recibido
					switch (mEstado)
					{
						case S_NOCOMMAND:
							outputData = RFC5321.getError(RFC5321.E_500_SINTAXERROR) + SP +
							RFC5321.getErrorMsg(RFC5321.E_500_SINTAXERROR) + CRLF;
							break;
						//HELO response
						case S_HELO:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP +
							"Hello." + CRLF;
							break;
						//EHLO response
						case S_EHLO:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
							+ "Hello." + CRLF;
							break;
						case S_MAIL:
							if(!isHELO)
							{
								outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP 
								+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
							}
							else
							{
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
								+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							}
							break;
						case S_RCPT:
							if(!isHELO || !isMAIL)
							{
								outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP 
								+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
							}
							else if(isHELO && isMAIL && !isRCPT)
							{
								outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
								+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + CRLF;
							}
							else if(isHELO && isMAIL && isRCPT)
							{
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
								+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							}
							break;
						case S_RSET:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
							+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							break;
						//QUIT response
						case S_QUIT:
							outputData = RFC5321.getReply(RFC5321.R_221) + SP + 
							RFC5321.getReplyMsg(RFC5321.R_221) + SP + 
							RFC5321.MSG_BYE + CRLF;
							break;
					}
					
					//outputData = RFC5321.getReply(RFC5321.R_220) + SP + inputData + CRLF;
					output.write(outputData.getBytes());
					output.flush();
					
				}
				System.out.println("Servidor [Conexión finalizada]> "
						+ mSocket.getInetAddress().toString() + ":"
						+ mSocket.getPort());

				input.close();
				output.close();
				mSocket.close();
			} 
			catch (SocketException se) 
			{
				se.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}

		}

	}
}
