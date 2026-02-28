package notes.backend.config

import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures

final case class HttpConfig(
    host: String,
    port: Int
)

object HttpConfig:
  given ConfigReader[HttpConfig] =
    ConfigReader.forProduct2("host", "port")(HttpConfig.apply)

final case class DbConfig(
    url: String,
    user: String,
    password: String
)

object DbConfig:
  given ConfigReader[DbConfig] =
    ConfigReader.forProduct3("url", "user", "password")(DbConfig.apply)

final case class NotesConfig(
    http: HttpConfig,
    db: DbConfig
)

object NotesConfig:
  given ConfigReader[NotesConfig] =
    ConfigReader.forProduct2("http", "db")(NotesConfig.apply)

  def load(): Either[ConfigReaderFailures, NotesConfig] =
    ConfigSource.default.at("notes").load[NotesConfig]

  def loadOrThrow(): NotesConfig =
    load().fold(failures => throw new IllegalStateException(s"invalid notes config: $failures"), identity)
