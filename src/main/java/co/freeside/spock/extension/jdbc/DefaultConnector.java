package co.freeside.spock.extension.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import co.freeside.spock.TypedConnector;
import co.freeside.spock.Connector;
import groovy.sql.Sql;
import static java.lang.String.format;

public class DefaultConnector implements Connector {
  @Override public Connection connectionFrom(Object source) {
    if (DataSource.class.isAssignableFrom(source.getClass())) {
      return DATA_SOURCE_CONNECTOR.connectionFrom(source);
    } else if (Sql.class.isAssignableFrom(source.getClass())) {
      return GROOVY_SQL_CONNECTOR.connectionFrom(source);
    } else if (Connection.class.isAssignableFrom(source.getClass())) {
      return CONNECTION_CONNECTOR.connectionFrom(source);
    }
    throw new IllegalArgumentException(
      format(
        "Unable to obtain a database connection from an instance of %s",
        source.getClass()
      )
    );
  }

  private static final TypedConnector<Connection> CONNECTION_CONNECTOR =
    new TypedConnector<Connection>(Connection.class) {
      @Override protected Connection apply(Connection source) {
        return source;
      }
    };

  private static final TypedConnector<DataSource> DATA_SOURCE_CONNECTOR =
    new TypedConnector<DataSource>(DataSource.class) {
      @Override
      protected Connection apply(DataSource source) throws SQLException {
        return source.getConnection();
      }
    };

  private static final TypedConnector<Sql> GROOVY_SQL_CONNECTOR =
    new TypedConnector<Sql>(Sql.class) {
      @Override protected Connection apply(Sql source) throws SQLException {
        if (source.getConnection() != null) {
          return CONNECTION_CONNECTOR.connectionFrom(source.getConnection());
        } else {
          return DATA_SOURCE_CONNECTOR.connectionFrom(source.getDataSource());
        }
      }
    };
}
