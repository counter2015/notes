package notes.backend.api

import munit.FunSuite
import notes.backend.service.IdAllocator
import notes.backend.service.NoteService
import notes.shared.NoteId
import notes.shared.NoteSnapshot
import ox.Ox
import ox.supervised
import sttp.tapir.server.netty.sync.NettySyncServer

import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpClient.Redirect

class AllocateNoteE2ESuite extends FunSuite:
  test("GET / returns 302 with allocated location") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(new EmptyNoteService))
          .start()

        try
          val client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
          val request = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/"))
            .GET()
            .build()
          val response = client.send(request, HttpResponse.BodyHandlers.discarding())

          assertEquals(response.statusCode(), 302)
          assertEquals(response.headers().firstValue("Location").orElse(""), "/ab12")
        finally binding.stop()
    }
  }

  test("GET /{id} returns html page") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(new EmptyNoteService))
          .start()

        try
          val client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
          val request = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/ab12"))
            .GET()
            .build()
          val response = client.send(request, HttpResponse.BodyHandlers.ofString())
          assertEquals(response.statusCode(), 200)
          assert(response.body().contains("""<textarea class="content"></textarea>"""))
          assert(response.body().contains("""version: 0"""))
        finally binding.stop()
    }
  }

  test("GET /{id} with invalid id returns 400 json error") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(new EmptyNoteService))
          .start()

        try
          val client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
          val request = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/AB12"))
            .GET()
            .build()
          val response = client.send(request, HttpResponse.BodyHandlers.ofString())

          assertEquals(response.statusCode(), 400)
          assert(response.body().contains("INVALID_ID"))
        finally binding.stop()
    }
  }

  private def randomFreePort(): Int =
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

private final class FixedAllocator(id: String) extends IdAllocator:
  override def allocate(): String = id

private final class EmptyNoteService extends NoteService:
  override def getOrEmpty(id: NoteId): NoteSnapshot =
    NoteSnapshot(id = id.value, content = "", version = 0L)
