package ai.dat.adapter.duckdb;

import dev.langchain4j.exception.UnsupportedFeatureException;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * @Author JunjieM
 * @Date 2025/9/16
 */
public class DuckDBDataSource implements DataSource {

    private final String dbUrl;

    public DuckDBDataSource(String filePath) {
        this.dbUrl = filePath != null ? "jdbc:duckdb:" + filePath : "jdbc:duckdb:";
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    @Override
    public Connection getConnection(String username, String password) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger("org.duckdb");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(this.getClass())) {
            return iface.cast(this);
        } else {
            throw new SQLException("Cannot unwrap to " + iface.getName());
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(this.getClass());
    }
}
