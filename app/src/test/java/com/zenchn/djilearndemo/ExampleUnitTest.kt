package com.zenchn.djilearndemo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    fun hello(): () -> Unit = {
        println("nihao")
    }

    @Test
    fun test() {
        val hello = hello()
        hello.invoke()

        Parent("xujiafeng")
        Child("xujiafeng")
    }


}

open class Parent(open var name: String) {
    var nameLength: Int

    init {
        nameLength = name.length
    }
}

class Child(override var name: String) : Parent(name) {

    init {
        nameLength = name.length
    }
}

