package heat;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import myutil.*;
import myutil.Format;

public class Heat extends Applet implements ActionListener
{

  TextField Top, Bottom, Right, Left, Table, X, Y;

  Button HeatButton, Stop;

  HeatCanvas hc;


  public static void main(String[] args)
    {
      new AppletFrame(new Heat(), 350, 700);
    }

  public void init()
    {
      setLayout(new FlowLayout(FlowLayout.LEFT));
      setBackground(Color.white);

      Top = new TextField("100", 5);
      Bottom = new TextField("80", 5);
      Right = new TextField("0", 5);
      Left = new TextField("0", 5);
      Table = new TextField("32", 5);
      X = new TextField("80", 5);
      Y = new TextField("80", 5);

      GridPanel gp = new GridPanel();

      gp.add(new Label("Top Temperature:"), 1, 1);
      gp.add(Top, 2, 1);

      gp.add(new Label("Bottom Temperature:"), 1, 2);
      gp.add(Bottom, 2, 2);

      gp.add(new Label("Right Temperature:"), 3, 1);
      gp.add(Right, 4, 1);

      gp.add(new Label("Left Temperature:"), 3, 2);
      gp.add(Left, 4, 2);

      gp.add(new Label("Plate Temperature:"), 1, 4);
      gp.add(Table, 2, 4);

      gp.add(new Label("X Panels:"), 1, 3);
      gp.add(X, 2, 3);

      gp.add(new Label("Y Panels:"), 3, 3);
      gp.add(Y, 4, 3);

      HeatButton = new Button("Begin Heating");
      Stop = new Button("Stop Heating");
      HeatButton.addActionListener(this);
      Stop.addActionListener(this);
      gp.add(HeatButton, 1, 5, 2, 1);
      gp.add(Stop, 3, 5, 2, 1);

      add(gp);

      hc = new HeatCanvas();
      add(hc);
      hc.setSize(300, 300);
    }

  public void actionPerformed(ActionEvent event)
    {
      Object source = event.getSource();

      if (source == HeatButton) {
	double top = SL.atof(Top.getText());
	double bottom = SL.atof(Bottom.getText());
	double right = SL.atof(Right.getText());
	double left = SL.atof(Left.getText());
	double plate = SL.atof(Table.getText());
	int xp = SL.atoi(X.getText());
	int yp = SL.atoi(Y.getText());
	hc.setData(top, bottom, right, left, plate, xp, yp);
	hc.start();
      } else
      if (source == Stop) {
	hc.stop();
      }

    }

  public void stop()
    {
       hc.stop();
    }

}

class HeatCanvas extends Canvas implements Runnable
{

  double top, bottom, right, left, plate;



  int xp;  //number of different points along x-axis
  int yp;  //number of different points along y-axis


  double[][] u;
  double[][] u_new;

  double maxtemp = -100000;
  double mintemp = 100000;

  int time;

  private Thread myThread = null;
  private volatile boolean running = false;

  public void start() {
	  if (myThread == null) {
		  running = true;
		  myThread = new Thread(this, "Heat");
		  myThread.start();
	  }
  }

  public void stop()
  {
	  running = false;
	  myThread = null;
  }

