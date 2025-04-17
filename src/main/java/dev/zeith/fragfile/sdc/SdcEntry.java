package dev.zeith.fragfile.sdc;

import lombok.Getter;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Objects;
import java.util.zip.CRC32;

@Getter
public class SdcEntry
		implements Cloneable
{
	String name;               // entry name
	FileTime lastModifiedTime; // last modification time, from extra field data
	FileTime creationTime;     // creation time, from extra field data
	long crc = -1;             // crc-32 of entry data
	long size = -1;            // uncompressed size of entry data
	
	public SdcEntry(String name)
	{
		Objects.requireNonNull(name, "name");
		if(name.isEmpty()) throw new IllegalArgumentException("entry name empty");
		if(name.length() > 0xFFFF) throw new IllegalArgumentException("entry name too long");
		this.name = name;
	}
	
	public SdcEntry(SdcEntry e)
	{
		Objects.requireNonNull(e, "entry");
		name = e.name;
		lastModifiedTime = e.lastModifiedTime;
		creationTime = e.creationTime;
		crc = e.crc;
		size = e.size;
	}
	
	SdcEntry() {}
	
	public SdcEntry setLastModifiedTime(FileTime time)
	{
		this.lastModifiedTime = Objects.requireNonNull(time, "lastModifiedTime");
		return this;
	}
	
	public SdcEntry setCreationTime(FileTime time)
	{
		this.creationTime = Objects.requireNonNull(time, "creationTime");
		return this;
	}
	
	public void setSize(long size)
	{
		if(size < 0) throw new IllegalArgumentException("invalid entry size");
		this.size = size;
	}
	
	public void setCrc(long crc)
	{
		if(crc < 0 || crc > 0xFFFFFFFFL) throw new IllegalArgumentException("invalid entry crc-32");
		this.crc = crc;
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
	
	@Override
	public SdcEntry clone()
	{
		try
		{
			return (SdcEntry) super.clone();
		} catch(CloneNotSupportedException e)
		{
			throw new AssertionError();
		}
	}
	
	public SdcEntry cloneWithName(String name)
	{
		SdcEntry e = clone();
		Objects.requireNonNull(name, "name");
		if(name.isEmpty()) throw new IllegalArgumentException("entry name empty");
		if(name.length() > 0xFFFF) throw new IllegalArgumentException("entry name too long");
		e.name = name;
		return e;
	}
	
	public static SdcEntry createHeader(Path root, Path input)
			throws IOException
	{
		return createHeader(root.relativize(input).toString().replace(File.separatorChar, '/'), input);
	}
	
	public static SdcEntry createHeader(String name, Path input)
			throws IOException
	{
		BasicFileAttributes attr = Files.readAttributes(input, BasicFileAttributes.class);
		SdcEntry e = createHeader(name, Files.newInputStream(input));
		return e.setCreationTime(attr.creationTime()).setLastModifiedTime(attr.lastModifiedTime());
	}
	
	public static SdcEntry createHeader(String name, InputStream input)
			throws IOException
	{
		try
		{
			CRC32 crc32 = new CRC32();
			byte[] buf = new byte[2048];
			int r;
			long len = 0;
			while((r = input.read(buf)) > 0)
			{
				crc32.update(buf, 0, r);
				len += r;
			}
			SdcEntry e = new SdcEntry(name);
			e.size = len;
			e.crc = crc32.getValue();
			return e;
		} finally
		{
			input.close();
		}
	}
}