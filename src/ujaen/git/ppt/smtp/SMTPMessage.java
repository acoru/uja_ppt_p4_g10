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
	public SMTPMessage(String data) {

		if(data.length()>998)
		{
			mHasError=true;
			
		}
		else
			mHasError = parseCommand(data);

	}

	/**
	 * 
	 * @param data
	 * @return true if there were errors
	 */
	protected boolean parseCommand(String data) {

		if (data.indexOf(":") > 0) {
			String[] commandParts = data.split(":");// Se busca los comandos con
													// varias palabras MAIL
													// FROM:
			if (commandParts.length != 2) {
				return true;
			}

			if (checkCommand(commandParts[0]) == RFC5321.C_NOCOMMAND) {
				this.mErrorCode = RFC5321.E_500_SINTAXERROR;
				return true;
			}
			commandParts[1] = commandParts[1].trim();
			String arguments[] = commandParts[1].split(SP);

			mArguments = arguments[0];

			if (arguments.length > 1) {
				mParameters = new String[arguments.length - 1];

				for (int i = 1; i < arguments.length; i++) {
					mParameters[i - 1] = arguments[i];
				}
			} else
				mParameters = null;

		} else {
			// Es un comando sin ":"
			String[] commandParts = data.split(SP);

			if (checkCommand(commandParts[0]) == RFC5321.C_NOCOMMAND) {
				this.mErrorCode = RFC5321.E_500_SINTAXERROR;
				return true;
			}
			if (commandParts.length >= 2)
				mArguments = commandParts[1];
			else
				mArguments = null;

			if (commandParts.length > 2) {
				mParameters = new String[commandParts.length - 2];

				for (int i = 2; i < commandParts.length; i++) {
					mParameters[i - 2] = commandParts[i];
				}
			} else
				mParameters = null;

		}

		return false;
	}

	public String toString() {
		if (!mHasError) {
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
		} else
			return "Error";
	}

	/**
	 * 
	 * @param data
	 * @return The id of the SMTP command
	 */
	protected int checkCommand(String data) {
		int index = 0;

		this.mCommandId = RFC5321.C_NOCOMMAND;

		for (String c : RFC5321.SMTP_COMMANDS) {
			if (data.compareToIgnoreCase(c) == 0)
				this.mCommandId = index;

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
