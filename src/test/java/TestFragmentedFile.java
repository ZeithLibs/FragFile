import dev.zeith.fragfile.*;
import dev.zeith.fragfile.reader.IDataSource;
import dev.zeith.fragfile.writer.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TestFragmentedFile
{
	protected static final Path frag = Paths.get("test", "fragmented");
	protected static final Path testBinary = Paths.get("test", "input.bin");
	protected static final Path testBinaryOut = Paths.get("test", "output.bin");
	
	private static final long MEGABYTE = 1024 * 1024;
	public static final long FILE_SIZE = Math.round(MEGABYTE * 2 + new Random().nextDouble() * MEGABYTE * 32);
	
	@BeforeAll
	public static void prepare()
			throws IOException
	{
		frag.toFile().mkdirs();
		
		Files.walk(frag)
			 .filter(Files::isRegularFile)
			 .map(Path::toFile)
			 .forEach(File::delete);
		
		System.out.println("Creating " + FILE_SIZE + " file to fragment!");
		
		Random rng = new Random();
		try(OutputStream out = Files.newOutputStream(testBinary))
		{
			long left = FILE_SIZE;
			
			byte[] buf = new byte[1024];
			
			while(left > 0)
			{
				if(left < buf.length) buf = new byte[(int) left];
				rng.nextBytes(buf);
				out.write(buf);
				left -= buf.length;
			}
		}
		
		System.out.println("SAMPLE SHA1: " + FFHashers.SHA1.genHash(testBinary.toFile()));
	}
	
	@AfterAll
	public static void complete()
	{
//		testBinary.toFile().delete();
	}
	
	@Test
	public void create()
			throws IOException
	{
		IDataSink output = IDataSink.ofDirectory(frag);
		IDataSource input = n -> Files.newInputStream(Paths.get("test", n));
		
		FragmentedFile.split(output, input, "input.bin", FFWriteConfig.DEFAULT);
	}
	
	@Test
	public void read()
			throws IOException
	{
		IDataSource input = n -> Files.newInputStream(frag.resolve(n));
		
		Files.deleteIfExists(testBinaryOut);
		
		FragFileHeader header;
		try(RandomAccessFile raf = new RandomAccessFile(testBinaryOut.toFile(), "rwd"))
		{
			ExecutorService ser = Executors.newWorkStealingPool();
			header = FragmentedFile.join(input, (position, buf, offset, length) ->
					{
						synchronized(raf)
						{
							raf.seek(position);
							raf.write(buf, offset, length);
						}
					}, ser, FFReadConfig.DEFAULT
			);
			ser.shutdown();
			System.out.println("Read header : " + header);
		}
		
		FFHashers hasher = new FFHashers(header.getHashAlgorithm());
		
		String in = hasher.genHash(testBinary.toFile());
		String out = hasher.genHash(testBinaryOut.toFile());
		
		System.out.println("Input: " + in);
		System.out.println("Output: " + out);
		
		assert Objects.equals(in, out);
	}
}