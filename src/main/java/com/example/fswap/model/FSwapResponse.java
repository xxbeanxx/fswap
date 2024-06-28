package com.example.fswap.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Immutable
@JsonDeserialize(as = ImmutableFSwapResponse.class)
public interface FSwapResponse {

	@JsonProperty("images")
	List<String> getImages();

}
