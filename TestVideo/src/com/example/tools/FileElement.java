package com.example.tools;

public class FileElement {

	String path;
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getTimes() {
		return times;
	}

	public void setTimes(int times) {
		this.times = times;
	}

	int times;

	public FileElement() {
		super();
	}

	public FileElement(String path, int times) {

		this.path = path;
		this.times = times;

	}

}
