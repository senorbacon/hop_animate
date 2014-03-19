import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

/**
 * Moving hopalong fractal.
 * @author Terren Suydam
 * 99/2/17
 */


public class MovingHop extends Applet
	implements ActionListener
{
	public CoordThread thread; 
	HopCanvas fractal;
	ControlPanel controls;
	
	public static String THREAD_FLD = "Threads";
	public static String DOTS_FLD = "Dots";
	public static String ZOOM_FLD = "Zoom";
	public static String MORPH_FLD = "Morph";
	
	public static String PLAY = "Play";
	public static String PAUSE = "Pause";
	public static String RESTART = "Restart";
	public static String QUIT = "Quit";
	public static String UPDATE = "Update";

	public static final int initThreads = 10;
	public static final int initDots = 300;
	public static final double initZoom = 100;
	public static final double initMorph = .01;

	public static final int MAX_THREADS = 100;
	public static final int MAX_DOTS = 1000;
	
	public void init()
	{
		setLayout(new BorderLayout());

		fractal = new HopCanvas(this);
		controls = new ControlPanel(this, fractal);

        add("South", controls);		
        add("Center", fractal);
		
		controls.setValues(initThreads, initDots, initZoom, initMorph);
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
			if (thread != null) {
				synchronized (thread) {
					thread.paused = false;
					thread.notify();
				}
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
			thread.debug();
			// advance one 'frame' if already paused
			synchronized (thread) {
				thread.notify();
			}
			
		} else if (MovingHop.QUIT.equals(command)) {
			thread.quit();
			
		} else if (MovingHop.UPDATE.equals(command)) {	
			thread.update(controls.getNumThreads(),
						  controls.getNumDots(),
						  controls.getZoom(),
						  controls.getMorph());
			fractal.repaint();
		}
	}
		
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
	MovingHop applet;
	HopCanvas fractal;

	public DrawerThread threads[] = new DrawerThread[MovingHop.MAX_THREADS];
	int coords1[] = new int[MovingHop.MAX_DOTS * 2];
	int coords2[] = new int[MovingHop.MAX_DOTS * 2];

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
	int colorSteps = 100;

	final int skipFirst = 5;
	
	double a, b, c;
	double ia, ib, ic;

	// these three factors affect the morphing of a, b, c
	double morphSize = MovingHop.initMorph;
	double randomStepSize = .4;
	double maxRange = 3f;
	
	double scaleFactor;
	
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

		Dimension d = fractal.getSize();
		windowSize = d.width;
		xc = d.width/2;
		yc = d.height/2;
        scaleFactor = (double)windowSize / (Math.sqrt(Math.abs(a * b * c)) * 25.0) * zoom / 100f;
	}
	
	void morphABC() {
		ia += (Math.random() - .5) * randomStepSize;
		ib += (Math.random() - .5) * randomStepSize;
		ic += (Math.random() - .5) * randomStepSize;
		normalize();
		
		double dist = Math.sqrt(a*a + b*b + c*c);
		if (dist > maxRange) {
			ia = -ia;
			ib = -ib;
			ic = -ic;

			double scale = dist/maxRange;
			a /= scale;
			b /= scale;
			c /= scale;
		}
		
		a += ia*morphSize;
		b += ib*morphSize;
		c += ic*morphSize;
        scaleFactor = (double)windowSize / (Math.sqrt(Math.abs(a * b * c)) * 25.0) * zoom / 100f;
	}
	
	void normalize() {
		double size = Math.sqrt(ia*ia + ib*ib + ic*ic);
		ia /= size;
		ib /= size;
		ic /= size;
	}

	public void debug() {
		System.out.println("a = " + a);
		System.out.println("b = " + b);
		System.out.println("c = " + c);
	}
	
	public void update(int numThreads, int numDots, double zoom, double morph) {
		updating = true;		
		this.numThreads = numThreads;
		this.numDots = numDots;
		this.zoom = zoom;
		this.morphSize = morph;
		hue = 0;
		colorSteps = numThreads * 2 + 10;
	}

	public void quit() {
		quit = true;
		System.out.println("Shutting down...");
	}

	void computeCoords(int[] coords) {
		double x=0, y=0, xp;
		int xs, ys;

		for (int i=-skipFirst; i<numDots; i++) {
			xp = y - (sign(x) * Math.sqrt(Math.abs(b*x - c)));
			y = a - x;
			x = xp;
			
			if (i < 0) continue;
			
			xs = (int) (x*scaleFactor) + xc;
			ys = yc - (int) (y*scaleFactor);
			coords[i*2] = xs;
			coords[i*2+1] = ys;
		}
	}
				
	public void run() {
		DrawerThread current;
		boolean firstRun = false;

		int[] temp;

		index = 0;
		hue = 0;
		Color color;

		computeCoords(coords2);
		
		while (true) {
			firstRun = false;
			
			if (paused && !quit) {
				synchronized (this) {
					try {
						wait();
					} catch (Exception e) {}
				}
			}
			
			if (quit) 
				break;
			
			morphABC();

			temp = coords1;
			coords1 = coords2;
			coords2 = temp;
			computeCoords(coords2);

			hue = (hue+1)%colorSteps;
            color = Color.getHSBColor((float)hue/(float)colorSteps,1f,1f);
		
			if (threads[index] == null) {
				threads[index] = new DrawerThread(this, fractal);
				firstRun = true;
				threads[index].turn = turn;
			} 
			
			current = threads[index];
				
			synchronized (current) {
				if (!firstRun && current.turn != turn) {
					if (current.turn != turn-1) {
						current.turn = turn;
					} else {
						try {
							// current thread busy, will wake this up when done
							current.wait();
						} catch (Exception e) {}
					}
				}
				
				// current thread ready and waiting
				current.setup(coords1, coords2, color);
				
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
			//try{
			//	sleep(500);
			//} catch(Exception e) {}
		}
		
		// exit run loop
		for (int i=0; i<MovingHop.MAX_THREADS; i++) {
			if (threads[i] == null) break;
			synchronized (threads[i]) {
				threads[i].setup(coords1, coords2, Color.black);
				threads[i].notify();
			}
		}
		
		applet.nullifyThread();
	}

	double sign(double a) {
		if (a == 0) return 0;
		return (double)((a<0)?-1:1);
	}
}


