package io.github.msayson.dynamodb.accesspath

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.DescribeTableRequest
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import io.github.msayson.dynamodb.accesspath.model.AccessPathType
import kotlinx.coroutines.runBlocking

/**
 * Encapsulates logic for determining the optimal DynamoDB access path for a given table and attribute.
 */
class AccessPathResolver(private val dynamoDbClient: DynamoDbClient) {

    /**
     * Determine access path asynchronously.
     */
    suspend fun resolveAccessType(tableArn: String, attributeName: String): AccessPathType {
        val tableDescription = dynamoDbClient.describeTable(DescribeTableRequest { tableName = tableArn })
        val table = requireNotNull(tableDescription.table) { "Table not found: $tableArn" }
        val keySchema = requireNotNull(table.keySchema) { "Table $tableArn has no key schema" }

        val partitionKey = keySchema.firstOrNull { it.keyType == KeyType.Hash }?.attributeName
            ?: throw IllegalArgumentException("Table $tableArn has no partition key")
        val hasSortKey = keySchema.any { it.keyType == KeyType.Range }
        val gsiPartitionKeys = table.globalSecondaryIndexes?.mapNotNull { gsi ->
            gsi.keySchema?.firstOrNull { it.keyType == KeyType.Hash }?.attributeName
        } ?: emptyList()

        return when {
            attributeName == partitionKey && !hasSortKey -> AccessPathType.GET_ITEM
            attributeName == partitionKey && hasSortKey -> AccessPathType.TABLE_QUERY
            gsiPartitionKeys.contains(attributeName) -> AccessPathType.GSI_QUERY
            else -> AccessPathType.SCAN
        }
    }

    /**
     * Synchronous convenience wrapper for callers that don't use coroutines.
     */
    fun resolveAccessTypeBlocking(tableArn: String, attributeName: String): AccessPathType = runBlocking {
        resolveAccessType(tableArn, attributeName)
    }
}
