package com.purdue.fw.api;

public class InvalidInputException extends Exception {

	public String name = null;
	public String value = null;
	
	public InvalidInputException(String name, String value) {
		this.name = name;
		this.value = value;
	}
}
