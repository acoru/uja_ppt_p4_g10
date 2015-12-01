package ujaen.git.ppt.smtp;


public class SMTPMessage implements RFC5322 {

	protected String mCommand = null;
	protected int mCommandId = RFC5321.C_NOCOMMAND;
	protected String mArguments = null;
	protected String[] mParameters = null;
	protected boolean mHasError = false;
	protected int mErrorCode = 0;

	/**
	 * The input string is processed to analyze the format of the message
	 * 
	 * @param data
	 */
	public SMTPMessage(String data)
	{

		if(data.length()>998)
		{
			mHasError=true;	
		}
		else
		{
			//mHasError = parseCommand(data);
			mHasError = false;
			/**
			 * try to split the received string by ":" for finding commands likes "RCPT TO" and "MAIL FROM"
			 * if fields size is 0, it means that there's no command like "RCPT TO", so it'll be split
			 * by " ", which means that the received commands is a "HELO", "EHLO", "DATA", "RSET" or "QUIT"
			 */
			String [] fields = data.split(":");
			//check if there is a "RCPT TO" or a "MAIL FROM" command
			if(data.length() < 4)
			{
				mCommandId = checkCommand(data);
				mArguments = null;
			}
			else if(fields.length == 2)
			{
				/**
				 * check if it is a correct command, if not, it will save "-1" in mCommandId
				 * if it is correct, it will save the associated command code
				 */
				//to filter the 4 char commands like HELO, if user insert text like HELO:HI
				if(fields[0].length() > 4)
				{
					mCommandId = checkCommand(fields[0]);
					mArguments = data.substring(fields[0].length() + 1, data.length());
				}
				else
				{
					mCommandId = RFC5321.C_NOCOMMAND;
					mArguments = null;
				}
			}
			/**
			 * if input string (message received) contains more than two ":" it is not a valid message
			 */
			else if(fields.length > 2)
			{
				mCommandId = checkCommand(fields[0]);
				mArguments = null;
			}
			else
			{
				mCommandId = checkCommand(data.substring(0, 4));
				if(data.length() > 4 && (mCommandId == RFC5321.C_HELO || mCommandId == RFC5321.C_EHLO))
				{
					/**
					 * if the 5º char is not a " " (space), it is not a valid command
					 */
					//if(data.substring(4,5).compareTo(" ") != 0)
					if(data.substring(4,5).equalsIgnoreCase(" ") == false)
					{
						mCommandId = RFC5321.C_NOCOMMAND;
						mArguments = null;
					}
					else
					{
						mArguments = data.substring(4, data.length());
					}
				}
				else if(data.length() > 4)
				{
					mCommandId = RFC5321.C_NOCOMMAND;
					mArguments = null;
				}
			}
		}	
	}

	/**
	 * 
	 * @param data
	 * @return true if there were errors
	 */
	/**
	protected boolean parseCommand(String data)
	{

		if (data.indexOf(":") > 0)
		{
			String[] commandParts = data.split(":");// Se busca los comandos con
													// varias palabras MAIL
													// FROM:
		}

		return false;
	}
	*/

	public String toString()
	{
		if (!mHasError)
		{
			String result = "";
			result = this.mCommand;
			if (this.mCommandId == RFC5321.C_MAIL
					|| this.mCommandId == RFC5321.C_RCPT)
				result = result + ":";
			if (this.mArguments != null)
				result = result + this.mArguments;
			if (this.mParameters != null)
				for (String s : this.mParameters)
					result = result + SP + s;

			result = result + CRLF;
			//opcional
			result=result+"id="+this.mCommandId;
			return result;
		}
		else
			return "Error";
	}

	/**
	 * 
	 * @param data
	 * @return The id of the SMTP command
	 */
	protected int checkCommand(String data)
	{
		int index = 0;

		this.mCommandId = RFC5321.C_NOCOMMAND;

		for (String c : RFC5321.SMTP_COMMANDS)
		{
			if (data.compareToIgnoreCase(c) == 0)
			{
				this.mCommandId = index;
			}
			index++;
		}

		if (mCommandId != RFC5321.C_NOCOMMAND)
			mCommand = RFC5321.getCommand(mCommandId);
		else
			mCommand = null;

		return this.mCommandId;
	}

	public String getCommand() {
		return mCommand;
	}

	public void setCommand(String mCommand) {
		this.mCommand = mCommand;
	}

	public int getCommandId() {
		return mCommandId;
	}

	public void setCommandId(int mCommandId) {
		this.mCommandId = mCommandId;
	}

	public String getArguments() {
		return mArguments;
	}

	public void setArguments(String mArguments) {
		this.mArguments = mArguments;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public void setParameters(String[] mParameters) {
		this.mParameters = mParameters;
	}

	public boolean hasError() {
		return mHasError;
	}

	
	public int getErrorCode() {
		return mErrorCode;
	}

	public void setErrorCode(int mErrorCode) {
		this.mErrorCode = mErrorCode;
	}

}
