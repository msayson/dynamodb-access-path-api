package io.github.msayson.dynamodb.accesspath.localinteg

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import io.github.msayson.dynamodb.accesspath.localinteg.testutil.DynamoDbLocalStack
import io.github.msayson.dynamodb.accesspath.localinteg.testutil.DynamoDbRepositoryUtils
import org.junit.jupiter.api.BeforeAll

abstract class AccessPathResolverIntegTest {
    protected lateinit var localstack: DynamoDbLocalStack
    protected lateinit var dynamoDb: DynamoDbClient
    protected val dynamoDbUtils: DynamoDbRepositoryUtils = DynamoDbRepositoryUtils()

    @BeforeAll
    fun setupLocalStack() {
        localstack = DynamoDbLocalStack()
        dynamoDb = localstack.dynamoDb
    }
}
