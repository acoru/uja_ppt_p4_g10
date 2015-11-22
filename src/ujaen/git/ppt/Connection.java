package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.FileReader;

import ujaen.git.ppt.mail.Mail;
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
	Mail mail = null;
	protected boolean isHELO = false;
	protected boolean isMAIL = false;
	protected boolean isRCPT = false;
	protected boolean fDataExec = false;
	protected boolean mailSend = false;
	protected String mFrom = "", mTo = "";
	protected String mArguments = "";
	protected String hostname = "";
	protected String heloArgument = "";
	protected String sMessID;
	protected String strDate;
	protected int dID = 0;

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
		//for taking the date
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		Date now = new Date();
		
		if (mSocket != null)
		{
			try
			{
				//unique MESSAGE-ID
				File fID = new File("ID.txt");
				//check if file exists, if not, it will create the file
				if(!fID.exists() && !fID.isFile())
				{
					//fID.createNewFile();
					PrintWriter writer = new PrintWriter(fID, "UTF-8");
					writer.println("0");
					writer.close();
				}
				
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
					if(mEstado != S_DATA && m.getCommandId() == S_DATA)
					{
						fDataExec = true;
					}
					/**
					 * if mEstad != S_DATA, means that the user is writing the mail content
					 */
					if(mEstado != S_DATA)
					{
						mEstado = m.getCommandId();
						mArguments = m.getArguments();
					}
					//System.out.println(mEstado + " " + mArguments);
					
					// TODO: Máquina de estados del protocolo
					switch (mEstado)
					{
						case S_HELO:
							heloArgument = mArguments;
							isHELO = true;
							break;
						case S_EHLO:
							heloArgument = mArguments;
							isHELO = true;
							break;
						/**
						 * for specifying the MAIL FROM user.
						 * It will only allow to the user to specify the MAIL FROM if a HELO message
						 * has been already sent to the SMTP server
						 */
						case S_MAIL:
							if(isHELO)
							{
								mFrom = mArguments;
								isMAIL = true;
							}
							break;
						/**
						 * for specifying the RCPT TO user (destination of the mail)
						 * It will only allow to the user to specify the RCPT TO if a HELO message and
						 * a MAIL FROM has been already sent to the server
						 * Note that, if the user that has been specified as RCPT TO does not exists on the
						 * local server it will send an error to the client
						 */
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
								//System.out.println(isRCPT);
							}
							break;
						/**
						 * for writing and sending mails
						 * it will check if a HELO message has been already send by the user, and if a MAIL FROM and
						 * RCPT TO has been already set by the user
						 */
						case S_DATA:
							if(isHELO && isMAIL && isRCPT)
							{
								//inputData += CRLF;
								if(fDataExec)
								{
									//read the ID number inside the file and close it
									BufferedReader brID = new BufferedReader(new FileReader(fID));
									dID = Integer.parseInt(brID.readLine());
									brID.close();
									dID++;
									
									//write the new number inside the file (overwrite) and close it
									PrintWriter writer = new PrintWriter(fID, "UTF-8");
									writer.println(dID);
									writer.close();
									
									//get the host name
									InetAddress addr;
								    addr = InetAddress.getLocalHost();
								    hostname = addr.getHostName();
								    
								    //create the MESSAGE-ID
								    sMessID = "<" + dID + "@" + hostname + ">";
								    
								    //taking date
								    strDate = sdf.format(now);
								    //System.out.println(strDate);
								    mail = new Mail();
								    
								    //adding headers to the mail
								    mail.addHeader("Return-Path", mFrom);
								    mail.addHeader("Received", heloArgument);
								    mail.addHeader("host", hostname);
								    mail.addHeader("date", strDate);
								    mail.addHeader("Message-ID", sMessID);

								}
								else
								{
									mail.addMailLine(inputData);
									inputData += CRLF;
								}
								//fDataExec = false;
								if(inputData.compareTo(ENDMSG) == 0)
								{
									//fDataExec = true;
									mailSend = true;
									isMAIL = false;
									isRCPT = false;
									System.out.println("Mail: \r\n" + mail.getMail());
								}
							}
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
						//if message received is not a valid command, it will return an error to the client
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
						//MAIL FROM response
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
						//RCPT TO response
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
						case S_DATA:
							if(isHELO && isMAIL && isRCPT && fDataExec)
							{
								outputData = RFC5321.getReply(RFC5321.R_354) + SP
								+ RFC5321.getReplyMsg(RFC5321.R_354) + CRLF;
							}
							else if(isHELO && !isMAIL && !isRCPT && mailSend)
							{
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
								+ "Queued." + CRLF;
								//changing the state for allowing user to continue with other actions
								mEstado = S_NOCOMMAND;
							}
							break;
						// RSET response
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
					if(!(mEstado == S_DATA && !mailSend) || (mEstado == S_DATA && fDataExec))
					{
						if(fDataExec)
						{
							fDataExec = false;
						}
						output.write(outputData.getBytes());
						output.flush();	
					}
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
