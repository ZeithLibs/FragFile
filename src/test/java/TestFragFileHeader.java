import dev.zeith.fragfile.FragFileHeader;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestFragFileHeader
{
	protected static final Path headerFile = Paths.get("test", "test.hdr");
	
	@BeforeAll
	public static void prepare()
	{
		headerFile.getParent().toFile().mkdirs();
	}
	
	@AfterAll
	public static void complete()
	{
		headerFile.toFile().delete();
	}
	
	@Test
	@Order(1)
	public void testWrite()
			throws IOException
	{
		FragFileHeader ffh = FragFileHeader
				.builder()
				.partHashes(Arrays.asList("a", "b", "c", "d"))
				.fileSize(159236L)
				.fileHash("fhewsdughrdh")
				.hashAlgorithm("SHA1")
				.build();
		
		System.out.println("Write: " + ffh);
		
		try(DataOutputStream out = new DataOutputStream(Files.newOutputStream(headerFile)))
		{
			ffh.write(out);
		}
	}
	
	@Test
	@Order(2)
	public void testRead()
			throws IOException
	{
		System.out.println("Reading...");
		
		try(DataInputStream in = new DataInputStream(Files.newInputStream(headerFile)))
		{
			System.out.println(FragFileHeader.read(in));
		}
	}
}