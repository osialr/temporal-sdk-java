/*
 * Copyright (C) 2022 Temporal Technologies, Inc. All Rights Reserved.
 *
 * Copyright (C) 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this material except in compliance with the License.
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

package io.temporal.spring.boot.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {
      AutoDiscoveryByTaskQueueTest.Configuration.class,
      WorkersAreNotStartingBeforeContextTest.EventChecks.class,
    })
@ActiveProfiles(profiles = "auto-discovery-by-task-queue")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WorkersAreNotStartingBeforeContextTest {
  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired EventChecks eventChecks;

  @Test
  @Timeout(value = 10)
  public void testWorkersAreGettingStartedOnlyWhenStartOfSpringContextIsCalled() {
    assertFalse(
        eventChecks.startedAtCtxRefresh,
        "Context refresh or initialization shouldn't cause workers start");
    assertFalse(
        eventChecks.startedAtAppStarted, "Application Started shouldn't cause workers start");
    applicationContext.start();
    assertTrue(eventChecks.startedAtAppReady, "Workers should be started at Application Ready");
  }

  public static class EventChecks {
    boolean startedAtAppStarted;
    boolean startedAtAppReady;
    boolean startedAtCtxRefresh;

    @Autowired WorkerFactory workerFactory;

    @EventListener
    public void applicationStarted(ContextRefreshedEvent event) {
      startedAtAppStarted = workerFactory.isStarted();
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
      startedAtAppStarted = workerFactory.isStarted();
    }

    @EventListener
    public void applicationStarted(ApplicationReadyEvent event) {
      startedAtAppReady = workerFactory.isStarted();
    }
  }

  @ComponentScan(
      excludeFilters =
          @ComponentScan.Filter(
              pattern = "io\\.temporal\\.spring\\.boot\\.autoconfigure\\.byworkername\\..*",
              type = FilterType.REGEX))
  public static class Configuration {}
}
