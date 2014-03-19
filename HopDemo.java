import java.lang.Math;
import java.awt.*;
import java.applet.Applet;
/**
 * This applet draws a random fractal image based on
 * Barry Martin's "Hopalong" mapping.
 * @author David Imai
 * 98/11/14
 */

public class HopDemo extends Applet
{
    public void init()
    {
        //String s = this.getParameter("scrWidthParam");
        //public int scrWidth = Integer.parseInt(s);
        //width of screen in pixels
        setLayout(new BorderLayout());
 //     TextArea showParam = new TextArea();
        PicCanvas picture = new PicCanvas();
        ControlPanel hopControls = new ControlPanel(picture);
 //     add("North", showParam);
        add("Center", picture);
        add("South", hopControls);
        picture.setparam();

    }
} //end class HopDemo

class PicCanvas extends Canvas
{
    int scrWidth = 600;  //width of image in pixels
    double a,b,c;        //parameters that determine shape
    double diam;         //area displayed
    double pixsize;      //length & width of one pixel
    double x0, y0;       //bottom right corner
    double x,y,xx;       //
    int u, v;            //position of dot
    float hue;           //color of dot
    int numDots = 2400;   //number of dots of each color
	int numColors = 100;
    Color bgcolor = Color.black;  // background color

    public void setparam()
    {
        //choose values of parameters a,b,c
        a = 8*(Math.random()-0.5);
        b = 4*(Math.random()-0.5);
        c = 8*(Math.random()-0.5);
 //+++++++++++++++++++++++++++++++++++++++++++++++
        diam = Math.sqrt(Math.abs(a*b*c))*32;  //width of area displayed
        pixsize = diam/((double)scrWidth); //size of 1 pixel
    }

    public void paint(Graphics g)
    {
        x = 0;
        y = 0;
        x0 = -0.5 * diam;
        y0 = 0.5 * (a + diam);  //center of screen is at (0,a).
        u = 0;
        v = 0;
        g.setColor(bgcolor);
        g.fillRect(0,0,scrWidth,scrWidth);

        for(int colorNum=0; colorNum<numColors; colorNum++)
        {
            hue = ((float)colorNum)/(float)numColors;
            g.setColor(Color.getHSBColor(hue,1f,1f));
            //draw numDots of this color.
            for(int i=1; i<numDots; i++)
            {
                xx = y - (this.sign(x) * Math.sqrt(Math.abs(b * x - c)));
                y = a - x;
                x = xx;
                u = (int)((x - x0)/pixsize);
                v = (int)((y0 - y)/pixsize);
                g.drawLine(u,v,u,v); //one point
            };
        };
        //g.setColor(Color.white);
        //g.drawString(("a="+a+"  b="+b+"  c="+c), 8,10);
    }   //end paint

    public void redraw(int buttonMsg)
    {
        if (buttonMsg == 1) // New image
        {   this.setparam();
//          showParam.redraw(a,b,c);
        }
        else if (buttonMsg == 2)  // Zoom in
        {   diam = diam/2;
            pixsize = pixsize/2;
        }
        else if (buttonMsg == 3)  // Zoom out
        {   diam = diam*2;
            pixsize = pixsize*2;
        }
        repaint();
    }  // end redraw

    public double sign(double x1)
    {
        if (x1 >= 0.0) return 1.0;
        else return -1.0;
    }  // end sign

} // end class HopCanvas

/* **********************
class TextArea extends Canvas
{
    public void redraw(double a,double b, double c)
    {
        Graphics g;
        g.setColor(Color.black);
        g.drawString(("a="+a+"  b="+b+"  c="+c), 8,10);
    }
}  // end class TextArea
************************ */

class ControlPanel extends Panel   // contains buttons
{
    PicCanvas c;

    public ControlPanel(PicCanvas c)
    {
        this.c = c;
        add(new Button("New Image"));
        add(new Button("Zoom In"));
        add(new Button("Zoom Out"));
    }

    public boolean action(Event ev, Object arg)
    // check which button is pressed
    {   if (ev.target instanceof Button)
        {
            if ("New Image".equals((String)arg))
            {
                c.redraw(1);
            }
            if ("Zoom In".equals((String)arg))
                c.redraw(2);
            if ("Zoom Out".equals((String)arg))
               c.redraw(3);
            return true;
        }
        return false;
    } // end action

}  // end class ControlPanel

