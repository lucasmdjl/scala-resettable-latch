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

import dev.lucasmdjl.delayedfuture.delayed
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.must.Matchers.{be, noException}

import java.util.concurrent.{
  CountDownLatch,
  CyclicBarrier,
  Executors,
  TimeoutException
}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait CountDownLatchLikeTest { this: AnyFlatSpecLike =>

  def latchFactory(count: Int): CountDownLatchLike

  "CountDownLatchLike::getCount" should "be initial count if no countDown called" in {
    val latch = latchFactory(42)
    assertResult(42)(latch.getCount)
  }

  "CountDownLatchLike" should "work if initialized with a zero count" in {
    noException should be thrownBy latchFactory(0)
  }

  it should "throw IllegalArgumentException if initialized with a negative count" in {
    assertThrows[IllegalArgumentException](latchFactory(-42))
  }

  "CountDownLatchLike::countDown" should "decrease count if it's > 0" in {
    val latch = latchFactory(42)
    latch.countDown()
    assertResult(41, "first call")(latch.getCount)
    latch.countDown()
    assertResult(40, "second call")(latch.getCount)
  }

  it should "do nothing if count is 0" in {
    val latch = latchFactory(0)
    latch.countDown()
    assertResult(0)(latch.getCount)
  }

  it should "work correctly under heavy concurrent access" in {
    val threadPool = Executors.newFixedThreadPool(11)
    given ExecutionContext = ExecutionContext.fromExecutor(threadPool)
    val latch = latchFactory(10)
    val start = CyclicBarrier(10)
    val futures = for (i <- 1 to 10) yield Future {
      start.await()
      latch.countDown()
    }

    Await.result(Future.sequence(futures), 1.second)

    assertResult(0)(latch.getCount)
  }

  "CountDownLatchLike::await" should "throw TimeoutException if timeout is exceeded" in {
    val latch = latchFactory(1)
    Future.delayed(1.second) {
      latch.countDown()
    }
    assertThrows[TimeoutException](latch.await(100.millis))
  }

  it should "unblock await when count reaches 0 before timeout" in {
    val latch = latchFactory(1)
    Future.delayed(100.millis) {
      latch.countDown()
    }
    noException should be thrownBy latch.await(1.second)
  }

  it should "complete immediately if count is 0" in {
    val latch = latchFactory(1)
    latch.countDown()
    noException should be thrownBy latch.await(0.millis)
  }

  it should "throw TimeoutException if count doesn't get to 0" in {
    val latch = latchFactory(2)
    latch.countDown()
    assertThrows[TimeoutException](latch.await(100.millis))
  }

  it should "throw InterruptedException if the thread is interrupted while waiting" in {
    val latch = latchFactory(1)
    val ready = CountDownLatch(1)
    @volatile var result: Option[Try[Unit]] = None
    val t = Thread(() =>
      ready.countDown()
      result = Some(
        try {
          latch.await()
          Success(())
        } catch {
          case e: InterruptedException => Failure(e)
        }
      )
    )
    t.start()
    ready.await()

    while (t.getState != Thread.State.WAITING) {
      Thread.`yield`()
    }

    t.interrupt()
    t.join()
    assert(result.isDefined, "result should have been set")
    assert(result.get.isFailure, "expected a failure")
    assert(
      result.get.failed.get.isInstanceOf[InterruptedException],
      "expected an InterruptedException"
    )
  }
}
