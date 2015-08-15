import java.math.BigInteger;
import java.util.Scanner;

public class Encode
{

    public static BigInteger zero = new BigInteger("0");
    public static BigInteger hundred = new BigInteger("100");

    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        BigInteger a;
        boolean okay = true;
        System.out.print("Please enter your plain-text message: ");
        String s = keys.nextLine();

        a = zero;

        for( int k=0; k<s.length(); k++ )
        {// turn symbol k into two digits and add to a
            BigInteger sym = new BigInteger( "" + (s.charAt(k)-31) );
            a = a.multiply( hundred ).add( sym );
        }

        System.out.println("The encoded message is\n" + a );
    }

}