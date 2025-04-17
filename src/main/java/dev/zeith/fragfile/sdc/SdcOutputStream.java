package dev.zeith.fragfile.sdc;

import dev.zeith.fragfile.errors.MalformedStreamException;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.zip.*;

public class SdcOutputStream
		extends OutputStream
{
	private final DataOutputStream output;
	
	SdcEntry entry;
	long allowedBytes;
	
	public SdcOutputStream(OutputStream output, boolean compress)
			throws IOException
	{
		output.write(Sdc.HEADER);
		output.write(Sdc.VERSION);
		output.write(compress ? 2 : 1);
		this.output = new DataOutputStream(compress ? new GZIPOutputStream(output, 1024) : output);
	}
	
	public void putNextEntry(SdcEntry entry)
			throws IOException
	{
		Objects.requireNonNull(entry, "entry");
		if(this.entry != null) closeEntry();
		output.writeUTF(Sdc.ENTRY_PREFIX + entry.name);
		output.writeLong(Sdc.fileTimeToUnixTime(entry.lastModifiedTime));
		output.writeLong(Sdc.fileTimeToUnixTime(entry.creationTime));
		output.writeLong(entry.crc);
		output.writeLong(entry.size);
		this.allowedBytes = entry.size;
		this.entry = entry;
	}
	
	public void closeEntry()
			throws IOException
	{
		if(entry == null) return;
		entry = null;
		if(allowedBytes != 0L) throw new MalformedStreamException("entry closed too early");
	}
	
	@Override
	public void write(int b)
			throws IOException
	{
		Objects.requireNonNull(entry, "active entry");
		if(allowedBytes < 0L) throw new EOFException("entry limit reached");
		--allowedBytes;
		output.write(b);
		if(allowedBytes == 0L) closeEntry();
	}
	
	@Override
	public void write(byte[] b)
			throws IOException
	{
		Objects.requireNonNull(entry, "active entry");
		if(allowedBytes < b.length) throw new EOFException("entry limit reached");
		allowedBytes -= b.length;
		output.write(b);
		if(allowedBytes == 0L) closeEntry();
	}
	
	@Override
	public void write(byte[] b, int off, int len)
			throws IOException
	{
		Objects.requireNonNull(entry, "active entry");
		if(allowedBytes < len) throw new EOFException("entry limit reached");
		output.write(b, off, len);
		allowedBytes -= len;
		if(allowedBytes == 0L) closeEntry();
	}
	
	@Override
	public void close()
			throws IOException
	{
		closeEntry();
		output.writeUTF("");
		output.close();
	}
	
	public long fromFile(Path root, Path path)
			throws IOException
	{
		putNextEntry(SdcEntry.createHeader(root, path));
		return copyFrom(Files.newInputStream(path));
	}
	
	public long fromFile(String name, Path path)
			throws IOException
	{
		putNextEntry(SdcEntry.createHeader(name, path));
		return copyFrom(Files.newInputStream(path));
	}
	
	public long copyFrom(InputStream stream)
			throws IOException
	{
		long a = 0L;
		try
		{
			byte[] buf = new byte[2048];
			int r;
			while((r = stream.read(buf)) > 0)
			{
				write(buf, 0, r);
				a += r;
			}
		} finally
		{
			stream.close();
		}
		return a;
	}
}