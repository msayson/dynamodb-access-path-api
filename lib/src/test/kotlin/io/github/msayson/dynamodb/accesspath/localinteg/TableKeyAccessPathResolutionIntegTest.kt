package io.github.msayson.dynamodb.accesspath.localinteg

import io.github.msayson.dynamodb.accesspath.AccessPathResolver
import io.github.msayson.dynamodb.accesspath.model.AccessPathType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Local integ tests for resolving DynamoDB table key access paths.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableKeyAccessPathResolutionIntegTest : AccessPathResolverIntegTest() {
    @Tag("localIntegTest")
    @Test
    fun `resolves GET_ITEM access path for partition key of partition-key-only table`() = runBlocking {
        val tableName = "PartitionKeyOnlyTable"
        val partitionKeyName = "TestPartitionKey"
        dynamoDbUtils.createTable(dynamoDb, tableName, partitionKeyName, null, null)
        val resolver = AccessPathResolver(dynamoDb)
        val accessPath = resolver.resolveAccessPath(tableName, partitionKeyName)
        assertEquals(AccessPathType.GET_ITEM, accessPath.type)
        assertNull(accessPath.indexName)
    }
}
