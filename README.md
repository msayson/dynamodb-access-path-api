# dynamodb-access-path-api

A Kotlin library that inspects a DynamoDB table's key schema and automatically selects the most efficient access path for a given attribute.

## Access path types

| `AccessPathType` | Condition |
|---|---|
| `GET_ITEM` | Attribute is the partition key and the table has no sort key |
| `TABLE_QUERY` | Attribute is the partition key and the table has a sort key |
| `GSI_QUERY` | Attribute is the partition key of a Global Secondary Index |
| `SCAN` | Attribute is not a partition key on the table or any GSI |

## Requirements

- JDK 21+
- Kotlin 2.x
- An AWS account with IAM permissions for `dynamodb:DescribeTable` on the target table

## Usage

### With coroutines (recommended)

```kotlin
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import io.github.msayson.dynamodb.accesspath.AccessPathResolver

DynamoDbClient.fromEnvironment().use { client ->
    val resolver = AccessPathResolver(client)
    val accessPath = resolver.resolveAccessType(
        tableArn = "arn:aws:dynamodb:us-east-1:123456789012:table/MyTable",
        attributeName = "userId"
    )
    println(accessPath) // e.g. GET_ITEM
}
```

### Without coroutines

```kotlin
val accessPath = resolver.resolveAccessTypeBlocking(
    tableArn = "arn:aws:dynamodb:us-east-1:123456789012:table/MyTable",
    attributeName = "userId"
)
```

## Error handling

| Exception | Cause |
|---|---|
| `IllegalArgumentException` | Table not found, key schema is missing, or table has no partition key |
| AWS SDK exceptions | Network errors, throttling, or insufficient IAM permissions |

## Building and testing

```bash
./gradlew build   # compile and run all checks
./gradlew test    # run unit tests
```

## License

[Apache 2.0](LICENSE)
