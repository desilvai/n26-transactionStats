package com.n26.controllers.exceptionHandlers

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
open class InputFieldExceptionHandler: ResponseEntityExceptionHandler()
{
//
//
//    override fun handleBindException(ex: BindException?,
//                                     headers: HttpHeaders?,
//                                     status: HttpStatus?,
//                                     request: WebRequest?): ResponseEntity<Any?>
//    {
//        if(ex != null && ex.bindingResult.fieldErrors.isNotEmpty())
//        {
////            val error = "Problem binding ${ex.bindingResult.fieldErrors
////                    .joinToString { it.field }}.  Make sure they are " +
////                        "formatted correctly."
//            return super.handleBindException(ex, headers, status, request)
//                    .let { ResponseEntity(it.body, it.headers, UNPROCESSABLE_ENTITY) }
//        }
//
//        return super.handleBindException(ex, headers, status, request)
//    }

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

    // Not needed.  We get a 400 by default.
//    @ExceptionHandler(BeanInstantiationException::class)
//    protected fun handleInstantiationError(e: BeanInstantiationException) =
//        ResponseEntity<Any?>(BAD_REQUEST)

    override fun handleHttpMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException?,
                                                 headers: HttpHeaders?,
                                                 status: HttpStatus?,
                                                 request: WebRequest?): ResponseEntity<Any>
    {
        // Changing from a 415 to a 422 per the instructions.
        return super.handleHttpMediaTypeNotSupported(ex,
                                                     headers,
                                                     status,
                                                     request)
                .let { ResponseEntity(it.body,
                                      it.headers,
                                      BAD_REQUEST) }
    }
}