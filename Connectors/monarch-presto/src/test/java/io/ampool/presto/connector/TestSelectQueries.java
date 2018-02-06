package io.ampool.presto.connector;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.facebook.presto.spi.connector.Connector;
import com.facebook.presto.spi.connector.ConnectorMetadata;
import com.facebook.presto.testing.MaterializedResult;
import com.facebook.presto.testing.TestingConnectorContext;
import com.facebook.presto.tests.DistributedQueryRunner;
import com.google.common.collect.ImmutableMap;
import io.ampool.internal.AmpoolOpType;
import io.ampool.monarch.table.MTable;
import io.ampool.monarch.table.Put;
import io.ampool.monarch.table.TableDescriptor;
import io.ampool.monarch.table.client.MClientCache;
import io.ampool.monarch.table.ftable.FTable;
import io.ampool.monarch.table.ftable.Record;
import io.ampool.monarch.table.internal.MTableUtils;
import io.ampool.monarch.table.internal.Table;
import io.ampool.monarch.types.TypeUtils;
import org.junit.Test;

import org.apache.geode.distributed.internal.ServerLocation;
import org.apache.geode.internal.cache.MonarchCacheImpl;
import org.apache.geode.test.dunit.IgnoredException;

/**
 * Created by deepak on 6/2/18.
 */
public class TestSelectQueries extends MonarchTestBase {

  static {
    IgnoredException.addIgnoredException("java.lang.NoSuchFieldError");
  }

  final int NO_OF_COLS = 5;
  final int NO_OF_RECORDS = 20;

  String keySpace = "ampool";

  @Test
  public void testRowCount()
      throws Exception {

    AmpoolConnectorFactory connectorFactory = new AmpoolConnectorFactory();
    String connectorId = "test-connector";
    Connector connector = connectorFactory.create(connectorId, ImmutableMap.of(
        MonarchProperties.LOCATOR_HOST, getLocatorHost(),
        MonarchProperties.LOCATOR_PORT, String.valueOf(getLocatorPort())),
        new TestingConnectorContext());

    ConnectorMetadata metadata = connector.getMetadata(AmpoolTransactionHandle.INSTANCE);

    String tableName = getTestMethodName().toLowerCase();
    createTableInAmpool(tableName, NO_OF_COLS);

    populateTable(tableName, NO_OF_RECORDS);

    MTable mTable = getmClientCache().getMTable(tableName);

    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
    MTableUtils.getLocationMap(mTable, null, primaryBucketMap, null, AmpoolOpType.ANY_OP);

    System.out.println();

    DistributedQueryRunner
        ampoolQueryRunner =
        MonarchPrestoQueryRunner
            .createAmpoolQueryRunner(getLocatorHost(), getLocatorPort(), Collections.emptyMap(),5);

    MaterializedResult execute = ampoolQueryRunner.execute("select * from " + tableName);

    assertEquals(NO_OF_RECORDS, execute.getRowCount());
  }

  private void populateTable(String tableName, int rows) {
    MClientCache mClientCache = getmClientCache();
    Table anyTable = ((MonarchCacheImpl) mClientCache).getAnyTable(tableName);
    TableDescriptor tableDescriptor = anyTable.getTableDescriptor();
    if (anyTable instanceof FTable) {
      for (int i = 0; i < rows; i++) {
        Record record = new Record();
        tableDescriptor.getColumnDescriptors().forEach(cd -> {
          Object randomValue = TypeUtils.getRandomValue(cd.getColumnType());
          record.add(cd.getColumnNameAsString(), randomValue);
        });
        ((FTable) anyTable).append(record);
      }

    } else if (anyTable instanceof MTable) {
      Put record = new Put(TypeUtils.getRandomBytes(8));
      tableDescriptor.getColumnDescriptors().forEach(cd -> {
        Object randomValue = TypeUtils.getRandomValue(cd.getColumnType());
        record.addColumn(cd.getColumnNameAsString(), randomValue);
      });
      ((MTable) anyTable).put(record);

      for (int i = 1; i < rows; i++) {
        record.setRowKey(TypeUtils.getRandomBytes(8));
        ((MTable) anyTable).put(record);
      }
    }
  }
}
