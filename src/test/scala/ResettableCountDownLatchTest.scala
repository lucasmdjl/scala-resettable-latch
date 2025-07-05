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

import org.scalatest.flatspec.AnyFlatSpecLike

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CyclicBarrier, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class ResettableCountDownLatchTest
    extends AnyFlatSpecLike
    with CountDownLatchLikeTest {
  override def latchFactory(count: Int): CountDownLatchLike =
    ResettableCountDownLatch(count)

  "ResettableCountDownLatch::reset" should "reset the count to the supplied value" in {
    val latch = ResettableCountDownLatch(2)
    latch.countDown()
    latch.countDown()
    latch.reset(3)
    assertResult(3, "after reset")(latch.getCount)
  }

  it should "reset to original count if no argument passed" in {
    val latch = ResettableCountDownLatch(2)
    latch.countDown()
    latch.countDown()
    latch.reset()
    assertResult(2, "after reset")(latch.getCount)
  }

  it should "throw LatchResetException if count is not 0" in {
    val latch = ResettableCountDownLatch(2)
    latch.countDown()
    assertThrows[LatchResetException](latch.reset())
    assertResult(1, "after failed reset")(latch.getCount)
  }

  it should "succeed at most once under heavy concurrent use" in {
    val threadPool = Executors.newFixedThreadPool(11)
    given ExecutionContext = ExecutionContext.fromExecutor(threadPool)

    val successCount = AtomicInteger()

    val latch = ResettableCountDownLatch(0)
    val start = CyclicBarrier(10)
    val futures = for (i <- 1 to 10) yield Future {
      start.await()
      try {
        latch.reset(3)
        successCount.incrementAndGet()
      } catch {
        case e: LatchResetException =>
      }
    }

    Await.result(Future.sequence(futures), 1.second)

    assertResult(3, "count")(latch.getCount)
    assertResult(1, "successes")(successCount.get())
  }

  "ResettableCountDownLatch::asOneShot" should "share state with original" in {
    val latch = ResettableCountDownLatch(2)
    val oneShotLatch = latch.asOneShot
    assertResult(2, "initial oneShot count")(oneShotLatch.getCount)
    latch.countDown()
    assertResult(1, "after resettable countDown")(oneShotLatch.getCount)
    oneShotLatch.countDown()
    assertResult(0, "after oneShot countDown")(latch.getCount)
  }

  it should "not reset when original resets" in {
    val latch = ResettableCountDownLatch(1)
    val oneShotLatch = latch.asOneShot
    latch.countDown()
    latch.reset()
    assertResult(0, "after reset")(oneShotLatch.getCount)
  }

}
