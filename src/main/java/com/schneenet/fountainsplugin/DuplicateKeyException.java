package com.schneenet.fountainsplugin;

public class DuplicateKeyException extends Exception {
	public DuplicateKeyException(Exception inner)
	{
		super(inner);
	}
}
