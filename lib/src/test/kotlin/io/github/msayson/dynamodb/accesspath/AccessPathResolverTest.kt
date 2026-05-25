package io.github.msayson.dynamodb.accesspath

import io.github.msayson.dynamodb.accesspath.model.AccessPathType

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class AccessPathResolverTest {
    @Test
    fun resolveAccessType() {
        val classUnderTest = AccessPathResolver()
        assertTrue(classUnderTest.resolveAccessType("tableArn", "attributeName") == AccessPathType.GET_ITEM,
            "resolveAccessType should return GET_ITEM")
    }
}
