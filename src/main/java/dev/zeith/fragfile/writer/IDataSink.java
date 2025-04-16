package dev.zeith.fragfile.writer;

import java.io.*;
import java.nio.file.*;

public interface IDataSink
{
	OutputStream createOutput(String file)
			throws IOException;
	
	
	void rename(String fileSrc, String fileDst)
			throws IOException;
	
	static IDataSink ofDirectory(Path path)
	{
		return new IDataSink()
		{
			@Override
			public OutputStream createOutput(String file)
					throws IOException
			{
				return Files.newOutputStream(path.resolve(file));
			}
			
			@Override
			public void rename(String fileSrc, String fileDst)
					throws IOException
			{
				if(fileDst == null)
				{
					Files.delete(path.resolve(fileSrc));
					return;
				}
				
				Files.move(path.resolve(fileSrc), path.resolve(fileDst), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
		};
	}
}