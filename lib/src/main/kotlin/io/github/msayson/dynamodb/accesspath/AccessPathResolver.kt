package io.github.msayson.dynamodb.accesspath

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.DescribeTableRequest
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import io.github.msayson.dynamodb.accesspath.model.AccessPath
import io.github.msayson.dynamodb.accesspath.model.AccessPathType
import kotlinx.coroutines.runBlocking

/**
 * Encapsulates logic for determining the optimal DynamoDB access path for a given table and attribute.
 */
class AccessPathResolver(private val dynamoDbClient: DynamoDbClient) {

    /**
     * Determine access path asynchronously.
     */
    suspend fun resolveAccessPath(tableArn: String, attributeName: String): AccessPath {
        val tableDescription = dynamoDbClient.describeTable(DescribeTableRequest { tableName = tableArn })
        val table = requireNotNull(tableDescription.table) { "Table not found: $tableArn" }
        val keySchema = requireNotNull(table.keySchema) { "Table $tableArn has no key schema" }

        val partitionKeyName = keySchema.firstOrNull { it.keyType == KeyType.Hash }?.attributeName
        requireNotNull(partitionKeyName) { "Table $tableArn has no partition key" }

        val hasSortKey = keySchema.any { it.keyType == KeyType.Range }
        val matchingGsi = table.globalSecondaryIndexes?.firstOrNull { gsi ->
            gsi.keySchema?.any { it.keyType == KeyType.Hash && it.attributeName == attributeName } == true
        }

        return when {
            attributeName == partitionKeyName && !hasSortKey -> AccessPath(AccessPathType.GET_ITEM)
            attributeName == partitionKeyName && hasSortKey -> AccessPath(AccessPathType.TABLE_QUERY)
            matchingGsi != null -> AccessPath(AccessPathType.GSI_QUERY, indexName = matchingGsi.indexName)
            else -> AccessPath(AccessPathType.SCAN)
        }
    }

    /**
     * Synchronous convenience wrapper for callers that don't use coroutines.
     */
    fun resolveAccessPathBlocking(tableArn: String, attributeName: String): AccessPath = runBlocking {
        resolveAccessPath(tableArn, attributeName)
    }
}
