package com.netmonkey.hop;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

class CoordThread extends Thread
{
	public DrawerThread threads[] = new DrawerThread[MovingHop.MAX_THREADS];
	MovingHop applet;
	HopCanvas fractal;

	public int numThreads = MovingHop.initThreads;
	public int numDots = MovingHop.initDots;
	public double zoom = MovingHop.initZoom;
	public boolean paused = false;
	public boolean updating = false;

	public boolean quit = false;
	public int deadThreads = 0;

	// turn keeps all threads on sync;
	// index refers to which thread on a given turn
	long turn = 0;
	int index = 0;
	
	int hue;
	int colorSteps;
	
	double a, b, c;
	double ia, ib, ic; 	// randomly changing incrementals

	// these three factors affect the morphing of a, b, c
	double morphSize = .01;
	double randomStepSize = .4;
	double maxRange = 3f;
	
	int windowSize;
	int xc, yc; 		// center of window;
	
	public CoordThread(MovingHop applet, HopCanvas fractal) {
		this.applet = applet;
		this.fractal = fractal;
	}
	void morphABC() {
		ia += (Math.random() - .5) * randomStepSize;
		ib += (Math.random() - .5) * randomStepSize;
		ic += (Math.random() - .5) * randomStepSize;
		normalize();
		
		if ((a*a + b*b + c*c) > (maxRange * maxRange)) {
			ia = -ia;
			ib = -ib;
			ic = -ic;
		}
		
		a += ia*morphSize;
		b += ib*morphSize;
		c += ic*morphSize;
	}
	void normalize() {
		double size = Math.sqrt(ia*ia + ib*ib + ic*ic);
		ia /= size;
		ib /= size;
		ic /= size;
	}
	public void quit() {
		quit = true;
	}
	// give each drawer thread it's own copy of a, b, and c, and a color and let
	// it run, pausing until the current drawer thread is ready for a new set.
	public void run() {
		DrawerThread current;

		index = 0;
		hue = 0;
		Color color;

		boolean firstRun = false;
		
		while (true) {
			firstRun = false;
			
			if (paused && !quit) {
				synchronized (this) {
					try {
						wait();
					} catch (Exception e) {}
				}
			}
			
			if (quit) break;
			
			morphABC();

			hue = (hue+1)%colorSteps;
			color = Color.getHSBColor((float)hue/(float)colorSteps,1f,1f);
		
			if (threads[index] == null) {
				threads[index] = new DrawerThread(this, fractal);
				firstRun = true;
				threads[index].turn = turn;
			} 
			
			current = threads[index];
				
			synchronized (current) {
				// if drawer thread's turn # has been updated and equals ours, it's ready
				// otherwise, we wait for it to finish executing
				if (!firstRun && current.turn != turn) {
					// if user lowers # of threads, then raises it back up,
					// those old threads will have been waiting for turns
					if (current.turn != turn-1) {
						//System.out.println("Re-using old thread");
						current.turn = turn;
					} else {
						try {
							// current drawer thread is busy, 
							// and will wake us up when it's done
							current.wait();
						} catch (Exception e) {}
					}
				}
				
				// current drawer thread ready and waiting
				current.setup(a, b, c, color);
				
				if (firstRun)
					current.start();
				else
					current.notify();
			}
			
			if (++index >= numThreads) {
				index = 0;
				turn++;
				updating = false;
			}			
		}

		// quit... cleanup threads by letting them all die, cleaning up as they go
		for (int i=0; i<MovingHop.MAX_THREADS; i++) {
			DrawerThread thread = threads[i];
			if (thread == null) break;
			synchronized (thread) {
				thread.setup(0, 0, 0, Color.black);
				thread.notify();
			}
		}

		applet.nullifyThread();
	}
	public void setup() {
		a = maxRange*(Math.random()-0.5);
		b = maxRange*(Math.random()-0.5);
		c = maxRange*(Math.random()-0.5);
		
		ia = Math.random() - .5;
		ib = Math.random() - .5;
		ic = Math.random() - .5;
		colorSteps = numThreads * 3 / 2 + 30;
	}
	public void update(int numThreads, int numDots, double zoom) {
		updating = true;		
		this.numThreads = numThreads;
		this.numDots = numDots;
		this.zoom = zoom;
		hue = 0;
		colorSteps = numThreads * 3 / 2 + 30;
	}
}