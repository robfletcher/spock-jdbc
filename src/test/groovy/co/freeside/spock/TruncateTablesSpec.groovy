package co.freeside.spock

import java.sql.Connection
import javax.sql.DataSource
import groovy.sql.Sql
import org.h2.jdbcx.JdbcDataSource
import org.skife.jdbi.v2.DBI
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
abstract class TruncateTablesSpec extends Specification {

  @Shared def dataSource = new JdbcDataSource(url: "jdbc:h2:mem:test")
  def sql = new Sql(dataSource)

  def setupSpec() {
    new Sql(dataSource.connection).with {
      execute """
        create table performer (
          id integer auto_increment primary key, 
          name varchar(50) not null
        );
        create table band (
          id integer auto_increment primary key, 
          name varchar(50) not null
        );
        create table band_member (
          band_id integer not null, 
          performer_id integer not null, 
          role varchar(50) not null,
          foreign key (band_id) references band (id),
          foreign key (performer_id) references performer (id),
          constraint band_member_unique unique(band_id, performer_id)
        );
      """
    }
  }

  def cleanupSpec() {
    new Sql(dataSource.connection).with {
      execute """
        drop table band_member;
        drop table band;
        drop table performer;
      """
    }
  }

  def "create some data"() {
    given:
    insertBand(band)
    members.each {
      insertPerformer(band, it.value, it.key)
    }
    sql.commit()

    expect:
    count("band") == 1
    count("performer") == members.size()
    count("band_member") == members.size()

    and:
    members.every {
      find(band, it.key) == it.value
    }

    where:
    band = "Dead Kennedys"
    members = [
      vocals: "Jello Biafra",
      guitar: "East Bay Ray"
    ]
  }

  def "data should have been deleted"() {
    expect:
    count("band") == 0
    count("performer") == 0
    count("band_member") == 0
  }

  int count(String tableName) {
    sql.firstRow("select count(*) as n from " + tableName).n
  }

  String find(String bandName, String role) {
    sql.firstRow("""
      select performer.name 
        from performer, band_member, band 
       where band.id = band_member.band_id 
         and performer.id = band_member.performer_id
         and band.name = ?
         and band_member.role = ? 
    """, bandName, role).name
  }

  void insertBand(String name) {
    sql.executeInsert "insert into band (name) values (?)", [name]
  }

  void insertPerformer(String band, String name, String role) {
    sql.executeInsert "insert into performer (name) values (?)", [name]
    sql.executeInsert """
      insert into band_member (band_id, performer_id, role) 
           select band.id, performer.id, ? 
             from band, performer
            where band.name = ? 
              and performer.name = ?
    """, [role, band, name]
  }

  static class TruncateTablesWithRawConnectionSpec extends TruncateTablesSpec {
    @TruncateTables(verbose = true) Connection connection = dataSource.connection
  }

  static class TruncateTablesWithDataSourceSpec extends TruncateTablesSpec {
    @TruncateTables(verbose = true) DataSource ds = dataSource
  }

  static class TruncateTablesWithGroovySqlSpec extends TruncateTablesSpec {
    @TruncateTables(verbose = true) Sql groovySql = new Sql(dataSource)
  }

  static class TruncateTablesWithSingleConnectionGroovySqlSpec extends TruncateTablesSpec {
    @TruncateTables(verbose = true) Sql groovySql = new Sql(dataSource.connection)
  }

  static class TruncateTablesWithJdbiSpec extends TruncateTablesSpec {
    @TruncateTables(value = DBIConnector, verbose = true) DBI dbi = new DBI(dataSource);
  }

  static class DBIConnector extends TypedConnector<DBI> {
    DBIConnector() { super(DBI) }

    @Override
    protected Connection apply(DBI source) {
      source.open().connection
    }
  }
}
