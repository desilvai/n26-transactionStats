package com.n26.controllers.exceptionHandlers

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
@Suppress("UNUSED")
open class InputFieldExceptionHandler: ResponseEntityExceptionHandler()
{
    // TODO -- Make this print out helpful messages instead of just getting
    // the response code correct.

    override fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException,
                                              headers: HttpHeaders,
                                              status: HttpStatus,
                                              request: WebRequest): ResponseEntity<Any>
    {
        val expectedResponse = super.handleHttpMessageNotReadable(ex, headers, status, request)

        if(ex.contains(InvalidFormatException::class.java))
        {
            return ResponseEntity(expectedResponse.body,
                                  expectedResponse.headers,
                                  UNPROCESSABLE_ENTITY)
        }
        else
        {
            return expectedResponse
        }
    }

    // While we are supposed to return a 422 for malformed JSON, I think it
    // is better to leave this as a 415 if it isn't even JSON.
//    override fun handleHttpMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException,
//                                                 headers: HttpHeaders,
//                                                 status: HttpStatus,
//                                                 request: WebRequest): ResponseEntity<Any>
//    {
//        // Changing from a 415 to a 422 per the instructions.
//        return super.handleHttpMediaTypeNotSupported(ex,
//                                                     headers,
//                                                     status,
//                                                     request)
//                .let { ResponseEntity(it.body,
//                                      it.headers,
//                                      BAD_REQUEST) }
//    }
}