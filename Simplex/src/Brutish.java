/*  Brutish is like ManualSimplex except
    it doesn't show the z column or row
    when hit 't'
*/

import java.awt.*;
import java.awt.event.*;
import java.text.*;

import java.util.*;
import java.io.*;

public class Brutish extends Basic
{

//==================================================================================================
    // NOTE:  edit these constants appropriately to adjust for different displays

    // total size of the window in pixels
    private static final int pixelWidth = 1010, pixelHeight = 700;  // total window size in pixels

    // amount to shift drawing area to the right and down to not hit the title bar or window borders
    private static final int windowHorizOffset = 15, windowVertOffset = 50;
    private static final int bottomPixels = 10;

    // pixel dimension of each tableau cell, same for numbers and labels
    private final static int pixelsHorizInCell = 110, pixelsVertInCell = 20;

    // fontsize for all text
    private final static int TEXTSIZE = 15;

    private static final Color rowLabelsColor = new Color( 100, 200, 200 );
    private static final Color rowLabelsBackColor = new Color( 200, 230, 230 );
    private static final Color colLabelsColor = rowLabelsColor;
    private static final Color colLabelsBackColor = rowLabelsBackColor;
    private static final Color rhsColor = new Color( 100, 200, 100 );
    private static final Color objRowColor = rhsColor;
    private static final Color rhsBackColor = new Color( 200, 230, 200 );
    private static final Color mainColor = new Color( 240, 240, 255 );
    private static final Color cursorColor = new Color( 200, 200, 255 );
    private static final Color editCursorColor = new Color( 100, 255, 100 );
    private static final Color urcColor = new Color( 255, 200, 200 );

    private static final Color editingColor = new Color( 200, 255, 200 );
    private static final Color helpColor = new Color( 240, 240, 255 );

    private static final double tiny = 1e-20;

//==================================================================================================


    public static void main(String[] args)
    {
        String fname = "";
        if( args.length != 1 )
            fname = FileBrowser.chooseFile( true );
        else
            fname = args[0];

        Brutish ms = new Brutish("Manual Simplex Method Tool", 0, 0, pixelWidth, pixelHeight, fname );
    }

    // instance variables for the application:
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    private double[][] a;  // holds the numbers in the tableau
    private String[] rowLabels, colLabels;
    private int numRows, numCols;  // convenience access to size of array
    private int firstWinRow, firstWinCol;  // row, col of upper left corner of window
    private int winRows, winCols;  // # of rows, columns in window
    private int cursorRow, cursorCol;

    private String state;
    private String editString; // the string being edited in main table
    private String rhsEditString;  // the string being edited in rhs
    private char direction;  // 'h' or 'v' for direction of travel on space/enter

    private static final int stepsToFlash = 5;  // steps to flash on error
    private int flashUntil;  // step to stop flashing

