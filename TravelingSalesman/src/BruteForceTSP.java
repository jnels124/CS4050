/*  toy (only works for rather small n)
    demonstration of brute force TSP solution
*/

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import java.util.*;
import java.io.*;

public class BruteForceTSP extends Basic
{

    //==================================================================================================
    // total size of the window in pixels
    private static final int pixelWidth = 700, pixelHeight = 700;  // total window size in pixels

    // amount to shift drawing area to the right and down to not hit the title bar or window borders
    private static final int windowHorizOffset = 15, windowVertOffset = 50,
            rightMargin = 5, bottomMargin = 5;

    private static Color textColor = new Color( 0, 0, 255 );
    private static Color gridColor = new Color( 180, 255, 180 );
    private static double gridSize = 0.05;

//==================================================================================================

    public static void main(String[] args)
    {
        BruteForceTSP bf;
        if( args.length == 1 )
            bf = new BruteForceTSP("Brute Force TSP", 0, 0, pixelWidth, pixelHeight, args[0] );
        else
            bf = new BruteForceTSP("Brute Force TSP", 0, 0, pixelWidth, pixelHeight, "" );
    }

    // instance variables for the application:
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    private ArrayList<Pair> pts;  // the vertex locations
    private ArrayList<ArrayList<Integer>> perms;

    private String state;   // state of the app
    private int currentPerm;  // which perm we are showing while "solving"
    private boolean useGrid;  // snap mouse to grid and show grid, or free form
    private boolean showLabels;  // show vertex numbers

    private ArrayList<Integer> bestPerm;  // index of best perm seen so far
    private double bestScore;  // score for that perm

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    public BruteForceTSP( String title, int ulx, int uly, int pw, int ph, String fileName )
    {
        super(title,ulx,uly,pw,ph);

        pts = new ArrayList<Pair>();
        state = "choosing";
        useGrid = true;
        showLabels = true;
        perms = new ArrayList<ArrayList<Integer>>();
        currentPerm = 0;
        bestPerm = null;

        // if command line argument is there, load those points
        if( ! fileName.equals("") )
        {
            try{
                Scanner input = new Scanner( new File( fileName ) );
                int num = input.nextInt();  input.nextLine();

                String format = input.nextLine();
                if( ! format.equals( "points" ) )
                {
                    System.out.println("data file needs \"points\" after # of points");
                    System.exit(1);
                }

                for( int k=0; k<num; k++ )
                {
                    pts.add( new Pair( input.nextDouble()/100, input.nextDouble()/100 ) );
                }
                input.close();
            }
            catch(Exception e)
            {
                System.out.println("Couldn't load points from file [" + fileName + "]" );
                e.printStackTrace();
                System.exit(1);
            }
        }

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

        // draw the vertices
        double vertSize = 0.005;
        cam.setColor( Color.black );
        for( int k=0; k<pts.size(); k++ )
        {
            Pair p = pts.get(k);
            cam.fillRect( p.x-vertSize, p.y-vertSize, 2*vertSize, 2*vertSize );
            if( showLabels )
            {
                cam.drawText( "" + (k+1), p.x, p.y+2*vertSize );
            }
        }

        if( state.equals( "choosing" ) )
        {
        }

        else if( state.equals( "solving" ) )
        {// show current perm and best seen so far
            show( cam, textColor, perms.get( currentPerm ) );
        }

        else if( state.equals( "optimal" ) )
        {// show optimal tour
            show( cam, Color.red, bestPerm );
        }

    }

    // draw in the graphics window the edges represented
    // by perm---a permutation of 1...n-1 (0 is viewed as first point
    // in the tour)
    private void show( Camera cam, Color tourColor, ArrayList<Integer> perm )
    {
        // display perm (using 1...n notation) and its score
        String text = "1 ";
        for( int k=0; k<perm.size(); k++ )
        {
            text += (perm.get(k)+1) + " ";
        }

        Pair one, two;
        double score;

        cam.setColor( tourColor );

        // draw edge from 0 to perm[1]
        one = pts.get(0);  two = pts.get( perm.get(0) );
        cam.drawLine( one.x, one.y, two.x, two.y );
        score = cost( one, two );

        // draw edges along perm:
        for( int k=0; k<perm.size()-1; k++ )
        {
            one = pts.get( perm.get(k) );  two = pts.get( perm.get(k+1) );
            cam.drawLine( one.x, one.y, two.x, two.y );
            score += cost( one, two );
        }

        // draw edge from last in perm to 0
        one = pts.get( perm.get( perm.size()-1 ) );  two = pts.get(0);
        cam.drawLine( one.x, one.y, two.x, two.y );
        score += cost( one, two );

        text += " Score: " + score;

        // update best:
        if( bestPerm==null || score < bestScore )
        {
            bestPerm = perm;  bestScore = score;
        }

        text += "   " + "Best so far: 1 ";
        for( int k=0; k<bestPerm.size(); k++ )
        {
            text += (bestPerm.get(k)+1) + " ";
        }

        text += " with score: " + bestScore;

        cam.setColor( tourColor );
        cam.drawText( text, 0.01, 0.01 );
    }

