/*
   a node of the branch and bound tree
   for AutoHeuristicTSP
*/

import java.util.ArrayList;
import java.util.StringTokenizer;

public class Node
{
    private static int currentId = 0;

    public int id;
    public ArrayList<ArrayList<Integer>> cuts;
    public ArrayList<Pair> zeros;
    public ArrayList<Pair> ones;

    // remember info from Tableau
    public double score;
    public String[] variables;
    public double[] values;

    // build a node with no commands
    public Node()
    {
        currentId++;  id = currentId;

        cuts = new ArrayList<ArrayList<Integer>>();
        zeros = new ArrayList<Pair>();
        ones = new ArrayList<Pair>();

        solve();
    }

    public void solve()
    {
        Tableau2 table = new Tableau2( this );
        table.doSimplexMethod();
        score = - table.getOptimalCost();
        variables = table.getBasicVariableNames();
        values = table.getBasicVariableValues();
    }

    // deep copy original node and depending on whether which is
    // "zero" or "one" add (k,j) to zeros or ones
    public Node( Node orig, String which, int k, int j )
    {
        currentId++;  id = currentId;

        cuts = new ArrayList<ArrayList<Integer>>();
        for( int r=0; r<orig.cuts.size(); r++ )
        {
            // make copy of orig list r
            ArrayList<Integer> copy = new ArrayList<Integer>();
            for( int c=0; c<orig.cuts.get(r).size(); c++ )
                copy.add( orig.cuts.get(r).get(c) );
            cuts.add( copy );
        }

        zeros = new ArrayList<Pair>();
        for( int m=0; m<orig.zeros.size(); m++ )
        {
            zeros.add( new Pair( orig.zeros.get( m ) ) );
        }

        ones = new ArrayList<Pair>();
        for( int m=0; m<orig.ones.size(); m++ )
        {
            ones.add( new Pair( orig.ones.get( m ) ) );
        }

        // now add the new zero or one command
        if( which.equals( "zero" ) )
            zeros.add( new Pair( k, j ) );
        else
            ones.add( new Pair( k, j ) );

        solve();
    }

    // process s into a cut command, if
    // is valid, and return true, otherwise
    // return false
    public boolean addCut( String s )
    {
        ArrayList<Integer> aCut = new ArrayList<Integer>();
        s = s.substring(4);  // toss the irritating "cut " part

        StringTokenizer st = new StringTokenizer( s );
        while( st.hasMoreTokens() )
        {
            String w = st.nextToken();
            try
            {
                int x = Integer.parseInt( w );
                if( 1<=x && x<=AutoHeuristicTSP.points.length )
                    aCut.add( x );
                else
                    return false;
            }
            catch(Exception e)
            {
                return false;
            }
        }

        // succeeded
        cuts.add( aCut );

        return true;

    }// addCut

    // process s into two new nodes, one with
    // a zero command and the other with a one
    // command, both for the two points coded up in s,
    // or return null if s is invalid
    public Node[] branch( String s )
    {
        Node[] nodes = new Node[2];

        s = s.substring(10);  // toss the irritating "branch on " part

        ArrayList<Integer> points = new ArrayList<Integer>();
        StringTokenizer st = new StringTokenizer( s );
        while( st.hasMoreTokens() )
        {
            String w = st.nextToken();
            try
            {
                int x = Integer.parseInt( w );
                if( 1<=x && x<=AutoHeuristicTSP.points.length )
                    points.add( x );
                else
                    return null;
            }
            catch(Exception e)
            {
                return null;
            }
        }

        if( points.size() == 2 && points.get(0).compareTo(points.get(1)) != 0 )
        {// have two valid points coded up in s

            nodes[0] = new Node( this, "zero", points.get(0), points.get(1) );
            nodes[1] = new Node( this, "one", points.get(0), points.get(1) );

            return nodes;
        }
        else
            return null;

    }// addCut

    public String toString()
    {
        String r = "Id: " + id + " score: " + score + "\n";
        r += "Cuts:\n";
        for( int k=0; k<cuts.size(); k++ )
        {
            r += "[";
            for( int j=0; j<cuts.get(k).size(); j++ )
                r += cuts.get(k).get(j).toString() + " ";
            r += "] ";

        }
        r += "\n";

        r += "Zeros: ";
        for( int m=0; m<zeros.size(); m++ )
            r += zeros.get(m).first + "-" + zeros.get(m).second + " ";

        r += "  ";

        r += "Ones: ";
        for( int m=0; m<ones.size(); m++ )
            r += ones.get(m).first + "-" + ones.get(m).second + " ";

        r += "\n";

        return r;
    }

}