package notes.backend.api

import notes.backend.service.IdAllocator
import notes.shared.ApiCodecs.given
import notes.shared.ApiErrorEnvelope
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

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
