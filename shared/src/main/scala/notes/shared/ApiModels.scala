package notes.shared

final case class SaveNoteForm(
    t: String,
    version: Long
)

final case class SaveNoteResponse(
    id: String,
    version: Long,
    updatedAt: String
)

final case class ApiErrorBody(
    code: ApiErrorCode,
    message: String,
    requestId: String
)

final case class ApiConflictCurrent(
    version: Long
)

final case class ApiErrorEnvelope(
    error: ApiErrorBody,
    current: Option[ApiConflictCurrent] = None
)

enum ApiErrorCode(val code: String):
  case InvalidId extends ApiErrorCode("INVALID_ID")
  case InvalidArgument extends ApiErrorCode("INVALID_ARGUMENT")
  case ContentTooLarge extends ApiErrorCode("CONTENT_TOO_LARGE")
  case TooManyRequests extends ApiErrorCode("TOO_MANY_REQUESTS")
  case VersionConflict extends ApiErrorCode("VERSION_CONFLICT")
  case IdPoolBusy extends ApiErrorCode("ID_POOL_BUSY")
  case InternalError extends ApiErrorCode("INTERNAL_ERROR")

object ApiErrorCode:
  private val all = ApiErrorCode.values.toList
  private val byCode = all.map(item => item.code -> item).toMap

  def parse(value: String): Option[ApiErrorCode] =
    byCode.get(value)
