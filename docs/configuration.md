# Configuration

To use KIST ORM, you need to configure your build system to include the runtime library and the KSP processor.

## Dependencies

Add the necessary dependencies to your `build.gradle.kts` (assuming Kotlin Multiplatform).

### 1. Apply KSP Plugin

First, apply the KSP plugin in your `plugins` block.

```kotlin
plugins {
    kotlin("multiplatform") 
    // Check for the latest KSP version compatible with your Kotlin version
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" 
}
```

### 2. Add Library Dependencies

Add the KIST API runtime library and the KSP processor to your source sets.

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // KIST API Runtime
                implementation("io.rss.knative.tools.kist:kist-api:1.0") 
            }
        }
    }
}

dependencies {
    // Register the KSP processor for metadata generation
    add("kspCommonMainMetadata", "io.rss.knative.tools.kist:kist-ksp:1.0")
    
    // Register for specific targets (e.g., Apple targets, Linux, etc.)
    // Replace 'kspApple' with your specific target configuration name if different
    add("kspApple", "io.rss.knative.tools.kist:kist-ksp:1.0")
}
```

::: note
Adjust the version numbers (`1.0` or similar) to match the version of KIST you are using.
:::

## Setup Explanation

**KIST** relies heavily on **KSP (Kotlin Symbol Processing)** to bridge the gap between your annotated interfaces and the actual database calls.

1.  **Annotation Processing**: When you build your project, KSP scans your code for `@Entity` and `@Dao` annotations.
2.  **Code Generation**: The `kist-ksp` processor generates implementation classes (like `MyDaoImpl`) and metadata registries that map your classes to the database schema.
3.  **Runtime Registration**: The generated code interacts with `PersistenceContext` at runtime.

## Native Linker Options

Since KIST uses the `sqlite3` C library, you must ensure your native target links against it.

```kotlin
kotlin {
    nativeTarget.apply {
        binaries {
            executable {
                // Link against SQLite3
                linkerOpts.add("-lsqlite3")
                // additional params might be needed for Windows
            }
        }
    }
}
```

## Troubleshooting

### "Unresolved reference: injectDao" or similar errors

If the compiler cannot find generated code or extensions:

1.  Ensure the KSP plugin is applied.
2.  Ensure you have added the `kist-ksp` dependency to the correct configuration (e.g., `kspKotlinApple`, `kspNative`, etc.).
3.  Try running a generic `./gradlew clean build` to force KSP to run.
