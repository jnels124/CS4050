/* 
    simple (all stored in one 2D array) tableau
    for simplex method
*/

import java.io.*;
import java.util.*;

public class Tableau
{
    private final static int MAXROWS = 1000, MAXCOLS = 2000;
    private final static double tiny = 1e-12;

    private int numRows, numCols;
    private int n;
    private double[][] a;  // holds the tableau
    private String[] rowLabels, colLabels;

    private int stepNumber;
    private int haltStep;

    // special for TSP format:
    private double[][] pts;

    private double[][] aOrig;  // store original for debugging
    private double[][] bigB;  // track B in theory for debugging
    // bigB col j of a should always be col j of aOrig

    // Version 2:
    private ArrayList<String> setToZero, setToOne;

    private PrintWriter out;

    // **************************************************
    // Version 2: some utilities for tableau modification

    // return column in tableau for given variable name
    private int findColumn( String varName )
    {
        for( int k=0; k<numCols; k++ )
            if( varName.equals( colLabels[k] ) )
                return k;
        return -1;
    }

    // delete the given column from the tableau, including
    // colLabels
    private void removeColumn( int j )
    {
        // shift numeric values
        for( int r=0; r<numRows; r++ )
        {// shift items in row r one to the left
            for( int m=j+1; m<numCols; m++ )
            {
                a[r][m-1] = a[r][m];
            }
        }

        // shift col labels
        for( int m=j+1; m<numCols; m++ )
        {
            colLabels[m-1] = colLabels[m];
        }

        // adjust number of columns
        numCols--;
    }

    // return row in tableau for given variable name
    private int findRow( String varName )
    {
        for( int k=0; k<numRows; k++ )
            if( varName.equals( rowLabels[k] ) )
                return k;
        return -1;
    }

    // delete the given row, including rowLabels
    private void removeRow( int j )
    {
        // shift numeric values
        for( int c=0; c<numCols; c++ )
        {// shift items in column c up one
            for( int m=j+1; m<numRows; m++ )
                a[m-1][c] = a[m][c];
        }

        // shift row labels
        for( int m=j+1; m<numRows; m++ )
            rowLabels[m-1] = rowLabels[m];

        // adjust number of rows
        numRows--;
    }

    // (end Version 2 additional methods)****************

