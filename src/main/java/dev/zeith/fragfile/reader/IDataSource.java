package dev.zeith.fragfile.reader;

import java.io.*;

public interface IDataSource
{
	InputStream getInput(String file)
			throws IOException;
}