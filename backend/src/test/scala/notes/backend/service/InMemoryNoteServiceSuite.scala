package notes.backend.service

import munit.FunSuite
import notes.shared.NoteId

class InMemoryNoteServiceSuite extends FunSuite:
  test("save creates new note when expectedVersion is 0") {
    val service = new InMemoryNoteService
    val noteId = NoteId.unsafe("ab12")

    val result = service.save(noteId, "hello", expectedVersion = 0L)

    assert(result.isRight)
    val snapshot = result.toOption.get
    assertEquals(snapshot.version, 1L)
    assertEquals(snapshot.content, "hello")
  }

  test("save returns conflict when expectedVersion mismatches") {
    val service = new InMemoryNoteService
    val noteId = NoteId.unsafe("ab12")
    service.save(noteId, "hello", expectedVersion = 0L)

    val result = service.save(noteId, "world", expectedVersion = 0L)

    result match
      case Left(NoteSaveError.VersionConflict(currentVersion)) =>
        assertEquals(currentVersion, 1L)
      case other =>
        fail(s"expected VersionConflict, got: $other")
  }
