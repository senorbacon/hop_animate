package com.netmonkey.hop;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

class ControlPanel extends Panel
{
	TextField threadsFld;
	TextField dotsFld;
	TextField zoomFld;
	
	public ControlPanel(MovingHop applet)
	{
		setBackground(Color.black);
		
		Panel panel = new Panel();
		panel.setLayout(new BorderLayout());
		
		Button button = new Button(MovingHop.PLAY);
		button.setActionCommand(MovingHop.PLAY);
		button.addActionListener(applet);
		panel.add("North", button);
		
		button = new Button(MovingHop.PAUSE);
		button.setActionCommand(MovingHop.PAUSE);
		button.addActionListener(applet);
		panel.add("South", button);
		add(panel);


		panel = new Panel();
		panel.setLayout(new BorderLayout());
		
		button = new Button(MovingHop.RESTART);
		button.setActionCommand(MovingHop.RESTART);
		button.addActionListener(applet);
		panel.add("North", button);
		
		button = new Button(MovingHop.QUIT);
		button.setActionCommand(MovingHop.QUIT);
		button.addActionListener(applet);
		panel.add("South", button);
		add(panel);

		
		panel = new Panel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(Color.gray);

	 	panel.add("North", new Label(MovingHop.THREAD_FLD + ":"));
		threadsFld = new TextField(2);
		panel.add("South", threadsFld);
		add(panel);


		panel = new Panel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(Color.gray);

	 	panel.add("North", new Label(MovingHop.DOTS_FLD + ":"));
		dotsFld = new TextField(7);
		panel.add("South", dotsFld);
		add(panel);


		panel = new Panel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(Color.gray);

	 	panel.add("North", new Label(MovingHop.ZOOM_FLD + ":"));
		zoomFld = new TextField(5);
		panel.add("South", zoomFld);
		add(panel);

		button = new Button(MovingHop.UPDATE);
		button.setActionCommand(MovingHop.UPDATE);
		button.addActionListener(applet);
		add(button);
	}
	public int getNumDots() {
		String t = dotsFld.getText();
		try {
			return Integer.valueOf(t).intValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public int getNumThreads() {
		String t = threadsFld.getText();
		try {
			return Integer.valueOf(t).intValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public double getZoom() {
		String t = zoomFld.getText();
		try {
			return Double.valueOf(t).doubleValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	public void setValues(int threads, int dots, double zoom) {
		threadsFld.setText("" + threads);
		dotsFld.setText("" + dots);
		zoomFld.setText("" + zoom);
		repaint();
	}
}