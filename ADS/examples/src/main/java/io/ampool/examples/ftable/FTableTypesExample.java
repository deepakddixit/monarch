package io.ampool.examples.ftable;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import io.ampool.client.AmpoolClient;
import io.ampool.conf.Constants;
import io.ampool.monarch.table.Admin;
import io.ampool.monarch.table.Bytes;
import io.ampool.monarch.table.Cell;
import io.ampool.monarch.table.Row;
import io.ampool.monarch.table.Scan;
import io.ampool.monarch.table.Scanner;
import io.ampool.monarch.table.Schema;
import io.ampool.monarch.table.ftable.FTable;
import io.ampool.monarch.table.ftable.FTableDescriptor;
import io.ampool.monarch.table.ftable.Record;
import io.ampool.monarch.types.BasicTypes;

//
// Example for FTable APIs
public class FTableTypesExample {
  private static final String tableName = "FTableTypesExample";
  private static final String[] columnNames = {"NAME", "ID", "AGE", "SALARY", "DEPT", "DOJ"};
  private static AmpoolClient client;
  private static final int numRows = 20;

  // connect to the ampool cluster using ampool locator host and port
  private static void connect(final String locator_host, final int locator_port) {
    Properties props = new Properties();
    props.setProperty(Constants.MClientCacheconfig.MONARCH_CLIENT_LOG, "/tmp/FTableExample.log");
    client = new AmpoolClient(locator_host, locator_port, props);
    System.out.println("Connection to monarch distributed system is successfully done!");
  }

  // create ftable
  private static void createTable() {
    final Admin admin = client.getAdmin();
    FTableDescriptor tableDescriptor = new FTableDescriptor();
    final Schema schema = new Schema.Builder().column(columnNames[0], BasicTypes.CHARS)
        .column(columnNames[1], BasicTypes.CHARS).column(columnNames[2], BasicTypes.INT)
        .column(columnNames[3], BasicTypes.INT).column(columnNames[4], BasicTypes.CHARS)
        .column(columnNames[5], BasicTypes.DATE).build();
    tableDescriptor.setSchema(schema);

    // set the partitioning column
    tableDescriptor.setPartitioningColumn(columnNames[1]);

    // delete the table if exists
    if (admin.existsFTable(tableName)) {
      admin.deleteFTable(tableName);
    }

    // create table
    admin.createFTable(tableName, tableDescriptor);
  }

  private static void appendRecords() {
    final FTable fTable = client.getFTable(tableName);

    // ingest records using batch append
    Record[] records = new Record[10];
    for (int i = 0; i < 10; i++) {
      Record record = new Record();
      record.add(columnNames[0], "NAME" + i);
      record.add(columnNames[1], "ID" + i);
      record.add(columnNames[2], 10 + i);
      record.add(columnNames[3], 10000 * (i));
      record.add(columnNames[4], "DEPT");
      record.add(columnNames[5], new Date(System.currentTimeMillis()));
      records[i] = record;
    }
    fTable.append(records);

    // ingest records using append
    for (int i = 10; i < 20; i++) {
      Record record = new Record();
      record.add(columnNames[0], "NAME" + i);
      record.add(columnNames[1], "ID" + i);
      record.add(columnNames[2], 10 + i);
      record.add(columnNames[3], 10000 * i);
      record.add(columnNames[4], "DEPT");
      record.add(columnNames[5], new Date(System.currentTimeMillis()));
      fTable.append(record);
    }

  }

  private static void scanRecords() {
    final FTable fTable = client.getFTable(tableName);
    final Scanner scanner = fTable.getScanner(new Scan());
    final Iterator<Row> iterator = scanner.iterator();

    int recordCount = 0;
    while (iterator.hasNext()) {
      recordCount++;
      final Row result = iterator.next();
      System.out.println("============= Record " + recordCount + " =============");
      // read the columns
      final List<Cell> cells = result.getCells();
      // NAME
      System.out.println(
          Bytes.toString(cells.get(0).getColumnName()) + " : " + cells.get(0).getColumnValue());

      // ID
      System.out.println(
          Bytes.toString(cells.get(1).getColumnName()) + " : " + cells.get(1).getColumnValue());

      // AGE
      System.out.println(
          Bytes.toString(cells.get(2).getColumnName()) + " : " + cells.get(2).getColumnValue());

      // SALARY
      System.out.println(
          Bytes.toString(cells.get(3).getColumnName()) + " : " + cells.get(3).getColumnValue());

      // DEPT
      System.out.println(
          Bytes.toString(cells.get(4).getColumnName()) + " : " + cells.get(4).getColumnValue());

      // DOJ
      System.out.println(
          Bytes.toString(cells.get(5).getColumnName()) + " : " + cells.get(5).getColumnValue());
      System.out.println();
    }
    System.out.println("Successfully scanned " + recordCount + "records.");
  }

  // create ftable
  private static void deleteTable() {
    final Admin admin = client.getAdmin();
    if (admin.existsFTable(tableName)) {
      admin.deleteFTable(tableName);
    }
  }

  // disconnect the ampool locator
  private static void disconnect() {
    client.close();
  }

  public static void main(String[] args) {
    System.out.println("Running FTable example!");

    // get the locator host and port.
    // default is localhost and 10334
    String locator_host = "localhost";
    int locator_port = 10334;
    if (args.length == 2) {
      locator_host = args[0];
      locator_port = Integer.parseInt(args[1]);
    }

    // connect to the ampool cluster
    connect(locator_host, locator_port);

    // create table
    createTable();

    // ingest records
    appendRecords();

    // scan the ingested records
    scanRecords();

    // delete table
    deleteTable();

    // disconnect from ampool cluster
    disconnect();
  }


}
