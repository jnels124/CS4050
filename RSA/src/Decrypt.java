import java.math.BigInteger;
import java.util.Scanner;

public class Decrypt
{

    public static BigInteger zero = new BigInteger("0");
    public static BigInteger hundred = new BigInteger("100");

    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        System.out.print("Enter the RSA public key value n: " );
        BigInteger n = new BigInteger( keys.nextLine() );
        System.out.print("Enter the RSA ultra-secret value d: " );
        BigInteger d = new BigInteger( keys.nextLine() );

        System.out.print("Please enter the encrypted message:\n");
        BigInteger c = new BigInteger( keys.nextLine() );

        BigInteger a = c.modPow( d, n );

        System.out.println("The original message is\n" + a );

        String s = "";
        while( a.compareTo( zero ) > 0 )
        {
            int sym = a.mod( hundred ).intValue();
            a = a.divide( hundred );
            s = ""+(char)(sym+31) + s;
        }

        System.out.println("or, in plain-text:\n" + s );

    }

}