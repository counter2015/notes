package notes.backend

import notes.backend.api.Endpoints
import notes.backend.service.InMemoryNoteService
import notes.backend.service.RandomIdAllocator
import sttp.tapir.server.netty.sync.NettySyncServer

object Main:
  def main(args: Array[String]): Unit =
    val idAllocator = new RandomIdAllocator()
    val noteService = new InMemoryNoteService()
    val server = NettySyncServer()
      .port(8080)
      .host("0.0.0.0")
      .addEndpoint(Endpoints.allocateNote(idAllocator))
      .addEndpoint(Endpoints.getNotePage(noteService))
      .addEndpoint(Endpoints.saveNote(noteService))
    println("[backend] starting server on http://0.0.0.0:8080")
    server.startAndWait()
