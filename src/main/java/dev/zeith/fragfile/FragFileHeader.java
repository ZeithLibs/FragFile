package dev.zeith.fragfile;

import dev.zeith.fragfile.errors.FragFileIOException;
import lombok.*;

import java.io.*;
import java.util.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class FragFileHeader
{
	private static final long MAGIC_NUM = 0xFAFEEAEBL;
	public static final short CURRENT_VERSION = 1;
	
	private final short version;
	private final long fileSize;
	private final long chunkSize;
	private final String hashAlgorithm;
	private final String fileHash;
	private final List<String> partHashes;
	
	public static FragFileHeader read(DataInputStream input)
			throws IOException
	{
		if(input.readLong() != MAGIC_NUM)
			throw new FragFileIOException("Invalid header start.");
		
		short version = input.readShort();
		var res = builder().version(version);
		
		long partHash = input.readLong();
		
		long fileSize = input.readLong();
		long chunkSize = input.readLong();
		String hashAlgorithm = input.readUTF();
		String fileHash = input.readUTF();
		res.fileSize(fileSize).chunkSize(chunkSize).hashAlgorithm(hashAlgorithm).fileHash(fileHash);
		
		int parts = input.readInt();
		List<String> partList = new ArrayList<>(parts);
		
		long partsResult = 1;
		for(int i = 0; i < parts; i++)
		{
			String s = input.readUTF();
			partsResult = partsResult * 31L + s.hashCode();
			partList.add(s);
		}
		
		res.partHashes(Collections.unmodifiableList(partList));
		
		if(FFHashers.hashCodeL(
				fileSize,
				chunkSize,
				hashAlgorithm,
				fileHash,
				parts,
				partsResult
		) != partHash)
			throw new FragFileIOException("Hash check failed!");
		
		return res.build();
	}
	
	public void write(DataOutputStream output)
			throws IOException
	{
		output.writeLong(MAGIC_NUM);
		output.writeShort(version);
		
		long partHash = 1;
		for(String s : partHashes) partHash = partHash * 31L + s.hashCode();
		output.writeLong(FFHashers.hashCodeL(
				fileSize,
				chunkSize,
				hashAlgorithm,
				fileHash,
				partHashes.size(),
				partHash
		));
		
		output.writeLong(fileSize);
		output.writeLong(chunkSize);
		output.writeUTF(hashAlgorithm);
		output.writeUTF(fileHash);
		
		output.writeInt(partHashes.size());
		for(String s : partHashes) output.writeUTF(s);
	}
	
	public static class FragFileHeaderBuilder
	{
		public FragFileHeaderBuilder()
		{
			version(CURRENT_VERSION);
		}
	}
}