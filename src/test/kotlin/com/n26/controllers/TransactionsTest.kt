package com.n26.controllers

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

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
    }
}