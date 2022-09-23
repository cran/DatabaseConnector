package org.ohdsi.databaseConnector;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;

public class BatchedInsert {
	public static int		INTEGER						= 0;
	public static int		NUMERIC						= 1;
	public static int		STRING						= 2;
	public static int		DATE						= 3;
	public static int		DATETIME					= 4;
	public static int		BIGINT						= 5;
	
	public static final int	BIG_DATA_BATCH_INSERT_LIMIT	= 1000;
	
	private Object[]		columns;
	private int[]			columnTypes;
	private Connection		connection;
	private int				columnCount;
	private int				rowCount;
	private String			sql;
	
	public BatchedInsert(Connection connection, String sql, int columnCount) throws SQLException {
		this.connection = connection;
		this.sql = sql;
		this.columnCount = columnCount;
		columns = new Object[columnCount];
		columnTypes = new int[columnCount];
	}
	
	private void trySettingAutoCommit(Connection connection, boolean value) throws SQLException {
		try {
			connection.setAutoCommit(value);
		} catch (SQLFeatureNotSupportedException exception) {
			// Do nothing
		}
	}
	
	private void checkColumns() {
		for (int i = 0; i < columnCount; i++) {
			if (columns[i] == null)
				throw new RuntimeException("Column " + (i + 1) + " not set");
			if (columnTypes[i] == INTEGER) {
				if (((int[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i + 1) + " data not of correct length");
			} else if (columnTypes[i] == NUMERIC) {
				if (((double[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i + 1) + " data not of correct length");
			} else if (columnTypes[i] == BIGINT) {
				if (((long[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i + 1) + " data not of correct length");
			} else {
				if (((String[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i + 1) + " data not of correct length");
			}
		}
	}
	
	private void setValue(PreparedStatement statement, int statementIndex, int rowIndex, int columnIndex) throws SQLException {
		if (columnTypes[columnIndex] == INTEGER) {
			int value = ((int[]) columns[columnIndex])[rowIndex];
			if (value == Integer.MIN_VALUE)
				statement.setObject(statementIndex, null);
			else
				statement.setInt(statementIndex, value);
		} else if (columnTypes[columnIndex] == NUMERIC) {
			double value = ((double[]) columns[columnIndex])[rowIndex];
			if (Double.isNaN(value))
				statement.setObject(statementIndex, null);
			else
				statement.setDouble(statementIndex, value);
		} else if (columnTypes[columnIndex] == DATE) {
			String value = ((String[]) columns[columnIndex])[rowIndex];
			if (value == null)
				statement.setObject(statementIndex, null);
			else
				statement.setDate(statementIndex, java.sql.Date.valueOf(value));
		} else if (columnTypes[columnIndex] == DATETIME) {
			String value = ((String[]) columns[columnIndex])[rowIndex];
			if (value == null)
				statement.setObject(statementIndex, null);
			else
				statement.setTimestamp(statementIndex, java.sql.Timestamp.valueOf(value));
		} else if (columnTypes[columnIndex] == BIGINT) {
			long value = ((long[]) columns[columnIndex])[rowIndex];
			if (value == Long.MIN_VALUE)
				statement.setObject(statementIndex, null);
			else
				statement.setLong(statementIndex, value);
		} else {
			String value = ((String[]) columns[columnIndex])[rowIndex];
			if (value == null)
				statement.setObject(statementIndex, null);
			else
				statement.setString(statementIndex, value);
		}
	}
	
	public boolean executeBatch() throws SQLException {
		checkColumns();
		try {
			trySettingAutoCommit(connection, false);
			PreparedStatement statement = connection.prepareStatement(sql);
			for (int i = 0; i < rowCount; i++) {
				for (int j = 0; j < columnCount; j++)
					setValue(statement, j + 1, i, j);
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
			statement.close();
			connection.clearWarnings();
			trySettingAutoCommit(connection, true);
		} finally {
			for (int i = 0; i < columnCount; i++) {
				columns[i] = null;
			}
			rowCount = 0;
		}
		return true;
	}
	
	/**
	 * Not all drivers support batch operations, for example GoogleBigQueryJDBC42.jar. In order to save data most efficiently, we implement saving through an
	 * insert with multiple values.
	 * @throws SQLException 
	 */
	public boolean executeBigQueryBatch() throws SQLException {
		checkColumns();
		try {
			trySettingAutoCommit(connection, false);
			
			int offset = 0;
			while (offset < rowCount) {
				
				int size = Math.min(rowCount - offset, BIG_DATA_BATCH_INSERT_LIMIT);
				// create insert query by adding multiply values like insert values (?,?),(?,?),(?,?)
				String params = String.join(",", Collections.nCopies(columnCount, "?"));
				String sqlWithValues = sql + "," + String.join(",", Collections.nCopies(size - 1, String.format("(%s)", params)));
				PreparedStatement statement = connection.prepareStatement(sqlWithValues);
				
				for (int i = 0; i < size; i++) {
					for (int j = 0; j < columnCount; j++) {
						int statementIndex = columnCount * i + j + 1;
						int rowIndex = offset + i;
						setValue(statement, statementIndex, rowIndex, j);
					}
				}
				statement.executeUpdate();
				statement.close();
				connection.clearWarnings();
				trySettingAutoCommit(connection, true);
				offset += BIG_DATA_BATCH_INSERT_LIMIT;
			}
		} finally {
			for (int i = 0; i < columnCount; i++) {
				columns[i] = null;
			}
			rowCount = 0;
		}
		return true;
	}
	
	private static long[] convertFromInteger64ToLong(double[] value) {
		long[] result = new long[value.length];
		ByteBuffer byteBuffer = ByteBuffer.allocate(8 * value.length);
		for (int i = 0; i < value.length; i++)
			byteBuffer.putDouble(value[i]);
		byteBuffer.flip();
		for (int i = 0; i < value.length; i++)
			result[i] = byteBuffer.getLong();
		return result;
	}
	
	public static boolean validateInteger64(double[] value) {
		long[] integers = convertFromInteger64ToLong(value);
		boolean result = (integers[0] == 1 & integers[1] == -1 & integers[2] == (long) Math.pow(2, 33) & integers[3] == (long) Math.pow(-2, 33));
		return result;
	}
	
	public void setInteger(int columnIndex, int[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = INTEGER;
		rowCount = column.length;
	}
	
	public void setNumeric(int columnIndex, double[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = NUMERIC;
		rowCount = column.length;
	}
	
	public void setDate(int columnIndex, String[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = DATE;
		rowCount = column.length;
	}
	
	public void setDateTime(int columnIndex, String[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = DATETIME;
		rowCount = column.length;
	}
	
	public void setBigint(int columnIndex, double[] column) {
		columns[columnIndex - 1] = convertFromInteger64ToLong(column);
		columnTypes[columnIndex - 1] = BIGINT;
		rowCount = column.length;
	}
	
	public void setString(int columnIndex, String[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = STRING;
		rowCount = column.length;
	}
	
	public void setInteger(int columnIndex, int column) {
		setInteger(columnIndex, new int[] { column });
	}
	
	public void setNumeric(int columnIndex, double column) {
		setNumeric(columnIndex, new double[] { column });
	}
	
	public void setDate(int columnIndex, String column) {
		setDate(columnIndex, new String[] { column });
	}
	
	public void setDateTime(int columnIndex, String column) {
		setDateTime(columnIndex, new String[] { column });
	}
	
	public void setString(int columnIndex, String column) {
		setString(columnIndex, new String[] { column });
	}
	
	public void setBigint(int columnIndex, double column) {
		setBigint(columnIndex, new double[] { column });
	}
}
