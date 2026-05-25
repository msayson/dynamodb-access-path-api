package io.github.msayson.dynamodb.accesspath.model

/**
 * The recommended DynamoDB access path for a given table and attribute.
 *
 * @param type The type of access path to use.
 * @param indexName The GSI name to query, populated only when [type] is [AccessPathType.GSI_QUERY].
 */
data class AccessPath(
    val type: AccessPathType,
    val indexName: String? = null
)
