package notes.shared

final case class NoteId private (value: String) extends AnyVal

object NoteId:
  private val Pattern = "^[a-z0-9]{4}$".r

  def from(value: String): Either[String, NoteId] =
    if Pattern.matches(value) then Right(NoteId(value))
    else Left("note id must match [a-z0-9]{4}")

  def unsafe(value: String): NoteId = NoteId(value)

final case class NoteSnapshot(
    id: String,
    content: String,
    version: Long
)
