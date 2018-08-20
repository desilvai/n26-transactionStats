package com.n26.controllers

import org.junit.After
import org.junit.Assert
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
 * NOTE: Parameterized testing was too much of a pain to get to work with
 *      Spring, so I flattened out the structure a bit.  This has the advantage
 *      of allowing for more readable test names.
 */
@RunWith(SpringRunner::class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WebMvcTest(value = [Transactions::class], secure = false)
class TransactionsTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    /**
     * After each run, clear out the repository.  We will confirm that it is
     * clean before the next run.
     */
    @After
    fun cleanUp()
    {
        clearAll()
        Assert.assertEquals(getCount(), 0)
    }

    @Test
    fun `post accepted transaction`() = postTransaction(
            """
                {
                    "amount": 12.3343,
                    "timestamp": "2018-07-17T09:59:51.312Z"
                }
            """.trimIndent(),
            201
        )

    @Test
    fun `post accepted transaction with string delimiters`() = postTransaction(
            """
                {
                    "amount": 12.3343,
                    "timestamp": "2018-07-17T09:59:51.312Z"
                }
            """.trimIndent(),
            201
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
    fun `post with additional fields`() = postTransaction(
            """
                {
                    "amount": 12.3343,
                    "timestamp": "2018-07-17T09:59:51.312Z",
                    "forceIn": "true"
                }
            """.trimIndent(),
            201
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
    fun `post now`()
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
    fun `post a withdraw`()
            = postTransaction(
            """
                {
                    "amount": "-5000429274.110372957392027",
                    "timestamp": "1980-12-25T12:59:07.999Z"
                }
            """.trimIndent(),
            201
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

    @Test
    fun `add multiple transactions`()
    {
        TODO("Implement Me")
    }


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

        Assert.assertEquals(result.response.status, expectedResponse)
        Assert.assertEquals(result.response.contentLength, 0)

        if(expectedResponse in 200..299)
            Assert.assertEquals(getCount(), 1)
    }


    private fun clearAll()
    {
        MockMvcRequestBuilders.delete("/transactions")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .let (mockMvc::perform)
                .andExpect(MockMvcResultMatchers.status().isNoContent)
                .andReturn()
                .also { Assert.assertEquals(it.response.contentLength, 0) }
    }


    /**
     * We use this to verify that our transaction operation was successful.
     */
    private fun getCount(): Int
    {
        return MockMvcRequestBuilders.get("/transactions")
                .let (mockMvc::perform)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .let { Integer.parseInt(it.response.contentAsString) }
    }


    private fun formatDateTime(time: Temporal): String
    {
        return DateTimeFormatter.ISO_INSTANT
                .format(time)
    }
}