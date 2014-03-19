package com.netmonkey.hop;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

class HopCanvas extends Canvas {
	MovingHop applet;
	
	public HopCanvas(MovingHop applet) {
		this.applet = applet;
		setBackground(Color.black);
	}
	public void paint(Graphics g) {
		super.paint(g);
		applet.hopalong();
	}
}