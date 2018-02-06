/*
* Copyright (c) 2017 Ampool, Inc. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you
* may not use this file except in compliance with the License. You
* may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
* implied. See the License for the specific language governing
* permissions and limitations under the License. See accompanying
* LICENSE file.
*/
package io.ampool.presto.connector;

import static com.facebook.presto.testing.TestingSession.testSessionBuilder;

import java.util.Map;

import com.facebook.presto.Session;
import com.facebook.presto.tests.DistributedQueryRunner;
import com.google.common.collect.ImmutableMap;
import io.airlift.log.Logger;
import io.ampool.client.AmpoolClient;

public final class MonarchPrestoQueryRunner {
  private static final Logger LOG = Logger.get(MonarchPrestoQueryRunner.class);

  MonarchPrestoQueryRunner() {

  }

  public AmpoolClient getAmpoolClient(String host, int port) {
    return new AmpoolClient(host, port);
  }

  public static DistributedQueryRunner createAmpoolQueryRunner(String host, int port,
                                                               Map<String, String> extraProperties, int workers)
      throws Exception {

    // set test mode so splits will be assigned locallly
    AmpoolSplitManager.TEST_MODE = true;

    DistributedQueryRunner queryRunner =
        new DistributedQueryRunner(createSession(), workers, extraProperties);

    queryRunner.installPlugin(new AmpoolPlugin());
    Map<String, String> ampoolProperties =
        ImmutableMap.<String, String>builder()
            .put(MonarchProperties.LOCATOR_HOST, host)
            .put(MonarchProperties.LOCATOR_PORT, String.valueOf(port))
            .build();

    queryRunner.createCatalog("ampool", "ampool", ampoolProperties);

    return queryRunner;
  }

  public static Session createSession() {
    return testSessionBuilder().setCatalog("ampool").setSchema("ampool").build();
  }
}
