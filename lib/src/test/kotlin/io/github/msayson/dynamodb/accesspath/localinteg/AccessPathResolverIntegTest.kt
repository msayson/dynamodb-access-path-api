package io.github.msayson.dynamodb.accesspath.localinteg

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.GlobalSecondaryIndex
import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import aws.sdk.kotlin.services.dynamodb.model.Projection
import aws.sdk.kotlin.services.dynamodb.model.ProjectionType
import io.github.msayson.dynamodb.accesspath.AccessPathResolver
import io.github.msayson.dynamodb.accesspath.TestConstants
import io.github.msayson.dynamodb.accesspath.localinteg.testutil.DynamoDbLocalStack
import io.github.msayson.dynamodb.accesspath.localinteg.testutil.DynamoDbRepositoryUtils
import io.github.msayson.dynamodb.accesspath.model.AccessPathType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Local integ tests for resolving DynamoDB table key access paths.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessPathResolverIntegTest {
    private lateinit var localstack: DynamoDbLocalStack
    private lateinit var dynamoDb: DynamoDbClient
    private val dynamoDbUtils: DynamoDbRepositoryUtils = DynamoDbRepositoryUtils()

    @BeforeAll
    fun setupLocalStack() {
        localstack = DynamoDbLocalStack()
        dynamoDb = localstack.dynamoDb
    }

    @Tag("localIntegTest")
    @Test
    fun `resolves GET_ITEM access path for partition key of partition-key-only table`() = runBlocking {
        val tableName = "PartitionKeyOnlyTable"
        dynamoDbUtils.createTable(dynamoDb, tableName, TestConstants.TABLE_PK_NAME, null, null)
        val resolver = AccessPathResolver(dynamoDb)
        val accessPath = resolver.resolveAccessPath(tableName, TestConstants.TABLE_PK_NAME)
        assertEquals(AccessPathType.GET_ITEM, accessPath.type)
        assertNull(accessPath.indexName)
    }

    @Tag("localIntegTest")
    @Test
    fun `resolves TABLE_QUERY access path for partition key of table with sort key`() = runBlocking {
        val tableName = "PartitionAndSortKeyTable"
        dynamoDbUtils.createTable(dynamoDb, tableName, TestConstants.TABLE_PK_NAME, TestConstants.TABLE_SK_NAME, null)
        val resolver = AccessPathResolver(dynamoDb)
        val accessPath = resolver.resolveAccessPath(tableName, TestConstants.TABLE_PK_NAME)
        assertEquals(AccessPathType.TABLE_QUERY, accessPath.type)
        assertNull(accessPath.indexName)
    }

    @Tag("localIntegTest")
    @Test
    fun `resolves GSI_QUERY access path for GSI partition key`() = runBlocking {
        val tableName = "TableWithGsi"
        val matchingGsiName = "TestGsi"
        val matchingGsiPartitionKeyName = "GsiPartitionKey"
        dynamoDbUtils.createTable(dynamoDb, tableName, TestConstants.TABLE_PK_NAME, null, listOf(
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

    @Tag("localIntegTest")
    @Test
    fun `resolves SCAN access path for non-key attribute`() = runBlocking {
        val tableName = "TableWithoutMatchingKey"
        dynamoDbUtils.createTable(dynamoDb, tableName, TestConstants.TABLE_PK_NAME, null, listOf(
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
            }
        ))
        val resolver = AccessPathResolver(dynamoDb)
        val accessPath = resolver.resolveAccessPath(tableName, "NonKeyAttribute")
        assertEquals(AccessPathType.SCAN, accessPath.type)
        assertNull(accessPath.indexName)
    }
}
