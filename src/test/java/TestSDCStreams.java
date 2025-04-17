import dev.zeith.fragfile.FFHashers;
import dev.zeith.fragfile.sdc.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSDCStreams
{
	protected static final Path sdcFile = Paths.get("test", "sdc-files", "test.sdc");
	protected static final Path zipFile = Paths.get("test", "sdc-files", "test.zip");
	protected static final Path sdcFilesIn = Paths.get("test", "sdc-files", "in");
	protected static final Path sdcFilesOut = Paths.get("test", "sdc-files", "out");
	
	private static final long MEGABYTE = 1024 * 1024;
	
	@BeforeAll
	public static void prepare()
			throws IOException
	{
		sdcFilesIn.toFile().mkdirs();
		sdcFilesOut.toFile().mkdirs();
		
		Files.walk(sdcFilesIn)
			 .filter(Files::isRegularFile)
			 .map(Path::toFile)
			 .forEach(File::delete);
		
		Files.walk(sdcFilesOut)
			 .filter(Files::isRegularFile)
			 .map(Path::toFile)
			 .forEach(File::delete);
		
		Random rng = new Random();
		for(int i = 0; i < 16; i++)
		{
			Path binFile = sdcFilesIn.resolve("Part-" + i + ".bin");
			try(OutputStream out = Files.newOutputStream(binFile))
			{
				long left = MEGABYTE;
				
				byte[] buf = new byte[1024];
				
				while(left > 0)
				{
					if(left < buf.length) buf = new byte[(int) left];
					rng.nextBytes(buf);
					out.write(buf);
					left -= buf.length;
				}
			}
		}
	}
	
	@Test
	@Order(1)
	public void encode()
			throws IOException
	{
		try(SdcOutputStream out = new SdcOutputStream(Files.newOutputStream(sdcFile), false))
		{
			Iterator<Path> itr = Files.walk(sdcFilesIn).filter(Files::isRegularFile).iterator();
			while(itr.hasNext())
			{
				Path p = itr.next();
				out.fromFile(sdcFilesIn, p);
			}
		}
	}
	
	@Test
	@Order(2)
	public void decode()
			throws IOException
	{
		try(SdcInputStream in = new SdcInputStream(Files.newInputStream(sdcFile)))
		{
			SdcEntry e;
			while((e = in.readNextEntry()) != null)
			{
				System.out.println("Read " + e);
				sdcFilesOut.resolve(e.getName()).toAbsolutePath().getParent().toFile().mkdirs();
				try(OutputStream out = Files.newOutputStream(sdcFilesOut.resolve(e.getName())))
				{
					in.writeTo(out);
				}
			}
		}
	}
	
	@Test
	@Order(3)
	public void sdcToZip()
			throws IOException
	{
		try(SdcInputStream in = new SdcInputStream(Files.newInputStream(sdcFile));
			ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zipFile)))
		{
			SdcHelper.toZip(in, out);
		}
	}
	
	@Test
	@Order(4)
	public void checkValidity()
			throws IOException
	{
		Iterator<Path> itr = Files.walk(sdcFilesIn).filter(Files::isRegularFile).iterator();
		while(itr.hasNext())
		{
			Path pi = itr.next();
			Path po = sdcFilesOut.resolve(sdcFilesIn.relativize(pi));
			System.out.println("Validating " + pi + " against " + po);
			assertEquals(FFHashers.SHA1.genHash(pi.toFile()), FFHashers.SHA1.genHash(po.toFile()));
		}
	}
}