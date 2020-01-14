/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class AutoCompensateInsertHandler extends AutoCompensateHandler {

    private static volatile AutoCompensateInsertHandler autoCompensateInsertHandler = null;

    public static AutoCompensateInsertHandler newInstance() {
        if (autoCompensateInsertHandler == null) {
            synchronized (AutoCompensateInsertHandler.class) {
                if (autoCompensateInsertHandler == null) {
                    autoCompensateInsertHandler = new AutoCompensateInsertHandler();
                }
            }
        }
        return autoCompensateInsertHandler;
    }

    public boolean prepareCompensationAfterInserting(PreparedStatement delegate, SQLStatement sqlStatement, String executeSql, String globalTxId, String localTxId, String server, Map<String, Object> standbyParams) throws SQLException {

        if (JdbcConstants.MYSQL.equals(sqlStatement.getDbType())) {
            return MySqlInsertHandler.newInstance().prepareCompensationAfterInserting(delegate, sqlStatement, executeSql, globalTxId, localTxId, server, standbyParams);
        }

        return false;
    }

}
