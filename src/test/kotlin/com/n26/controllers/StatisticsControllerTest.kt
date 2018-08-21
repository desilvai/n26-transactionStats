package com.n26.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.n26.data.Stats
import com.n26.services.TransactionService
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.`should be equal to`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import roundUp
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP


@RunWith(SpringRunner::class)
@WebMvcTest(value = [StatisticsController::class])
class StatisticsControllerTest
{
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var personDTOJsonTester: JacksonTester<Stats>

    /**
     * The mocked service.  Normally, I'd set the default answer to throw an
     * exception, but it isn't one of the default options.  I'd need to
     * research ways to make this work with MockBean.
     */
    @MockBean
    private lateinit var transactionService: TransactionService


    @Before
    fun setup()
    {
        JacksonTester.initFields(this, objectMapper);
    }


    @Test
    fun `stats return 0s when no transactions present`()
    {
        val cannedAnswer = Stats(min = BigDecimal.ZERO,
                                 max = BigDecimal.ZERO.setScale(8, HALF_UP),
                                 sum = BigDecimal.ZERO,
                                 avg = BigDecimal.ZERO,
                                 count = 0L)
        whenever(transactionService.getStats()).thenReturn(cannedAnswer)

        // We check serialization correctness in another test case.  Here,
        // assume it works.
        val expectedDTO = cannedAnswer.copy(min = cannedAnswer.min.roundUp(2),
                                            max = cannedAnswer.max.roundUp(2),
                                            sum = cannedAnswer.sum.roundUp(2),
                                            avg = cannedAnswer.avg.roundUp(2))
        val expectedJSON = personDTOJsonTester.write(expectedDTO).json

        MockMvcRequestBuilders.get("/statistics")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .let (mockMvc::perform)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON))
    }


    @Test
    fun `stats return transaction when in window`()
    {
        // Luckily, this doesn't have to make sense.
        val cannedAnswer = Stats(min = BigDecimal.ONE,
                                 max = BigDecimal.ONE,
                                 sum = BigDecimal.ZERO,
                                 avg = BigDecimal(123.826498276463),
                                 count = 2L)
        whenever(transactionService.getStats()).thenReturn(cannedAnswer)

        // We check serialization correctness in another test case.  Here,
        // assume it works.
        val expectedDTO = cannedAnswer.copy(min = cannedAnswer.min.roundUp(2),
                                            max = cannedAnswer.max.roundUp(2),
                                            sum = cannedAnswer.sum.roundUp(2),
                                            avg = cannedAnswer.avg.roundUp(2))
        val expectedJSON = personDTOJsonTester.write(expectedDTO).json

        MockMvcRequestBuilders.get("/statistics")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .let (mockMvc::perform)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().json(expectedJSON))
    }


    /**
     * We can't take Jackson's serialization for granted.  So, make sure it
     * correctly shows two decimal places.
     */
    @Test
    fun `check serialization always shows two decimal places`()
    {
        // Luckily, this doesn't have to make sense.
        // GIVEN 4 numbers that must be shown with two decimal places:
        //  - 1 with no decimals
        //  - 1 with 1 decimal place
        //  - 1 with 2 decimal places
        //  - 1 with more than two decimal places, rounding up
        val toSerialize = Stats(min = BigDecimal.ZERO,
                                max = BigDecimal(-6154382.2),
                                avg = BigDecimal(57628.72),
                                sum = BigDecimal(123.826498276463),
                                count = 100000L)

        val expectedSerialization = "{\"sum\":\"123.83\",\"avg\":\"57628.72\"," +
                                    "\"max\":\"-6154382.20\",\"min\":\"0.00\"," +
                                    "\"count\":100000}"

        // WHEN we serialize the DTO
        val actualSerialization = personDTOJsonTester.write(toSerialize).json

        // THEN it write each out with two decimal places of precision.
        actualSerialization `should be equal to` expectedSerialization
    }
}