class DrawerThread extends Thread
{
	CoordThread coord;
	HopCanvas fractal;
	
	Color color;
		
	public long turn = 0;

	//int[] oldCoords1, oldCoords2;
	int[] newCoords1, newCoords2;
		
	public DrawerThread(CoordThread coord, HopCanvas fractal) {
		this.coord = coord;
		this.fractal = fractal;
		
		//oldCoords1 = new int[MovingHop.MAX_DOTS * 2];
		//oldCoords2 = new int[MovingHop.MAX_DOTS * 2];
		newCoords1 = new int[MovingHop.MAX_DOTS * 2];
		newCoords2 = new int[MovingHop.MAX_DOTS * 2];
	}

	public void setup(int[] coords1, int[] coords2, Color color) {
		//copyCoords(newCoords1, oldCoords1);
		//copyCoords(newCoords2, oldCoords2);
		copyCoords(coords1, newCoords1);
		copyCoords(coords2, newCoords2);
		this.color = color;
	}

	void copyCoords(int[] source, int[] dest) {
		for (int i=0; i<coord.numDots*2; i++)
			dest[i] = source[i];
	}
	
	public void run() {
		int x[] = new int[3];
		int y[] = new int[3];
		
		while (true) {
			Graphics g = fractal.getGraphics();

			if (g != null) {
				//g.setColor(Color.black);
				
				//for (int i=0; i<coord.numDots; i++)
				//	g.drawLine(oldCoords1[i*2], oldCoords1[i*2+1],
				//			   oldCoords2[i*2], oldCoords2[i*2+1]); 
	
				//synchronized(this) {
					if (!(coord.updating || coord.quit)) {
						g.setColor(color);

						for (int i=1; i<coord.numDots; i++) {
							x[0] = newCoords1[i*2-2];
							x[1] = newCoords1[i*2];
							x[2] = newCoords2[i*2];
							y[0] = newCoords1[i*2-1];
							y[1] = newCoords1[i*2+1];
							y[2] = newCoords2[i*2+1];
												
							color = Color.getHSBColor((float)Math.random(), (float)Math.random(), (float)Math.random());
							g.setColor(color);
							g.fillPolygon(x, y, 3);
						}
						
						//for (int i=0; i<coord.numDots; i++)
						//	g.drawLine(newCoords1[i*2], newCoords1[i*2+1],
						//			   newCoords2[i*2], newCoords2[i*2+1]); 
						//for (int i=1; i<coord.numDots; i++)
						//	g.drawLine(newCoords1[i*2-2], newCoords1[i*2-1],
						//			   newCoords1[i*2], newCoords1[i*2+1]); 
					}
					
					//g.setColor(Color.black);
					
					//for (int i=0; i<coord.numDots; i++)
					//	g.drawLine(newCoords1[i*2], newCoords1[i*2+1],
					//			   newCoords2[i*2], newCoords2[i*2+1]); 
					//for (int i=1; i<coord.numDots; i++)
					//		g.drawLine(newCoords1[i*2-2], newCoords1[i*2-1],
					//				   newCoords1[i*2], newCoords1[i*2+1]); 
					g = null;
				//}
			}

			if (coord.quit) break;
												
			synchronized(this) {
				turn++;
				try {
					notify(); 	// wake up coord thread if waiting
					wait();
				} catch (Exception e) {}
			}
		}

		coord.deadThreads++;
		System.out.println("Drawer threads dead:" + coord.deadThreads);
		synchronized (this) {
			notify();
		}
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
	MovingHop applet;
    HopCanvas fractal;

	TextField threadsFld;
	TextField dotsFld;
	TextField zoomFld;
	TextField morphFld;
	
    public ControlPanel(MovingHop applet, HopCanvas fractal)
    {
		this.applet = applet;
        this.fractal = fractal;
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


		panel = new Panel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(Color.gray);

	 	panel.add("North", new Label(MovingHop.MORPH_FLD + ":"));
		morphFld = new TextField(5);
		panel.add("South", morphFld);
		add(panel);


        button = new Button(MovingHop.UPDATE);
		button.setActionCommand(MovingHop.UPDATE);
		button.addActionListener(applet);
		add(button);
    }
	
	public void setValues(int threads, int dots, double zoom, double morph) {
		threadsFld.setText("" + threads);
		dotsFld.setText("" + dots);
		zoomFld.setText("" + zoom);
		morphFld.setText("" + morph);
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

	public double getMorph() {
		String t = morphFld.getText();
		try {
			return Double.valueOf(t).doubleValue();
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}

