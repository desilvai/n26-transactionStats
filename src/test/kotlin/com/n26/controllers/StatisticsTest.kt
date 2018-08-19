package com.n26.controllers

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


@RunWith(SpringRunner::class)
@WebMvcTest(value = [Statistics::class])
class StatisticsTest
{

    @Autowired
    private lateinit var mockMvc: MockMvc

//    @LocalServerPort
//    private val port: Int = 0
//
//    @Value("\${local.management.port}")
//    private val mgt: Int = 0

    @Test
    fun getsStats()
    {
        val transaction = """
            {
                "amount": 12.3343,
                "timestamp": "2018-07-17T09:59:51.312Z"
            }
        """.trimIndent()


        val result = MockMvcRequestBuilders.get("/statistics")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .let (mockMvc::perform)
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        println(result.response.contentAsString)

    }

}