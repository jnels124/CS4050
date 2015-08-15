import java.math.BigInteger;
import java.util.Scanner;

public class Encrypt
{

    public static BigInteger zero = new BigInteger("0");
    public static BigInteger hundred = new BigInteger("100");

    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        System.out.print("Enter the RSA public key value n: " );
        BigInteger n = new BigInteger( keys.nextLine() );
        System.out.print("Enter the RSA public key value e: " );
        BigInteger e = new BigInteger( keys.nextLine() );

        BigInteger a;
        boolean okay = true;
        do{
            System.out.print("Please enter your plain-text message: ");
            String s = keys.nextLine();

            a = zero;

            for( int k=0; k<s.length(); k++ )
            {// turn symbol k into two digits and add to a
                BigInteger sym = new BigInteger( "" + (s.charAt(k)-31) );
                a = a.multiply( hundred ).add( sym );
            }

            if( a.compareTo( n ) >= 0 )
            {
                okay = false;
                System.out.println("Oops, your message is too long, try again");
            }
            else
                okay = true;
        }while( !okay );

        System.out.println("The encoded message is\n" + a );

        BigInteger c = a.modPow( e, n );
        System.out.println("Send the encrypted message:\n" + c );

    }

}