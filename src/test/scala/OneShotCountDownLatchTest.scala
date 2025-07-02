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

package dev.lucasmdjl.scala.resettablelatch

import org.scalatest.flatspec.AnyFlatSpecLike

class OneShotCountDownLatchTest
    extends AnyFlatSpecLike
    with CountDownLatchLikeTest {
  override def latchFactory(count: Int): CountDownLatchLike =
    OneShotCountDownLatch(count)

  "OneShotCountDownLatch::asJava" should "share state with original" in {
    val latch = OneShotCountDownLatch(2)
    val javaLatch = latch.asJava
    assertResult(2, "initial java count")(javaLatch.getCount)
    latch.countDown()
    assertResult(1, "after scala countDown")(javaLatch.getCount)
    javaLatch.countDown()
    assertResult(0, "after java countDown")(latch.getCount)
  }
}
