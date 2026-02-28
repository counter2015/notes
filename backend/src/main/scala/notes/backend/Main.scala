package notes.backend

import notes.backend.api.Endpoints
import notes.backend.config.NotesConfig
import notes.backend.service.PostgresNoteService
import notes.backend.service.RandomIdAllocator
import sttp.tapir.server.netty.sync.NettySyncServer

object Main:
  def main(args: Array[String]): Unit =
    val config = NotesConfig.loadOrThrow()
    val idAllocator = new RandomIdAllocator()
    val noteService = PostgresNoteService.fromConfig(config.db)
    val server = NettySyncServer()
      .port(config.http.port)
      .host(config.http.host)
      .addEndpoint(Endpoints.allocateNote(idAllocator))
      .addEndpoint(Endpoints.getNotePage(noteService))
      .addEndpoint(Endpoints.saveNote(noteService))
    println(s"[backend] starting server on http://${config.http.host}:${config.http.port}")
    server.startAndWait()
