package dev.zeith.fragfile.writer;

import lombok.*;

@With
@Data
@Builder(toBuilder = true)
public class FFWriteConfig
{
	public static final FFWriteConfig DEFAULT = FFWriteConfig.builder().build();
	
	@Builder.Default
	long chunkSize = 1024L * 1024L; // 1 Megabyte
}