    public Tableau( String format, Scanner input, int halter ){

        // prepare TeX output file for later possible use
        try{
            out = new PrintWriter( new File( "tempReport.tex" ) );

            // prepare stand-alone document for convenience
            out.println("\\input pictex");
            out.println("\\parindent 0true in");
            out.println("\\nopagenumbers");
        }
        catch(Exception e)
        {
            System.out.println("crazy---couldn't open tempReport.tex?");
            System.exit(1);
            out = null;
        }

        // Version 2:
        setToZero = new ArrayList<String>();
        setToOne = new ArrayList<String>();

        haltStep = halter;

        a = new double[MAXROWS][MAXCOLS];
        rowLabels = new String[MAXROWS];
        colLabels = new String[MAXCOLS];

        if( format.equals( "raw" ) ){
            numRows = input.nextInt();  numCols = input.nextInt();  input.nextLine();

            // read a label for each row:
            for( int k=0; k<numRows; k++ ){
                rowLabels[k] = input.nextLine();
                System.out.println( rowLabels[k] );
            }

            // read a label for each col:
            for( int k=0; k<numCols; k++ ){
                colLabels[k] = input.nextLine();
                System.out.println( colLabels[k] );
            }

            for( int r=0; r<numRows; r++ ){
                for( int c=0; c<numCols; c++ ){
                    a[r][c] = input.nextDouble();
                    System.out.print( a[r][c] + " " );
                }
                System.out.println();
            }

        }// raw format

        else if( format.equals( "tsp" ) )
        {// build tableau from info in the file

            // build the base tableau (before any cuts or branches)

            // read points data from input

            n = input.nextInt();  input.nextLine();

            String frmt = input.nextLine();

            if( ! frmt.equals( "points" ) )
            {
                System.out.println("Data file must have \"points\" after # of points");
                System.exit(1);
            }

            pts = new double[ n ][2];

            for( int k=0; k<n; k++ )
            {
                pts[k][0] = input.nextDouble();
                pts[k][1] = input.nextDouble();
            }

            // prepare for reading command lines later
            if( input.hasNextLine() )
                input.nextLine();

            // now build the tableau:
            //        z  ak    sij
            int tspVars = (n*(n-1))/2;

            numRows = 1 + n + tspVars;
            //        z  xij  sij ak
            //   (put rhs in separately at the end)
            numCols = 1 + tspVars + tspVars + n;

            System.out.println("tableau size before processing commands: " + numRows + " " + numCols );

            rowLabels[0] = "z";  // obj func row
            a[0][0] = 1;         // z as the permanent basic var for row 0

            colLabels[ 0 ] = "z";

            // put rhs values in special storage, place in a[...][numCols-1] at end
            double[] rhs = new double[MAXROWS];

            // fill in rows 1 through n except for the xij part
            for( int r=1; r<=n; r++ )  // the core constraints
            {
                rowLabels[ r ] = "a" + r;
                a[ r ][ 2*tspVars + r ] = 1;
                rhs[ r ] = 2;

                colLabels[ 2*tspVars + r ] = "a" + r;
            }

            // move through 2D chart of all pairs of points to
            // fill in rest of table
            int count = 0;  // number of previous spots in the 2D chart
            for( int k=1; k<n; k++ )
                for( int j=k+1; j<=n; j++ )
                {// fill in everything needed for x_k,j
                    int row = n+1+count;

                    // fill in the skj rows
                    rowLabels[ row ] = "s" + k + "," + j;
                    a[ row ][ 1+count ] = 1;
                    a[ row ][ tspVars+1 + count ] = 1;

                    rhs[ row ] = 1;

                    colLabels[ count+1 ] = "x" + k + "," + j;
                    colLabels[ tspVars + 1 + count ] = "s" + k + "," + j;

                    // fill in the ckj in obj func row
                    a[0][ count+1 ] = dist( k, j );

                    // fill in xkj coeffs in first n constraints:
                    a[ k ][ count+1 ] = 1;
                    a[ j ][ count+1 ] = 1;

                    count++;  // get ready for next square
                }

            // now read commands and arguments until there aren't any
            // more, each command adding a row and column to the tableau and rhs
            // Version 2:  for zero and one commands, just note variable name
            //             for later changing of tableau

            int lastSurplus = 1;  // haven't had any s_k type variables yet
            int lastArtificial = n+1;  // a1...an are artificial vars already used

            while( input.hasNextLine() )
            {
                String line = input.nextLine();
                System.out.println("read command line: " + line );
                StringTokenizer st = new StringTokenizer( line );

                String command;
                if( st.hasMoreTokens() )
                    command = st.nextToken();
                else
                    command = "";


                if( command.equals("") )
                {// ignore a blank line
                }

                else if( command.equals( "cut" ) )
                {// add row and col to put in cut constraint
                    // read all the point indices forming S
                    ArrayList<Integer> s = new ArrayList<Integer>();
                    while( st.hasMoreTokens() )
                    {
                        String w = st.nextToken();
                        s.add( Integer.parseInt( w ) );
                    }

                    // for each k in S and j not in S, add in 1 in xk,j
                    for( int kIndex=0; kIndex<s.size(); kIndex++ )
                    {
                        int k = s.get(kIndex);
                        for( int j=1; j<=n; j++ )
                        {
                            if( ! s.contains( j ) )
                            {// j not in S, add xk,j coefficient
                                a[numRows][ getDecVar( k, j )] = 1;
                            }// for j not in S
                        }// for each j in 1..n
                    }// for each k in S

                    // build the rest of the tableau for new row and col
                    rhs[ numRows ] = 2;
                    rowLabels[ numRows ] = "a" + lastArtificial;
                    colLabels[ numCols ] = "s" + lastSurplus;
                    a[ numRows ][ numCols ] = -1;  // coeff for new surplus variable
                    numCols++;
                    colLabels[ numCols ] = "a" + lastArtificial;
                    a[ numRows ][ numCols ] = 1;  // coeff for new artificial variable

                    // update numRows, numCols, last vars
                    numRows++;
                    numCols++;
                    lastSurplus++;
                    lastArtificial++;

                }// cut

                else if( command.equals( "zero" ) || command.equals( "one" ) )
                {// Version 2:  don't change tableau yet, just add to lists:

                    // read the two indices k and j specifying xk,j to be set to 0 or 1

                    int k = Integer.parseInt(st.nextToken());
                    int j = Integer.parseInt(st.nextToken());

                    // make sure they are in order k < j (Version 2 incidental improvement):
                    if( k > j )
                    {
                        int temp = k;
                        k = j;
                        j = temp;
                    }

                    if( command.equals( "zero" ) )
                    {
                        setToZero.add( "x" + k + "," + j );
                    }
                    else
                    {// command "one"
                        setToOne.add( "x" + k + "," + j );
                    }

                }// zero or one

            }// read next command and arguments

            // copy rhs into last column of a
            numCols++;  // make room for rhs column

            colLabels[ numCols-1 ] = "rhs";

            for( int r=0; r<numRows; r++ )
                a[ r ][ numCols-1 ] = rhs[ r ];

//      show( "After building tsp format");

            System.out.println("tableau size: " + numRows + " " + numCols );

            // ***********************
            // Version 2:  now change the tableau to take into account
            //             any zero or one commands

            System.out.println(
                    "Version 2:  adjust tableau to reflect zero, one commands:");

            // adjust for zero commands:
            for( int k=0; k<setToZero.size(); k++ )
            {// simply remove column xk,j and sk,j and row for sk,j
                String var = setToZero.get(k);
                int col = findColumn( var );
                removeColumn( col );
                var = "s" + var.substring( 1 );
                col = findColumn( var );
                removeColumn( col );
                int row = findRow( var );
                removeRow( row );
            }// simply remove column xk,j and sk,j and row for sk,j

            // adjust for one commands:
            for( int k=0; k<setToOne.size(); k++ )
            {// remove column xk,j and sk,j and row for sk,j
                // but also adjust rhs---including row 0---for xkj=1 fact
                String var = setToOne.get(k);
                int col = findColumn( var );

                // adjust rhs to reflect xk,j=1
                // Note that this is only difference between zero and one commands
                for( int r=0; r<numRows; r++ )
                {
                    a[r][numCols-1] -= a[r][col];
                    // notice if user stupidly made too
                    // many edges out of a vertex
                    if( r>0 && a[r][numCols-1] <= -1 )
                    {
                        System.out.println("Too many edges at one point forced by " + var + "=1" );
                        System.exit(0);
                    }
                }


                removeColumn( col );
                var = "s" + var.substring( 1 );
                col = findColumn( var );
                removeColumn( col );
                int row = findRow( var );
                removeRow( row );

            }// remove column xk,j and sk,j and row for sk,j and adjust rhs

            toTeX( "Initial tableau after commands processed" );
            finishTeX();
            // (end of Version 2 changes here) ***********************

        }// tsp format

        // store original tableau for later checking that equality constraints
        // are satisfied
        //   (aOrig includes obj func row, which is not checked, and rhs)
        aOrig = new double[MAXROWS][MAXCOLS];

        for( int r=0; r<numRows; r++ )
        {
            for( int c=0; c<numCols; c++ )
                aOrig[r][c] = a[r][c];
        }

    }// constructor

