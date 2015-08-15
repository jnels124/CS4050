/*  
   automate (somewhat) the heuristic TSP algorithm
   (user still has to enter cuts, branches, and notice tours)
*/

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import java.util.*;
import java.io.*;

public class AutoHeuristicTSP extends Basic
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

    private static final double tiny = 0.0000001;

    public static double[][] points;  // points for problem, globally known

//==================================================================================================

    public static void main(String[] args)
    {
        AutoHeuristicTSP bf = new AutoHeuristicTSP("Auto Heuristic TSP", 0, 0,
                pixelWidth, pixelHeight, args[0] );
    }

    // instance variables for the application:
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    private Node node;  // the current node
    private Tableau table;  // the tableau currently being used
    private ArrayList<Node> pq;  // the priority queue (implemented crudely)
    // of all unexplored nodes

    private String editMode; // is "cutting" or "branching" or "regular"
    private String editString; // accumulate symbols until reach stop state

    private Node bestNode;

    private boolean useGrid;  // snap mouse to grid and show grid, or free form
    private boolean showLabels;  // show vertex numbers

    private PrintWriter tex;

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    public AutoHeuristicTSP( String title, int ulx, int uly, int pw, int ph, String fileName )
    {
        super(title,ulx,uly,pw,ph);

        try{
            Scanner input = new Scanner( new File( fileName ) );
            tex = new PrintWriter( new File( "temp" + fileName + ".tex" ) );

            int n = input.nextInt();   input.nextLine();

            // read and ignore the line that says "points" for compatibility
            // with HeuristicTSP which has some options not supported here
            input.nextLine();

            points = new double[ n ][2];

            for( int k=0; k<n; k++ )
            {
                points[k][0] = input.nextDouble();
                points[k][1] = input.nextDouble();
            }

            node = new Node();
            bestNode = null;

            pq = new ArrayList<Node>();
            pq.add( node );

            branchAndBoundStep();

        }
        catch(Exception e)
        {
            System.out.println("failed to open tableau and solve LP");
            e.printStackTrace();
            System.exit(1);
        }

        useGrid = true;
        showLabels = true;

        editMode = "regular";
        editString = "";

        setBackgroundColor( new Color( 64, 64, 64 ) );
        cameras.add( new Camera( windowHorizOffset, windowVertOffset,
                pixelWidth - windowHorizOffset - rightMargin,
                pixelHeight - windowVertOffset - bottomMargin,
                0, 1, 0, 1,
                new Color( 200, 200, 200 ) ) );

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

        String[] vars = node.variables;
        double[] vals = node.values;
        double[][] pts = points;

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

                    // add in colors for 1/6, 2/6, 4/6, 5/6:

                else if( Math.abs( vals[m] - 0.1666666666 ) < tiny )
                    cam.setColor( Color.yellow );
                else if( Math.abs( vals[m] - 0.3333333333 ) < tiny )
                    cam.setColor( Color.orange );
                else if( Math.abs( vals[m] - 0.6666666666 ) < tiny )
                    cam.setColor( Color.green );
                else if( Math.abs( vals[m] - 0.8333333333 ) < tiny )
                    cam.setColor( Color.magenta );
                else if( vals[m] > tiny )
                    cam.setColor( Color.cyan );  // not fitting the color scheme

                cam.drawLine( x1, y1, x2, y2 );
            }
        }

        // show the cost and node id
        cam.setColor( Color.magenta );
        cam.drawText( "" + node.score, .025, 0.975 );
        cam.drawText( "" + node.id, 0.975, 0.975 );


        // show the "one k j" edges in black, the
        //          "zero k j" edges in white
        // so user can see what node they are working
        // with

        // draw the forced to zero edges in white
        cam.setColor( Color.white );
        for( int k1=0; k1<node.zeros.size(); k1++ )
        {
            Pair p = node.zeros.get(k1);
            int k=p.first-1, j=p.second-1;
            cam.drawLine( pts[k][0]/displayScale, pts[k][1]/displayScale,
                    pts[j][0]/displayScale, pts[j][1]/displayScale );
        }

        // draw the forced to one edges in black
        cam.setColor( Color.black );
        for( int k1=0; k1<node.ones.size(); k1++ )
        {
            Pair p = node.ones.get(k1);
            int k=p.first-1, j=p.second-1;
            cam.drawLine( pts[k][0]/displayScale, pts[k][1]/displayScale,
                    pts[j][0]/displayScale, pts[j][1]/displayScale );
        }

        // show the current editString
        cam.setColor( Color.black );
        cam.drawText( editString, 0.05, 0.05 );

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

        else if( key == 'q' )
        {// save picture to tex file and quit
            tex.println("$$\n\\beginpicture");
            tex.println("\\setcoordinatesystem units <1true mm,1true mm>");

            String[] vars = node.variables;
            double[] vals = node.values;
            double[][] pts = points;

            // draw the points

            for( int k=0; k<pts.length; k++ )
            {
                double px = pts[k][0], py = pts[k][1];
                tex.println("\\put {$\\bullet$} at " + px + " " + py );
                tex.println("\\put {\\tinytt " + (k+1) + "} [bl] at " + (px+1) + " " + (py+1) );
            }

            // draw the edges corresponding to xk,j > 0

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

                    double x1 = pts[k-1][0], y1 = pts[k-1][1];
                    double x2 = pts[j-1][0], y2 = pts[j-1][1];

                    if( Math.abs( vals[m] - 0.5 ) < tiny )
                        tex.println("\\setdots <1true mm>");
                    else
                        tex.println("\\setsolid");
                    tex.println("\\plot " + x1 + " " + y1 + " " + x2 + " " + y2 + " /" );
                }
            }

            tex.println("\\setsolid");
            tex.println("\\endpicture\n$$");

            tex.close();

            System.exit(0);

        }// quit

    }// keyTyped

    public void keyPressed( KeyEvent e )
    {
        int code = e.getKeyCode();

        if( editMode.equals( "regular" ) )
        {
            if( code == KeyEvent.VK_C )
            {
                editMode = "cutting";
                editString = "cut ";
            }
            else if( code == KeyEvent.VK_B )
            {
                editMode = "branching";
                editString = "branch on ";
            }
            else if( code == KeyEvent.VK_T )
            {// process current node/tableau as a tour
                editMode = "regular";  editString = "";

                // update best node using current node
                // based on user's claim that this node
                // represents a tour

                if( bestNode == null ||
                        bestNode.score > node.score )
                {
                    bestNode = node;  // will never be changed again, so okay
                    System.out.println("Updating best node to " + node );
                }

                branchAndBoundStep();  // get next best node in PQ
            }
        }

        else if( editMode.equals( "cutting" ) )
        {
            if( KeyEvent.VK_0 <= code && code <= KeyEvent.VK_9)
                editString += "" + (code - KeyEvent.VK_0 );
            else if( code == KeyEvent.VK_SPACE )
                editString += " ";
            else if( code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE )
            {
                if( editString.length() > 4 )
                    editString = editString.substring( 0, editString.length()-1 );
            }
            else if( code == KeyEvent.VK_ENTER )
            {// finish attempted cut

                if( node.addCut( editString ) )
                {// is okay cut, was added to node, re-solve this node with no change to pq
                    node.solve();
                    System.out.println("\n------------------");
                    System.out.println("after cut, current node is " + node );
                    System.out.println("------------------");
                }

                // whether cut succeeded or not, start anew
                editMode = "regular";  editString = "";

            }
        }// cutting mode

        else if( editMode.equals( "branching" ) )
        {
            if( KeyEvent.VK_0 <= code && code <= KeyEvent.VK_9)
                editString += "" + (code - KeyEvent.VK_0 );
            else if( code == KeyEvent.VK_SPACE )
                editString += " ";
            else if( code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE )
            {
                if( editString.length() > 10 )
                    editString = editString.substring( 0, editString.length()-1 );
            }
            else if( code == KeyEvent.VK_ENTER )
            {// finish attempted branching

                Node[] branchNodes = node.branch( editString );
                if( branchNodes != null )
                {
                    pq.add( branchNodes[0] );
                    pq.add( branchNodes[1] );

                    branchAndBoundStep();
                }

                // whether cut succeeded or not, start anew
                editMode = "regular";  editString = "";

            }
        }// branching mode

        else if( editMode.equals( "optimal" ) )
        {
            if( code == KeyEvent.VK_ESCAPE )
                System.exit(0);
        }

    }// keyPressed

    // do a step of the main branch and bound algorithm
    private void branchAndBoundStep()
    {
        System.out.println("\n-------------------- priority queue in branch and bound step ----------");

        // remove all nodes with score worse than bestNode
        if( bestNode != null )
        {
            for( int k=pq.size()-1; k>=0; k-- )
            {
                Node current = pq.get(k);
                if( current.score  > bestNode.score )
                {
                    System.out.println("pruning node " + current.id + " with score " +
                            current.score );
                    pq.remove(k);
                }
            }
        }

        if( pq.size() == 0 )
        {
            System.out.println("Priority queue is empty of nodes");
            if( bestNode == null )
            {
                System.out.println("Never found any tour???");
                System.exit(0);
            }
            else
            {
                node = bestNode;
                System.out.println("Best node: " + node );
                editMode = "optimal";
            }
        }
        else
        {// pq is not empty

            System.out.println( pq.get(0) );

            // remove best node
            int best = 0;
            for( int k=1; k<pq.size(); k++ )
            {
                System.out.println( pq.get(k) );
                if( pq.get(best).score > pq.get(k).score )
                    best = k;
            }

            node = pq.get(best);
            System.out.println("Explore node <<< " + node.id + " >>> and remove from PQ");
            pq.remove( best );
        }

    }

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