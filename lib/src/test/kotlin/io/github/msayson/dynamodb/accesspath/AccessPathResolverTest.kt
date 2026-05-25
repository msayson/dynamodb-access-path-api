package io.github.msayson.dynamodb.accesspath

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.DescribeTableResponse
import aws.sdk.kotlin.services.dynamodb.model.GlobalSecondaryIndexDescription
import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import aws.sdk.kotlin.services.dynamodb.model.TableDescription
import io.github.msayson.dynamodb.accesspath.model.AccessPathType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccessPathResolverTest {

    private fun describeTableResponse(vararg keySchema: KeySchemaElement, gsis: List<GlobalSecondaryIndexDescription> = emptyList()) =
        DescribeTableResponse {
            table = TableDescription {
                this.keySchema = keySchema.toList()
                this.globalSecondaryIndexes = gsis
            }
        }

    private fun keyElement(name: String, type: KeyType) = KeySchemaElement {
        attributeName = name
        keyType = type
    }

    @Test
    fun `returns GET_ITEM for partition key of a partition-key-only table`() = runBlocking {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns describeTableResponse(keyElement("pk", KeyType.Hash))
        assertEquals(AccessPathType.GET_ITEM, AccessPathResolver(client).resolveAccessType("arn:table", "pk"))
    }

    @Test
    fun `returns TABLE_QUERY for partition key of table with sort key`() = runBlocking {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns describeTableResponse(
            keyElement("pk", KeyType.Hash), keyElement("sk", KeyType.Range)
        )
        assertEquals(AccessPathType.TABLE_QUERY, AccessPathResolver(client).resolveAccessType("arn:table", "pk"))
    }

    @Test
    fun `returns GSI_QUERY for GSI partition key`() = runBlocking {
        val client = mockk<DynamoDbClient>()
        val gsi = GlobalSecondaryIndexDescription { keySchema = listOf(keyElement("gsiPk", KeyType.Hash)) }
        coEvery { client.describeTable(any()) } returns describeTableResponse(
            keyElement("pk", KeyType.Hash), gsis = listOf(gsi)
        )
        assertEquals(AccessPathType.GSI_QUERY, AccessPathResolver(client).resolveAccessType("arn:table", "gsiPk"))
    }

    @Test
    fun `returns SCAN for non-key attribute`() = runBlocking {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns describeTableResponse(keyElement("pk", KeyType.Hash))
        assertEquals(AccessPathType.SCAN, AccessPathResolver(client).resolveAccessType("arn:table", "other"))
    }

    @Test
    fun `returns SCAN for non-key attribute when globalSecondaryIndexes is null`() = runBlocking {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns DescribeTableResponse {
            table = TableDescription {
                keySchema = listOf(keyElement("pk", KeyType.Hash))
                globalSecondaryIndexes = null
            }
        }
        assertEquals(AccessPathType.SCAN, AccessPathResolver(client).resolveAccessType("arn:table", "other"))
    }

    @Test
    fun `resolveAccessTypeBlocking returns same result as suspend version`() {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns describeTableResponse(keyElement("pk", KeyType.Hash))
        assertEquals(AccessPathType.GET_ITEM, AccessPathResolver(client).resolveAccessTypeBlocking("arn:table", "pk"))
    }

    @Test
    fun `throws when table is not found`() {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns DescribeTableResponse { table = null }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AccessPathResolver(client).resolveAccessType("arn:table", "pk") }
        }
    }

    @Test
    fun `throws when describeTable raises an exception`() {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } throws RuntimeException("Service error")
        assertThrows(RuntimeException::class.java) {
            runBlocking { AccessPathResolver(client).resolveAccessType("arn:table", "pk") }
        }
    }

    @Test
    fun `throws when key schema has no partition key`() {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns describeTableResponse(keyElement("sk", KeyType.Range))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AccessPathResolver(client).resolveAccessType("arn:table", "sk") }
        }
    }

    @Test
    fun `throws when key schema is null`() {
        val client = mockk<DynamoDbClient>()
        coEvery { client.describeTable(any()) } returns DescribeTableResponse {
            table = TableDescription { keySchema = null }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { AccessPathResolver(client).resolveAccessType("arn:table", "pk") }
        }
    }
}
