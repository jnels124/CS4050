/*  algorithm to read LP's modeling TSP problems with
    various extra constraints added
*/

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import java.util.*;
import java.io.*;

public class RandomTestingHeuristicTSP extends Basic
{

//==================================================================================================

    // number of points in random test to be generated
    private static int NUMBER;

    // total size of the window in pixels
    private static final int pixelWidth = 700, pixelHeight = 700;  // total window size in pixels

    // amount to shift drawing area to the right and down to not hit the title bar or window borders
    private static final int windowHorizOffset = 15, windowVertOffset = 50,
            rightMargin = 5, bottomMargin = 5;

    private static Color textColor = new Color( 0, 0, 255 );
    private static Color gridColor = new Color( 180, 255, 180 );
    private static double gridSize = 0.05;

    private static final double tiny = 0.0000001;

//==================================================================================================

    public static void main(String[] args)
    {
        if( args.length != 1 )
        {
            System.out.println("Usage:  java RandomTestingHeuristicTSP 20\n" +
                    "for 20 points");
            System.exit(1);
        }
        NUMBER = Integer.parseInt( args[0] );
        RandomTestingHeuristicTSP bf;
        bf = new RandomTestingHeuristicTSP("Random Test Heuristic TSP", 0, 0,
                pixelWidth, pixelHeight );
    }

    // instance variables for the application:
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    private Tableau table;  // the tableau being used

    private boolean useGrid;  // snap mouse to grid and show grid, or free form
    private boolean showLabels;  // show vertex numbers

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    public RandomTestingHeuristicTSP( String title, int ulx, int uly, int pw, int ph  )
    {
        super(title,ulx,uly,pw,ph);

        try{
            Random rng;
            rng = new Random();
            int seed = rng.nextInt( 1000 ) + 1;
            System.out.println("random seed is " + seed );
            rng = new Random( seed );

            table = new Tableau( rng, NUMBER, 1000 );  // 1000 is the max simplex step
            table.doSimplexMethod();
        }
        catch(Exception e)
        {
            System.out.println("failed to open tableau and solve LP");
            e.printStackTrace();
            System.exit(1);
        }

        useGrid = true;
        showLabels = true;

        setBackgroundColor( new Color( 128, 128, 128 ) );
        cameras.add( new Camera( windowHorizOffset, windowVertOffset,
                pixelWidth - windowHorizOffset - rightMargin,
                pixelHeight - windowVertOffset - bottomMargin,
                0, 1, 0, 1,
                Color.white ) );

        // start up the animation:
        super.start();
    }

    public void step()
    {
        Camera cam;

        cam = cameras.get(0);
        cam.activate();

        // draw grid if desired
        if( useGrid )
        {
            // draw grid to aid in selection
            double x=0, y=0;
            cam.setColor( gridColor );
            while( x <= 1 )
            {// draw vertical line at x
                cam.drawLine( x, 0, x, 1 );
                x += gridSize;
            }
            while( y <= 1 )
            {// draw horizontal line at y
                cam.drawLine( 0, y, 1, y );
                y += gridSize;
            }
        }// useGrid so show grid

        // draw the LP optimal
        // (look for variables of form xk,j > 0, draw .5, 1 different colors)


        String[] vars = table.getBasicVariableNames();
        double[] vals = table.getBasicVariableValues();
        double[][] pts = table.getPoints();

        // draw the points

        double displayScale = 100;

        double vertSize = 0.005;
        cam.setColor( Color.black );
        for( int k=0; k<pts.length; k++ )
        {
            double px = pts[k][0]/displayScale, py = pts[k][1]/displayScale;

            cam.fillRect( px-vertSize, py-vertSize, 2*vertSize, 2*vertSize );
            if( showLabels )
            {
                cam.drawText( "" + (k+1), px, py+2*vertSize );
            }
        }

        // draw the edges corresponding to xk,j > 0 (and compute the cost)

        double cost = 0;

        for( int m=0; m < vars.length; m++ )
        {
            if( vars[m].charAt(0) == 'x' &&
                    vals[m] > tiny
                    )
            {// pull out the indices and draw the edge

                String w = vars[m];
                int comma = w.indexOf( ',' );
                int k = Integer.parseInt( w.substring(1,comma) );
                int j = Integer.parseInt( w.substring(comma+1) );

                double x1 = pts[k-1][0]/displayScale, y1 = pts[k-1][1]/displayScale;
                double x2 = pts[j-1][0]/displayScale, y2 = pts[j-1][1]/displayScale;

                cost += vals[m] * Math.hypot( x1-x2, y1-y2 );

                if( Math.abs( vals[m] - 0.5 ) < tiny )
                    cam.setColor( Color.red );
                else if( Math.abs( vals[m] - 1 ) < tiny )
                    cam.setColor( Color.blue );
                else if( vals[m] < -tiny )
                    cam.setColor( Color.yellow );
                else if( vals[m] > 1+tiny )
                    cam.setColor( Color.green );

                cam.drawLine( x1, y1, x2, y2 );
            }
        }

        // show the cost
        cam.setColor( Color.magenta );
//    cam.drawText( "" + cost*20, .05, 0.95 );
        cam.drawText( "" + table.getOptimalCost(), .05, 0.9 );

    }

    public void keyTyped( KeyEvent e )
    {
        char key = e.getKeyChar();

        if( key == 'g' )
        {
            useGrid = !useGrid;
        }

        else if( key == 'l' )
        {
            showLabels = !showLabels;
        }

    }// keyTyped

    public void keyPressed( KeyEvent e )
    {
        int code = e.getKeyCode();

    }// keyPressed

    public void mouseMoved(MouseEvent e)
    {
        super.mouseMoved(e);
    }

    public void mouseDragged(MouseEvent e)
    {
        super.mouseDragged(e);
    }

    public void mouseClicked(MouseEvent e)
    {
        super.mouseClicked(e);
    }

    public void mousePressed(MouseEvent e)
    {
        super.mousePressed(e);

    }

    private int nearestInt( double a )
    {
        return (int) Math.round( a );
    }

    public void mouseReleased(MouseEvent e)
    {
        super.mouseReleased(e);
    }

    public void mouseEntered(MouseEvent e)
    {
        super.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e)
    {
        super.mouseExited(e);
    }

}