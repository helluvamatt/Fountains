package com.schneenet.fountainsplugin.models;

public class Intake implements ILocatable, IRequireRedstone
{
	public static final int MAX_SPEED = 10;
	
	private long id;
	private String name;
	private String worldName;
	private long x;
	private long y;
	private long z;
	private int speed;
	private RedstoneRequirementState redstoneRequirementState;
	
	public Intake(String name, String worldName, long x, long y, long z, int speed, RedstoneRequirementState redstoneRequirementState)
	{
		this.name = name;
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.speed = speed;
		this.redstoneRequirementState = redstoneRequirementState;
	}
	
	public Intake(String name, String worldName, long x, long y, long z, int speed, RedstoneRequirementState redstoneRequirementState, long id)
	{
		this(name, worldName, x, y, z, speed, redstoneRequirementState);
		this.id = id;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getWorldName()
	{
		return worldName;
	}

	public long getX()
	{
		return x;
	}

	public long getY()
	{
		return y;
	}

	public long getZ()
	{
		return z;
	}
	
	public int getSpeed()
	{
		return speed;
	}
	
	public void setSpeed(int speed)
	{
		this.speed = speed;
	}

	@Override
	public RedstoneRequirementState getRedstoneRequirementState() {
		return redstoneRequirementState;
	}
}
