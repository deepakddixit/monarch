package io.ampool.presto.connector;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.presto.testing.MaterializedResult;
import com.facebook.presto.testing.MaterializedRow;
import com.facebook.presto.tests.DistributedQueryRunner;
import io.ampool.internal.AmpoolOpType;
import io.ampool.monarch.table.MTable;
import io.ampool.monarch.table.Put;
import io.ampool.monarch.table.TableDescriptor;
import io.ampool.monarch.table.client.MClientCache;
import io.ampool.monarch.table.ftable.FTable;
import io.ampool.monarch.table.ftable.Record;
import io.ampool.monarch.table.internal.MTableUtils;
import io.ampool.monarch.table.internal.Table;
import io.ampool.monarch.types.BasicTypes;
import io.ampool.monarch.types.TypeUtils;
import org.junit.Test;

import org.apache.geode.distributed.internal.ServerLocation;
import org.apache.geode.internal.cache.MonarchCacheImpl;
import org.apache.geode.test.dunit.IgnoredException;

/**
 * Created by deepak on 6/2/18.
 */
public class TestSelectQueries extends MonarchTestBase {

  final int NO_OF_COLS = 5;
  final int NO_OF_RECORDS = 20;


  String keySpace = "ampool";

  @Test
  public void testRowCount()
      throws Exception {
    IgnoredException.addIgnoredException("java.lang.NoSuchFieldError");

    String tableName = getTestMethodName().toLowerCase();
    createTableInAmpool(tableName, NO_OF_COLS);

    populateTable(tableName, NO_OF_RECORDS);

    MTable mTable = getmClientCache().getMTable(tableName);

    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
    MTableUtils.getLocationMap(mTable, null, primaryBucketMap, null, AmpoolOpType.ANY_OP);

    System.out.println();

    DistributedQueryRunner ampoolQueryRunner = getDistributedQueryRunner();

    MaterializedResult execute = ampoolQueryRunner.execute("select count(*) from " + tableName);

    assertEquals(1, execute.getRowCount());

    MaterializedRow materializedRow = execute.getMaterializedRows().get(0);
    Object field = materializedRow.getField(0);
    assertEquals(new Long(NO_OF_RECORDS), new Long((long) field));

    ampoolQueryRunner.close();
  }


  @Test
  public void testSelectStar()
      throws Exception {
    IgnoredException.addIgnoredException("java.lang.NoSuchFieldError");

    String tableName = getTestMethodName().toLowerCase();
    createTableInAmpool(tableName, NO_OF_COLS);

    populateTable(tableName, NO_OF_RECORDS);

    MTable mTable = getmClientCache().getMTable(tableName);

    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
    MTableUtils.getLocationMap(mTable, null, primaryBucketMap, null, AmpoolOpType.ANY_OP);

    System.out.println();

    DistributedQueryRunner ampoolQueryRunner = getDistributedQueryRunner();

    MaterializedResult execute = ampoolQueryRunner.execute("select * from " + tableName);
    List<MaterializedRow> materializedRows = execute.getMaterializedRows();
    assertEquals(NO_OF_RECORDS, materializedRows.size());
    ampoolQueryRunner.close();
  }

  @Test
  public void testSelectWhere()
      throws Exception {
    IgnoredException.addIgnoredException("java.lang.NoSuchFieldError");

    String tableName = getTestMethodName().toLowerCase();
    createTableInAmpool(tableName, NO_OF_COLS, BasicTypes.INT);

    populateTable(tableName, NO_OF_RECORDS);

    MTable mTable = getmClientCache().getMTable(tableName);

    for (int i = 1; i <= NO_OF_RECORDS; i++) {
      Put put = new Put(TypeUtils.getRandomBytes(8));
      put.addColumn(getColumnName(0), i);
      mTable.put(put);
    }

    Map<Integer, ServerLocation> primaryBucketMap = new HashMap<>(113);
    MTableUtils.getLocationMap(mTable, null, primaryBucketMap, null, AmpoolOpType.ANY_OP);

    DistributedQueryRunner ampoolQueryRunner = getDistributedQueryRunner();

    MaterializedResult
        execute =
        ampoolQueryRunner.execute(
            "select * from " + tableName + " where " + getColumnName(0) + " = 15 AND "+ getColumnName(1) + " = 10");
    List<MaterializedRow> materializedRows = execute.getMaterializedRows();
    assertEquals(1, materializedRows.size());
    ampoolQueryRunner.close();
  }


}
