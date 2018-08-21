N26 Statistics Webservice
=========================

This web application provides a RESTful API for gathering real-time statistics for the last 60 seconds of transactions.

The API has the following endpoints:

    `POST /transactions` – called every time a transaction is made. It is also the sole input of this rest API.
    `GET /statistics` – returns the statistic based of the transactions of the last 60 seconds.
    `DELETE /transactions` – deletes all transactions.

These endpoints are provided by the `TransactionsController` (POST and DELETE) and `StatisticsController` (GET).  The specs for the endpoints are included in the method headers.  Both controllers interact with the `TransactionService`, which contains the add/delete/statics-gathering logic.  Since there is no database (and thus no DAOs), this service tracks the transactions that have been added and generates the statistics for the service.

There were a few different options that we considered for solving the issue.  Rather than have a second thread that maintains the statistics, we opted for organizing the transactions into buckets corresponding to a 500 millisecond-sized time range.  There are a fixed number of buckets within the statistics window (exactly 120 buckets).  Each bucket (a `TransactionContainer`) tracks the max, min, count, and sum of the transactions in the bucket.

When the user asks for statistics within the 60-second time window, we will aggregate the statistics for each bucket and then compute the average on the result.  Because we expect there to be many transactions stored, this size will dwarf the constant number of buckets we have to aggregate statistics from.  We know that we can aggregate the statistics in O(1) time.  Further, since there are 120 buckets and each tracks the stats, we know we can store this information in O(4*120) = O(1) space.

Because we are using buckets to categorize transactions according to their time, there is going to be some inaccuracy in the computation of the stats.  However, this function is inherently inaccurate due to ambiguity over the ending time of "the last 60 seconds" time period.  This period could run from the time the client submitted the request, the time the server received the request, or the time the server services the request.  Further, the transaction times could be off due to challenges synchronizing clocks and clock drift (we assume all transactions are standardized around Java's definition of time primitives).  Due to all of these sources of inaccuracy, we believe the additional inaccuracy from a bucket-categorization of transactions is acceptable.  If additional accuracy is needed, the program can be updated (by increasing the `TransactionService.Companion.BUCKETS_PER_SECOND` constant) to bring about the desired improvements.

An additional source of inaccuracy codes from the possibility of omitting pertinent, uncommitted transactions from the statistics.  This occurs when there is a high-volume of adds within the current time window.  It is also worth noting here that transactions could arrive out-of-order.  In deference to responsiveness, we will ignore uncommmited transactions and use the stats at the point when the system reaches a stable state.


Requirements
------------
This application is a Java/Kotlin hybrid project.  All sources provided by N26 remain in Java while all new sources are in Kotlin.  All dependencies are handled by the Maven build.  The following programs are required to build and run the application:
 * Java 8 JDK
 * Maven 3.5.2 (other versions may work, but tested with this version)


Build and Installation
----------------------
This application is built with Maven.  Maven will handle all dependencies.

To run the server, execute:

    mvn clean spring-boot:run

To install the application, execute:

    mvn clean install

To run all tests, execute:
    
    mvn clean integration-test; cat target/customReports/result.txt



### Testing: Mocks with Kotlin
Mockito, the standard mocking framework, does not work out of the box with Kotlin due to Kotlin's final-by-default class model.  There is an incubating Mockito feature that allows it to work with Kotlin by wrapping/inlining the mocks.  This is enabled in this project by including the `org.mockito.plugins.MockMaker` resource file in the `test/resources/mockito-extensions` directory.  Make sure you do not delete this file/directory as it will cause the test cases to fail.


Additional Verification
-----------------------
I did reach 96% line coverage according to IntelliJ's coverage checker.  The remaining 4% cannot be covered and were manually checked.

Due to time constraints, my verification was limited.  With additional time, I'd also do performance testing with something like JMeter or Locust.  If there were better static analysis tools for Kotlin (specifically ones that handle concurrency), I'd also use those.  For what it's worth, I ran FindBugs and all detected "issues" are things that Kotlin inherently protects against.

