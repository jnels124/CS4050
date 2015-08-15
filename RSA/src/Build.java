/*  
   A tiny program that takes p and q and 
   reports n and f
*/

import java.util.Scanner;
import java.math.BigInteger;

public class Build
{
    private static BigInteger one=new BigInteger("1");

    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        BigInteger p, q, n, f;

        System.out.print("enter first prime number:\n");
        p = new BigInteger( keys.nextLine() );
        System.out.print("enter second prime number:\n");
        q = new BigInteger( keys.nextLine() );

        n = p.multiply(q);
        f = p.subtract(one).multiply( q.subtract(one) );
        System.out.println("n=\n\n" + n + "\n\nf =\n\n" + f + "\n\n" );
        System.out.println("\n\nHit <enter> to close the window");
        keys.nextLine();
    }

}