  public void setData(double top, double bottom, double right, double left,
		   double plate, int xp, int yp)
    {
      int i, j;

      this.top = top;
      this.bottom = bottom;
      this.right = right;
      this.left = left;
      this.plate = plate;
      this.xp = xp;
      this.yp = yp;

      u = new double[xp+1][yp+1];
      u_new = new double[xp+1][yp+1];

      for(i = 1; i < xp; i++) {
	for (j = 1; j < yp; j++) {
	  u[i][j] = plate;
	}
      }

      for (i = 0; i < xp; i++) {
	u[i][ 0] = bottom;
	u[i][yp] = top;
      }

      for (j = 0; j < yp; j++) {
	u[xp][j] = right;
	u[ 0][j] = left;
      }

      maxtemp = -100000;
      mintemp = 100000;

      for (i = 0; i < xp; i++) {
	  if (u[i][yp] > maxtemp) { maxtemp = u[i][yp]; }
	  if (u[i][yp] < mintemp) { mintemp = u[i][yp]; }
	  if (u[i][ 0] > maxtemp) { maxtemp = u[i][ 0]; }
	  if (u[i][ 0] < mintemp) { mintemp = u[i][ 0]; }
      }
      for (j = 0; j < yp; j++) {
	  if (u[ 0][j] > maxtemp) { maxtemp = u[ 0][j]; }
	  if (u[ 0][j] < mintemp) { mintemp = u[ 0][j]; }
	  if (u[xp][j] > maxtemp) { maxtemp = u[xp][j]; }
	  if (u[xp][j] < mintemp) { mintemp = u[xp][j]; }
      }
      if (plate > maxtemp) {maxtemp = plate;}
      if (plate < mintemp) {mintemp = plate;}
      System.out.println("plate = "+plate);
      System.out.println("maxtemp = "+maxtemp);
      System.out.println("mintemp = "+mintemp);

      time = 0;
    }

  public void run()
    {
      int i, j;

      while (running) {

	time++;

	paint(getGraphics());

	try{Thread.sleep(12);} catch(InterruptedException ie){};

	for (i = 1; i < xp; i++) {
	  for (j = 1; j < yp; j++) {
	    u_new[i][j] = (u[i+1][j] + u[i-1][j] + u[i][j+1] + u[i][j-1])/4;
	  }
	}

	for (i = 1; i < xp; i++) {
	  for (j = 1; j < yp; j++) {
	    u[i][j] = u_new[i][j];
	  }
	}

      }
    }

  public int reltemp(int i, int j)
    {
      double d;

      int t;

      d = (u[i][j] - mintemp) * (255 /(maxtemp - mintemp));

      t = (int) d;

      return t;
    }



  public void paint  (Graphics g)
    {


      GL.ginit(getGraphics(), getSize().width, getSize().height, this);
      GL.ortho2(-0.1*xp, 1.1*(xp), -0.1*yp, 1.1*(yp));
      GL.color(Color.black);
      GL.clear();

      Color code;

      int i, j;
      int t, mid;

      Color red = new Color(255, 0, 0);
      Color blue = new Color(0, 0, 255);

      if (u == null) { return;}

      int X = ((int) xp / 2);
      int Y = ((int) yp / 2);

      mid = reltemp(X, Y);

      GL.color(red);
      GL.drawString("Max:" + maxtemp, (-.1*xp) + .5, (1.1*yp) - 1,
		    "LEFT", "TOP");

      GL.color(blue);
      GL.drawString("Min:" + mintemp, .75*xp, (1.1*yp) - 1, "LEFT", "TOP");

      Color middle = new Color(mid, 0, 255 - mid);
      GL.color(middle);
      GL.drawString("Mid:" + Format.floating(6,2,u[X][Y]),
		      (-.1*xp) + .5, (-.1*yp) + .5, "LEFT", "BOTTOM");

      GL.color(Color.white);
      GL.drawString("Time:" + time, .75*xp, (-.1*yp) + .5, "LEFT", "BOTTOM");

      for (i = 0; i <= xp; i++) {
	for (j = 0; j <= yp; j++) {
	  t = reltemp(i, j);
	  code = new Color(t, 0, 255 - t);
	  GL.color(code);
	  GL.rectf(i-0.5,j-0.5,i+0.5,j+0.5);
	}
      }

      GL.color(Color.black);

      GL.rectf(-0.5,-0.5,0.5,0.5);
      GL.rectf(xp-0.5,-0.5,xp+0.5,0.5);
      GL.rectf(xp-0.5,yp-0.5,xp+0.5,yp+0.5);
      GL.rectf(-0.5,yp-0.5,0.5,yp+0.5);

      GL.swapbuffers();
    }

}
