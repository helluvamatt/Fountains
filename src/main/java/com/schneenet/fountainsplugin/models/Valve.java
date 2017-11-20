package com.schneenet.fountainsplugin.models;

public class Valve implements ILocatable {

	private long id;
	private String name;
	private String worldName;
	private long x;
	private long y;
	private long z;

	public Valve(String name, String worldName, long x, long y, long z) {
		this.name = name;
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Valve(String name, String worldName, long x, long y, long z, long id) {
		this(name, worldName, x, y, z);
		this.id = id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String getWorldName() {
		return worldName;
	}

	@Override
	public long getX() {
		return x;
	}

	@Override
	public long getY() {
		return y;
	}

	@Override
	public long getZ() {
		return z;
	}
}