    // build n random points to form tsp
    public Tableau( Random rng, int n, int halter )
    {
        // not used, but avoid crash
        setToZero = new ArrayList<String>();
        setToOne = new ArrayList<String>();

        haltStep = halter;

        a = new double[MAXROWS][MAXCOLS];
        rowLabels = new String[MAXROWS];
        colLabels = new String[MAXCOLS];

        // build the base tableau (before any cuts or branches)

        pts = new double[ n ][2];

        for( int k=0; k<n; k++ )
        {
            pts[k][0] = rng.nextInt(99) + 1;
            pts[k][1] = rng.nextInt(99) + 1;
        }

        // save data file
        try{
            PrintWriter output = new PrintWriter( new File( "randomTest" ) );
            output.println( n );
            output.println( "points" );

            for( int k=0; k<n; k++ )
                output.println( (int) (pts[k][0]) + " " + (int) (pts[k][1]) );

            output.close();
        }
        catch(Exception e)
        {
            System.out.println("failed to construct random TSP problem");
            System.exit(1);
        }

        // now build the tableau:
        //        z  ak    sij
        int tspVars = (n*(n-1))/2;

        numRows = 1 + n + tspVars;
        //        z  xij  sij ak
        //   (put rhs in separately at the end)
        numCols = 1 + tspVars + tspVars + n;

        System.out.println("tableau size before processing commands: " + numRows + " " + numCols );

        rowLabels[0] = "z";  // obj func row
        a[0][0] = 1;         // z as the permanent basic var for row 0

        colLabels[ 0 ] = "z";

        // put rhs values in special storage, place in a[...][numCols-1] at end
        double[] rhs = new double[MAXROWS];

        // fill in rows 1 through n except for the xij part
        for( int r=1; r<=n; r++ )  // the core constraints
        {
            rowLabels[ r ] = "a" + r;
            a[ r ][ 2*tspVars + r ] = 1;
            rhs[ r ] = 2;

            colLabels[ 2*tspVars + r ] = "a" + r;
        }

        // move through 2D chart of all pairs of points to
        // fill in rest of table
        int count = 0;  // number of previous spots in the 2D chart
        for( int k=1; k<n; k++ )
            for( int j=k+1; j<=n; j++ )
            {// fill in everything needed for x_k,j
                int row = n+1+count;

                // fill in the skj rows
                rowLabels[ row ] = "s" + k + "," + j;
                a[ row ][ 1+count ] = 1;
                a[ row ][ tspVars+1 + count ] = 1;

                rhs[ row ] = 1;

                colLabels[ count+1 ] = "x" + k + "," + j;
                colLabels[ tspVars + 1 + count ] = "s" + k + "," + j;

                // fill in the ckj in obj func row
                a[0][ count+1 ] = dist( k, j );

                // fill in xkj coeffs in first n constraints:
                a[ k ][ count+1 ] = 1;
                a[ j ][ count+1 ] = 1;

                count++;  // get ready for next square
            }

        // copy rhs into last column of a
        numCols++;  // make room for rhs column

        colLabels[ numCols-1 ] = "rhs";

        for( int r=0; r<numRows; r++ )
            a[ r ][ numCols-1 ] = rhs[ r ];

        // store original tableau for later checking that equality constraints
        // are satisfied
        //   (aOrig includes obj func row, which is not checked, and rhs)
        aOrig = new double[MAXROWS][MAXCOLS];

        for( int r=0; r<numRows; r++ )
        {
            for( int c=0; c<numCols; c++ )
                aOrig[r][c] = a[r][c];
        }

    }// constructor

