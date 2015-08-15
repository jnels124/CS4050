/*
  tiny utility class
  to hold (k,j)
  with k<j
*/
public class Pair
{
    public int first, second;

    public Pair( int f, int s )
    {
        if( f < s )
        {
            first = f;
            second = s;
        }
        else
        {
            first = s;
            second = f;
        }
    }

    public Pair( Pair other )
    {
        first = other.first;
        second = other.second;
    }

}