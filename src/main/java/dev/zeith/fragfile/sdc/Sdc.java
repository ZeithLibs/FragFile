package dev.zeith.fragfile.sdc;

import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

class Sdc
{
	static final byte[] HEADER = "%SDC=".getBytes(StandardCharsets.UTF_8);
	static final int VERSION = 1;
	static final String ENTRY_PREFIX = "\r\n\r\n";
	static final boolean[] COMPRESS = {false, true};
	
	static boolean validHeader(byte[] in)
	{
		for(int i = 0; i < HEADER.length; i++)
			if(in[i] != HEADER[i])
				return false;
		return true;
	}
	
	public static FileTime unixTimeToFileTime(long utime)
	{
		return FileTime.from(utime, TimeUnit.SECONDS);
	}
	
	public static long fileTimeToUnixTime(FileTime ftime)
	{
		return ftime.to(TimeUnit.SECONDS);
	}
}