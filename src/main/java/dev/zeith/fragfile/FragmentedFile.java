package dev.zeith.fragfile;

import dev.zeith.fragfile.reader.IDataSource;
import dev.zeith.fragfile.writer.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class FragmentedFile
{
	public static FragFileHeader join(IDataSource source, IDirectWriter writer, Executor multiThreading, FFReadConfig config)
			throws IOException
	{
		FragFileHeader header;
		try(DataInputStream in = new DataInputStream(source.getInput("header")))
		{
			header = FragFileHeader.read(in);
		}
		
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();
		
		FFHashers hh = new FFHashers(header.getHashAlgorithm());
		
		int i = 0;
		for(String hash : header.getPartHashes())
		{
			final int offsetIndex = i;
			long offset = i * header.getChunkSize();
			futures.add(CompletableFuture.supplyAsync(() ->
					{
						int attempts = config.getChunkRetryAttempts();
						while(attempts-- > 0)
						{
							FFHashers.Digestion d = hh.digestion();
							long left = header.getChunkSize();
							try(InputStream in = source.getInput(hash))
							{
								d.start();
								long position = offset;
								byte[] buf = new byte[1024];
								int r;
								while((r = in.read(buf)) > 0 && left > 0)
								{
									d.feed(buf, 0, r);
									writer.write(position, buf, 0, r);
									left -= r;
									position += r;
								}
								if(hash.equals(d.digestHex()))
									return true;
							} catch(IOException e)
							{
								throw new CompletionException(e);
							}
						}
						throw new CompletionException(new IOException("Unable to retrieve part " + offsetIndex + " @ " + offset + "."));
					}, multiThreading
			));
			++i;
		}
		
		// Wait for all to complete...
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		
		return header;
	}
	
	public static FragFileHeader split(IDataSink output, IDataSource source, String name, FFWriteConfig config)
			throws IOException
	{
		FragFileHeader.FragFileHeaderBuilder b = FragFileHeader.builder();
		
		List<String> hashes = new ArrayList<>();
		String algorithm = "SHA1";
		
		b.hashAlgorithm(algorithm).chunkSize(config.getChunkSize());
		
		class ChunkAccumulator
				implements Closeable
		{
			final FFHashers.Digestion hasher = new FFHashers(algorithm).digestion();
			boolean writingPart;
			long bytesLeft;
			
			OutputStream out;
			
			void startPart()
					throws IOException
			{
				if(writingPart) return;
				bytesLeft = config.getChunkSize();
				writingPart = true;
				hasher.start();
				out = output.createOutput("tmp");
			}
			
			void finishPart()
					throws IOException
			{
				if(!writingPart) return;
				writingPart = false;
				String hex = hasher.digestHex();
				hashes.add(hex);
				out.close();
				out = null;
				output.rename("tmp", hex);
			}
			
			void writeToPart(byte[] buf, int off, int length)
					throws IOException
			{
				if(length == 0)
					return;
				
				if(!writingPart)
					startPart();
				
				hasher.feed(buf, off, length);
				out.write(buf, off, length);
				bytesLeft -= length;
			}
			
			void write(byte[] buf, int length)
					throws IOException
			{
				if(length > 0 && !writingPart)
					startPart();
				
				int off = 0;
				
				while(length > bytesLeft)
				{
					int lim = (int) Math.min(length, bytesLeft);
					writeToPart(buf, off, lim);
					off += lim;
					length -= lim;
					finishPart();
				}
				
				writeToPart(buf, off, length);
				if(bytesLeft <= 0L)
					finishPart();
			}
			
			@Override
			public void close()
					throws IOException
			{
				finishPart();
			}
		}
		
		try(InputStream input = source.getInput(name); ChunkAccumulator accum = new ChunkAccumulator())
		{
			final FFHashers.Digestion hasher = new FFHashers(algorithm).digestion();
			hasher.start();
			
			long length = 0L;
			
			byte[] buf = new byte[1024];
			int r;
			while((r = input.read(buf)) > 0)
			{
				accum.write(buf, r);
				hasher.feed(buf, 0, r);
				length += r;
			}
			
			b.fileHash(hasher.digestHex()).fileSize(length);
		}
		
		b.partHashes(Collections.unmodifiableList(hashes));
		
		FragFileHeader header = b.build();
		
		try(DataOutputStream out = new DataOutputStream(output.createOutput("header")))
		{
			header.write(out);
		}
		
		return header;
	}
}