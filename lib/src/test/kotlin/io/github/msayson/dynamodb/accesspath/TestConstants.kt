package io.github.msayson.dynamodb.accesspath

import aws.sdk.kotlin.services.dynamodb.model.ProvisionedThroughput

object TestConstants {
    private const val DDB_CAPACITY_UNITS = 10L

    public val PROVISIONED_THROUGHPUT = ProvisionedThroughput {
        readCapacityUnits = DDB_CAPACITY_UNITS
        writeCapacityUnits = DDB_CAPACITY_UNITS
    }

    public const val TABLE_PK_NAME = "TestPartitionKey"
    public const val TABLE_SK_NAME = "TestSortKey"
}
