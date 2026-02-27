package notes.backend.api

import notes.backend.service.IdAllocator
import notes.backend.service.NoteService
import notes.backend.web.HtmlPageRenderer
import notes.shared.ApiCodecs.given
import notes.shared.ApiErrorBody
import notes.shared.ApiErrorCode
import notes.shared.ApiErrorEnvelope
import notes.shared.NoteId
import sttp.shared.Identity
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

object Endpoints:
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
          Left(
            StatusCode.BadRequest,
            ApiErrorEnvelope(
              error = ApiErrorBody(
                code = ApiErrorCode.InvalidId,
                message = message,
                requestId = newRequestId()
              )
            )
          )
        case Right(noteId) =>
          val snapshot = noteService.getOrEmpty(noteId)
          Right(HtmlPageRenderer.notePage(snapshot))
    }

  private def newRequestId(): String =
    s"req_${UUID.randomUUID().toString.replace("-", "")}"
