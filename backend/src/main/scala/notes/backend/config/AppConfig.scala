package notes.backend.config

import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures

final case class HttpConfig(
    host: String,
    port: Int
) derives ConfigReader

final case class DbConfig(
    url: String,
    user: String,
    password: String
) derives ConfigReader

final case class NotesConfig(
    http: HttpConfig,
    db: DbConfig
) derives ConfigReader

object NotesConfig:

  def load(): Either[ConfigReaderFailures, NotesConfig] =
    ConfigSource.default.at("notes").load[NotesConfig]

  def loadOrThrow(): NotesConfig =
    load().fold(failures => throw new IllegalStateException(s"invalid notes config: $failures"), identity)
