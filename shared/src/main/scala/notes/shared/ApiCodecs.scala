package notes.shared

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder

object ApiCodecs:
  given Encoder[ApiErrorCode] = Encoder.encodeString.contramap(_.code)
  given Decoder[ApiErrorCode] = Decoder.decodeString.emap { value =>
    ApiErrorCode.parse(value).toRight(s"unknown ApiErrorCode: $value")
  }

  given Encoder[SaveNoteForm] = deriveEncoder
  given Decoder[SaveNoteForm] = deriveDecoder

  given Encoder[SaveNoteResponse] = deriveEncoder
  given Decoder[SaveNoteResponse] = deriveDecoder

  given Encoder[ApiErrorBody] = deriveEncoder
  given Decoder[ApiErrorBody] = deriveDecoder

  given Encoder[ApiConflictCurrent] = deriveEncoder
  given Decoder[ApiConflictCurrent] = deriveDecoder

  given Encoder[ApiErrorEnvelope] = deriveEncoder
  given Decoder[ApiErrorEnvelope] = deriveDecoder

  given Encoder[NoteSnapshot] = deriveEncoder
  given Decoder[NoteSnapshot] = deriveDecoder
