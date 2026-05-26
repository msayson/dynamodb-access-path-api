# Developer Guide
## Building the project

### First-time set-up
To build the project for the first time, run

```sh
./gradlew build
```

and validate that it completes successfully.

### Subsequent builds
In order to clean up stale build artifacts and rebuild the library based on your latest changes, run

```sh
./gradlew clean build
```

If you do not clean before building, your local environment may continue to use stale, cached artifacts in builds.

## Running local integ tests
Prerequisite set-up for Windows:
1. Install Docker Desktop from https://www.docker.com/products/docker-desktop/
2. Launch Docker Desktop and create or sign into a Docker account

To run local integ tests against test containers, run

```sh
./gradlew localIntegTest -DrunLocalIntegTests=true
```

### Debugging local integ tests

Helpful localIntegTest options:
* `--info` - print info logs to console
* `--rerun-tasks` - force rerun even if prior integ test run, use if auto-completes with message `Configuration cache entry reused.`

Eg.

```sh
./gradlew localIntegTest -DrunLocalIntegTests=true --info --rerun-tasks
```

Integ test code snippets to help with debugging:
* Can use `localstack.printLogs()` to print container logs for debugging.
* If need to debug LocalStack startup issues, update the `LocalStackContainer` definition to include:
  * `.withEnv("LS_LOG", "trace") // Enable detailed logging for debugging`
  * `.withEnv("DEBUG", "1") // Enable debug mode`

## Publishing to Maven Central

### One-time set-up

1. Create a [Sonatype Central Portal account](https://central.sonatype.com/) and request access to the `io.github.msayson` namespace.

2. Generate a GPG key for signing artifacts:
   ```sh
   gpg --gen-key
   gpg --list-secret-keys --keyid-format SHORT  # note your key ID (last 8 chars of fingerprint)
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```
   Export the secret keyring file if it doesn't exist:
   ```sh
   # Windows
   gpg --export-secret-keys YOUR_KEY_ID > C:/Users/YourUser/.gnupg/secring.gpg
   # macOS/Linux
   gpg --export-secret-keys YOUR_KEY_ID > ~/.gnupg/secring.gpg
   ```

3. Add signing and Central Portal credentials to `~/.gradle/gradle.properties`:
   ```properties
   signing.keyId=LAST8CHARS_OF_KEY_ID
   signing.password=YOUR_GPG_PASSPHRASE

   # Windows
   signing.secretKeyRingFile=C:/Users/YourUser/.gnupg/secring.gpg
   # macOS/Linux
   # signing.secretKeyRingFile=/home/youruser/.gnupg/secring.gpg

   centralUsername=YOUR_CENTRAL_PORTAL_TOKEN_USERNAME
   centralPassword=YOUR_CENTRAL_PORTAL_TOKEN_PASSWORD
   ```
   Generate these token values at https://central.sonatype.com/account by clicking **Generate User Token**. These are separate from your login credentials.

### Publishing a new version

1. Update `version` in `lib/build.gradle.kts`.

2. Verify all checks pass:
   ```sh
   ./gradlew clean build
   ```

3. Create the Maven Central bundle:
   ```sh
   ./gradlew bundleForMavenCentral
   ```
   The bundle ZIP is written to `lib/build/mavenCentralBundle/bundle.zip`.

4. Upload via the [Maven Central Portal](https://central.sonatype.com/publishing): go to **Publish** → **Upload Deployment** and select the ZIP file.

## Helpful commands

* `./gradlew build` - build project, run lint checker, and run unit tests
* `./gradlew clean build` - clear build artifacts, rebuild project, run lint checker, and run unit tests
* `./gradlew detekt` - run lint checker
* `./gradlew tasks` - list available Gradle tasks
* `./gradlew test` - run unit tests
* `./gradlew test --tests TestClass --info` - run unit tests from a specific test class with info-level logging, helpful when debugging errors
* `./gradlew test --tests TestClass.TestMethod --info` - run a specific unit test with info-level logging, helpful when debugging errors

## Troubleshooting

#### My local tests failed but the output doesn't include logs or stack traces needed to debug

Run `./gradlew build --info` to rerun the tests with info logging enabled, which will include logs and stack traces for failed tests.

#### My local builds are not picking up Gradle dependency changes

Run `./gradlew clean build --refresh-dependencies` to ignore your Gradle environment's cached entries for modules and artifacts, and download new versions if they have different published hashsums.

## Resources

* [AWS SDK for Kotlin API docs](https://docs.aws.amazon.com/sdk-for-kotlin/api/latest/)
* [AWS SDK for Kotlin DynamoDB code examples](https://docs.aws.amazon.com/sdk-for-kotlin/latest/developer-guide/kotlin_dynamodb_code_examples.html)
* [Kotlin API docs](https://kotlinlang.org/docs/api-references.html)
* [Testcontainers quickstart guide for JUnit 5](https://java.testcontainers.org/quickstart/junit_5_quickstart/)