    public void show( String label )
    {
        // debug display of tableau to screen
        System.out.println("-------------------- " + label + " -------------");
        System.out.print("        ");
        for( int c=0; c<numCols; c++ )
            System.out.printf("%8s", colLabels[c] );
        System.out.println();

        for( int r=0; r<numRows; r++ )
        {
            System.out.printf("%8s", rowLabels[r] );

            for( int c=0; c<numCols; c++ )
            {
                System.out.printf("%8.2f", a[r][c] );
            }

            System.out.println();
        }
    }

    public double getOptimalCost()
    {
        return a[0][numCols-1];
    }

    // return the column in tableau for decision variable xk,j
    // (only works before any one or zero commands are processed)
    public int getDecVar( int k, int j )
    {
        // make sure k < j
        if( k > j )
        {
            int temp = k;
            k = j;
            j = temp;
        }

        // compute index for xk,j
        return ((k-1)*k)/2 + (k-1)*(n-k) + (j-k);
    }

    // return Euclidean distance between pts k and j
    public double dist( int k, int j )
    {
        return Math.hypot( pts[k-1][0] - pts[j-1][0], pts[k-1][1] - pts[j-1][1] );
    }

    private String fix( String s )
    {
        if( s.length()>1 )
            return s.charAt(0) + "_{" + s.substring(1) + "}";
        else
            return s;
    }

