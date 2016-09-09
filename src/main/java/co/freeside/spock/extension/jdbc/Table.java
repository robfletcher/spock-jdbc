package co.freeside.spock.extension.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import static java.lang.String.format;

class Table {
  private final String catalog;
  private final String schema;
  private final String name;

  public Table(String catalog, String schema, String name) {
    this.catalog = catalog;
    this.schema = schema;
    this.name = name;
  }

  static Table fromTableMetadata(ResultSet rs) throws SQLException {
    return new Table(
      rs.getString("TABLE_CAT"),
      rs.getString("TABLE_SCHEM"),
      rs.getString("TABLE_NAME")
    );
  }

  static Table fromFkMetadata(ResultSet rs) throws SQLException {
    return new Table(
      rs.getString("FKTABLE_CAT"),
      rs.getString("FKTABLE_SCHEM"),
      rs.getString("FKTABLE_NAME")
    );
  }

  public String getCatalog() {
    return catalog;
  }

  public String getSchema() {
    return schema;
  }

  public String getName() {
    return name;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Table table = (Table) o;
    return Objects.equals(catalog, table.catalog) &&
      Objects.equals(schema, table.schema) &&
      Objects.equals(name, table.name);
  }

  @Override public int hashCode() {
    return Objects.hash(catalog, schema, name);
  }

  @Override public String toString() {
    return format("Table{catalog='%s', schema='%s', name='%s'}", catalog, schema, name);
  }
}
