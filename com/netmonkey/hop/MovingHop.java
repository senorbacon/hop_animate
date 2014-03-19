package com.netmonkey.hop;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

/**
 * Moving hopalong fractal.
 * @author Terren Suydam
 * 99/2/24
 */


public class MovingHop extends Applet
	implements ActionListener
{
	HopCanvas fractal;
	ControlPanel controls;

	public CoordThread thread; 

	public static final int MAX_THREADS = 100;
	
	public static String THREAD_FLD = "Threads";
	public static String DOTS_FLD = "Dots";
	public static String ZOOM_FLD = "Zoom";
	public static String PLAY = "Play";
	public static String PAUSE = "Pause";
	public static String RESTART = "Restart";
	public static String QUIT = "Quit";
	public static String UPDATE = "Update";

	public static final int initThreads = 20;
	public static final int initDots = 5000;
	public static final double initZoom = 100;

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (MovingHop.PLAY.equals(command) ||		
			MovingHop.RESTART.equals(command) )
		{
			if (thread != null && thread.quit) 
				thread = null;
				
			if (thread == null) {
				fractal.repaint();
				return;
			}
		} else {
			if (thread == null) return;
		}			
		
		if (MovingHop.PLAY.equals(command)) {		
			thread.paused = false;
			synchronized (thread) {
				thread.notify();
			}
			
		} else if (MovingHop.RESTART.equals(command)) {
			thread.setup();
			fractal.repaint();
			thread.paused = false;
			synchronized (thread) {
				thread.notify();
			}
			
		} else if (MovingHop.PAUSE.equals(command)) {
			thread.paused = true;
			// advance one 'frame' if already paused
			synchronized (thread) {
				thread.notify();
			}
			
		} else if (MovingHop.QUIT.equals(command)) {
			thread.quit();
			
		} else if (MovingHop.UPDATE.equals(command)) {	
			thread.update(controls.getNumThreads(),
						  controls.getNumDots(),
						  controls.getZoom());
			fractal.repaint();
			thread.paused = false;
			synchronized (thread) {
				thread.notify();
			}
		}
	}
	public void destroy() {
		if (thread != null)
			thread.quit();
	}
	// called by the HopCanvas.paint() method to kick off thread
	public void hopalong() {		
		if (thread == null) {
			thread = new CoordThread(this, fractal);
			thread.setup();
			thread.start();
		}
	}
	public void init()
	{
		setLayout(new BorderLayout());

		fractal = new HopCanvas(this);
		controls = new ControlPanel(this);

		add("South", controls);		
		add("Center", fractal);
		
		controls.setValues(initThreads, initDots, initZoom);
	}
	public void nullifyThread() {
		thread = null;
		System.out.println("Threads killed");
	}
	public void stop() {
		if (thread != null)
			thread.quit();
	}
}