    // improved toTeX that splits large (column-wise) tableau
    // into multiple pages

    private static int maxCols=20;  // number of columns per sheet
    private static int displayWidth = 6;  // tableau numbers displayed % displayWidth.decimals
    private static int decimals = 3;

    private void toTeX( String message )
    {
        System.out.println( message );
        out.println("{\\bf " + message + "}\n\\medskip" );

        int firstCol = 0;
        int lastCol = Math.min( maxCols, numCols );  // last column shown on current sheet

        while( firstCol < numCols )
        {// draw columns firstCol through lastCol-1 on a sheet

//        System.out.println("toTeX sending columns " + firstCol + " through " + lastCol + " to file" );

            out.println("\\beginpicture");
            out.println("\\setcoordinatesystem units <24pt,15pt>");

            int h = numRows;
            int w = lastCol-firstCol;

            out.println("\\putrectangle corners at 0 0 and " + w + " " + h );

            // draw the labels
            int k;
            for( k=0; k<numRows; ++k )
                out.println("\\put {$" + fix( rowLabels[k] ) +
                        "$} [r] at -0.25 " + (h-k-0.5) );

            for( k=firstCol; k<lastCol; ++k )
                out.println("\\put {$" + fix( colLabels[k] ) +
                        "$} [b] at " + ((k-firstCol)+0.5) + " " + (h+0.25) );

            for( int r=0; r<numRows; r++ )
                for( int c=firstCol; c<lastCol; c++ )
                {
                    //           System.out.println("toTeX printing row " + r + " col " + c );
                    if( a[r][c] == (int) a[r][c] )
                        out.println("\\put {$" + ((int) a[r][c]) +
                                "$} at " + (0.5+(c-firstCol)) + " " + (h-r-0.5) );
                    else
                        out.println("\\put {$" + String.format("%6.3f",a[r][c]) + "$} at " +
                                (0.5+(c-firstCol)) + " " + (h-r-0.5) );
                }

            out.println("\\endpicture");

            out.println("\\vfil\\eject");

//        System.out.println("finished with columns " + firstCol + " through " + lastCol );

            // advance to next block of columns
            firstCol = lastCol;
            lastCol = Math.min( firstCol + maxCols, numCols );
//        System.out.println("advance to firstCol " + firstCol + " lastCol " + lastCol + " and numCols " + numCols );

        }// draw columns firstCol through lastCol on a sheet

    }// toTeX

    public void finishTeX()
    {
        out.println("\\bye");
        out.close();
    }

