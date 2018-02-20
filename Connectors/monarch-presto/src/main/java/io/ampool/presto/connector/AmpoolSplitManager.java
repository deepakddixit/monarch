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

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.predicate.TupleDomain;
import io.ampool.client.AmpoolClient;
import io.ampool.monarch.table.TableDescriptor;
import io.ampool.monarch.table.filter.Filter;
import io.ampool.monarch.table.internal.MTableUtils;
import io.ampool.monarch.types.TypeHelper;
import io.ampool.presto.log.AmpoolLogger;
import org.apache.geode.distributed.internal.ServerLocation;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class AmpoolSplitManager implements ConnectorSplitManager {
  private static final AmpoolLogger log = AmpoolLogger.get(AmpoolSplitManager.class);
  public static boolean TEST_MODE = false;

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

    TupleDomain<ColumnHandle> queryConstraints = layoutHandle.getConstraints();
    Filter filter = FilterUtils.convertToAmpoolFilters(queryConstraints);

    List<ConnectorSplit> splits = new ArrayList<>();
    // TODO Pass here bucket id
    TableDescriptor tableDescriptor = table.getTable().getTableDescriptor();
    int buckets = tableDescriptor.getTotalNumOfSplits();
    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
//    MTableUtils.getLocationMap(table.getTable(), null, primaryBucketMap, null, AmpoolOpType.ANY_OP);
    Map<Integer, Set<ServerLocation>> bucketLocationMap = new HashMap<>();
    List<MTableUtils.MSplit> splits1 = MTableUtils.getSplits(table.getTable().getName(), 4, buckets, bucketLocationMap);

    for (MTableUtils.MSplit mSplit : splits1) {
      splits.add(
          new AmpoolSplit(connectorId, tableHandle.getSchemaName(), tableHandle.getTableName(), mSplit.getBuckets(),
              getAddress(mSplit.getServers().iterator().next()), filter));
    }
    log.debug("Ampool splits location " + TypeHelper.deepToString(splits));
//
//    log.debug("Ampool splits location " + TypeHelper.deepToString(primaryBucketMap));
//    log.debug("Using filters " + TypeHelper.deepToString(filter));
//    primaryBucketMap.forEach((k, v) -> {
//      splits.add(
//          new AmpoolSplit(connectorId, tableHandle.getSchemaName(), tableHandle.getTableName(), k,
//              getAddress(v), filter));
//    });

    return new FixedSplitSource(splits);
  }


  private HostAddress getAddress(String v) {
    if (TEST_MODE) {
      return HostAddress.fromString("127.0.0.1");
    }
    return HostAddress.fromString(v);
  }

}
