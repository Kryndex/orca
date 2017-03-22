/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.q

import java.util.*
import java.util.concurrent.TimeUnit

interface Queue<T : Message> {
  fun poll(): T?
  fun push(message: T): Unit
  fun push(message: T, delay: Long, unit: TimeUnit)
  fun ack(message: T): Unit
}

typealias CommandQueue = Queue<Command>

typealias EventQueue = Queue<Event>

interface Message {
  val id: UUID
}