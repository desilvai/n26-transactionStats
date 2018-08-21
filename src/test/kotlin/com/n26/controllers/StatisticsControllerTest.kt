package com.n26.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.n26.data.Stats
import com.n26.services.TransactionService
import com.nhaarman.mockito_kotlin.whenever
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
        val cannedAnswer = Stats(BigDecimal.ZERO,
                                 BigDecimal.ZERO.setScale(8, HALF_UP),
                                 BigDecimal.ZERO,
                                 BigDecimal.ZERO,
                                 0L)
        whenever(transactionService.getStats()).thenReturn(cannedAnswer)

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

}