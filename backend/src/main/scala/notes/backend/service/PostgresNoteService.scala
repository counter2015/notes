package notes.backend.service

import notes.shared.NoteId
import notes.shared.NoteSnapshot

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

final class PostgresNoteService(
    jdbcUrl: String,
    dbUser: String,
    dbPassword: String
) extends NoteService:
  Class.forName("org.postgresql.Driver")
  ensureSchema()

  override def getOrEmpty(id: NoteId): NoteSnapshot =
    withConnection { connection =>
      val sql = "select content, version from notes where id = ?"
      val statement = connection.prepareStatement(sql)
      try
        statement.setString(1, id.value)
        val rs = statement.executeQuery()
        if rs.next() then
          NoteSnapshot(
            id = id.value,
            content = rs.getString("content"),
            version = rs.getLong("version")
          )
        else NoteSnapshot(id = id.value, content = "", version = 0L)
      finally statement.close()
    }

  override def save(id: NoteId, content: String, expectedVersion: Long): Either[NoteSaveError, NoteSnapshot] =
    try
      withConnection { connection =>
        if expectedVersion == 0L then saveCreate(connection, id, content)
        else saveUpdate(connection, id, content, expectedVersion)
      }
    catch
      case ex: SQLException =>
        Left(NoteSaveError.StorageFailure(s"database operation failed: ${ex.getMessage}"))

  private def saveCreate(connection: Connection, id: NoteId, content: String): Either[NoteSaveError, NoteSnapshot] =
    val sql =
      """insert into notes (id, content, version, created_at, updated_at)
        |values (?, ?, 1, now(), now())
        |on conflict (id) do nothing
        |""".stripMargin
    val statement = connection.prepareStatement(sql)
    try
      statement.setString(1, id.value)
      statement.setString(2, content)
      val updated = statement.executeUpdate()
      if updated == 1 then Right(NoteSnapshot(id = id.value, content = content, version = 1L))
      else Left(NoteSaveError.VersionConflict(currentVersion = fetchCurrentVersion(connection, id)))
    finally statement.close()

  private def saveUpdate(
      connection: Connection,
      id: NoteId,
      content: String,
      expectedVersion: Long
  ): Either[NoteSaveError, NoteSnapshot] =
    val sql =
      """update notes
        |set content = ?, version = version + 1, updated_at = now()
        |where id = ? and version = ?
        |""".stripMargin
    val statement = connection.prepareStatement(sql)
    try
      statement.setString(1, content)
      statement.setString(2, id.value)
      statement.setLong(3, expectedVersion)
      val updated = statement.executeUpdate()
      if updated == 1 then Right(NoteSnapshot(id = id.value, content = content, version = expectedVersion + 1L))
      else Left(NoteSaveError.VersionConflict(currentVersion = fetchCurrentVersion(connection, id)))
    finally statement.close()

  private def fetchCurrentVersion(connection: Connection, id: NoteId): Long =
    val sql = "select version from notes where id = ?"
    val statement = connection.prepareStatement(sql)
    try
      statement.setString(1, id.value)
      val rs = statement.executeQuery()
      if rs.next() then rs.getLong("version") else 0L
    finally statement.close()

  private def ensureSchema(): Unit =
    withConnection { connection =>
      val ddl =
        """create table if not exists notes (
          |  id char(4) primary key,
          |  content text not null default '',
          |  version bigint not null default 0,
          |  created_at timestamptz not null default now(),
          |  updated_at timestamptz not null default now()
          |)
          |""".stripMargin
      val statement = connection.createStatement()
      try statement.execute(ddl)
      finally statement.close()
    }

  private def withConnection[A](f: Connection => A): A =
    val connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)
    try f(connection)
    finally connection.close()

object PostgresNoteService:
  def fromEnv(): PostgresNoteService =
    val jdbcUrl = sys.env.getOrElse("NOTES_DB_URL", "jdbc:postgresql://127.0.0.1:5432/notes")
    val dbUser = sys.env.getOrElse("NOTES_DB_USER", "notes")
    val dbPassword = sys.env.getOrElse("NOTES_DB_PASSWORD", "notes")
    new PostgresNoteService(jdbcUrl, dbUser, dbPassword)
