package com.example.fswap.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;

@Immutable
public interface FSwapUnit {

	@JsonProperty("source_face")
	String getSourceFace();

	@JsonProperty("faces_index")
	List<Integer> getFacesIndex();

	@JsonProperty("swapping_options")
	FSwapSwappingOptions getfSwapSwappingOptions();

	@Default
	@JsonProperty("same_gender")
	default Boolean getSameGender() {
		return Boolean.TRUE;
	}

	@Default
	@JsonProperty("sort_by_size")
	default Boolean getSortBySize() {
		return Boolean.FALSE;
	}

}
