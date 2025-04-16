package dev.zeith.fragfile;

import java.io.*;
import java.math.BigInteger;
import java.security.*;

public class FFHashers
{
	public static final FFHashers MD5 = new FFHashers("MD5");
	public static final FFHashers SHA1 = new FFHashers("SHA1");
	public static final FFHashers SHA256 = new FFHashers("SHA256");
	
	public static long hashCodeL(Object... a)
	{
		if(a == null)
			return 0;
		long result = 1;
		for(Object element : a)
		{
			long add;
			
			if(element instanceof Number)
			{
				add = ((Number) element).longValue();
			} else if(element instanceof Iterable)
			{
				long l = 1L;
				for(Object o : ((Iterable) element))
				{
					l = 31L * l + o.hashCode();
				}
				add = l;
			} else if(element instanceof CharSequence)
				add = hashCodeL4Chars(element.toString().toCharArray());
			else
				add = element == null ? 0 : element.hashCode();
			
			result = 31L * result + add;
		}
		return result;
	}
	
	public static long hashCodeL4Chars(char... a)
	{
		if(a == null)
			return 0;
		long result = 1;
		for(char el : a) result = 31L * result + Character.hashCode(el);
		return result;
	}
	
	final String algorithm;
	
	public FFHashers(String algorithm)
	{
		this.algorithm = algorithm;
	}
	
	public Digestion digestion()
	{
		return new Digestion(this);
	}
	
	protected MessageDigest newDigest()
	{
		try
		{
			return MessageDigest.getInstance(algorithm);
		} catch(NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public String hashify(byte[] data)
	{
		MessageDigest messageDigest = newDigest();
		messageDigest.reset();
		messageDigest.update(data);
		byte[] digest = messageDigest.digest();
		
		BigInteger bigInt = new BigInteger(1, digest);
		StringBuilder md5Hex = new StringBuilder(bigInt.toString(16));
		while(md5Hex.length() < 32) md5Hex.insert(0, "0");
		return md5Hex.toString();
	}
	
	public String hashify(String line)
	{
		return hashify(line.getBytes());
	}
	
	public String genHash(File file)
	{
		byte[] b = null;
		try
		{
			b = createChecksum(file);
		} catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
		BigInteger bigInt = new BigInteger(1, b);
		StringBuilder md5Hex = new StringBuilder(bigInt.toString(16));
		while(md5Hex.length() < 32)
			md5Hex.insert(0, "0");
		return md5Hex.toString();
	}
	
	private byte[] createChecksum(File file)
			throws Exception
	{
		int numRead;
		if(!file.exists())
		{
			MessageDigest messageDigest = newDigest();
			messageDigest.reset();
			messageDigest.update("0".getBytes());
			return messageDigest.digest();
		}
		FileInputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance(algorithm);
		do
		{
			if((numRead = fis.read(buffer)) <= 0)
				continue;
			complete.update(buffer, 0, numRead);
		} while(numRead != -1);
		fis.close();
		return complete.digest();
	}
	
	public static class Digestion
	{
		final FFHashers hasher;
		MessageDigest digest;
		
		public Digestion(FFHashers hasher)
		{
			this.hasher = hasher;
		}
		
		public Digestion start()
		{
			digest = hasher.newDigest();
			return this;
		}
		
		public Digestion feed(byte[] input)
		{
			digest.update(input);
			return this;
		}
		
		public Digestion feed(byte[] input, int off, int len)
		{
			digest.update(input, off, len);
			return this;
		}
		
		public byte[] digestRaw()
		{
			byte[] r = digest.digest();
			digest.reset();
			return r;
		}
		
		public String digestHex()
		{
			byte[] raw = digestRaw();
			StringBuilder hex = new StringBuilder(raw.length * 2);
			for(byte b : raw) hex.append(String.format("%02x", b));
			return hex.toString();
		}
	}
}