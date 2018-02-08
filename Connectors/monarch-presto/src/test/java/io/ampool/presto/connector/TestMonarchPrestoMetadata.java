/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ampool.presto.connector;

import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorTableHandle;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.connector.Connector;
import com.facebook.presto.spi.connector.ConnectorMetadata;
import com.facebook.presto.testing.TestingConnectorContext;
import com.google.common.collect.ImmutableMap;
import org.apache.geode.test.dunit.IgnoredException;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.facebook.presto.testing.TestingConnectorSession.SESSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestMonarchPrestoMetadata extends MonarchTestBase {

  final int NO_OF_COLS = 5;

  String keySpace = "ampool";

  @Test
  public void testListTable()
      throws Exception {
    IgnoredException.addIgnoredException("java.lang.NoSuchFieldError");

    AmpoolConnectorFactory connectorFactory = new AmpoolConnectorFactory();
    String connectorId = "test-connector";
    Connector connector = connectorFactory.create(connectorId, ImmutableMap.of(
        MonarchProperties.LOCATOR_HOST, getLocatorHost(),
        MonarchProperties.LOCATOR_PORT, String.valueOf(getLocatorPort())),
        new TestingConnectorContext());

    ConnectorMetadata metadata = connector.getMetadata(AmpoolTransactionHandle.INSTANCE);

    String tableName = getTestMethodName().toLowerCase();
    createTableInAmpool(tableName, NO_OF_COLS);
    List<SchemaTableName>
        schemaTableNames =
        metadata.listTables(SESSION, keySpace);

    assertEquals(1, schemaTableNames.size());
    SchemaTableName schemaTableName = schemaTableNames.get(0);
    assertTrue(tableName.equalsIgnoreCase(schemaTableName.getTableName()));
  }

  @Test
  public void testListColumnsTable()
      throws Exception {

    IgnoredException.addIgnoredException("java.lang.NoSuchFieldError");


    AmpoolConnectorFactory connectorFactory = new AmpoolConnectorFactory();
    String connectorId = "test-connector";
    Connector connector = connectorFactory.create(connectorId, ImmutableMap.of(
        MonarchProperties.LOCATOR_HOST, getLocatorHost(),
        MonarchProperties.LOCATOR_PORT, String.valueOf(getLocatorPort())),
        new TestingConnectorContext());

    ConnectorMetadata metadata = connector.getMetadata(AmpoolTransactionHandle.INSTANCE);

    String tableName = getTestMethodName().toLowerCase();
    createTableInAmpool(tableName, NO_OF_COLS);
    List<SchemaTableName>
        schemaTableNames =
        metadata.listTables(SESSION, keySpace);

    SchemaTableName schemaTableName = schemaTableNames.get(0);
    ConnectorTableHandle
        tableHandle =
        metadata.getTableHandle(SESSION, schemaTableName);
    Map<String, ColumnHandle> columnHandles = metadata.getColumnHandles(SESSION, tableHandle);
    assertEquals(NO_OF_COLS + 1, columnHandles.size());
    Iterator<Map.Entry<String, ColumnHandle>> iterator = columnHandles.entrySet().iterator();
    for (int i = 0; i < NO_OF_COLS; i++) {
      assertEquals(getColumnName(i).toLowerCase(), iterator.next().getKey().toLowerCase());
    }
  }


}
