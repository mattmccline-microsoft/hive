/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hive.service.cli;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.hive.metastore.ExceptUtils;
import org.apache.hive.service.rpc.thrift.TStatus;
import org.apache.hive.service.rpc.thrift.TStatusCode;

import com.google.common.annotations.VisibleForTesting;

/**
 * An exception that provides information on a Hive access error or other
 * errors.
 */
public class HiveSQLException extends SQLException {

  private static final long serialVersionUID = -6095254671958748095L;

  @VisibleForTesting
  public static final List<String> DEFAULT_INFO =
      Collections.singletonList("Server-side error; please check HS2 logs.");

  /**
   * Constructor.
   */
  public HiveSQLException() {
    super();
  }

  /**
   * Constructs a SQLException object with a given reason. The SQLState is
   * initialized to null and the vendor code is initialized to 0. The cause is
   * not initialized, and may subsequently be initialized by a call to the
   * Throwable.initCause(java.lang.Throwable) method.
   *
   * @param reason a description of the exception
   */
  public HiveSQLException(String reason) {
    super(reason);
  }

  /**
   * Constructs a SQLException object with a given cause. The SQLState is
   * initialized to null and the vendor code is initialized to 0. The reason is
   * initialized to null if cause==null or to cause.toString() if cause!=null.
   *
   * @param cause the underlying reason for this SQLException - may be null
   *          indicating the cause is non-existent or unknown
   */
  public HiveSQLException(Throwable cause) {
    super(cause);
  }

  /**
   * @param reason
   * @param sqlState
   */
  public HiveSQLException(String reason, String sqlState) {
    super(reason, sqlState);
  }

  /**
   * @param reason
   * @param cause
   */
  public HiveSQLException(String reason, Throwable cause) {
    super(reason, cause);
  }

  /**
   * @param reason
   * @param sqlState
   * @param vendorCode
   */
  public HiveSQLException(String reason, String sqlState, int vendorCode) {
    super(reason, sqlState, vendorCode);
  }

  /**
   * @param reason
   * @param sqlState
   * @param cause
   */
  public HiveSQLException(String reason, String sqlState, Throwable cause) {
    super(reason, sqlState, cause);
  }

  /**
   * @param reason
   * @param sqlState
   * @param vendorCode
   * @param cause
   */
  public HiveSQLException(String reason, String sqlState, int vendorCode, Throwable cause) {
    super(reason, sqlState, vendorCode, cause);
  }

  public HiveSQLException(TStatus status) {
    super(status.getErrorMessage(), status.getSqlState(), status.getErrorCode());
  }


  /**
   * Wrap an Exception caught by ThriftCLIService operation method such as GetTables, etc.
   *
   * We even wrap a HiveSQLException with itself because we want to show where in the code an
   * Exception was caught, stuffed into the Thrift Response, and not rethrown.
   * Otherwise, a stack trace can be difficult to interpret if it isn't clear how far it went up
   * the call chain it went. Was it an uncaught Exception that killed the thread?
   *
   * @param operationName the name of the request. E.g. GetInfo.
   * @param cause         the Exception that was caught and failed the request.
   *
   * @return a {@link HiveSQLException} object
   */
  public static HiveSQLException wrapForResponse(String operationName, Exception cause) {
    Throwable rootCause = cause;
    while (true) {
      Throwable nextCause = rootCause.getCause();
      if (nextCause == null) {
        break;
      }
      rootCause = nextCause;
    }
    String rootMsg = rootCause.getMessage();

    String msg = operationName + " error: " + rootCause.getClass().getName() + (rootMsg.isEmpty() ? "" : " " + rootMsg);
    HiveSQLException hse = new HiveSQLException(msg, cause);
    // Get rid of the call to wrapForResponse.
    ExceptUtils.removeFirstStackTraceEle(hse);
    return hse;
  }

  /**
   * Converts current object to a {@link TStatus} object.
   *
   * @return a {@link TStatus} object
   */
  public TStatus toTStatus() {
    // TODO: convert sqlState, etc.
    TStatus tStatus = new TStatus(TStatusCode.ERROR_STATUS);
    tStatus.setSqlState(getSQLState());
    tStatus.setErrorCode(getErrorCode());
    tStatus.setErrorMessage(getMessage());
    tStatus.setInfoMessages(DEFAULT_INFO);
    return tStatus;
  }

  /**
   * Converts the specified {@link Exception} object into a {@link TStatus}
   * object.
   *
   * @param e a {@link Exception} object
   * @return a {@link TStatus} object
   */
  public static TStatus toTStatus(Exception e) {
    if (e instanceof HiveSQLException) {
      return ((HiveSQLException) e).toTStatus();
    }
    TStatus tStatus = new TStatus(TStatusCode.ERROR_STATUS);
    tStatus.setErrorMessage(e.getMessage());
    tStatus.setInfoMessages(DEFAULT_INFO);
    return tStatus;
  }

}
