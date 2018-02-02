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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import io.airlift.log.Logger;
import io.ampool.client.AmpoolClient;
import io.ampool.internal.AmpoolOpType;
import io.ampool.monarch.table.Pair;
import io.ampool.monarch.table.TableDescriptor;
import io.ampool.monarch.table.internal.MTableUtils;
import io.ampool.monarch.types.TypeHelper;
import io.ampool.presto.log.AmpoolLogger;

import org.apache.geode.distributed.internal.ServerLocation;

public class AmpoolSplitManager implements ConnectorSplitManager {
  private static final AmpoolLogger log = AmpoolLogger.get(AmpoolSplitManager.class);

  private final String connectorId;
  private final AmpoolClient ampoolClient;

  @Inject
  public AmpoolSplitManager(AmpoolConnectorID connectorId, AmpoolClient ampoolClient) {
    this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
    this.ampoolClient = requireNonNull(ampoolClient, "client is null");
    log.info("INFORMATION: AmpoolSplitManager created successfully.");
  }

  @Override
  public ConnectorSplitSource getSplits(ConnectorTransactionHandle handle, ConnectorSession session,
                                        ConnectorTableLayoutHandle layout) {
    log.info("INFORMATION: AmpoolSplitManager getSplits() called.");

    AmpoolTableLayoutHandle layoutHandle = (AmpoolTableLayoutHandle) layout;
    AmpoolTableHandle tableHandle = layoutHandle.getTable();
    AmpoolTable table = new AmpoolTable(ampoolClient, tableHandle.getTableName());
    // this can happen if table is removed during a query
    checkState(table.getColumnsMetadata() != null, "Table %s.%s no longer exists",
        tableHandle.getSchemaName(), tableHandle.getTableName());

    List<ConnectorSplit> splits = new ArrayList<>();
    // TODO Pass here bucket id
    TableDescriptor tableDescriptor = table.getTable().getTableDescriptor();
    int buckets = tableDescriptor.getTotalNumOfSplits();
    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
    MTableUtils.getLocationMap(table.getTable(), null, primaryBucketMap, null, AmpoolOpType.ANY_OP);
    log.debug("Ampool splits location " + TypeHelper.deepToString(primaryBucketMap));
    for (int i = 0; i < buckets; i++) {
      ServerLocation serverLocation = primaryBucketMap.get(i);
      splits.add(
          new AmpoolSplit(connectorId, tableHandle.getSchemaName(), tableHandle.getTableName(), i,
              HostAddress.fromParts(getIPAddress(serverLocation.getHostName()),
                  serverLocation.getPort())));
    }
//        Collections.shuffle(splits);
    return new FixedSplitSource(splits);
  }

  private String getIPAddress(String hostName) {
    if (hostName.contains("9fxk")) {
      return "10.128.0.13";
    } else if (hostName.contains("kb9m")) {
      return "10.128.0.23";
    }
    if (hostName.contains("xdv0")) {
      return "10.128.0.24";
    }
    return hostName;
  }
}
