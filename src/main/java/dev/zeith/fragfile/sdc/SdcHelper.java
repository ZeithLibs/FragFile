package dev.zeith.fragfile.sdc;

import java.io.IOException;
import java.util.zip.*;

public class SdcHelper
{
	public static void toZip(SdcInputStream input, ZipOutputStream output)
			throws IOException
	{
		SdcEntry e;
		while((e = input.readNextEntry()) != null)
		{
			ZipEntry ze = new ZipEntry(e.getName());
			ze.setCrc(e.getCrc());
			ze.setSize(e.getSize());
			ze.setCreationTime(e.getCreationTime());
			ze.setLastModifiedTime(e.getLastModifiedTime());
			output.putNextEntry(ze);
			input.writeTo(output);
			output.closeEntry();
		}
	}
}