# scala-resettable-latch

A Scala 3 micro-library providing thread-safe countdown latches with reset capabilities and Scala-friendly APIs.

## Overview

While Java's `CountDownLatch` is useful for one-time synchronization, it cannot be reset once the count reaches zero. This library provides a resettable alternative while maintaining full compatibility with existing countdown latch usage patterns.

## Features

- **Scala-friendly alternative** to Java's CountDownLatch with improved APIs
- **Resettable implementation** that can be safely reset and reused
- **Scala-friendly API** with `FiniteDuration` support and proper exception handling
- **Thread-safe** operations using atomic references
- **Timeout exceptions** instead of boolean returns for clearer error handling

## Installation

```scala
// Add to your build.sbt
libraryDependencies += "dev.lucasmdjl" %% "scala-resettable-latch" % "0.2.0"
```

## Usage

### Basic Usage (Drop-in replacement)

```scala
import scala.concurrent.duration._

// Create a latch that waits for 3 operations
val latch: CountDownLatchLike = new OneShotCountDownLatch(3)

// In worker threads
latch.countDown()

// In waiting thread
latch.await() // blocks until count reaches 0
// or with timeout
latch.await(5.seconds) // throws TimeoutException if not completed in time
```

### Resettable Latch

```scala
val resettableLatch = new ResettableCountDownLatch(2)

// Use normally
resettableLatch.countDown()
resettableLatch.countDown()
resettableLatch.await() // completes

// Reset for reuse
resettableLatch.reset(3) // now waiting for 3 new countdowns

// Reset only works when count is 0
// resettableLatch.reset(1) // would throw LatchResetException if count > 0
```

### Integration with Existing Code

```scala
// Your existing function that takes CountDownLatch
def waitForTasks(latch: java.util.concurrent.CountDownLatch): Unit = {
  latch.await()
}

// OneShotCountDownLatch cannot be passed directly - it's a wrapper, not a subclass
// You would need to refactor to use the trait:
def waitForTasks(latch: CountDownLatchLike): Unit = {
  latch.await(30.seconds)
}

// Then both implementations work:
val oneShotLatch = new OneShotCountDownLatch(3)
val resettableLatch = new ResettableCountDownLatch(3)
waitForTasks(oneShotLatch)    // ✓ works
waitForTasks(resettableLatch) // ✓ works
```

## API Reference

### CountDownLatchLike

The main interface providing countdown latch functionality:

- `getCount: Int` - Returns the current count
- `countDown(): Unit` - Decrements the count by one
- `await(): Unit` - Blocks until count reaches zero
- `await(timeout: FiniteDuration): Unit` - Blocks with timeout, throws `TimeoutException` on timeout

### OneShotCountDownLatch

A Scala-friendly wrapper around Java's CountDownLatch:
- All operations from `CountDownLatchLike`
- `asJava: CountDownLatch` - Returns a view of this as a CountDownLatch

### ResettableCountDownLatch

A thread-safe resettable countdown latch:
- All operations from `CountDownLatchLike`
- `reset(newCount: Int): Unit` - Resets the latch with a new count (only when current count is 0)
- `asOneShot: OneShotCountDownLatch` - Returns a view of this as a OneShotCountDownLatch (doesn't share resets)

## Thread Safety

All implementations are fully thread-safe:
- `OneShotCountDownLatch` inherits thread safety from Java's `CountDownLatch`
- `ResettableCountDownLatch` uses `AtomicReference` with `updateAndGet` for atomic operations

## Error Handling

- **Timeout operations** throw `TimeoutException` with descriptive messages
- **Invalid reset attempts** throw `LatchResetException` when trying to reset a latch with count > 0
- **No silent failures** - all error conditions result in clear exceptions

## When to Use

**Use OneShotCountDownLatch when:**
- Migrating from Java's CountDownLatch
- You want Scala-friendly timeout handling
- One-time synchronization is sufficient

**Use ResettableCountDownLatch when:**
- You need to reuse the same latch multiple times
- Implementing recurring synchronization patterns
- Building reusable coordination primitives

## License

AGPL-3.0