    private PrintWriter texOut;

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    public Brutish( String title, int ulx, int uly, int pw, int ph,
                    String fname )
    {
        super(title,ulx,uly,pw,ph);

        // code to initialize instance variables before animation begins:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        String fileName = fname;

        boolean recoverable = false; // monitor whether m n rowlabels collabels read and a built---but not filled

        try{

            texOut = new PrintWriter( new File( fileName + ".tex" ) );
            texOut.println( "\\input pictex" );

            Scanner input = new Scanner( new File( fileName ) );
            // read size of tableau
            numRows = input.nextInt();  numCols = input.nextInt();  input.nextLine();

            // read a label for each row:
            rowLabels = new String[numRows];
            for( int k=0; k<numRows; k++ )
            {
                rowLabels[k] = input.nextLine();
                System.out.println( rowLabels[k] );
            }

            // read a label for each col:
            colLabels = new String[numCols];
            for( int k=0; k<numCols; k++ )
            {
                colLabels[k] = input.nextLine();
                System.out.println( colLabels[k] );
            }

            // if data file only has the data above, will put in all zeros, with
            // identity columns for basic variables, after throwing exception below
            // when tries to read first number (or any incorrect number of elements)

            // read all the numbers, row by row
            a = new double[numRows][numCols];

            recoverable = true;  // all that is required has been done

            for( int r=0; r<numRows; r++ ){
                for( int c=0; c<numCols; c++ ){
                    a[r][c] = input.nextDouble();
                    System.out.print( a[r][c] + " " );
                }
                System.out.println();
            }

        }
        catch(Exception e)
        {
            if( !recoverable )
            {
                System.out.println("unrecoverable problem reading data file [" + fileName + "]" );
                System.exit(1);
            }
            else
            {// make a hold all 0's, except put in  1's for basic variables

                for( int r=0; r<numRows; r++ )
                {
                    for( int c=0; c<numCols; c++ )
                    {
                        if( rowLabels[r].equals( colLabels[c] ) )
                            a[r][c] = 1;
                        else
                            a[r][c] = 0;
                    }
                }
            }
        }

        // initialize various settings:
        firstWinRow = 1;  firstWinCol = 0;
        cursorRow = 1;  cursorCol = 0;

        state = "regular";
        flashUntil = -1;
        editString = "";
        direction = 'h';

        setBackgroundColor( new Color( 128, 128, 128 ) );

        // code to set up camera(s)

        // camera 0 shows the row labels:

        // figure out winRows, winCols = number of cells vert, horiz in main tablueau body
        // windowVerticalOffset + (winRows+2) pixelsVertInCell must be <= pixelHeight
        winRows = (pixelHeight - windowVertOffset - bottomPixels ) / pixelsVertInCell - 2;
        cameras.add( new Camera( windowHorizOffset, windowVertOffset + pixelsVertInCell,
                pixelsHorizInCell, (winRows+1)*pixelsVertInCell,
                0, 1, 0, winRows+1,
                rowLabelsColor ) );

        // camera 1 shows the col labels:

        // figure out winCols, windowHorizOffset + (winCols+2) pixelsHorizInCell <= pixelWidth
        winCols = (pixelWidth-windowHorizOffset) / pixelsHorizInCell - 2;
        cameras.add( new Camera( windowHorizOffset+pixelsHorizInCell, windowVertOffset-10,
                (winCols+1)*pixelsHorizInCell, pixelsVertInCell+10,
                0, winCols+1, 0, 1,
                colLabelsColor ) );

        // camera 2 shows the objective function row:
        cameras.add( new Camera( windowHorizOffset+pixelsHorizInCell, windowVertOffset + pixelsVertInCell,
                (winCols+1)*pixelsHorizInCell, pixelsVertInCell,
                0, winCols+1, 0, 1,
                objRowColor ) );

        // camera 3 shows the main body of the tableau:
        cameras.add( new Camera( windowHorizOffset+pixelsHorizInCell, windowVertOffset + 2*pixelsVertInCell,
                winCols*pixelsHorizInCell, winRows*pixelsVertInCell,
                0, winCols, 0, winRows,
                mainColor ) );

        // camera 4 shows the rhs:
        cameras.add( new Camera( windowHorizOffset+(winCols+1)*pixelsHorizInCell, windowVertOffset + 2*pixelsVertInCell,
                pixelsHorizInCell, winRows*pixelsVertInCell,
                0, 1, 0, winRows,
                rhsColor ) );

        //------------------------------------------------------------------
        // start up the animation:
        super.start();
    }

