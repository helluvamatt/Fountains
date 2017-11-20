package com.schneenet.fountainsplugin.models;

public enum RedstoneRequirementState {
	IGNORED(0),
	ACTIVE(1),
	INACTIVE(2);

	private int value;

	RedstoneRequirementState(int value)
	{
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static RedstoneRequirementState valueOf(int value)
	{
		for (RedstoneRequirementState state : values())
		{
			if (state.value == value) return state;
		}
		return IGNORED;
	}
}
