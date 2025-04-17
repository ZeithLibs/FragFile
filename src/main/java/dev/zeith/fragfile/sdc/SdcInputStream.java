package dev.zeith.fragfile.sdc;

import dev.zeith.fragfile.errors.MalformedStreamException;

import java.io.*;
import java.util.Objects;
import java.util.zip.*;

public class SdcInputStream
		extends InputStream
{
	private final DataInputStream input;
	
	boolean eof;
	SdcEntry entry;
	long lengthLeft;
	
	public SdcInputStream(InputStream input)
			throws IOException
	{
		byte[] sdcHeader = new byte[Sdc.HEADER.length];
		for(int i = 0; i < sdcHeader.length; i++)
		{
			int j = input.read();
			if(j == -1) throw new EOFException();
			sdcHeader[i] = (byte) j;
		}
		if(!Sdc.validHeader(sdcHeader)) throw new MalformedStreamException("Invalid SDC header!");
		if(input.read() != Sdc.VERSION) throw new MalformedStreamException("Invalid SDC version!");
		boolean compress = Sdc.COMPRESS[input.read() - 1];
		this.input = new DataInputStream(compress ? new GZIPInputStream(input, 1024) : input);
	}
	
	public SdcEntry readNextEntry()
			throws IOException
	{
		if(eof) return null;
		if(entry != null) closeEntry();
		String utf = input.readUTF();
		if(utf.isEmpty())
		{
			eof = true;
			return null;
		}
		if(!utf.startsWith(Sdc.ENTRY_PREFIX)) throw new MalformedStreamException("SDC entry does not start with correct prefix.");
		entry = new SdcEntry();
		entry.name = utf.substring(Sdc.ENTRY_PREFIX.length());
		entry.lastModifiedTime = Sdc.unixTimeToFileTime(input.readLong());
		entry.creationTime = Sdc.unixTimeToFileTime(input.readLong());
		entry.crc = input.readLong();
		lengthLeft = entry.size = input.readLong();
		return entry;
	}
	
	public void closeEntry()
			throws IOException
	{
		if(entry == null) return;
		if(input.skip(lengthLeft) != lengthLeft) throw new EOFException();
		entry = null;
	}
	
	public SdcEntry getCurrentEntry()
	{
		return entry;
	}
	
	@Override
	public int read()
			throws IOException
	{
		Objects.requireNonNull(entry, "active entry");
		
		if(lengthLeft > 0L)
		{
			int ch = input.read();
			if(ch < 0) throw new EOFException();
			--lengthLeft;
			return ch;
		}
		
		return -1;
	}
	
	@Override
	public int read(byte[] b)
			throws IOException
	{
		Objects.requireNonNull(entry, "active entry");
		
		if(lengthLeft > 0L)
		{
			int ch = input.read(b, 0, (int) Math.min(lengthLeft, b.length));
			if(ch < 0) throw new EOFException();
			lengthLeft -= ch;
			return ch;
		}
		
		return 0;
	}
	
	@Override
	public int read(byte[] b, int off, int len)
			throws IOException
	{
		Objects.requireNonNull(entry, "active entry");
		
		if(lengthLeft > 0L)
		{
			int ch = input.read(b, off, (int) Math.min(lengthLeft, len));
			if(ch < 0) throw new EOFException();
			lengthLeft -= ch;
			return ch;
		}
		
		return 0;
	}
	
	@Override
	public void close()
			throws IOException
	{
		input.close();
	}
	
	public void writeTo(OutputStream out)
			throws IOException
	{
		// Perform CRC check if we haven't read any bytes in the entry.
		if(lengthLeft == entry.size)
		{
			CRC32 crc32 = new CRC32();
			
			byte[] buf = new byte[2048];
			int r;
			while((r = read(buf)) > 0)
			{
				out.write(buf, 0, r);
				crc32.update(buf, 0, r);
			}
			
			if(crc32.getValue() != entry.crc)
				throw new MalformedStreamException("CRC32 malformed for " + entry);
			
			return;
		}
		
		byte[] buf = new byte[2048];
		int r;
		while((r = read(buf)) > 0)
			out.write(buf, 0, r);
	}
}