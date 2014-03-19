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

	public void init()
	{
		setLayout(new BorderLayout());

		fractal = new HopCanvas(this);
		controls = new ControlPanel(this);

        add("South", controls);		
        add("Center", fractal);
		
		controls.setValues(initThreads, initDots, initZoom);
    }
	
	public void stop() {
		if (thread != null)
			thread.quit();
	}		

	public void destroy() {
		if (thread != null)
			thread.quit();
	}
		
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

	// called by the HopCanvas.paint() method to kick off thread
	public void hopalong() {		
		if (thread == null) {
			thread = new CoordThread(this, fractal);
			thread.setup();
			thread.start();
		}
	}	

	public void nullifyThread() {
		thread = null;
		System.out.println("Threads killed");
	}
} 


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

	public void setup() {
        a = maxRange*(Math.random()-0.5);
        b = maxRange*(Math.random()-0.5);
        c = maxRange*(Math.random()-0.5);
		
		ia = Math.random() - .5;
		ib = Math.random() - .5;
		ic = Math.random() - .5;
		colorSteps = numThreads * 3 / 2 + 30;
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
	
	public void update(int numThreads, int numDots, double zoom) {
		updating = true;		
		this.numThreads = numThreads;
		this.numDots = numDots;
		this.zoom = zoom;
		hue = 0;
		colorSteps = numThreads * 3 / 2 + 30;
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

	public void quit() {
		quit = true;
	}
}


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
	
	double sign(double a) {
		if (a == 0) return 0;
		return (double)((a<0)?-1:1);
	}
}

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

	public void setValues(int threads, int dots, double zoom) {
		threadsFld.setText("" + threads);
		dotsFld.setText("" + dots);
		zoomFld.setText("" + zoom);
		repaint();
	}
	
	public int getNumThreads() {
		String t = threadsFld.getText();
		try {
			return Integer.valueOf(t).intValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public int getNumDots() {
		String t = dotsFld.getText();
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
}
