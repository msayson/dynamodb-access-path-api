package io.github.msayson.dynamodb.accesspath

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import io.github.msayson.dynamodb.accesspath.model.AccessPathType
import io.mockk.mockk

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class AccessPathResolverTest {
    @Test
    fun resolveAccessType() {
        val mockDynamoDbClient = mockk<DynamoDbClient>()
        val classUnderTest = AccessPathResolver(mockDynamoDbClient)
        assertTrue(classUnderTest.resolveAccessType("tableArn", "attributeName") == AccessPathType.GET_ITEM,
            "resolveAccessType should return GET_ITEM")
    }
}