    // perform phase 1 and phase 2 simplex method automatically,
    // with debug output to screen
    public void doSimplexMethod()
    {
        // grab row 0 --- the phase 2 obj func coeffs ---
        // and save for phase 2, and create phase 1 obj func row
        double[] phase2ObjFunc = new double[ numCols ];
        for( int c=0; c<numCols; c++ ){
            phase2ObjFunc[c] = a[0][c];
            if( colLabels[c].charAt(0) == 'a' )
                a[0][c] = 1;
            else
                a[0][c] = 0;
        }
        a[0][0] = 1;  // permanent z coefficient

        System.out.println("Begin Phase 1=================================");

        export( 1 );

        // price out for phase 1
        // (just for simplicity, price out all basic vars instead
        //  of just the artificials)
        for( int pr=1; pr<numRows; pr++ )
        {// for each pivot row, scan across to find pivot spot and
            // add multiple to cost row

            boolean found = false;
            int pc=-1;

            for( int c=0; !found && c<numCols-1; c++ )
            {
                if( rowLabels[pr].equals( colLabels[c] ) )
                {
                    pc = c;
                    found = true;
                    System.out.println("price out " + colLabels[c] );
                }
            }

            // add multiple of pivot row to zero out pivot col
            // (can assume there's a 1 in a[pr][pc])
            double val = a[0][pc];
            for( int c=0; c< numCols; c++ )
            {
                a[0][c] -= val * a[pr][c];
            }

        }// for each basic variable

        // do pivot steps until reach optimal tableau
        boolean foundNeg = true;
        do{
            int j = findNegReducedCost( true );
            if( j > 0 )
            {// found one
                int pr = findMinRatio( j );
                if( pr > 0 )
                    pivot( pr, j, true );
                else
                {// if no candidates for min ratio, unbounded, stop
                    System.out.println("Found unbounded problem");
                    System.exit(0);
                }
            }
            else
                foundNeg = false;
        }while( foundNeg );

        // remove degenerate artificial variables (probably only introduced by "one" command)
        // by finding paired surplus variable and making it basic instead of the artificial
        // Can be any number of such rows, though, since can have any number of "one" commands
        for( int r=1; r<numRows; r++ )
            if( rowLabels[ r ].charAt(0) == 'a' )
            {// have degenerate basic artificial variable
                if( a[r][numCols-1] > 0 )
                {
                    System.out.println("ended Phase 1 without getting rid of all artificial variables");
                    System.exit(0);
                }

                // replace this artificial variable by surplus variable immediately to its left
                // (first have to locate its column, though)
                int j = -1;
                for( int c=1; c<numCols-1 && j<0; c++ )
                    if( colLabels[ c ].equals( rowLabels[r] ) )
                        j = c;
                // artificial is in column j, replace by variable one column to left
                System.out.println("replacing degenerate artificial variable"+
                        " from \"one\" command by surplus paired with it");
                pivot( r, j-1, true );
                a[r][numCols-1] = 0;

            }// have degenerate basic artificial variable

        // copy saved phase 2 obj func row back in
        for( int c=0; c<numCols; c++ )
            a[0][c] = phase2ObjFunc[ c ];

        System.out.println("Begin Phase 2=================================");

        // price out for phase 2
        for( int pr=1; pr<numRows; pr++ )
        {// for each pivot row, scan across to find pivot spot and
            // add multiple to cost row

            boolean found = false;
            int pc=-1;

            for( int c=0; !found && c<numCols-1; c++ )
            {
                if( rowLabels[pr].equals( colLabels[c] ) )
                {
                    pc = c;
                    found = true;
                }
            }

            // add multiple of pivot row to zero out pivot col
            // (can assume there's a 1 in a[pr][pc])
            double val = a[0][pc];
            for( int c=0; c< numCols; c++ )
            {
                a[0][c] -= val * a[pr][c];
            }

        }// for each pivot row

        // export priced out phase 2 tableau so can manually
        // check (a serious debugging step)
        export( 2 );

        // do pivot steps until reach phase 2 optimal tableau
        foundNeg = true;
        do{

            int j = findNegReducedCost( false );
            if( j > 0 )
            {// found one
                int pr = findMinRatio( j );
                if( pr > 0 )
                    pivot( pr, j, false );
                else
                {// if no candidates for min ratio, unbounded, stop
                    System.out.println("Found unbounded problem");
                    System.exit(0);
                }
            }
            else
                foundNeg = false;
        }while( foundNeg && stepNumber < haltStep );

        if( !foundNeg )
            System.out.println("Found optimal point in Phase 2");

        // export( 3 );

    }

    public double[][] getPoints()
    {
        return pts;
    }

    public String[] getBasicVariableNames()
    {
        // produce the actual basic variables
        String[] names = new String[ numRows + setToOne.size() ];
        for( int r=0; r<numRows; r++ )
            names[r] = rowLabels[r];

        // append the variables forced to one
        for( int k=0; k<setToOne.size(); k++ )
            names[numRows+k] = setToOne.get(k);

        return names;
    }

    public double[] getBasicVariableValues()
    {
        double[] values = new double[ numRows + setToOne.size() ];
        for( int r=0; r<numRows; r++ )
            values[r] = a[r][numCols-1];

        for( int k=0; k<setToOne.size(); k++ )
            values[numRows+k] = 1;

        return values;
    }

