package dev.zeith.fragfile.errors;

import java.io.IOException;

public class MalformedStreamException
		extends IOException
{
	public MalformedStreamException(String message)
	{
		super(message);
	}
	
	public MalformedStreamException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public MalformedStreamException(Throwable cause)
	{
		super(cause);
	}
}