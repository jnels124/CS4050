import java.math.BigInteger;
import java.util.Scanner;

public class Decode
{
    private static BigInteger zero = new BigInteger( "0" );
    private static BigInteger hundred = new BigInteger( "100" );

    public static void main(String[] args)
    {
        Scanner keys = new Scanner( System.in );
        System.out.print("Please enter the encoded message:\n");
        BigInteger a = new BigInteger( keys.nextLine() );

        String s = "";
        while( a.compareTo( zero ) > 0 )
        {
            int sym = a.mod( hundred ).intValue();
            a = a.divide( hundred );
            s = ""+(char)(sym+31) + s;
        }

        System.out.println("The decoded message is:\n" + s );


    }

}