    public void step()
    {
        Camera cam;

        if( state.equals( "flashing" ) )
        {
            cam = cameras.get(3);
            cam.activate();
            cam.setColor( Color.red );
            cam.fillRect( 0, 0, winCols*pixelsHorizInCell, winRows*pixelsVertInCell );

            if( flashUntil <= getStepNumber() )
                state = "regular";

        }// state "flashing"

        else if( state.equals( "help" ) )
        {
            cam = cameras.get(3);
            cam.activate();
            cam.setColor( Color.black );
            showMenu( cam );
        }

        else if( state.equals( "regular" ) || state.equals( "editing" ) || state.equals( "rhsEditing" ) )
        {
            double x, y;   // utility variables for positioning

            double xf = 0.0;  // xf for "x fudge" to make left edges a little clearer
            double yf = 0.0;  // yf for "y fudge" to make bottom edges a little clearer
            double cf = 0.0; // cf for "cell fudge" to squeeze in cell border

            // camera 0:  row labels ----------------------------------------------------
            cam = cameras.get(0);
            cam.activate();
            cam.setColor( Color.black );

            cam.drawText( rowLabels[0], xf, winRows+yf );   // "z" is always shown

            y = winRows-1+yf;

            for( int k=firstWinRow; k<Math.min(numRows,firstWinRow+winRows); k++ )
            {
                if( cursorRow==k )
                {
                    cam.setColor( rowLabelsBackColor );
                    cam.fillRect( cf, y-yf+cf, 1-2*cf, 1-2*cf );
                    cam.setColor( Color.black );
                }
                cam.drawText( rowLabels[k], xf, y );
                y--;
            }

            // camera 3:  main body  ----------------------------------------------------

            cam = cameras.get(3);
            cam.activate();
            cam.setColor( Color.black );

            cam.setFont( new Font( "Monospaced", Font.PLAIN, TEXTSIZE ) );

            y = winRows-1 + yf;

            for( int k=firstWinRow; k<Math.min(numRows,firstWinRow+winRows); k++ )
            {
                x=xf;

                for( int j=firstWinCol; j<Math.min(numCols-1,firstWinCol+winCols); j++ )
                {
                    if( cursorRow==k && cursorCol==j )
                    {
                        cam.setColor( cursorColor );
                        cam.fillRect( x-xf, y-yf, 1, 1 );
                        cam.setColor( Color.black );
                    }

                    cam.drawText( nice(a[k][j]), x, y );
                    x++;
                }

                y--;
            }

            // do special work for editing:
            if( state.equals( "editing" ) && cursorRow > 0 )
            {// editing the main body
                cam.setColor( editCursorColor );
                x = cursorCol - firstWinCol;  y = firstWinRow + winRows - 1 - cursorRow;
                cam.fillRect( x-xf, y-yf, 1, 1 );
                cam.setColor( Color.black );
                cam.drawText( editString, x, y );
            }

            // camera 4:  rhs  ----------------------------------------------------------

            cam = cameras.get(4);
            cam.activate();
            cam.setColor( Color.black );

            y = winRows-1+yf;

            for( int k=firstWinRow; k<Math.min(numRows,firstWinRow+winRows); k++ )
            {
                if( cursorRow==k )
                {
                    cam.setColor( rhsBackColor );
                    cam.fillRect( cf, y-yf+cf, 1-2*cf, 1-2*cf );
                    cam.setColor( Color.black );
                }
                cam.drawText( nice(a[k][ numCols-1 ]), xf, y );
                y--;
            }

            // do special work for rhs editing:
            if( state.equals( "rhsEditing" ) && cursorRow > 0 )
            {// editing the main part of the rhs (not upper right corner)
                cam.setColor( editCursorColor );
                x = 0;  y = firstWinRow + winRows - 1 - cursorRow;
                cam.fillRect( x-xf, y-yf, 1, 1 );
                cam.setColor( Color.black );
                cam.drawText( rhsEditString, x, y );
            }

            // camera 1:  col labels ----------------------------------------------------

            cam = cameras.get(1);
            cam.activate();
            cam.setColor( Color.black );

            cam.drawText( colLabels[ numCols-1 ], winCols+xf, yf+0.25 );

            x = xf;

            for( int k=firstWinCol; k<Math.min(numCols-1,firstWinCol+winCols); k++ )
            {
                if( cursorCol==k )
                {
                    cam.setColor( colLabelsBackColor );
                    cam.fillRect( x, yf, 1-2*cf, 1-2*cf );
                    cam.setColor( Color.black );
                }

                cam.drawText( colLabels[k], x, yf+0.25 );

                x++;
            }

            // camera 2:  obj func row --------------------------------------------------

            cam = cameras.get(2);
            cam.activate();

            cam.setColor( urcColor );     // upper right corner has special color---is overlap of two strips
            cam.fillRect( winCols+xf, yf, 1, 1 );
            cam.setColor( Color.black );
            cam.drawText( nice(a[0][ numCols-1 ]), winCols + xf, yf );


            x = xf;

            for( int k=firstWinCol; k<Math.min(numCols-1,firstWinCol+winCols); k++ )
            {
                cam.drawText( nice(a[0][k]), x, yf );
                x++;
            }

            if( state.equals("editing") && cursorRow==0 )
            {// editing the obj func row
                cam.setColor( editCursorColor );
                x = cursorCol - firstWinCol;  y = 0;
                cam.fillRect( x-xf, y-yf, 1, 1 );
                cam.setColor( Color.black );
                cam.drawText( editString, x, y );
            }

        }// state "regular"

    }

