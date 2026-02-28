package notes.backend.api

import notes.backend.service.IdAllocator
import notes.backend.service.NoteSaveError
import notes.backend.service.NoteService
import notes.backend.web.HtmlPageRenderer
import notes.shared.ApiCodecs.given
import notes.shared.ApiErrorBody
import notes.shared.ApiErrorCode
import notes.shared.ApiConflictCurrent
import notes.shared.ApiErrorEnvelope
import notes.shared.NoteId
import notes.shared.SaveNoteForm
import notes.shared.SaveNoteResponse
import sttp.shared.Identity
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import scala.util.control.NonFatal

object Endpoints:
  private val maxContentBytes = 200 * 1024

  val allocateNoteEndpoint: PublicEndpoint[Unit, ApiErrorEnvelope, String, Any] =
    endpoint.get
      .in("")
      .errorOut(jsonBody[ApiErrorEnvelope])
      .out(statusCode(sttp.model.StatusCode.Found))
      .out(header[String]("Location"))

  def allocateNote(idAllocator: IdAllocator): ServerEndpoint[Any, Identity] =
    allocateNoteEndpoint.serverLogicSuccess { _ =>
      val id = idAllocator.allocate()
      s"/$id"
    }

  val getNotePageEndpoint: PublicEndpoint[String, (StatusCode, ApiErrorEnvelope), String, Any] =
    endpoint.get
      .in(path[String]("id"))
      .errorOut(statusCode.and(jsonBody[ApiErrorEnvelope]))
      .out(stringBody)
      .out(header("Content-Type", "text/html; charset=utf-8"))

  def getNotePage(noteService: NoteService): ServerEndpoint[Any, Identity] =
    getNotePageEndpoint.serverLogic { rawId =>
      NoteId.from(rawId) match
        case Left(message) =>
          Left(error(StatusCode.BadRequest, ApiErrorCode.InvalidId, message))
        case Right(noteId) =>
          try
            val snapshot = noteService.getOrEmpty(noteId)
            Right(HtmlPageRenderer.notePage(snapshot))
          catch
            case NonFatal(ex) =>
              Left(error(StatusCode.InternalServerError, ApiErrorCode.InternalError, s"load failed: ${ex.getMessage}"))
    }

  val saveNoteEndpoint: PublicEndpoint[(String, SaveNoteForm), (StatusCode, ApiErrorEnvelope), SaveNoteResponse, Any] =
    endpoint.post
      .in(path[String]("id"))
      .in(formBody[SaveNoteForm])
      .errorOut(statusCode.and(jsonBody[ApiErrorEnvelope]))
      .out(jsonBody[SaveNoteResponse])

  def saveNote(noteService: NoteService): ServerEndpoint[Any, Identity] =
    saveNoteEndpoint.serverLogic { case (rawId, form) =>
      NoteId.from(rawId) match
        case Left(message) =>
          Left(error(StatusCode.BadRequest, ApiErrorCode.InvalidId, message))

        case Right(_) if form.version < 0 =>
          Left(error(StatusCode.BadRequest, ApiErrorCode.InvalidArgument, "version must be a non-negative integer"))

        case Right(_) if form.t.getBytes(StandardCharsets.UTF_8).length > maxContentBytes =>
          Left(error(StatusCode.PayloadTooLarge, ApiErrorCode.ContentTooLarge, s"content exceeds $maxContentBytes bytes"))

        case Right(noteId) =>
          noteService.save(noteId, form.t, form.version) match
            case Right(saved) =>
              Right(
                SaveNoteResponse(
                  id = saved.id,
                  version = saved.version,
                  updatedAt = Instant.now().toString
                )
              )
            case Left(NoteSaveError.VersionConflict(currentVersion)) =>
              Left(
                StatusCode.Conflict,
                ApiErrorEnvelope(
                  error = ApiErrorBody(
                    code = ApiErrorCode.VersionConflict,
                    message = "note has been modified by another session",
                    requestId = newRequestId()
                  ),
                  current = Some(ApiConflictCurrent(currentVersion))
                )
              )
            case Left(NoteSaveError.StorageFailure(message)) =>
              Left(error(StatusCode.InternalServerError, ApiErrorCode.InternalError, message))
    }

  private def error(statusCode: StatusCode, code: ApiErrorCode, message: String): (StatusCode, ApiErrorEnvelope) =
    (
      statusCode,
      ApiErrorEnvelope(
        error = ApiErrorBody(
          code = code,
          message = message,
          requestId = newRequestId()
        )
      )
    )

  private def newRequestId(): String =
    s"req_${UUID.randomUUID().toString.replace("-", "")}"
