/*  user creates primes p, q above input values,
    sees n, f, enters e, 
    check gcd(f,e)=1, see d, enter repeated
    a and see c followed by a check
*/

import java.util.Scanner;
import java.math.BigInteger;

public class BuildRSA
{
    private static BigInteger one=new BigInteger("1");
    private static BigInteger two=new BigInteger("2");
    private static BigInteger zero=new BigInteger("0");

    public static void main(String[] args)
    {
        BigInteger one=new BigInteger("1");
        BigInteger two=new BigInteger("1");

        Scanner keys = new Scanner( System.in );
        BigInteger start, p, q, n, f, e, d, a, c, aCheck;

        System.out.print("enter integer to be below prime p: ");
        start = new BigInteger( keys.nextLine() );
        p = start.nextProbablePrime();
        System.out.println("p: " + p );
        System.out.print("enter integer to be below prime q: ");
        start = new BigInteger( keys.nextLine() );
        q = start.nextProbablePrime();
        System.out.println("q: " + q );

        n = p.multiply(q);
        f = p.subtract(one).multiply( q.subtract(one) );
        System.out.println("n= " + n + " f = " + f );

        boolean okay=true;
        do{
            System.out.print("Enter e: ");
            e = keys.nextBigInteger();

            if( ! f.gcd( e ).equals( one ) )
            {
                System.out.println("Oops, f and e are not relatively prime");
                System.out.println("Try again");
                okay=false;
            }
            else
                okay = true;
        }while( !okay );

        BigInteger[] info = gcd( f, e, 0 );
        System.out.println("gcd(f,e)=" + info[0] +
                " f multiplier= " + info[1] + " e multiplier= " + info[2] );
        d = info[2];
        System.out.println("d= " + d );

        System.out.println("Check: d*e in Zf = " + (d.multiply(e)).mod( f ) );

        do{
            System.out.print("enter message a: ");
            a = keys.nextBigInteger();

            c = a.modPow( e, n );
            System.out.print("encrypted message is: " + c );

            aCheck = c.modPow( d, n );
            System.out.println(" unencrypted message is: " + aCheck );

        }while( true );

    }

    public static BigInteger[] gcd( BigInteger a, BigInteger b, int depth )
    {
        BigInteger[] x = new BigInteger[3];
        if( a.equals( zero ) )
        {
            if( depth % 2 == 1 )
            {
                x[0] = b;  x[1] = zero; x[2] = one;
            }
            else
            {
                x[0] = b;  x[1] = one; x[2] = one;
            }
        }
        else if( b.equals(zero) )
        {
            if( depth % 2 == 1 )
            {
                x[0] = a;  x[1] = one;  x[2] = zero;
            }
            else
            {
                x[0] = a;  x[1] = one;  x[2] = one;
            }
        }
        else
        {
            BigInteger q = a.divide(b), r = a.mod(b);
            BigInteger[] xprime = gcd( b, r, depth+1 );

            x[0] = xprime[0];
            x[1] = xprime[2];  x[2] = xprime[1].subtract( q.multiply(xprime[2]));
        }

        System.out.println("gcd on " + a + " " + b + " gives " + x[0] + " " +
                x[1] + " " + x[2] );

        return x;
    }

}