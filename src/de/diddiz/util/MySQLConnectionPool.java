package de.diddiz.util;

import java.io.Closeable;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MySQLConnectionPool implements Closeable
{
	private final static int poolsize = 10;
	private final static long timeToLive = 300000;
	private final Vector<JDCConnection> connections;
	private final ConnectionReaper reaper;
	private final String url, user, password;
	private final Lock lock = new ReentrantLock();

	public MySQLConnectionPool(String url, String user, String password) throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		this.url = url;
		this.user = user;
		this.password = password;
		connections = new Vector<JDCConnection>(poolsize);
		reaper = new ConnectionReaper();
		reaper.start();
	}

	@Override
	public void close() {
		lock.lock();
		final Enumeration<JDCConnection> conns = connections.elements();
		while (conns.hasMoreElements()) {
			final JDCConnection conn = conns.nextElement();
			connections.remove(conn);
			conn.terminate();
		}
		lock.unlock();
	}

	public Connection getConnection() throws SQLException {
		lock.lock();
		try {
			final Enumeration<JDCConnection> conns = connections.elements();
			while (conns.hasMoreElements()) {
				final JDCConnection conn = conns.nextElement();
				if (conn.lease()) {
					if (conn.isValid())
						return conn;
					connections.remove(conn);
					conn.terminate();
				}
			}
			final JDCConnection conn = new JDCConnection(DriverManager.getConnection(url, user, password));
			conn.lease();
			if (!conn.isValid()) {
				conn.terminate();
				throw new SQLException("Failed to validate a brand new connection");
			}
			connections.add(conn);
			return conn;
		} finally {
			lock.unlock();
		}
	}

	private void reapConnections() {
		lock.lock();
		final long stale = System.currentTimeMillis() - timeToLive;
		for (final JDCConnection conn : connections)
			if (conn.inUse() && stale > conn.getLastUse() && !conn.isValid())
				connections.remove(conn);
		lock.unlock();
	}

	private class ConnectionReaper extends Thread
	{
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(300000);
				} catch (final InterruptedException e) {}
				reapConnections();
			}
		}
	}

	private class JDCConnection implements Connection
	{
		private final Connection conn;
		private boolean inuse;
		private long timestamp;
		private int networkTimeout;
		private String schema;

		JDCConnection(Connection conn) {
			this.conn = conn;
			inuse = false;
			timestamp = 0;
			networkTimeout = 30;
			schema = "default";
		}

		@Override
		public void clearWarnings() throws SQLException {
			conn.clearWarnings();
		}

		@Override
		public void close() {
			inuse = false;
			try {
				if (!conn.getAutoCommit())
					conn.setAutoCommit(true);
			} catch (final SQLException ex) {
				connections.remove(conn);
				terminate();
			}
		}

		@Override
		public void commit() throws SQLException {
			conn.commit();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return conn.createArrayOf(typeName, elements);
		}

		@Override
		public Blob createBlob() throws SQLException {
			return conn.createBlob();
		}

		@Override
		public Clob createClob() throws SQLException {
			return conn.createClob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			return conn.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return conn.createSQLXML();
		}

		@Override
		public Statement createStatement() throws SQLException {
			return conn.createStatement();
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return conn.createStruct(typeName, attributes);
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return conn.getAutoCommit();
		}

		@Override
		public String getCatalog() throws SQLException {
			return conn.getCatalog();
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return conn.getClientInfo();
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return conn.getClientInfo(name);
		}

		@Override
		public int getHoldability() throws SQLException {
			return conn.getHoldability();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return conn.getMetaData();
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return conn.getTransactionIsolation();
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return conn.getTypeMap();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return conn.getWarnings();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return conn.isClosed();
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return conn.isReadOnly();
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return conn.isValid(timeout);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return conn.isWrapperFor(iface);
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return conn.nativeSQL(sql);
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return conn.prepareCall(sql);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return conn.prepareStatement(sql);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return conn.prepareStatement(sql, autoGeneratedKeys);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return conn.prepareStatement(sql, columnIndexes);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return conn.prepareStatement(sql, columnNames);
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			conn.releaseSavepoint(savepoint);
		}

		@Override
		public void rollback() throws SQLException {
			conn.rollback();
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
			conn.rollback(savepoint);
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			conn.setAutoCommit(autoCommit);
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			conn.setCatalog(catalog);
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			conn.setClientInfo(properties);
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			conn.setClientInfo(name, value);
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			conn.setHoldability(holdability);
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			conn.setReadOnly(readOnly);
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return conn.setSavepoint();
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return conn.setSavepoint(name);
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			conn.setTransactionIsolation(level);
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			conn.setTypeMap(map);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return conn.unwrap(iface);
		}
		
		public int getNetworkTimeout() throws SQLException {
			return networkTimeout;
		}
		
		public void setNetworkTimeout(Executor exec, int timeout) throws SQLException {
			networkTimeout = timeout;
		}
		
		public void abort(Executor exec) throws SQLException {
			//Not implemented really...
		}
		
		public String getSchema() throws SQLException {
			return schema;
		}
		
		public void setSchema(String str) throws SQLException {
			schema = str;
		}

		long getLastUse() {
			return timestamp;
		}

		boolean inUse() {
			return inuse;
		}

		boolean isValid() {
			try {
				return conn.isValid(1);
			} catch (final SQLException ex) {
				return false;
			}
		}

		synchronized boolean lease() {
			if (inuse)
				return false;
			inuse = true;
			timestamp = System.currentTimeMillis();
			return true;
		}

		void terminate() {
			try {
				conn.close();
			} catch (final SQLException ex) {}
		}
	}
}
