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

package io.ampool.monarch.table.ftable.exceptions;

import io.ampool.classification.InterfaceAudience;
import io.ampool.classification.InterfaceStability;
import io.ampool.monarch.table.exceptions.MException;

/**
 * A Exception indicates that non registered store is specified for request.
 * 
 * @since 1.2.3.0
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class TierStoreNotAvailableException extends MException {


  private static final long serialVersionUID = 2959545841617976099L;

  public TierStoreNotAvailableException(String s) {
    super(s);
  }
}