package dev.zeith.fragfile.writer;

import java.io.IOException;

public interface IDirectWriter
{
	void write(long position, byte[] buf, int offset, int length)
			throws IOException;
}