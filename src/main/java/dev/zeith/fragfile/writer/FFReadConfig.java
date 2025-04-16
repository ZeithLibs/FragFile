package dev.zeith.fragfile.writer;

import lombok.*;

@With
@Data
@Builder(toBuilder = true)
public class FFReadConfig
{
	public static final FFReadConfig DEFAULT = FFReadConfig.builder().build();
	
	@Builder.Default
	int chunkRetryAttempts = 25; // 25 retries
}