/*  user enters an integer and the program finds the
    next probable prime from that number upward
*/

import java.util.Scanner;
import java.math.BigInteger;

public class FindPrime
{
    private static BigInteger one=new BigInteger("1");
    private static BigInteger two=new BigInteger("2");
    private static BigInteger zero=new BigInteger("0");

    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        System.out.print("enter starting integer: ");
        BigInteger start = new BigInteger( keys.nextLine() );

        boolean done = false;
        BigInteger n = start;
        if( n.mod( two ).equals( zero ) )
            n = n.add( one );

        int count = 1;

        do{
            BigInteger nMinus1 = n.subtract( one );
//      BigInteger x = ModExp.modExp( two, nMinus1, n, true );
            BigInteger x = two.modPow( nMinus1, n );
            if( x.equals( one ) )
            {
                System.out.println("After " + count + " tries, Found probable prime:\n" + n );
                done = true;
                System.out.println("\n\nHit <enter> to close the window");
                keys.nextLine();
            }
            else
            {
                n = n.add( two );
                System.out.println("Try " + n + "\n\n\n" );
                count++;
            }
        }while( !done );

    }

}