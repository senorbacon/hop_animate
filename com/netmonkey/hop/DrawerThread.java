package com.netmonkey.hop;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

class DrawerThread extends Thread
{
	CoordThread coord;
	HopCanvas fractal;
	
	double a, b, c;
	double ao, bo, co;
	double scaleFactor, sfo;

	Color color;
		
	int windowSize;
	int xc, yc; 		// center of window;

	public long turn = 0;
		
	public DrawerThread(CoordThread coord, HopCanvas fractal) {
		this.coord = coord;
		this.fractal = fractal;
	}
	public void run() {
		int xs = 0, ys = 0;  			// screen coords
		double x = 0, y = 0;			// actual coords
		double xp;

		while (true) {
			xs = 0;
			ys = 0;
			x = 0;
			y = 0;
			
			Graphics g = fractal.getGraphics();
			if (g != null) {

				// erase previous drawing
				g.setColor(Color.black);
				
				for (int i=0; i<coord.numDots; i++) {		
					xp = y - (sign(x) * Math.sqrt(Math.abs(bo * x - co)));
					y = ao - x;
					x = xp;
					
					xs = (int) (x * sfo) + xc;
					ys = yc - (int) (y * sfo);
		
					g.drawLine(xs, ys, xs, ys);
				}
	
				// make new drawing 
				if (!(coord.updating || coord.quit)) {
					g.setColor(color);
					xs = 0;
					ys = 0;
					x = 0;
					y = 0;
					
					for (int i=0; i<coord.numDots; i++) {		
						xp = y - (sign(x) * Math.sqrt(Math.abs(b * x - c)));
						y = a - x;
						x = xp;
						
						xs = (int) (x * scaleFactor) + xc;
						ys = yc - (int) (y * scaleFactor);
			
						g.drawLine(xs, ys, xs, ys);
					}
				}
							
				g = null;
			}

			if (coord.quit) break;
												
			synchronized(this) {
				turn++;
				try {
					notify(); 	// wake up coord thread if it was waiting for us
					wait();		// now we're done, so wait until coord thread needs us again
				} catch (Exception e) {}
			}
		}
		coord.deadThreads++;
		System.out.println("Drawer threads dead:" + coord.deadThreads);
		synchronized (this) {
			notify();
		}
	}
	public void setup(double a, double b, double c, Color color) {
		ao = this.a;
		bo = this.b;
		co = this.c;
		sfo = scaleFactor;
		
		this.a = a;
		this.b = b;
		this.c = c;
		this.color = color;

		Dimension d = fractal.getSize();
		windowSize = d.width;
		xc = d.width/2;
		yc = d.height/2;
		scaleFactor = (double)windowSize / (Math.sqrt(Math.abs(a * b * c)) * 25.0) * coord.zoom / 100f;
	}
	double sign(double a) {
		if (a == 0) return 0;
		return (double)((a<0)?-1:1);
	}
}