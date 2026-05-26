package io.github.msayson.dynamodb.accesspath.localinteg

import aws.sdk.kotlin.services.dynamodb.model.GlobalSecondaryIndex
import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import aws.sdk.kotlin.services.dynamodb.model.Projection
import aws.sdk.kotlin.services.dynamodb.model.ProjectionType
import io.github.msayson.dynamodb.accesspath.AccessPathResolver
import io.github.msayson.dynamodb.accesspath.TestConstants
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

    @Tag("localIntegTest")
    @Test
    fun `resolves TABLE_QUERY access path for partition key of table with sort key`() = runBlocking {
        val tableName = "PartitionAndSortKeyTable"
        val partitionKeyName = "TestPartitionKey"
        val sortKeyName = "TestSortKey"
        dynamoDbUtils.createTable(dynamoDb, tableName, partitionKeyName, sortKeyName, null)
        val resolver = AccessPathResolver(dynamoDb)
        val accessPath = resolver.resolveAccessPath(tableName, partitionKeyName)
        assertEquals(AccessPathType.TABLE_QUERY, accessPath.type)
        assertNull(accessPath.indexName)
    }

    @Tag("localIntegTest")
    @Test
    fun `resolves GSI_QUERY access path for GSI partition key`() = runBlocking {
        val tableName = "TableWithGsi"
        val partitionKeyName = "TestPartitionKey"
        val matchingGsiName = "TestGsi"
        val matchingGsiPartitionKeyName = "GsiPartitionKey"
        dynamoDbUtils.createTable(dynamoDb, tableName, partitionKeyName, null, listOf(
            GlobalSecondaryIndex {
                indexName = "NonMatchingIndex"
                keySchema = listOf(
                    KeySchemaElement {
                        attributeName = "nonMatchingGsiPk"
                        keyType = KeyType.Hash
                    }
                )
                provisionedThroughput = TestConstants.PROVISIONED_THROUGHPUT
                projection = Projection {
                    projectionType = ProjectionType.KeysOnly
                }
            },
            GlobalSecondaryIndex {
                indexName = matchingGsiName
                keySchema = listOf(
                    KeySchemaElement {
                        attributeName = matchingGsiPartitionKeyName
                        keyType = KeyType.Hash
                    }
                )
                provisionedThroughput = TestConstants.PROVISIONED_THROUGHPUT
                projection = Projection {
                    projectionType = ProjectionType.All
                }
            }
        ))
        val resolver = AccessPathResolver(dynamoDb)
        val accessPath = resolver.resolveAccessPath(tableName, matchingGsiPartitionKeyName)
        assertEquals(AccessPathType.GSI_QUERY, accessPath.type)
        assertEquals(matchingGsiName, accessPath.indexName)
    }
}
