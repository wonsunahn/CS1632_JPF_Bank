- [Exercise 5 - Static Analysis Extra Credit](#exercise-5---static-analysis-extra-credit)
  * [Manual Testing the Application](#manual-testing-the-application)
  * [Writing a Property-based Test for the Application](#writing-a-property-based-test-for-the-application)
  * [Running Property-based Test on top of JPF](#running-property-based-test-on-top-of-jpf)
  * [Fixing Your First Data Race](#fixing-your-first-data-race)
  * [Fixing Other Data Races](#fixing-other-data-races)
    + [Restrictions while Fixing](#restrictions-while-fixing)
    + [Hints while Fixing](#hints-while-fixing)
  * [Submission](#submission)
  * [GradeScope Feedback](#gradescope-feedback)
  * [Groupwork Plan](#groupwork-plan)

# Exercise 5 - Static Analysis Extra Credit

Fall Semester 2023 - Exercise 5

* DUE: December 15 (Friday), 2023 11:59 PM

For this extra credit, you will learn how to use the Java Path Finder to debug
nondeterministic programs due to parallelism.  You will encounter problems such
as data races, deadlocks, and incorrect thread interleavings and debug them
with the help of JPF.  An extra credit of 1 point out of 100 points for the
entire course will be awarded to any group that is able to completely debug the
program.

The application that you will be debugging is a banking system with multiple
customer accounts that allows the customer to transfer money between accounts.
Each transfer transaction is performed in a thread that runs concurrently with
the main thread, as well as other transaction threads.

## Manual Testing the Application

If you are not using VSCode, you may need to first compile the application:

```
mvn compile
```

Let's try manual testing the main method of the Bank class:

```
java -cp target/classes edu.pitt.cs.Bank 4 100
```

... and the system responds with:

```
Usage: Bank <number of accounts> <account initial balanace>
```

Let's create a bank with 4 accounts with $100 in each account:

```
java -cp target/classes edu.pitt.cs.Bank 4 100
```

Here is an example session with the application:

```
$ java -cp target/classes edu.pitt.cs.Bank 4 100
Enter command ('t' for transfer or 's' for summary or 'q' to quit): s
[Account Summary]
Account 0: $100
Account 1: $100
Account 2: $100
Account 3: $100
Total balance: $400
Total transaction count: 0
Enter command ('t' for transfer or 's' for summary or 'q' to quit): t
Enter your 'from' account number [0-3]: 1
Enter your 'to' account number [0-3]: 2
Enter amount: 10
Enter command ('t' for transfer or 's' for summary or 'q' to quit): s
[Account Summary]
Account 0: $100
Account 1: $90
Account 2: $110
Account 3: $100
Total balance: $400
Total transaction count: 1
Enter command ('t' for transfer or 's' for summary or 'q' to quit): q
Goodbye!
```

The application seems to work fine doing manual testing but don't be fooled,
there are multiple data races hiding in the shadows.  The only reason that you
didn't observe the data races is because the probability that the relative
speeds of the threads being just right in order to trigger the data races is
exceedingly low.

## Writing a Property-based Test for the Application

Currently, the only properties that are being checked by JPF while doing state
space exploration is: 1) there are no uncaught exceptions and 2) there are no
data races or deadlocks.  The latter was enabled by adding a data race listener
to the [JUnit.win.jpf](JUnit.win.jpf) and [JUnit.macos.jpf](JUnit.macos.jpf)
files:

```
listener = gov.nasa.jpf.listener.PreciseRaceDetector
```

In order to more rigorously test the banking system, we want to add another
property-based test that checks some fundamental invariant according to the
semantics of the program.

Please complete the "@Test public void testTransfer()" method inside
[BankTest.java](src/test/java/edu/pitt/cs/BankTest.java) according to the
Javadoc specifications.

## Running Property-based Test on top of JPF

If you are not using VSCode, you may need to compile your test classes:

```
mvn test-compile
```

To run JPF on BankTest, do (.bat for WIndows, .sh for Mac/Linux):

```
.\runJPF.bat JUnit.win.jpf
```
```
bash runJPF.sh JUnit.macos.jpf
```

We are using the same TestRunner class that we used for Exercise 5 Part 2 for the target of JPF.

If you implemented testTransfer() properly, you should see a data race property violation being reported:

```
$ bash runJPF.sh JUnit.macos.jpf 
JavaPathfinder core system v8.0 (rev 2f8f3c4dc847b8945fc13d2cb60896fc9c34265b) - (C) 2005-2014 United States Government. All rights reserved.


====================================================== system under test
edu.pitt.cs.TestRunner.main("trace")

====================================================== search started: 11/7/23 2:27 PM
TRACE GENERATION FOR FIRST FAILURE


====================================================== error 1
gov.nasa.jpf.listener.PreciseRaceDetector
race for field edu.pitt.cs.Transaction.transactionCount
  Thread-1 at edu.pitt.cs.Transaction.run(Transaction.java:45)
                "transactionCount++;"  WRITE: putstatic edu.pitt.cs.Transaction.transactionCount
  Thread-2 at edu.pitt.cs.Transaction.run(Transaction.java:45)
                "transactionCount++;"  READ:  getstatic edu.pitt.cs.Transaction.transactionCount


====================================================== trace #1
------------------------------------------------------ transition #0 thread: 0
gov.nasa.jpf.vm.choice.ThreadChoiceFromSet {id:"ROOT" ,1/1,isCascaded:false}
      [6345 insn w/o sources]
  edu/pitt/cs/TestRunner.java:14 : public class TestRunner {

...

------------------------------------------------------ transition #3 thread: 1
gov.nasa.jpf.vm.choice.ThreadChoiceFromSet {id:"START" ,2/3,isCascaded:false}
      [1 insn w/o sources]
  edu/pitt/cs/Transaction.java:1 : package edu.pitt.cs;
  edu/pitt/cs/Transaction.java:45 : transactionCount++;
------------------------------------------------------ transition #4 thread: 1
gov.nasa.jpf.vm.choice.ThreadChoiceFromSet {id:"SHARED_CLASS" ,2/3,isCascaded:false}
  edu/pitt/cs/Transaction.java:45 : transactionCount++;
------------------------------------------------------ transition #5 thread: 2
gov.nasa.jpf.vm.choice.ThreadChoiceFromSet {id:"SHARED_CLASS" ,3/3,isCascaded:false}
      [1 insn w/o sources]
  edu/pitt/cs/Transaction.java:1 : package edu.pitt.cs;
  edu/pitt/cs/Transaction.java:45 : transactionCount++;
------------------------------------------------------ transition #6 thread: 0
gov.nasa.jpf.vm.choice.ThreadChoiceFromSet {id:"SHARED_CLASS" ,1/3,isCascaded:false}
      [1 insn w/o sources]

====================================================== results
error #1: gov.nasa.jpf.listener.PreciseRaceDetector "race for field edu.pitt.cs.Transaction.transaction..."

...

```

Note the error reported for the data race on transactionCount.  It shows that
there was a race between a WRITE access on Transaction.java:45 and also a READ
access on Transaction.java:45 that were not properly synchronized.  If you look
towards the end of the trace, you will notice two threads "thread: 1" and
"thread: 2" attempted "transactionCount++" concurrently without holding a lock.
"Thread: 1" and "Thread: 2" are the transaction threads spawned off by the main
thread.

## Fixing Your First Data Race

Let's remove the above data race by wrapping in between a mutex lock and unlock:

```
countLock.lock();
transactionCount++;
countLock.unlock();
```

Note that I've already initialized the static "countLock" variable in its declaration:

```
private static Lock countLock = new ReentrantLock();
```

A ReentrantLock is just a fancy word for a mutex.  If you want to know more, here
is the Java 8 reference:
https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html

Functionally speaking, the above is no different from doing:

```
synchronized (countLock) {
	transactionCount++;
}
```

... which you may be more familiar with.  I used the "lock()" and "unlock()"
API because it gives you more flexibility on when to lock and unlock, which is
going to become important in other data races you have to debug.

## Fixing Other Data Races

After inserting the synchronization using "countLock", you will see the data race on "transactionCount" go away.  But you will see a new data race getting detected:

```
$ bash runJPF.sh JUnit.macos.jpf 
JavaPathfinder core system v8.0 (rev 2f8f3c4dc847b8945fc13d2cb60896fc9c34265b) - (C) 2005-2014 United States Government. All rights reserved.


====================================================== system under test
edu.pitt.cs.TestRunner.main("trace")

====================================================== search started: 11/7/23 2:56 PM
TRACE GENERATION FOR FIRST FAILURE


====================================================== error 1
gov.nasa.jpf.listener.PreciseRaceDetector
race for field edu.pitt.cs.Account@221.balance
  main at edu.pitt.cs.Account.getBalance(Account.java:36)
                "return balance;"  READ:  getfield edu.pitt.cs.Account.balance
  Thread-1 at edu.pitt.cs.Account.withdraw(Account.java:28)
                "balance -= amount;"  WRITE: putfield edu.pitt.cs.Account.balance
...
```

Apparently, there is a data race on the "balance" variable.  I will leave it to
you to read the trace, figure out the problem and debug this.  Repeat until JPF
does not display any errors.

### Restrictions while Fixing

The only lock that you are allowed to use for synchronization to remove the
remaining data races is "lock" variable in
[Account.java](src/main/java/edu/pitt/cs/Account.java), accessible through the
"getLock()" method.  Note that the lock belongs to each individual account, so
if there are 4 accounts, there are 4 locks you can play with.  As you probably
learned, mutual exclusion only happens when you attempt to lock the same mutex
object.

In order for your solution to count for the extra credit, you cannot use any
other synchronization other than to lock or unlock the aforementioned "lock"
variable.  That means:

1. You cannot create any more lock objects.

2. You cannot do Thread.join() to wait for a thread to terminate.

### Hints while Fixing

This application is a demonstration of the idea that "locks don't compose":

https://en.wikipedia.org/wiki/Lock_(computer_science)#Lack_of_composability

Reading the above wikipedia entry will give you an overview of all the issues
you will be encountering while debugging.  One issue you will likely encounter
is deadlocks that happen when you perform locking incorrectly such that a
cyclical dependency is formed.  The following wikipedia article explains why
deadlocks occur and some deadlock avoidance strategies:

https://en.wikipedia.org/wiki/Deadlock_prevention_algorithms

One simple strategy mentioned above is using a lock hierarhcy, and the concept
is explained in the Oracle Java documentation with code examples:

https://docs.oracle.com/cd/E19455-01/806-5257/sync-12/index.html

## Submission

When you are done, submit your repository to GradeScope at the **Exercise 5
Extra Credit** link.  Once you submit, GradeScope will run the autograder to
grade you and give feedback.  If you get deductions, fix your code based on the
feedback and resubmit.  Repeat until you don't get deductions.

## GradeScope Feedback

GradeScope autograder is under construction.  I will announce it when it is complete.

## Groupwork Plan

You will work together on the same repository.  You only need a handful of
source line changes to fix these bugs.  The hard part is thinking of the right
way to do synchronization.
