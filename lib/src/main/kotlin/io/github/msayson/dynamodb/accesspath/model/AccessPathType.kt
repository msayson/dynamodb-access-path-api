package io.github.msayson.dynamodb.accesspath.model

/**
 * Enum representing types of DynamoDB access paths.
 */
enum class AccessPathType {
    GET_ITEM,
    TABLE_QUERY,
    GSI_QUERY,
    SCAN
}
