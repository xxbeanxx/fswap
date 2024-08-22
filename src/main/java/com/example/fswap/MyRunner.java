package com.example.fswap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
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

	private enum Quality { Super, High, Low }

	private static final List<Face> sourceFaces = List.of(
		/* TODO :: add a face! */
	);

	private static final Integer sourceIndex = 0;
	private static final Boolean sameGender = false;
	private static final Quality quality = Quality.Super;
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

		final var nFiles = Files.list(Paths.get(inputDir)).filter(Files::isRegularFile).count() * sourceFaces.size();
		final var currentFile = new AtomicInteger();
		final var stopwatch = new StopWatch();

		stopwatch.start();

		Files.list(Paths.get(inputDir)).filter(Files::isRegularFile).sorted().forEach(path -> {
			sourceFaces.forEach(sourceFace -> {
				log.info("Processing file [{}/{}]: {} using face [{}]", currentFile.incrementAndGet(), nFiles, path.getFileName(), sourceFace);

				final var fSwapRequest = createFSwapRequest(encode(path), sourceFace);
				final var fSwapResponse = restTemplate.postForObject(url, fSwapRequest, FSwapResponse.class);
				fSwapResponse.getImages().forEach(image -> saveImage(sourceFace, path, image));
			});

			if (removeOriginal) { delete(path); }
		});

		stopwatch.stop();
		log.info("Finished... Elapsed time: {}", getFormattedTime(stopwatch.getTotalTimeMillis()));
	}

	private FSwapRequest createFSwapRequest(String image, Face face) {
		return ImmutableFSwapRequest.builder()
			.image(image)
			.addFSwapUnits(ImmutableFSwapUnit.builder()
				.sourceFace("%s.safetensors".formatted(face))
				.addFacesIndex(sourceIndex)
				.sameGender(sameGender)
				.sortBySize(sortBySize)
				.getfSwapSwappingOptions(ImmutableFSwapSwappingOptions.builder()
					.faceRestorerName(getFaceRestorerName())
					.upscalerName(getUpscalerName())
					.improvedMask(getImprovedMask())
					.restorerVisibility(restorerVisibility)
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

	private String getFaceRestorerName() {
		return switch (quality) {
			case Quality.Super -> "CodeFormer";
			case Quality.High -> "CodeFormer";
			case Quality.Low -> "None";
		};
	}

	private boolean getImprovedMask() {
		return switch (quality) {
			case Quality.Super -> true;
			case Quality.High -> true;
			case Quality.Low -> false;
		};
	}

	private String getUpscalerName() {
		return switch (quality) {
			case Quality.Super -> "LDSR";
			case Quality.High -> "Lanczos";
			case Quality.Low -> "None";
		};
	}

	private void saveImage(Face face, Path path, String image) {
		try {
			final var fileName = face.name() + "-" + removeFileExtension(path.getFileName().toString()) + ".png";
			final var outputPath = Paths.get(outputDir, fileName);
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

	private String removeFileExtension(String filename) {
		return filename.replaceAll("(?<!^)[.][^.]*$", "");
	}

}
