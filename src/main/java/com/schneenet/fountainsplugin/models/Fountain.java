package com.schneenet.fountainsplugin.models;

public class Fountain implements ILocatable, IRequireRedstone
{
	public static final int MAX_POWER = 16;

	public static final int MIN_FILL_POWER = 11;
	
	private long id;
	private String worldName;
	private long x;
	private long y;
	private long z;
	private int power;
	private String name;
	private RedstoneRequirementState redstoneRequirementState;
	
	public Fountain(String name, String worldName, long x, long y, long z, int power, RedstoneRequirementState redstoneRequirementState)
	{
		this.name = name;
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.power = power;
		this.redstoneRequirementState = redstoneRequirementState;
	}
	
	public Fountain(String name, String worldName, long x, long y, long z, int power, RedstoneRequirementState redstoneRequirementState, long id)
	{
		this(name, worldName, x, y, z, power, redstoneRequirementState);
		this.id = id;
	}
	
	public long getId()
	{
		return this.id;
	}
	
	public void setId(long id)
	{
		this.id = id;
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
	
	public int getPower()
	{
		return power;
	}
	
	public void setPower(int power)
	{
		this.power = power;
	}
	
	public String getName()
	{
		return this.name;
	}

	@Override
	public RedstoneRequirementState getRedstoneRequirementState() {
		return redstoneRequirementState;
	}
}
