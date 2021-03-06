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

package com.netflix.spinnaker.orca.echo.spring

import com.netflix.spinnaker.orca.ExecutionStatus
import com.netflix.spinnaker.orca.echo.EchoService
import com.netflix.spinnaker.orca.front50.Front50Service
import com.netflix.spinnaker.orca.front50.model.ApplicationNotifications
import com.netflix.spinnaker.orca.front50.model.ApplicationNotifications.Notification
import com.netflix.spinnaker.orca.pipeline.model.Pipeline
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class EchoNotifyingExecutionListenerSpec extends Specification {

  def echoService = Mock(EchoService)
  def front50Service = Mock(Front50Service)

  @Subject
  def echoListener = new EchoNotifyingExecutionListener(echoService, front50Service)

  @Shared
  ApplicationNotifications notifications = new ApplicationNotifications()

  @Shared
  Notification slackPipes

  @Shared
  Notification slackTasks

  @Shared
  Notification emailTasks

  void setup() {
    slackPipes = new Notification([
      when   : ["pipeline.started", "pipeline.failed"],
      type   : "slack",
      address: "spinnaker"
    ])
    slackTasks = new Notification([
      when   : ["task.completed"],
      type   : "slack",
      address: "spinnaker-tasks"
    ])
    emailTasks = new Notification([
      when: ["task.started"],
      type: "email"
    ])

    notifications.set("slack", [slackPipes, slackTasks])
    notifications.set("email", [emailTasks])
  }

  void "adds notifications to pipeline on beforeExecution"() {
    given:
    Pipeline pipeline = new Pipeline(application: "myapp")

    when:
    echoListener.beforeExecution(null, pipeline)

    then:
    pipeline.notifications == [slackPipes]
    1 * front50Service.getApplicationNotifications("myapp") >> notifications
    1 * echoService.recordEvent(_)
    0 * _
  }

  void "adds notifications to pipeline on afterExecution"() {
    given:
    Pipeline pipeline = new Pipeline(application: "myapp")

    when:
    echoListener.afterExecution(null, pipeline, ExecutionStatus.TERMINAL, false)

    then:
    pipeline.notifications == [slackPipes]
    1 * front50Service.getApplicationNotifications("myapp") >> notifications
    1 * echoService.recordEvent(_)
    0 * _
  }

  void "dedupes notifications"() {
    given:
    Pipeline pipeline = new Pipeline(application: "myapp")
    def pipelineConfiguredNotification = [
      when   : ["pipeline.started", "pipeline.completed"],
      type   : "slack",
      address: "spinnaker",
      extraField: "extra"
    ]
    pipeline.notifications.add(pipelineConfiguredNotification)

    when:
    echoListener.beforeExecution(null, pipeline)

    then:
    pipeline.notifications.size() == 2
    pipeline.notifications.when == [["pipeline.started", "pipeline.completed"], ["pipeline.failed"]]
    pipeline.notifications.extraField == ["extra", null]
    1 * front50Service.getApplicationNotifications("myapp") >> notifications
    1 * echoService.recordEvent(_)
    0 * _
  }

  void "handles case where no notifications are present"() {
    given:
    Pipeline pipeline = new Pipeline(application: "myapp")

    when:
    echoListener.beforeExecution(null, pipeline)

    then:
    pipeline.notifications == []
    1 * front50Service.getApplicationNotifications("myapp") >> null
    1 * echoService.recordEvent(_)
    0 * _
  }

  void "handles case where no application notifications are present"() {
    given:
    Pipeline pipeline = new Pipeline(application: "myapp")
    def pipelineConfiguredNotification = [
      when   : ["pipeline.started", "pipeline.completed"],
      type   : "slack",
      address: "spinnaker"
    ]
    pipeline.notifications.add(pipelineConfiguredNotification)

    when:
    echoListener.beforeExecution(null, pipeline)

    then:
    pipeline.notifications.size() == 1
    pipeline.notifications[0].when == ["pipeline.started", "pipeline.completed"]
    1 * front50Service.getApplicationNotifications("myapp") >> null
    1 * echoService.recordEvent(_)
    0 * _
  }
}
