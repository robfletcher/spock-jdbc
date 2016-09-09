package co.freeside.spock.extension.jdbc;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import org.spockframework.util.WrappedException;
import static java.lang.String.format;

public class TableTruncator {

  private final Connection connection;
  private final boolean verbose;
  private final Collection<Table> remaining = new HashSet<>();

  public static void truncateTables(Connection connection, boolean verbose) {
    new TableTruncator(connection, verbose).truncateTables();
  }

  private TableTruncator(Connection connection, boolean verbose) {
    this.connection = connection;
    this.verbose = verbose;
  }

  public void truncateTables() {
    findTables();
    while (!remaining.isEmpty()) {
      deleteFrom(remaining.iterator().next());
    }
  }

  private void findTables() {
    try {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet rs = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
        while (rs.next()) {
          remaining.add(Table.fromTableMetadata(rs));
        }
      }
    } catch (SQLException e) {
      throw new WrappedException("Error finding database tables", e);
    }
  }

  private void deleteFrom(Table table) {
    deleteFromDependentTables(table);
    try {
      String sql = format("DELETE FROM %s.%s.%s", table.getCatalog(), table.getSchema(), table.getName());
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        int rowCount = statement.executeUpdate();
        if (verbose) {
          System.out.printf("Executing '%s'... %d rows deleted%n", sql, rowCount);
        }
        remaining.remove(table);
      }
    } catch (SQLException e) {
      throw new WrappedException(format("Error deleting data from %s", table), e);
    }
  }

  private void deleteFromDependentTables(Table table) {
    try {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet fks = metaData.getExportedKeys(table.getCatalog(), table.getSchema(), table.getName())) {
        while (fks.next()) {
          Table referencedTable = Table.fromFkMetadata(fks);
          if (remaining.contains(referencedTable)) {
            deleteFrom(referencedTable);
          }
        }
      }
    } catch (SQLException e) {
      throw new WrappedException(format("Error analyzing exported keys of %s", table), e);
    }
  }
}
