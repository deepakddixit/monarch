/*
 * Copyright (c) 2017 Ampool, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. See accompanying LICENSE file.
 */
package io.ampool.presto.connector;

import io.ampool.monarch.table.MTableDUnitHelper;
import io.ampool.monarch.table.MTableDescriptor;

import org.apache.geode.test.dunit.standalone.DUnitLauncher;

/**
 * Base test class to be inherited by all TestNG tests so that DUnit
 * VM creation can be done only once and reused for all tests by
 * having individual tests create unique region (based on test name)
 * and all tests can be run parallel to boost the test case execution
 * time.
 */
public class MonarchTestBase extends MTableDUnitHelper {


  @Override
  public void postSetUp() throws Exception {
    super.postSetUp();
    startServerOn(this.vm0, DUnitLauncher.getLocatorString());
    startServerOn(this.vm1, DUnitLauncher.getLocatorString());
    startServerOn(this.vm2, DUnitLauncher.getLocatorString());
    createClientCache(vm3);
    createClientCache();
  }


  @Override
  public void tearDown2() throws Exception {
    closeMClientCache();
    closeMClientCache(vm3);
    stopServerOn(vm0);
    stopServerOn(vm1);
    stopServerOn(vm2);
    super.tearDown2();
  }

  public String getLocatorHost() {
    String locatorString = DUnitLauncher.getLocatorString();
    return locatorString.substring(0, locatorString.indexOf('['));
  }

  public int getLocatorPort() {
    return Integer.parseInt(DUnitLauncher.getLocatorPortString());
  }

  public void createTableInAmpool(String tableName, int numberOfCols) {
    MTableDescriptor mTableDescriptor = new MTableDescriptor();
    for (int i = 0; i < numberOfCols; i++) {
      mTableDescriptor.addColumn(getColumnName(i));
    }
    getmClientCache().getAdmin().createMTable(tableName,mTableDescriptor);
  }

  public String getColumnName(int suffix){
    return "COL_"+suffix;
  }
}