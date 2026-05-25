package io.github.msayson.dynamodb.accesspath

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import io.github.msayson.dynamodb.accesspath.model.AccessPathType

/**
 * Encapsulates logic for determining the optimal DynamoDB access path for a given table and attribute.
 *
 * @param dynamoDbClient The AWS SDK DynamoDB client used to query table schemas.
 */
class AccessPathResolver(private val dynamoDbClient: DynamoDbClient) {
    /**
     * Queries the DynamoDB table schema and determines the optimal access path for the given attribute.
     *
     * If the attribute is the partition key for a table without a sort key, it returns GET_ITEM.
     * Else if the attribute is the partition key for a table with a sort key, it returns TABLE_QUERY.
     * Else if the attribute is a partition key for a GSI, it returns GSI_QUERY.
     * Else it returns SCAN.
     *
     * @param tableArn The ARN of the DynamoDB table.
     * @param attributeName The name of the attribute being accessed.
     * @return The optimal AccessPathType for the given parameters.
     */
    fun resolveAccessType(tableArn: String, attributeName: String): AccessPathType {
        // TODO: Query DynamoDB table schema and determine optimal access path
        return AccessPathType.GET_ITEM
    }
}
