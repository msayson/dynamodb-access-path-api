package io.github.msayson.dynamodb.accesspath.localinteg.testutil

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.seconds

class DynamoDbLocalStack {
    companion object {
        private const val DDB_PORT = 4566
        private const val MAX_ATTEMPTS_DDB_AVAILABILITY = 10
        private const val WAIT_TIME_BETWEEN_DDB_CHECKS_MS = 3000L
    }

    val localstack: LocalStackContainer
    val dynamoDb: DynamoDbClient

    constructor() {
        localstack = LocalStackContainer(DockerImageName.parse("localstack/localstack:stable"))
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withEnv("DYNAMODB_PROVIDER", "dynamodb-local") // Use DynamoDB Local
            .withEnv("SERVICES", "dynamodb") // Required to start DynamoDB service in LocalStack 2.x
            .withEnv("GATEWAY_LISTEN", "$DDB_PORT") // Fix the edge port
            .withExposedPorts(DDB_PORT) // Expose edge port
            .waitingFor(Wait.forLogMessage(".*Ready\\..*", 1))

        localstack.start()

        val edgePort = localstack.getMappedPort(DDB_PORT)
        val dynamoDbEndpoint = "http://localhost:$edgePort"
        dynamoDb = DynamoDbClient {
            region = localstack.region
            endpointUrl = Url.parse(dynamoDbEndpoint)
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "test"
                secretAccessKey = "test"
            }
            httpClient {
                maxConcurrency = 32u
                connectTimeout = 30.seconds
            }
        }
        runBlocking {
            waitForDynamoDb()
        }
    }

    fun printLogs() {
        println(localstack.logs)
    }

    /**
     * Waits for the local DynamoDB server to become available by periodically querying ListTables.
     *
     * Improves LocalStack integ test reliability since the DynamoDB service can take a few seconds
     * to become available after the container starts.
     */
    private suspend fun waitForDynamoDb() {
        println("Waiting for DynamoDB availability...")

        repeat(MAX_ATTEMPTS_DDB_AVAILABILITY) {
            try {
                println("Checking DynamoDB availability (attempt ${it + 1}/$MAX_ATTEMPTS_DDB_AVAILABILITY)...")
                dynamoDb.listTables()
                return
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                println("DynamoDB not available yet: ${e.message}. Retrying after ${WAIT_TIME_BETWEEN_DDB_CHECKS_MS}ms...")
                Thread.sleep(WAIT_TIME_BETWEEN_DDB_CHECKS_MS)
            }
        }
        error("DynamoDB did not become ready in time")
    }
}
