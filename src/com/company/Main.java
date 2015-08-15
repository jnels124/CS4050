package com.company;

public class Main {

    public static void main(String[] args) {
	// write your code here
        System.out.println(modExp(7, 128, 13));
    }

    public static int modExp (int a, int k, int n) {
        if (k == 0)
            return 1;

        else {
            int temp = modExp(a, k / 2, n);
            int result = (temp * temp) % n;

            if ( k % 2 == 1)
                result = (result * a) % n;

            return result;
        }
    }
}
