package io.github.msayson.dynamodb.accesspath.localinteg.testutil

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeDefinition
import aws.sdk.kotlin.services.dynamodb.model.CreateTableRequest
import aws.sdk.kotlin.services.dynamodb.model.GlobalSecondaryIndex
import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import aws.sdk.kotlin.services.dynamodb.model.Projection
import aws.sdk.kotlin.services.dynamodb.model.ProjectionType
import aws.sdk.kotlin.services.dynamodb.model.ProvisionedThroughput
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType

class DynamoDbRepositoryUtils {
    companion object {
        private const val DDB_CAPACITY_UNITS = 10L
        private val PROVISIONED_THROUGHPUT = ProvisionedThroughput {
            readCapacityUnits = DDB_CAPACITY_UNITS
            writeCapacityUnits = DDB_CAPACITY_UNITS
        }
    }

    suspend fun createTable(
        dynamoDb: DynamoDbClient,
        tableName: String,
        partitionKeyName: String,
        sortKeyName: String?,
        gsis: List<GlobalSecondaryIndex>?
    ) {
        val createTableRequest = buildCreateTableRequest(tableName, partitionKeyName, sortKeyName, gsis)
        dynamoDb.createTable(createTableRequest)
        println("Created test table $tableName")
    }

    fun buildGlobalSecondaryIndexModel(gsiName: String, gsiPartitionKey: String, gsiSortKey: String?): GlobalSecondaryIndex {
        var keySchema = mutableListOf(
            KeySchemaElement {
                attributeName = gsiPartitionKey
                keyType = KeyType.Hash
            }
        )
        if (gsiSortKey != null) {
            keySchema.add(
                KeySchemaElement {
                    attributeName = gsiSortKey
                    keyType = KeyType.Range
                }
            )
        }

        return GlobalSecondaryIndex {
            indexName = gsiName
            this.keySchema = keySchema
            provisionedThroughput = PROVISIONED_THROUGHPUT
            projection = Projection {
                projectionType = ProjectionType.KeysOnly
            }
        }
    }

    fun buildCreateTableRequest(
        tableName: String,
        partitionKeyName: String,
        sortKeyName: String?,
        gsis: List<GlobalSecondaryIndex>?
    ): CreateTableRequest {
        var keySchema = mutableListOf(
            KeySchemaElement {
                attributeName = partitionKeyName
                keyType = KeyType.Hash
            }
        )
        var attributeDefinitions = mutableListOf(
            AttributeDefinition {
                attributeName = partitionKeyName
                attributeType = ScalarAttributeType.S
            }
        )

        if (sortKeyName != null) {
            keySchema.add(
                KeySchemaElement {
                    attributeName = sortKeyName
                    keyType = KeyType.Range
                }
            )
            attributeDefinitions.add(
                AttributeDefinition {
                    attributeName = sortKeyName
                    attributeType = ScalarAttributeType.S
                }
            )
        }

        // Add attribute definitions for GSI keys
        gsis?.forEach { gsi ->
            gsi.keySchema.forEach { keyElement ->
                val attributeName = keyElement.attributeName
                if (attributeDefinitions.none { it.attributeName == attributeName }) {
                    attributeDefinitions.add(
                        AttributeDefinition {
                            this.attributeName = attributeName
                            attributeType = ScalarAttributeType.S
                        }
                    )
                }
            }
        }

        return CreateTableRequest {
            this.tableName = tableName
            this.keySchema = keySchema
            this.attributeDefinitions = attributeDefinitions
            globalSecondaryIndexes = gsis
            provisionedThroughput = PROVISIONED_THROUGHPUT
        }
    }
}
