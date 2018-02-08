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
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.predicate.Domain;
import com.facebook.presto.spi.predicate.Range;
import com.facebook.presto.spi.predicate.TupleDomain;
import io.ampool.client.AmpoolClient;
import io.ampool.internal.AmpoolOpType;
import io.ampool.monarch.table.TableDescriptor;
import io.ampool.monarch.table.filter.Filter;
import io.ampool.monarch.table.filter.FilterList;
import io.ampool.monarch.table.filter.SingleColumnValueFilter;
import io.ampool.monarch.table.internal.MTableUtils;
import io.ampool.monarch.types.CompareOp;
import io.ampool.monarch.types.TypeHelper;
import io.ampool.presto.log.AmpoolLogger;

import org.apache.geode.distributed.internal.ServerLocation;

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
    Filter filter = convertToAmpoolFilters(queryConstraints);

    List<ConnectorSplit> splits = new ArrayList<>();
    // TODO Pass here bucket id
    TableDescriptor tableDescriptor = table.getTable().getTableDescriptor();
    int buckets = tableDescriptor.getTotalNumOfSplits();
    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
    MTableUtils.getLocationMap(table.getTable(), null, primaryBucketMap, null, AmpoolOpType.ANY_OP);
    log.debug("Ampool splits location " + TypeHelper.deepToString(primaryBucketMap));
    log.debug("Using filters " + TypeHelper.deepToString(filter));
    primaryBucketMap.forEach((k, v) -> {
      splits.add(
          new AmpoolSplit(connectorId, tableHandle.getSchemaName(), tableHandle.getTableName(), k,
              getAddress(v), filter));
    });

    return new FixedSplitSource(splits);
  }

  private Filter convertToAmpoolFilters(TupleDomain<ColumnHandle> queryConstraints) {
    FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ALL);

    Map<ColumnHandle, Domain> columnHandleDomainMap = queryConstraints.getDomains().get();

    columnHandleDomainMap.forEach((columnHandle, domain) -> {
      if (domain != null) {
        filters.addFilter(toPredicate(((AmpoolColumnHandle) columnHandle).getColumnName(), domain));
      }
    });
    return filters;
  }

  private Filter toPredicate(String columnName, Domain domain) {
//    checkArgument(domain.getType().isOrderable(), "Domain type must be orderable");

//    if (domain.getValues().isNone()) {
//      return domain.isNullAllowed() ? columnName + " IS NULL" : "FALSE";
//    }

//    if (domain.getValues().isAll()) {
//      return domain.isNullAllowed() ? "TRUE" : columnName + " IS NOT NULL";
//    }

    List<Filter> disjuncts = new ArrayList<>();
    List<Object> singleValues = new ArrayList<>();
    for (Range range : domain.getValues().getRanges().getOrderedRanges()) {
//      checkState(!range.isAll()); // Already checked
      if (range.isSingleValue()) {
        singleValues.add(range.getLow().getValue());
      } else {
        List<Filter> rangeConjuncts = new ArrayList<>();
        if (!range.getLow().isLowerUnbounded()) {
          switch (range.getLow().getBound()) {
            case ABOVE:
              rangeConjuncts.add(toPredicate(columnName, ">", range.getLow().getValue()));
              break;
            case EXACTLY:
              rangeConjuncts.add(toPredicate(columnName, ">=", range.getLow().getValue()));
              break;
            case BELOW:
              throw new IllegalArgumentException("Low marker should never use BELOW bound");
            default:
              throw new AssertionError("Unhandled bound: " + range.getLow().getBound());
          }
        }
        if (!range.getHigh().isUpperUnbounded()) {
          switch (range.getHigh().getBound()) {
            case ABOVE:
              throw new IllegalArgumentException("High marker should never use ABOVE bound");
            case EXACTLY:
              rangeConjuncts.add(toPredicate(columnName, "<=", range.getHigh().getValue()));
              break;
            case BELOW:
              rangeConjuncts.add(toPredicate(columnName, "<", range.getHigh().getValue()));
              break;
            default:
              throw new AssertionError("Unhandled bound: " + range.getHigh().getBound());
          }
        }
        if (!rangeConjuncts.isEmpty()) {
          FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
          rangeConjuncts.forEach(filter -> {
            filterList.addFilter(filter);
          });
          disjuncts.add(filterList);
        }
      }
    }

    // Add back all of the possible single values either as an equality or an IN predicate
    if (singleValues.size() == 1) {
      disjuncts.add(toPredicate(columnName, "=", getOnlyElement(singleValues)/*, type*/));
    } else if (singleValues.size() > 1) {
      FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
      for (Object value : singleValues) {
        filterList.addFilter(toPredicate(columnName, "=", value));
      }
      disjuncts.add(filterList);
    }

    // Add nullability disjuncts
//    checkState(!disjuncts.isEmpty());
//    if (domain.isNullAllowed()) {
//      disjuncts.add(columnName + " IS NULL");
//    }
    FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
    disjuncts.forEach(filter -> {
      filterList.addFilter(filter);
    });
    disjuncts.add(filterList);

    return filterList;
//    return "(" + Joiner.on(" OR ").join(disjuncts) + ")";
  }

  private SingleColumnValueFilter toPredicate(String columnName, String operator, Object value/*, Type type*/) {
    CompareOp scanOperator = getScanOperator(operator);
    SingleColumnValueFilter filter = new SingleColumnValueFilter(columnName, scanOperator, value);
    return filter;
  }

  private CompareOp getScanOperator(String operator) {
    switch (operator) {
      case "=":
        return CompareOp.EQUAL;
      case ">":
        return CompareOp.GREATER;
      case ">=":
        return CompareOp.GREATER_OR_EQUAL;
      case "<":
        return CompareOp.LESS;
      case "<=":
        return CompareOp.LESS_OR_EQUAL;
      case "!=":
        return CompareOp.NOT_EQUAL;
    }
    return null;
  }

  private HostAddress getAddress(ServerLocation v) {
    if (TEST_MODE) {
      return HostAddress.fromString("127.0.0.1");
    }
    return HostAddress.fromString(v.getHostName());
  }

}