    public void keyTyped( KeyEvent e )
    {
        char key = e.getKeyChar();

        if( state.equals( "editing" ) )
        {// editing so keys mean different things

            // only allow symbols that could be part of a double:
            if( key=='.' || ('0'<=key && key<='9') ||
                    key=='-' || key=='+' || key=='e' || key=='E' )
            {
                editString += key;
            }

        }

        if( state.equals( "rhsEditing" ) )
        {// editing so keys mean different things

            // only allow symbols that could be part of a double:
            if( key=='.' || ('0'<=key && key<='9') ||
                    key=='-' || key=='+' || key=='e' || key=='E' )
            {
                rhsEditString += key;
            }

        }

        if( key == 'r' && state.equals( "regular" ) )
        {// start rhs editing
            state = "rhsEditing";
            rhsEditString = "";
            cameras.get( 4 ).setBackgroundColor( editingColor );
        }

        else if( key == 'm' && state.equals("regular") )
        {// min ratio
            // look for first bigger than tiny row
            int start = 0;
            for( int r=1; r<numRows && start==0; r++ )
                if( a[r][cursorCol] > tiny )
                    start = r;

            if( start == 0 )
            {// unbounded problem---nobody in the column is nicely positive
                state = "flashing";
                flashUntil = getStepNumber() + stepsToFlash;
            }
            else
            {// have a place to start

                int minRow = start;
                double minRat = a[minRow][numCols-1]/a[minRow][cursorCol];

                for( int r=start+1; r<numRows; r++ )
                {
                    double arhs = a[r][numCols-1], aj = a[r][cursorCol];

                    if( aj > tiny && arhs/aj < minRat )
                    {// found a better row
                        minRow = r;
                        minRat = arhs/aj;
                    }
                }

                // place cursor in the min row and adjust window as needed
                moveCursor( minRow, cursorCol );

            }// have a place to start

        }// min ratio

        else if( key == 'p' && state.equals("regular") )
        {// pivot

            if( Math.abs( a[cursorRow][cursorCol] ) < tiny )
            {// pivot item too small, flash
                state = "flashing";
                flashUntil = getStepNumber() + stepsToFlash;
            }
            else
            {// can pivot on the cursor item

                // multiply cursorRow by 1/cursor item
                double value = a[cursorRow][cursorCol];
                for( int c=0; c<numCols; c++ )
                    a[cursorRow][c] /= value;

                // add multiples of cursor row to zero out cursor col in other rows
                for( int r=0; r<numRows; r++ )
                    if( r != cursorRow )
                    {
                        double mult = - a[r][cursorCol];
                        for( int c=0; c<numCols; c++ )
                            a[r][c] += mult * a[cursorRow][c];
                    }

                // replace row label with new col
                rowLabels[cursorRow] = colLabels[cursorCol];

            }// can pivot on the cursor item

        }// pivot

        else if( key == 'e' && state.equals("regular") )
        {// start editing
            editString = "";
            state = "editing";
            cameras.get( 2 ).setBackgroundColor( editingColor );
            cameras.get( 3 ).setBackgroundColor( editingColor );
        }

        else if( key == 'f' )        // works when regular or editing or rhsEditing
        {// move to first row or col
            if( direction == 'h' )
                moveCursor( cursorRow, 0 );
            else
            {
                if( state.equals( "regular" ) )
                    moveCursor( 1, cursorCol );
                else
                    moveCursor( 0, cursorCol );
            }
        }

        else if( key == 'l' )        // works when regular or editing or rhsEditing
        {// move to last row or col
            if( direction == 'h' )
                moveCursor( cursorRow, numCols-2 );
            else
                moveCursor( numRows-1, cursorCol );
        }

        else if( key == 'h' )        // works when regular or editing or rhsEditing
        {// horizontal
            direction = 'h';
        }

        else if( key == 'v' )        // works when regular or editing or rhsEditing
        {// vertical
            direction = 'v';
        }

        else if( key == 's'  && state.equals( "regular" ))
        {// save current tableau to selected file
            save();
        }

        else if( key == 'q' && state.equals( "regular" ) )
        {
            texOut.close();
            save();
            System.exit(0);
        }

        else if( key == 'k' && state.equals( "regular" ) )
        {// kill cursor col
            // shift col labels to remove
            for( int c=cursorCol+1; c<numCols; c++ )
                colLabels[c-1] = colLabels[c];

            // shift cols of a to remove
            for( int r=0; r<numRows; r++ )
            {// shift row r
                for( int c=cursorCol+1; c<numCols; c++ )
                    a[r][c-1] = a[r][c];
            }

            if( cursorCol == numCols-2 )
            {// cursor was on last non-rhs column, must adjust
                moveCursor( cursorRow, cursorCol-1 );
            }

            if( firstWinCol+winCols-1 == numCols-2 )
            {// last col in window has gone away, so shift window if can
                firstWinCol = Math.max( 0, firstWinCol-1 );
            }

            numCols--;

        }// kill

        else if( key == '?' )
        {
            state = "help";
            cameras.get( 3 ).setBackgroundColor( helpColor );
        }

        else if( key == 't' && state.equals("regular") )
        {
            toTeX();
        }

    }// keyTyped

