package dev.zeith.fragfile.writer;

import dev.zeith.fragfile.FragFileHeader;
import lombok.*;

import java.util.function.Consumer;

@With
@Data
@Builder(toBuilder = true)
public class FFReadConfig
{
	public static final FFReadConfig DEFAULT = FFReadConfig.builder().build();
	
	@Builder.Default
	int chunkRetryAttempts = 25; // 25 retries
	
	Consumer<FragFileHeader> onInitialHeaderRead;
}