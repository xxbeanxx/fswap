package com.example.fswap.model;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;

@Immutable
public interface FSwapSwappingOptions {

	// "swapping_options": {
	//     "face_restorer_name": "CodeFormer",
	//     "restorer_visibility": 1,
	//     "codeformer_weight": 1,
	//     "upscaler_name": "string",
	//     "improved_mask": false,
	//     "color_corrections": false,
	//     "sharpen": false,
	//     "erosion_factor": 1
	//   },

	@Default
	@JsonProperty("improved_mask")
	default Boolean getImprovedMask() {
		return Boolean.FALSE;
	}

	@Default
	@JsonProperty("upscaler_name")
	default String getUpscalerName() {
		return "None";
	}

	@Default
	@JsonProperty("face_restorer_name")
	default String getFaceRestorerName() {
		return "None";
	}

	@Default
	@JsonProperty("color_corrections")
	default Boolean getColorCorrections() {
		return true;
	}

	@Default
	@JsonProperty("sharpen")
	default Boolean getSharpen() {
		return true;
	}

}
