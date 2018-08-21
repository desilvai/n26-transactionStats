package com.n26.controllers

import com.n26.services.TransactionService
import org.amshove.kluent.`should be equal to`
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

/**
 * Tests the transaction controller.  I could have mocked these and checked
 * them, but most of these were simple enough that isolation from downstream
 * code wasn't as important.  Indeed, most just check for bad input.  Thus, I
 * will leave these as integration tests.
 *
 * NOTE: Parameterized testing was too much of a pain to get to work with
 *      Spring, so I flattened out the structure a bit.  This has the advantage
 *      of allowing for more readable test names.
 */
@RunWith(SpringRunner::class)
@WebMvcTest(value = [TransactionsController::class], secure = false)
class TransactionsControllerTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc


    @Autowired
    private lateinit var transactionService: TransactionService

    /**
     * After each run, clear out the repository.  We will confirm that it is
     * clean before the next run.
     */
    @After
    fun cleanUp()
    {
        clearAll()
        getCount() `should be equal to` 0
    }


    @Test
    fun `post accepted, outdated transaction`() = postTransaction(
            """
                {
                    "amount": 12.3343,
                    "timestamp": "2018-07-17T09:59:51.312Z"
                }
            """.trimIndent(),
            204
        )

    @Test
    fun `post accepted, outdated transaction with string delimiters`() =
            postTransaction(
            """
                {
                    "amount": 12.3343,
                    "timestamp": "2018-07-17T09:59:51.312Z"
                }
            """.trimIndent(),
            204
    )

    @Test
    fun `post with invalid amount (number format)`() = postTransaction(
            """
                {
                    "amount": "12.3343.5",
                    "timestamp": "2018-07-17T09:59:51.312Z"
                }
            """.trimIndent(),
            422
    )

    @Test
    fun `post with invalid amount (uses letters)`() = postTransaction(
            """
                {
                    "amount": "abc",
                    "timestamp": "2018-07-17T09:59:51.312Z"
                }
            """.trimIndent(),
            422
    )


    @Test
    fun `post with invalid timestamp (additional hour character)`()
            = postTransaction(
                    """
                        {
                            "amount": "12.3343",
                            "timestamp": "2018-07-17T019:59:51.312Z"
                        }
                    """.trimIndent(),
                    422
            )


    @Test
    fun `post with invalid JSON (missing colon)`()
            = postTransaction(
            """
                        {
                            "amount": "12.3343",
                            "timestamp" "2018-07-17T09:59:51.312Z"
                        }
                    """.trimIndent(),
            400
    )


    @Test
    fun `post with invalid JSON (missing closing bracket)`()
            = postTransaction(
            """
                        {
                            "amount": "12.3343",
                            "timestamp": "2018-07-17T09:59:51.312Z"
                    """.trimIndent(),
            400
    )

    @Test
    fun `post with invalid JSON (no comma separation)`()
            = postTransaction(
            """
                        {
                            "amount": "12.3343"
                            "timestamp": "2018-07-17T09:59:51.312Z"
                        }
                    """.trimIndent(),
            400
    )

    @Test
    fun `post outdated transaction with additional fields`() = postTransaction(
            """
                {
                    "amount": 12.3343,
                    "timestamp": "2018-07-17T09:59:51.312Z",
                    "forceIn": "true"
                }
            """.trimIndent(),
            204
    )


    @Test
    fun `post with missing amount`() = postTransaction(
            """
                {
                    "timestamp": "2018-07-17T09:59:51.312Z",
                }
            """.trimIndent(),
            400
    )

    @Test
    fun `post with missing timestamp`() = postTransaction(
            """
                {
                    "amount": 12.3343
                }
            """.trimIndent(),
            400
    )

    @Test
    fun `post with missing fields`() = postTransaction(
            """
                {
                }
            """.trimIndent(),
            400
    )

    @Test
    fun `post with future date`()
            = postTransaction(
                    """
                        {
                            "amount": "12.3343",
                            "timestamp": "${ZonedDateTime.now().plusMinutes(2L)
                                            .let(this::formatDateTime)}"
                        }
                    """.trimIndent(),
                    422
            )

    @Test
    fun `post deposit now`()
            = postTransaction(
            """
                {
                    "amount": "12.3343",
                    "timestamp": "${ZonedDateTime.now().let(this::formatDateTime)}"
                }
            """.trimIndent(),
            201
    )

    @Test
    fun `post an outdated withdraw`()
            = postTransaction(
            """
                {
                    "amount": "-5000429274.110372957392027",
                    "timestamp": "1980-12-25T12:59:07.999Z"
                }
            """.trimIndent(),
            204
    )

    @Test
    fun `post with alternate date format`()
            = postTransaction(
            """
                        {
                            "amount": "12.3343",
                            "timestamp": "${ZonedDateTime.now()}"
                        }
                    """.trimIndent(),
            422
    )


    /**
     * Helper function that will post the given transaction and check if we
     * get the expected response.
     */
    private fun postTransaction(requestBody: String, expectedResponse: Int)
    {
        val result = MockMvcRequestBuilders.post("/transactions")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .let (mockMvc::perform)
                .andReturn()

        result.response.status `should be equal to` expectedResponse
        result.response.contentLength `should be equal to` 0

        if(expectedResponse in 200..299)
            getCount() `should be equal to`  1
    }


    private fun clearAll()
    {
        MockMvcRequestBuilders.delete("/transactions")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .let (mockMvc::perform)
                .andExpect(MockMvcResultMatchers.status().isNoContent)
                .andReturn()
                .also { it.response.contentLength `should be equal to`  0 }
    }


    /**
     * We use this to verify that our transaction operation was successful.
     * This doesn't seem like it should work, but because we are only mocking
     * the call, we can access the service through autowiring and check the
     * count without using a service endpoint.  If we choose to use an
     * endpoint later, we can just change this function.
     */
    private fun getCount(): Long = transactionService.count()


    private fun formatDateTime(time: Temporal): String
            = DateTimeFormatter.ISO_INSTANT.format(time)

}