    public void keyPressed( KeyEvent e )
    {
        int code = e.getKeyCode();

        if( code == KeyEvent.VK_RIGHT )
        {
            moveCursor( cursorRow, cursorCol+1 );
        }
        else if( code == KeyEvent.VK_LEFT )
        {
            moveCursor( cursorRow, cursorCol-1 );
        }
        else if( code == KeyEvent.VK_DOWN )
        {
            moveCursor( cursorRow+1, cursorCol );
        }
        else if( code == KeyEvent.VK_UP )
        {
            moveCursor( cursorRow-1, cursorCol );
        }

        else if( code == KeyEvent.VK_HOME )
        {
            int newCol = Math.max( cursorCol-winCols, 0 );
            moveCursor( cursorRow, newCol );
        }
        else if( code == KeyEvent.VK_END )
        {
            int newCol = Math.min( cursorCol+winCols, numCols-2 );
            moveCursor( cursorRow, newCol );
        }
        else if( code == KeyEvent.VK_PAGE_UP )
        {
            int newRow = Math.max( cursorRow-winRows, 1 );
            moveCursor( newRow, cursorCol );
        }
        else if( code == KeyEvent.VK_PAGE_DOWN )
        {
            int newRow = Math.min( cursorRow+winRows, numRows-1 );
            moveCursor( newRow, cursorCol );
        }

        else if( state.equals("editing") && code == KeyEvent.VK_ESCAPE )
        {// exit editing
            moveCursor( cursorRow, cursorCol );
            // move out of obj func row
            if( cursorRow == 0 )
            {
                cursorRow = 1;
                firstWinRow = 1;  // just in case
            }
            state = "regular";
            // restore regular colors
            cameras.get( 2 ).setBackgroundColor( objRowColor );
            cameras.get( 3 ).setBackgroundColor( mainColor );
        }

        else if( state.equals("rhsEditing") && code == KeyEvent.VK_ESCAPE )
        {// exit rhs editing
            moveCursor( cursorRow, cursorCol );
            state = "regular";
            // restore regular color
            cameras.get( 4 ).setBackgroundColor( rhsColor );
        }

        else if( state.equals("help") && code == KeyEvent.VK_ESCAPE )
        {
            state = "regular";
            cameras.get( 3 ).setBackgroundColor( mainColor );
        }

        else if( code == KeyEvent.VK_SPACE )
        {// move to next cell
            if( direction == 'h' )
                moveCursor( cursorRow, cursorCol+1 );
            else
                moveCursor( cursorRow+1, cursorCol );
        }

        else if( code == KeyEvent.VK_ENTER )
        {// move to first cell of next row or col
            if( direction == 'h' )
                moveCursor( cursorRow+1, 0 );
            else// 'v'
                if( state.equals("regular") )
                    moveCursor( 1, cursorCol+1 );
                else// editing
                    moveCursor( 0, cursorCol+1 );
        }

        else if( state.equals("editing") && (code == KeyEvent.VK_BACK_SPACE ||
                code == KeyEvent.VK_DELETE ) )
        {
            if( editString.length() > 0 )
                editString = editString.substring( 0, editString.length()-1 );
        }

        else if( state.equals("rhsEditing") && (code == KeyEvent.VK_BACK_SPACE ||
                code == KeyEvent.VK_DELETE ) )
        {
            if( rhsEditString.length() > 0 )
                rhsEditString = rhsEditString.substring( 0, rhsEditString.length()-1 );
        }

    }// keyPressed

