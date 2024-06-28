package com.example.fswap.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;

@Immutable
public interface FSwapRequest {

	@JsonProperty("image")
	String getImage();

	@JsonProperty("units")
	List<FSwapUnit> getFSwapUnits();

}
