/*
 * Scala Resettable Latch - A Scala 3 micro-library providing thread-safe countdown latches with reset capabilities and Scala-friendly APIs.
 * Copyright (C) 2025 Lucas de Jong
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.lucasmdjl.resettablelatch

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CountDownLatch, TimeUnit, TimeoutException}
import scala.concurrent.duration.FiniteDuration

/** A thread-safe countdown latch abstraction that provides Scala-friendly APIs
  * for synchronization primitives.
  *
  * A countdown latch starts with a given count and allows threads to wait until
  * the count reaches zero through a series of countdown operations.
  */
trait CountDownLatchLike {

  /** Returns the current count of the latch.
    *
    * @return
    *   the current count, or 0 if the latch has been triggered
    */
  def getCount: Int

  /** Decrements the count of the latch by one.
    *
    * If the count reaches zero, all waiting threads are released. If the count
    * is already zero, this method has no effect.
    */
  def countDown(): Unit

  /** Causes the current thread to wait until the latch has counted down to
    * zero.
    *
    * If the current count is already zero, this method returns immediately.
    *
    * @throws InterruptedException
    *   if the current thread is interrupted while waiting
    */
  def await(): Unit

  /** Causes the current thread to wait until the latch has counted down to zero
    * or the specified timeout elapses.
    *
    * If the current count is already zero, this method returns immediately.
    *
    * @param timeout
    *   the maximum time to wait
    * @throws TimeoutException
    *   if the timeout elapses before the count reaches zero
    * @throws InterruptedException
    *   if the current thread is interrupted while waiting
    */
  def await(timeout: FiniteDuration): Unit
}

/** A Scala-friendly wrapper around Java's CountDownLatch.
  *
  * This implementation provides improved timeout handling by throwing
  * exceptions instead of returning boolean values, and accepts Scala's
  * FiniteDuration for timeout specifications.
  *
  * @param count
  *   the initial count; must be non-negative
  * @throws IllegalArgumentException
  *   if count is negative
  */
class OneShotCountDownLatch(count: Int) extends CountDownLatchLike {
  require(count >= 0, "count must not be negative")
  private val inner = CountDownLatch(count)

  /** Returns the current count of the latch.
    *
    * @return
    *   the current count, or 0 if the latch has been triggered
    */
  override def getCount: Int = inner.getCount.toInt

  /** Decrements the count of the latch by one.
    *
    * If the count reaches zero, all waiting threads are released. If the count
    * is already zero, this method has no effect.
    */
  override def countDown(): Unit = inner.countDown()

  /** Causes the current thread to wait until the latch has counted down to
    * zero.
    *
    * If the current count is already zero, this method returns immediately.
    *
    * @throws InterruptedException
    *   if the current thread is interrupted while waiting
    */
  override def await(): Unit = inner.await()

  /** Causes the current thread to wait until the latch has counted down to zero
    * or the specified timeout elapses.
    *
    * If the current count is already zero, this method returns immediately.
    *
    * @param timeout
    *   the maximum time to wait
    * @throws TimeoutException
    *   if the timeout elapses before the count reaches zero
    * @throws InterruptedException
    *   if the current thread is interrupted while waiting
    */
  override def await(timeout: FiniteDuration): Unit = if (
    !inner.await(timeout.toMillis, TimeUnit.MILLISECONDS)
  ) {
    throw TimeoutException(s"Latch timed-out after $timeout")
  }

  /** Returns the underlying Java CountDownLatch for interoperability with Java
    * APIs.
    *
    * @return
    *   the wrapped Java CountDownLatch instance
    */
  def asJava: CountDownLatch = inner
}

/** Exception thrown when attempting to reset a ResettableCountDownLatch that
  * has not yet reached zero.
  *
  * @param message
  *   the detail message explaining why the reset failed
  */
class LatchResetException(message: String) extends RuntimeException(message)

/** A thread-safe countdown latch that can be reset and reused multiple times.
  *
  * Unlike standard countdown latches, this implementation allows resetting the
  * count to a new value once the current count reaches zero, enabling reuse of
  * the same synchronization primitive across multiple operations.
  *
  * @param count
  *   the initial count; must be non-negative
  * @throws IllegalArgumentException
  *   if count is negative
  */
class ResettableCountDownLatch(count: Int) extends CountDownLatchLike {
  require(count >= 0, "count must not be negative")
  private val inner = AtomicReference(OneShotCountDownLatch(count))

  /** Returns the current count of the latch.
    *
    * @return
    *   the current count, or 0 if the latch has been triggered
    */
  override def getCount: Int = inner.get().getCount

  /** Decrements the count of the latch by one.
    *
    * If the count reaches zero, all waiting threads are released. If the count
    * is already zero, this method has no effect.
    */
  override def countDown(): Unit = inner.get().countDown()

  /** Causes the current thread to wait until the latch has counted down to
    * zero.
    *
    * If the current count is already zero, this method returns immediately.
    *
    * @throws InterruptedException
    *   if the current thread is interrupted while waiting
    */
  override def await(): Unit = inner.get().await()

  /** Causes the current thread to wait until the latch has counted down to zero
    * or the specified timeout elapses.
    *
    * If the current count is already zero, this method returns immediately.
    *
    * @param timeout
    *   the maximum time to wait
    * @throws TimeoutException
    *   if the timeout elapses before the count reaches zero
    * @throws InterruptedException
    *   if the current thread is interrupted while waiting
    */
  override def await(timeout: FiniteDuration): Unit = inner.get().await(timeout)

  /** Resets the latch to a new count, allowing it to be reused.
    *
    * This operation is only permitted when the current count is zero. If the
    * current count is greater than zero, a LatchResetException is thrown.
    *
    * The reset operation is atomic - either it succeeds completely or fails
    * without modifying the latch state.
    *
    * @param newCount
    *   the new count to set; defaults to the original count if not specified;
    *   must be non-negative
    * @throws LatchResetException
    *   if the current count is greater than zero
    * @throws IllegalArgumentException
    *   if newCount is negative
    */
  def reset(newCount: Int = count): Unit = {
    require(newCount >= 0, "newCount must not be negative")
    inner.updateAndGet(previous =>
      val count = previous.getCount
      if (count > 0)
        throw LatchResetException(
          s"Unable to reset because current count was $count > 0"
        )
      OneShotCountDownLatch(newCount)
    )
  }

  /** Returns the current latch state as a OneShotCountDownLatch that cannot be
    * reset.
    *
    * The returned latch shares the same underlying countdown state with this
    * ResettableCountDownLatch. This means:
    *   - Calling `countDown()` on either latch will affect both
    *   - Both latches will be triggered when the shared count reaches zero
    *   - Only reset operations are isolated - the returned latch cannot be
    *     reset
    *
    * This is useful for passing to code that expects a non-resettable latch
    * while maintaining shared countdown behavior, or for Java interoperability
    * via the `toJava` method.
    *
    * @return
    *   a OneShotCountDownLatch that shares countdown state but cannot be reset
    */
  def asOneShot: OneShotCountDownLatch = inner.get()
}