    // move cursor to nr, nc
    // (might refuse to move), and depending on whether editing,
    // process editString into cell before leaving
    private void moveCursor( int nr, int nc )
    {
        // moving (or escaping) while editing forces processing of editString
        if( state.equals( "editing" ) )
        {
            if( okay( editString ) )
            {
                a[cursorRow][cursorCol] = convert( editString );
            }
            editString = "";

        }
        else if( state.equals( "rhsEditing" ) )
        {
            if( okay( rhsEditString ) )
            {
                a[cursorRow][numCols-1] = convert( rhsEditString );
            }
            rhsEditString = "";
        }

        // move appropriately (might be illegal request)
        int oldRow=cursorRow, oldCol=cursorCol;  // remember so can restore as needed

        cursorRow = nr;  cursorCol = nc;

        if( ( (state.equals( "editing" ) && cursorRow == 0) || 1<=cursorRow)  // good row
                && cursorRow<numRows && 0<=cursorCol && cursorCol<numCols-1
                )
        {// new position is legal, shift window as needed so cursor visible

            // adjust window vertically
            if( cursorRow > 0 )  // when cursor on obj row leave firstWinRow at 1
            {
                if( cursorRow < firstWinRow )
                    firstWinRow = cursorRow;
                if( cursorRow > firstWinRow + winRows - 1 )
                    firstWinRow = cursorRow - winRows + 1;
            }

            // adjust window horizontally
            if( cursorCol < firstWinCol )
                firstWinCol = cursorCol;
            if( firstWinCol+winCols-1 < cursorCol )
                firstWinCol = cursorCol - winCols + 1;

        }// new position legal
        else
        {// restore original
            cursorRow=oldRow;  cursorCol=oldCol;
        }

    }// moveCursor