    // pivot on element (pr,pc) of a
    // (of course crashes if this element is 0)
    // and update basic variable for the pivot row
    public void pivot( int pr, int pc, boolean phase1 )
    {
        stepNumber++;

// if( stepNumber == 19 )
//   export( 1 );

        // divide each element of row pr by the pivot element
        double val = a[pr][pc];
        for( int c=0; c<numCols; c++ )
            a[pr][c] /= val;

        for( int r=0; r<numRows; r++ )
            if( r != pr )
            {// add multiple of pivot row to row r
                val = a[r][pc]; // the value to be zeroed out
                for( int c=0; c<numCols; c++ )
                    a[r][c] -= val * a[pr][c];
            }

        System.out.println("------- pivot step: " + stepNumber +
                " obj func = " + a[0][numCols-1] +
                ", " +
                rowLabels[pr] + " leaves, " +
                colLabels[pc] + " enters" );

        // update basic variable
        rowLabels[pr] = colLabels[pc];

        // debug output---show basic vars and values for all rows
        for( int r=0; r<numRows; r++ )
            if( rowLabels[r].charAt(0) == 'x' )
            {
                double v = a[r][numCols-1];

                if( Math.abs( v ) < tiny  ||
                        Math.abs( v - 0.5 ) < tiny ||
                        Math.abs( v - 1 ) < tiny
                        )
                    System.out.printf("%10s %20.10f\n", rowLabels[r], a[r][numCols-1] );
                else
                {
                    System.out.println("Something horribly wrong!");
                    System.out.printf("%10s %20.10f\n", rowLabels[r], a[r][numCols-1] );
//          System.exit(1);
                }
            }
            else
            {// just display all other kinds of variables
                System.out.printf("%10s %20.10f\n", rowLabels[r], a[r][numCols-1] );
            }

        // form vector of all variable values---spot 0 is first (omitting z)
        // and rhs not included
        double[] varValues = new double[ numCols-2 ];
        for( int c=1; c<numCols-1; c++ )
        {// figure out value of colLabels[c], put in varValues[c-1]:
            varValues[c-1] = valueOfVariable( colLabels[c] );
//      System.out.println( colLabels[c] + " has value " + varValues[c-1] );
        }

        boolean fatalError = false;

        // for each row, compute aOrig*varValues and compare to last col of aOrig
        // and warn/halt if noticeably different

        for( int r=1; r<numRows; r++ )
        {
            double value = 0;
            for( int c=1; c<numCols-1; c++ )
            {
                value += aOrig[r][c] * varValues[ c-1 ];
            }

            if( Math.abs( value - aOrig[r][numCols-1] ) > tiny )
            {
                fatalError = true;
                System.out.println("CHECKING constraints satisfied with original matrix----");
                System.out.println("Detected fatal error---original constraint in row " + r + " not satisfied!");
                System.out.println("Here are the variable names, values, and coeffs in the row" +
                        " where both are non-zero:");
                for( int c=1; c<numCols-1; c++ )
                {
                    double coeff = aOrig[r][c], var = valueOfVariable( colLabels[c] );
                    if( Math.abs(coeff)>tiny && Math.abs(var)>tiny )
                        System.out.printf("%10s  %18.9f %18.9f --->  %18.9f\n",
                                colLabels[c], var, coeff, var*coeff  );
                }

                System.out.println("whereas the original rhs for this row is " + aOrig[r][numCols-1] );
            }
        }

        // for each row, compute a*varValues and compare to last col of a
        // and warn/halt if noticeably different

        for( int r=1; r<numRows; r++ )
        {
            double value = 0;
            for( int c=1; c<numCols-1; c++ )
            {
                value += a[r][c] * varValues[ c-1 ];
            }

            if( Math.abs( value - a[r][numCols-1] ) > tiny )
            {
                fatalError = true;
                System.out.println("CHECKING constraints satisfied with current matrix----");
                System.out.println("Detected fatal error---original constraint in row " + r + " not satisfied!");
                System.out.println("Here are the variable names, values, and coeffs in the row" +
                        " where both are non-zero:");
                for( int c=1; c<numCols-1; c++ )
                {
                    double coeff = a[r][c], var = valueOfVariable( colLabels[c] );
                    if( Math.abs(coeff)>tiny && Math.abs(var)>tiny )
                        System.out.printf("%10s  %18.9f %18.9f --->  %18.9f\n",
                                colLabels[c], var, coeff, var*coeff  );
                }

                System.out.println("whereas the original rhs for this row is " + a[r][numCols-1] );
            }
        }

        if( fatalError )
            System.exit(1);

    }// pivot

