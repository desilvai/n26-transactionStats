/*
 * ----------------------------------------------------------------------------
 * Copyright (c) 2017, Ian J. De Silva
 * All rights reserved.
 *
 * Use, distribution, and modification of this work for any purpose is strictly
 * prohibited without the express consent of the copyright holder except as
 * permitted by law.
 * ----------------------------------------------------------------------------
 * Created by desilvai on 20170529'.
 */

package dev.desilvai.utils

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Marks a class of test cases as one that accesses private data within an
 * object.  Provides the means for accessing private properties.
 */
interface AccessesPrivateData {
    /**
     * Updates a (invisible) property within the object.
     *
     * @param propertyName  the symbolId of the property to getUpdated.
     * @param newValue  the mocked object to be inserted
     */
    fun <R: Any, T> R.setPrivateProperty(propertyName: String,
                                         newValue: T): Boolean
    {
        // Reflectively find the private property and getUpdated it.
        @Suppress("UNCHECKED_CAST")
        val property = this::class.memberProperties
                .filter { it.name == propertyName }
                .filterIsInstance<KMutableProperty1<R, *>>()
                .first() as KMutableProperty1<R, T>

        property.isAccessible = true
        property.set(this, newValue)
        property.isAccessible = false

        return true
    }

    /**
     * Reads a (invisible) property within the object.
     *
     * @param propertyName  the symbolId of the property to read.
     */
    fun <R: Any, T> R.getPrivateProperty(propertyName: String): T
    {
        // Reflectively find the private property and getUpdated it.
        val property = this::class.memberProperties
                .filter { it.name == propertyName }
                .filterIsInstance<KProperty1<R, T>>()
                .first()
        property.isAccessible = true
        val value: T = property.get(this)
        property.isAccessible = false

        return value
    }
}