    // return whether s is a legal double value
    private boolean okay( String s )
    {
        try{
            double x = Double.parseDouble( s );
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    private double convert( String s )
    {
        return Double.parseDouble( s );
    }

    public void mouseMoved(MouseEvent e)
    {
        super.mouseMoved(e);

        // code to respond to mouse motion:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public void mouseDragged(MouseEvent e)
    {
        super.mouseDragged(e);

        // code to respond to mouse motion with a button pressed:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public void mouseClicked(MouseEvent e)
    {
        super.mouseClicked(e);

        // code to respond to mouse click:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public void mousePressed(MouseEvent e)
    {
        super.mousePressed(e);

        // code to respond to mouse button press:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public void mouseReleased(MouseEvent e)
    {
        super.mouseReleased(e);

        // code to respond to mouse button release:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public void mouseEntered(MouseEvent e)
    {
        super.mouseEntered(e);

        // code to respond to mouse entering window:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    public void mouseExited(MouseEvent e)
    {
        super.mouseExited(e);

        // code to respond to mouse exiting window:
        // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

        // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    // some rather crude formatting stuff

    public static String nice( double x )
    {
        double y = Math.abs(x);

        // using very naive tolerance (assuming scaling around 1)!
        if( y <= tiny )
            return "0";

        if( y < 0.00001 )
            return expFormat.format( x );
        else if( y < 1 )
            return formats[0].format( x );
        else if( y < 10 )
            return formats[1].format( x );
        else if( y < 100 )
            return formats[2].format( x );
        else if( y < 1000 )
            return formats[3].format( x );
        else if( y < 10000 )
            return formats[4].format( x );
        else if( y < 100000 )
            return formats[5].format( x );
        else
            return expFormat.format( x );
    }

    private static DecimalFormat[] formats;
    // initialize formats in a sort of method-less way:
    {
        formats = new DecimalFormat[6];
        formats[0] = new DecimalFormat( ".########" );
        formats[1] = new DecimalFormat( "#.#######" );
        formats[2] = new DecimalFormat( "##.######" );
        formats[3] = new DecimalFormat( "###.#####" );
        formats[4] = new DecimalFormat( "####.####" );
        formats[5] = new DecimalFormat( "#####.###" );
    }

    private static DecimalFormat expFormat = new DecimalFormat( "0.####E00");

    // save current tableau to selected file
    private void save()
    {
        String saveFile = FileBrowser.chooseFile( false );
        try{
            PrintWriter output = new PrintWriter( new File( saveFile ) );

            output.println( numRows + " " + numCols );

            for( int r=0; r<numRows; r++ )
                output.println( rowLabels[r] );

            for( int c=0; c<numCols; c++ )
                output.println( colLabels[c] );

            for( int r=0; r<numRows; r++ )
            {
                for( int c=0; c<numCols; c++ )
                    output.print( a[r][c] + " " );
                output.println();
            }
            output.close();
        }
        catch(Exception e)
        {
            System.out.println("Uh-oh, save failed for some reason");
            e.printStackTrace();
        }
    }

    private void showMenu( Camera cam )
    {
        double y = winRows-1;

        menuItem( cam, "Key(s)", "Action", y );  y-=2;

        menuItem( cam, "arrow keys", "move one row/col in the chosen direction", y );  y-=2;
        menuItem( cam, "h,v", "select horizontal or vertical motion", y );  y-=2;
        menuItem( cam, "f", "move to first position in current row/col", y );  y-=2;
        menuItem( cam, "l", "move to last position in current row/col", y );  y-=2;
        menuItem( cam, "<space>", "move ahead one position", y );  y-=2;
        menuItem( cam, "<enter>", "move to beginning of next row/col", y );  y-=2;

        menuItem( cam, "e", "edit main table", y ); y-=2;
        menuItem( cam, "r", "edit rhs", y ); y-=2;
        menuItem( cam, "<esc>", "escape to regular mode", y ); y-=2;
        menuItem( cam, "m", "find min ratio row", y ); y-=2;
        menuItem( cam, "p", "pivot on cursor item", y ); y-=2;
        menuItem( cam, "k", "kill the current column", y ); y-=2;
        menuItem( cam, "s", "save the current tableau to selected file", y ); y-=2;
        menuItem( cam, "q", "save and quit", y ); y-=2;

    }

    private void menuItem( Camera cam, String keys, String action, double y )
    {
        cam.drawText( keys, 0.25, y );
        cam.drawText( action, 1.25, y );
    }

    private String fix( String s )
    {
        if( (s.charAt(0)=='x' || s.charAt(0)=='s' || s.charAt(0)=='a')
                && s.length()>1 )
            return s.charAt(0) + "_{" + s.substring(1) + "}";
        else
            return s;
    }

    private void toTeX()
    {
        System.out.println("adding to the TeX file");
        texOut.println("\\input pictex");
        texOut.println("\\beginpicture");
        texOut.println("\\setcoordinatesystem units <36true pt,18true pt>");

        int h = numRows;
        int w = numCols;

        texOut.println("\\putrectangle corners at 0 0 and " + w + " " + h );

        // draw the labels
        int k;
        for( k=1; k<numRows; ++k )
            texOut.println("\\put {$" + fix((String) rowLabels[k] ) + "$} [r] at -0.25 " +
                    (h-k-0.5) );

        for( k=1; k<numCols; ++k )
            texOut.println("\\put {$" + fix((String) colLabels[k]) + "$} [B] at " +
                    (k+0.5) + " " + (h+0.25) );

        // save the numeric values
        for( int r=1; r<numRows; r++ )
            for( int c=1; c<numCols; c++ )
            {
                if( a[r][c] == (int) a[r][c] )
                    texOut.println("\\put {$" + ((int) a[r][c]) + "$} at " +
                            (0.5+c) + " " + (h-r-0.5) );
                else
                    texOut.println("\\put {$" + String.format("%5.2f",a[r][c]) +
                            "$} at " + (0.5+c) + " " + (h-r-0.5) );
            }

        texOut.println("\\endpicture");
        texOut.println("\\bigskip");

    }// toTeX

}