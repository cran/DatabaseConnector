package org.ohdsi.databaseConnector;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class BatchedInsert {
	public static int	INTEGER		= 0;
	public static int	NUMERIC		= 1;
	public static int	STRING		= 2;
	public static int	DATE		= 3;
	public static int	DATETIME	= 4;
	public static int	BIGINT		= 5;

	private Object[]	columns;
	private int[]		columnTypes;
	private Connection	connection;
	private int			columnCount;
	private int			rowCount;
	private String		sql;

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

	public void executeBatch() {
		for (int i = 0; i < columnCount; i++) {
			if (columns[i] == null)
				throw new RuntimeException("Column " + (i + 1) + " not set");
			if (columnTypes[i] == INTEGER) {
				if (((int[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i) + " data not of correct length");
			} else if (columnTypes[i] == NUMERIC) {
				if (((double[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i) + " data not of correct length");
			} else {
				if (((String[]) columns[i]).length != rowCount)
					throw new RuntimeException("Column " + (i) + " data not of correct length");
			}
		}
		try {
			trySettingAutoCommit(connection, false);
			PreparedStatement statement = connection.prepareStatement(sql);
			for (int i = 0; i < rowCount; i++) {
				for (int j = 0; j < columnCount; j++) {
					if (columnTypes[j] == INTEGER) {
						int value = ((int[]) columns[j])[i];
						if (value == Integer.MIN_VALUE)
							statement.setObject(j + 1, null);
						else
							statement.setInt(j + 1, value);
					} else if (columnTypes[j] == NUMERIC) {
						double value = ((double[]) columns[j])[i];
						if (Double.isNaN(value))
							statement.setObject(j + 1, null);
						else
							statement.setDouble(j + 1, value);
					} else if (columnTypes[j] == DATE) {
						String value = ((String[]) columns[j])[i];
						if (value == null)
							statement.setObject(j + 1, null);
						else
							statement.setDate(j + 1, java.sql.Date.valueOf(value));
					} else if (columnTypes[j] == DATETIME) {
						String value = ((String[]) columns[j])[i];
						if (value == null)
							statement.setObject(j + 1, null);
						else
							statement.setTimestamp(j + 1, java.sql.Timestamp.valueOf(value));
					} else if(columnTypes[j] == BIGINT) {
						long value = ((long[]) columns[j])[i];
						if (value == Long.MIN_VALUE)
							statement.setObject(j + 1, null);
						else
							statement.setLong(j + 1, value);
					} else {
						String value = ((String[]) columns[j])[i];
						if (value == null)
							statement.setObject(j + 1, null);
						else
							statement.setString(j + 1, value);
					}
				}
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
			statement.close();
			connection.clearWarnings();
			trySettingAutoCommit(connection, true);
		} catch (SQLException e) {
			e.printStackTrace();
			if (e instanceof BatchUpdateException) {
				System.err.println(((BatchUpdateException) e).getNextException().getMessage());
			}
		} finally {
			for (int i = 0; i < columnCount; i++) {
				columns[i] = null;
			}
			rowCount = 0;
		}
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

	public void setBigint(int columnIndex, long[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = BIGINT;
		rowCount = column.length;
	}

	public void setString(int columnIndex, String[] column) {
		columns[columnIndex - 1] = column;
		columnTypes[columnIndex - 1] = STRING;
		rowCount = column.length;
	}

	public void setInteger(int columnIndex, int column) {
		columns[columnIndex - 1] = new int[] { column };
		columnTypes[columnIndex - 1] = INTEGER;
		rowCount = 1;
	}

	public void setNumeric(int columnIndex, double column) {
		columns[columnIndex - 1] = new double[] { column };
		columnTypes[columnIndex - 1] = NUMERIC;
		rowCount = 1;
	}

	public void setDate(int columnIndex, String column) {
		columns[columnIndex - 1] = new String[] { column };
		columnTypes[columnIndex - 1] = DATE;
		rowCount = 1;
	}
	
	public void setDateTime(int columnIndex, String column) {
		columns[columnIndex - 1] = new String[] { column };
		columnTypes[columnIndex - 1] = DATETIME;
		rowCount = 1;
	}

	public void setString(int columnIndex, String column) {
		columns[columnIndex - 1] = new String[] { column };
		columnTypes[columnIndex - 1] = STRING;
		rowCount = 1;
	}

	public void setBigint(int columnIndex, long column) {
		columns[columnIndex - 1] = new long[] { column };
		columnTypes[columnIndex - 1] = BIGINT;
		rowCount = 1;
	}
}
