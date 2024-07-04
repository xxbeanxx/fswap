package com.example.fswap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import com.example.fswap.model.FSwapRequest;
import com.example.fswap.model.FSwapResponse;
import com.example.fswap.model.ImmutableFSwapRequest;
import com.example.fswap.model.ImmutableFSwapSwappingOptions;
import com.example.fswap.model.ImmutableFSwapUnit;

@Component
public class MyRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MyRunner.class);

	private static final Face sourceFace = null; // TODO :: set me!
	private static final Integer sourceIndex = 0;
	private static final Boolean sameGender = false;
	private static final Boolean highQuality = true;
	private static final Boolean sortBySize = false;
	private static final Boolean removeOriginal = true;
	private static final String restorerVisibility = "1";

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${application.input-dir}")
	private String inputDir;

	@Value("${application.output-dir}")
	private String outputDir;

	@Value("${application.sd-url}")
	private String url;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Create output folder in case it doesn't exist
		Files.createDirectories(Paths.get(outputDir));

		final var nFrames = Files.list(Paths.get(inputDir)).filter(Files::isRegularFile).count();
		final var currentFrame = new AtomicInteger();
		final var stopwatch = new StopWatch();

		stopwatch.start();
		Files.list(Paths.get(inputDir)).filter(Files::isRegularFile).sorted().forEach(path -> {
			log.info("Processing file [{}/{}]: {}", currentFrame.incrementAndGet(), nFrames, path.getFileName());

			final var encodedFile = encode(path);
			final var fSwapRequest = createFSwapRequest(encodedFile);
			final var fSwapResponse = restTemplate.postForObject(url, fSwapRequest, FSwapResponse.class);
			fSwapResponse.getImages().forEach(image -> {
				saveImage(path, image);
				if (removeOriginal) { delete(path); }
			});
		});

		stopwatch.stop();
		log.info("Finished... Elapsed time: {}", getFormattedTime(stopwatch.getTotalTimeMillis()));
	}

	private FSwapRequest createFSwapRequest(String image) {
		return ImmutableFSwapRequest.builder()
			.image(image)
			.addFSwapUnits(ImmutableFSwapUnit.builder()
				.sourceFace("%s.safetensors".formatted(sourceFace))
				.addFacesIndex(sourceIndex)
				.sameGender(sameGender)
				.sortBySize(sortBySize)
				.getfSwapSwappingOptions(ImmutableFSwapSwappingOptions.builder()
					.faceRestorerName(highQuality ? "CodeFormer" : "None")
					.upscalerName(highQuality ? "Lanczos" : "None")
					.restorerVisibility(restorerVisibility)
					.improvedMask(highQuality)
					.build())
				.build())
			.build();
	}

	private void delete(Path path) {
		try {
			Files.delete(path);
		} catch (final IOException e) {
			log.warn("Failed to delete file: {}", path);
			throw new RuntimeException(e);
		}
	}

	private String encode(Path path) {
		try {
			return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
		}
		catch (final Exception e) {
			log.warn("Failed to read file: {}", path);
			throw new RuntimeException(e);
		}
	}

	private void saveImage(Path path, String image) {
		try {
			final var outputPath = Paths.get(outputDir, path.getFileName().toString());
			Files.write(outputPath, Base64.getDecoder().decode(image));
		}
		catch (final Exception e) {
			log.warn("Failed to write file: {}", path);
			throw new RuntimeException(e);
		}
	}

	private String getFormattedTime(long milliseconds) {
		final var hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
		final var minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1);
		final var seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1);
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

}
