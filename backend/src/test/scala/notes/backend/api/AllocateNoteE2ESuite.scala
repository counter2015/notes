package notes.backend.api

import munit.FunSuite
import notes.backend.service.IdAllocator
import notes.backend.service.InMemoryNoteService
import ox.Ox
import ox.supervised
import sttp.tapir.server.netty.sync.NettySyncServer

import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpClient.Redirect
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AllocateNoteE2ESuite extends FunSuite:
  test("GET / returns 302 with allocated location") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val noteService = new InMemoryNoteService
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(noteService))
          .addEndpoint(Endpoints.saveNote(noteService))
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
        val noteService = new InMemoryNoteService
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(noteService))
          .addEndpoint(Endpoints.saveNote(noteService))
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
        val noteService = new InMemoryNoteService
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(noteService))
          .addEndpoint(Endpoints.saveNote(noteService))
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

  test("POST /{id} saves content and then GET returns updated content") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val noteService = new InMemoryNoteService
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(noteService))
          .addEndpoint(Endpoints.saveNote(noteService))
          .start()

        try
          val client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
          val saveRequest = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/ab12"))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(form("hello world", 0)))
            .build()
          val saveResponse = client.send(saveRequest, HttpResponse.BodyHandlers.ofString())
          assertEquals(saveResponse.statusCode(), 200)
          assert(saveResponse.body().contains(""""version":1"""))

          val getRequest = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/ab12"))
            .GET()
            .build()
          val getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString())
          assertEquals(getResponse.statusCode(), 200)
          assert(getResponse.body().contains("hello world"))
          assert(getResponse.body().contains("version: 1"))
        finally binding.stop()
    }
  }

  test("POST /{id} returns 409 when version mismatches") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val noteService = new InMemoryNoteService
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(noteService))
          .addEndpoint(Endpoints.saveNote(noteService))
          .start()

        try
          val client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
          val saveV0 = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/ab12"))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(form("first", 0)))
            .build()
          assertEquals(client.send(saveV0, HttpResponse.BodyHandlers.ofString()).statusCode(), 200)

          val staleSave = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/ab12"))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(form("second", 0)))
            .build()
          val staleResponse = client.send(staleSave, HttpResponse.BodyHandlers.ofString())

          assertEquals(staleResponse.statusCode(), 409)
          assert(staleResponse.body().contains("VERSION_CONFLICT"))
          assert(staleResponse.body().contains(""""version":1"""))
        finally binding.stop()
    }
  }

  test("POST /{id} returns 413 when content is too large") {
    val port = randomFreePort()
    supervised {
      (ox: Ox) ?=>
        val noteService = new InMemoryNoteService
        val binding = NettySyncServer()
          .host("127.0.0.1")
          .port(port)
          .addEndpoint(Endpoints.allocateNote(new FixedAllocator("ab12")))
          .addEndpoint(Endpoints.getNotePage(noteService))
          .addEndpoint(Endpoints.saveNote(noteService))
          .start()

        try
          val client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
          val oversized = "a" * (200 * 1024 + 1)
          val request = HttpRequest.newBuilder()
            .uri(URI.create(s"http://127.0.0.1:$port/ab12"))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(form(oversized, 0)))
            .build()
          val response = client.send(request, HttpResponse.BodyHandlers.ofString())

          assertEquals(response.statusCode(), 413)
          assert(response.body().contains("CONTENT_TOO_LARGE"))
        finally binding.stop()
    }
  }

  private def randomFreePort(): Int =
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

  private def form(content: String, version: Long): String =
    val encoded = URLEncoder.encode(content, StandardCharsets.UTF_8)
    s"t=$encoded&version=$version"

private final class FixedAllocator(id: String) extends IdAllocator:
  override def allocate(): String = id
