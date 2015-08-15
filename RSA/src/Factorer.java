import java.math.BigInteger;
import java.util.Scanner;

public class Factorer
{
    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        System.out.print("Enter big odd positive composite integer to be factored: ");
        BigInteger n = new BigInteger( keys.nextLine() );

        BigInteger zero = new BigInteger( "0" );
        BigInteger two = new BigInteger( "2" );

        BigInteger f = new BigInteger( "3" );

        boolean done = false;

        while( !done )
        {
            if( n.mod( f ).equals( zero ) )
            {
                System.out.println("The target number factors as ");
                System.out.println( f );
                System.out.println( "*" );
                System.out.println( n.divide( f ) );
                done = true;
                System.out.println("\n\nHit <enter> to close the window");
                keys.nextLine();
            }
            else
            {
                System.out.println( f + " is not a factor");
                f = f.add( two );
            }
        }
    }
}