    private boolean weirdValue( double x )
    {
        return Math.abs( x - 0 ) > tiny &&
                Math.abs( x - 0.5 ) > tiny &&
                Math.abs( x - 1 ) > tiny;
    }

    // if the variable in column c is xk,j with
    // k or j equal to m, return its value, else
    // return 0
    private double valueIfMatches( int c, int m )
    {
        String var = colLabels[ c ];
        if( var.charAt(0) == 'x' )
        {// might match, get k, j
            int comma = var.indexOf( ',' );
            int k = Integer.parseInt( var.substring( 1, comma ) );
            int j = Integer.parseInt( var.substring( comma+1 ) );
            if( k==m || j==m )
            {// matches, look up value rowLabels
                return valueOfVariable( var );
            }
            else
                return 0;
        }
        else
            return 0;  // not an xk,j variable
    }

    // return value of variable s by looking for it
    // in rowLabels and if found giving its value,
    // otherwise 0
    private double valueOfVariable( String s )
    {
        int index = -1;
        for( int r=1; r<numRows && index==-1; r++ )
            if( rowLabels[r].equals( s ) )
                index = r;
        if( index == -1 )
            return 0;  // not found in basics
        else
            return a[index][numCols-1];
    }

    // return col index of most neg reduced cost
    // or -1 if none found
    // If doing phase1, don't ignore the artificial columns
    public int findNegReducedCost( boolean phase1 )
    {
        // find smallest reduced cost
        int mostNeg = 0; // a[0][0] is always 1, so good start
        for( int c=1; c<numCols-1; c++ )
            if( a[0][c] < a[0][mostNeg] &&
                    (phase1 || colLabels[c].charAt(0) != 'a' )
                    )
                mostNeg = c;

        // if smallest is not significantly negative, return -1
        if( a[0][mostNeg] > -tiny )
            return -1;
        else
            return mostNeg;
    }
/*

  NOTE:  this led to erroneous rounding error issues
         (left here to remind us of the difficulty)

  // return col index of leftmost neg reduced cost
  // or -1 if none found
  // If doing phase1, don't ignore the artificial columns
  public int findNegReducedCost( boolean phase1 )
  {
    for( int c=0; c<numCols-1; c++ )
      if( a[0][c] < -tiny && (phase1 || colLabels[c].charAt(0) != 'a' ) )
        return c;
    return -1;
  }
*/

    // find min ratio row for column j,
    // over rows with coefficients larger than tiny
    // (in case of ties, use one with largest denominator)
    // return -1 if no candidates
    public int findMinRatio( int  j )
    {
        // find first candidate row
        int start = 0;
        for( int r=1; r<numRows && start==0; r++ )
            if( a[r][ j ] > tiny )
                start = r;

        // either unbounded or have a min ratio row
        if( start == 0 )
            return -1;
        else
        {// have a place to start
            int minRow = start;
            double minRat = a[minRow][numCols-1]/a[minRow][ j ];
            for( int r=start+1; r<numRows; r++ )
            {
                double arhs = a[r][numCols-1], aj = a[r][ j ];
                if( aj > tiny && arhs/aj < minRat )
                {// found a better row
                    minRow = r;
                    minRat = arhs/aj;
                }
            }

            return minRow;

        }// have a place to start

    }// findMinRatio

    // save tableau to "export" file, ready
    // to be read by ManualSimplex for debugging
    private void export( int tabNumber )
    {
        try{
            PrintWriter output = new PrintWriter( new File( "tempExport" + tabNumber ) );

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
            System.out.println("export failed for some reason");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception
    {
        String fileName = FileBrowser.chooseFile( true );
        Scanner input = new Scanner( new File( fileName ) );
        Tableau table = new Tableau( "tsp", input, 100000 );

        table.toTeX("initial tableau in Tableau.main:" );

        table.doSimplexMethod();

        table.finishTeX();

        String[] names = table.getBasicVariableNames();
        double[] values = table.getBasicVariableValues();

        System.out.println("Optimal values:");
        for( int r=0; r<names.length; r++ )
            System.out.printf("%10s %20.10f\n", names[r], values[r] );
    }

}