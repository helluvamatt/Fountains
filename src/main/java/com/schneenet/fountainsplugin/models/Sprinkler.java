package com.schneenet.fountainsplugin.models;

public class Sprinkler implements ILocatable, IRequireRedstone {

	public static final int MAX_SPREAD = 6;

	private long id;
	private String name;
	private String worldName;
	private long x;
	private long y;
	private long z;
	private int spread;
	private RedstoneRequirementState redstoneRequirementState;

	public Sprinkler(String name, String worldName, long x, long y, long z, int spread, RedstoneRequirementState redstoneRequirementState, long id) {
		this(name, worldName, x, y, z, spread, redstoneRequirementState);
		this.id = id;
	}

	public Sprinkler(String name, String worldName, long x, long y, long z, int spread, RedstoneRequirementState redstoneRequirementState) {
		this.name = name;
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.spread = spread;
		this.redstoneRequirementState = redstoneRequirementState;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getWorldName() {
		return worldName;
	}

	public long getX() {
		return x;
	}

	public long getY() {
		return y;
	}

	public long getZ() {
		return z;
	}

	public int getSpread() {
		return spread;
	}

	public RedstoneRequirementState getRedstoneRequirementState() {
		return redstoneRequirementState;
	}
}
