/*
 * Generate spores behavior data.
 */

import java.math.BigInteger;


class SporesDataGen {

	static BigInteger choose (long n, final long m) {
		BigInteger r = BigInteger.valueOf (1);
		if (2*m>n) choose(n,n-m);
		for (int i=1; i<=m; n--, i++) {
			r = (r.multiply (BigInteger.valueOf(n))).divide (BigInteger.valueOf (i));
		}
		return (r);
	}

	static BigInteger sum (final long p) {
		BigInteger r = BigInteger.valueOf (0);
		for (long i=1; i<=p; i++) {
			r = r.add (fact(i).multiply (BigInteger.valueOf(sterling (p, i))));
		}
		return (r);
	}

	public static BigInteger fact (final long n) {
		BigInteger r = BigInteger.valueOf (1);
		for (long i=2; i<=n; i++) {
			r = r.multiply (BigInteger.valueOf (i));
		}
		return r;
	}

	static long sterling (final long n, final long i) {
		if (i==0) return 0;
		if (n==i) return 1;
		return (i*sterling(n-1,i)+sterling(n-1,i-1));
	}

    public static double probability(long networkSize, long fileCount, long numActive)
    {
        if (numActive == 0 || fileCount == 0) return 0.0;
        if (networkSize - fileCount < numActive) return 1.0;
        BigInteger n = choose(networkSize - fileCount, numActive).multiply(new BigInteger(new String("100")));
        double p = n.divide(choose(networkSize, numActive)).doubleValue() / 100.0;
        return 1.0 - p;
    }

	public static void main (String [] args) {
		long networkSize, numActive, increment, max;
		networkSize = numActive = increment = max = 0;
		try {
			networkSize = Long.valueOf(args[0]).longValue();
			numActive = Long.valueOf(args[1]).longValue();
			increment = Long.valueOf(args[2]).longValue();
			max = Long.valueOf(args[3]).longValue();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println ("Usage: java SporesDataGen <network size> <number of active peers> <file saturation increment> <file saturation maximum>");
			System.exit(1);
		} catch (NumberFormatException e) {
			System.out.println ("The argument must be an integer.");
			System.exit(1);
		}
		System.out.println("Network size = " + networkSize);
		System.out.println("Active peers = " + numActive);
		System.out.println("File saturation increment = " + increment);
		System.out.println("File saturation maximum = " + max);
		for (long i = 0; i <= max; i += increment)
		{
			System.out.println(i);
		}
		for (long i = 0; i <= max; i += increment)
		{
			System.out.println(probability(networkSize, i, numActive));
		}
	}
}


