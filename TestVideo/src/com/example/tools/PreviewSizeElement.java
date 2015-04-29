package com.example.tools;

public class PreviewSizeElement {

	double width;
	double height;
	double pri;

	
	public PreviewSizeElement() {
		super();
	}

	public PreviewSizeElement(double width, double height, double pri) {

		this.width = width;
		this.height = height;
		this.pri = pri;

	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getPri() {
		return pri;
	}

	public void setPri(double pri) {
		this.pri = pri;
	}
	
	@Override
	public String toString()
	{
		return "H:"+this.height+";W:"+this.width+";P:"+this.pri;
	}
	
}