    private double cost( Pair one, Pair two )
    {
        return Math.hypot( one.x-two.x, one.y-two.y );
    }

    private void finishSolving()
    {
        while( currentPerm < perms.size() )
        {
            Pair one, two;
            double score;
            ArrayList<Integer> perm = perms.get( currentPerm );

            // figure cost from 0 to first in perm
            one = pts.get(0);  two = pts.get( perm.get(0) );
            score = cost( one, two );

            // figure cost along edges of perm
            for( int k=0; k<perm.size()-1; k++ )
            {
                one = pts.get( perm.get(k) );  two = pts.get( perm.get(k+1) );
                score += cost( one, two );
            }

            // figure cost from last in perm back to 0
            one = pts.get( perm.get( perm.size()-1 ) );  two = pts.get(0);
            score += cost( one, two );

            if( score < bestScore )
            {
                bestScore = score;
                bestPerm = perm;
            }

            currentPerm++;
        }// loop

    }// finishSolving

    public void keyTyped( KeyEvent e )
    {
        char key = e.getKeyChar();

        if( key == 's' )
        {// solve the problem
            state = "solving";
            solveTSP();
        }

        else if( key == ' ' && state.equals( "solving" ) && currentPerm<perms.size()-1 )
        {// advance to next perm
            currentPerm++;
        }

        else if( key == 'o' && state.equals( "solving" ) )
        {// finish solving and show optimal
            finishSolving();
            state = "optimal";
        }

        else if( key == 'g' )
        {
            useGrid = !useGrid;
        }

        else if( key == 'l' )
        {
            showLabels = !showLabels;
        }

        else if( key == 'f' )
        {// make a file with the current points
            String name = FileBrowser.chooseFile( false );
            try{
                PrintWriter output = new PrintWriter( new File( name ) );
                output.println( pts.size() );
                output.println( "points" );
                for( int k=0; k<pts.size(); k++ )
                    output.println( 100*pts.get(k).x + " " + 100*pts.get(k).y );
                output.close();
            }
            catch(Exception e1)
            {
                System.out.println("Couldn't save to file [" + name + "]" );
                e1.printStackTrace();
                System.exit(1);
            }
        }// 'f'

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

        if( state.equals( "choosing" ) )
        {
            double x = getMouseX(), y = getMouseY();

            if( useGrid )
            {// move (x,y) to nearest grid point
                x = nearestInt( x/gridSize ) * gridSize;
                y = nearestInt( y/gridSize ) * gridSize;
            }

            pts.add( new Pair( x, y ) );

        }// choosing

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

    private void solveTSP()
    {
        // recursively generate all permutations of 1 through n-1
        generatePerms( pts.size()-1, new ArrayList<Integer>() );
    }

    // given the total number (1 through n)
    // and a list---in order---of values already chosen,
    // recursively try all the possible unchosen values
    private void generatePerms( int n, ArrayList<Integer> chosen )
    {
        if( chosen.size() == n )
        {// all have been chosen, publish the result
            System.out.println( chosen );  // track progress in command window
            perms.add( copy( chosen ) );
        }
        else
        {// have at least one unchosen item
            for( int k=1; k<=n; k++ )
            {// generate perms from chosen plus k if it's not already in
                boolean found = false;
                for( int j=0; !found && j<chosen.size(); j++ )
                    if( chosen.get(j).equals( k ) )
                        found = true;

                if( !found )
                {// add k to chosen, recurse, then remove it
                    chosen.add( k );
                    generatePerms( n, chosen );
                    chosen.remove( chosen.size()-1 );
                }

            }// do perms for chosen plus k if not already in
        }
    }

    private ArrayList<Integer> copy( ArrayList<Integer> x )
    {
        ArrayList<Integer> r = new ArrayList<Integer>();
        for( int k=0; k<x.size(); k++ )
            r.add( x.get(k) );
        return r;
    }

}