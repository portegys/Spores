/*
 * Combinations.
 *
 * Source:
 * Ryan Stansifer, e-mail: ryan@cs.fit.edu
 */

package spores;

import java.math.BigInteger;


class Combinations {

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

	public static void main (String [] args) {
		try {
			final long n = Long.valueOf(args[0]).longValue();
			final long m = Long.valueOf(args[1]).longValue();
			System.out.println (choose(n,m));
			System.out.println (sterling(n,m));
			System.out.println (sum(n));
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println ("Two arguments are required.");
		} catch (NumberFormatException e) {
			System.out.println ("The argument must be an integer.");
		}
	}
}


