package org.cockshott.commandshop.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class DatabaseUtils {

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }

    /**
     * 执行数据库事务操作
     * @param dataSource 数据源
     * @param operation 数据库操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws SQLException 如果发生SQL错误
     */
    public static <T> T executeTransaction(DataSource dataSource, DatabaseOperation<T> operation) throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            T result = operation.execute(connection);

            connection.commit();
            return result;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 执行数据库查询操作（不需要事务）
     * @param dataSource 数据源
     * @param operation 数据库操作
     * @param <T> 返回值类型
     * @return 查询结果
     * @throws SQLException 如果发生SQL错误
     */
    public static <T> T executeQuery(DataSource dataSource, DatabaseOperation<T> operation) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return operation.execute(connection);
        }
    }
}
