package edu.pitt.cs;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>Uses the Java Path Finder model checking tool to check the Bank transfer
 * method. The code seems to work when tested with plain JUnit using "mvn test".
 * But you will discover that it has several data races when you run it on top
 * of JPF. You may also encounter deadlocks in the debugging process.
 */

public class BankTest {
	/**
	 * Test case for void transfer(int from, int to, int amount).
	 * 
	 * <pre>
	 * Preconditions: bank is initialized with Bank with 4 accounts and an initial balance of 100.
	 * Execution steps: 1. Call bank.transfer(0, 2, 10);
	 *                  2. Call bank.transfer(2, 0, 10);
	 * Invariant: bank.getTotalBalance() returns 400.
	 * </pre>
	 */
	@Test
	public void testShoot() {
		// TODO: Implement
	}
}
