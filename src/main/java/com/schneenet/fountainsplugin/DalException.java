package com.schneenet.fountainsplugin;

public class DalException extends Exception
{
	private static final long serialVersionUID = -8418684408388120345L;
	
	public DalException(Exception inner)
	{
		super(inner);
	}
}
