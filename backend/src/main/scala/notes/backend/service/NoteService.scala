package notes.backend.service

import notes.shared.NoteId
import notes.shared.NoteSnapshot

import scala.collection.concurrent.TrieMap

trait NoteService:
  def getOrEmpty(id: NoteId): NoteSnapshot
  def save(id: NoteId, content: String, expectedVersion: Long): Either[NoteSaveError, NoteSnapshot]

sealed trait NoteSaveError

object NoteSaveError:
  final case class VersionConflict(currentVersion: Long) extends NoteSaveError

final class InMemoryNoteService extends NoteService:
  private val notes = TrieMap.empty[String, NoteSnapshot]

  override def getOrEmpty(id: NoteId): NoteSnapshot =
    notes.getOrElse(id.value, NoteSnapshot(id = id.value, content = "", version = 0L))

  override def save(id: NoteId, content: String, expectedVersion: Long): Either[NoteSaveError, NoteSnapshot] =
    this.synchronized {
      notes.get(id.value) match
        case None =>
          if expectedVersion == 0L then
            val created = NoteSnapshot(id = id.value, content = content, version = 1L)
            notes.put(id.value, created)
            Right(created)
          else Left(NoteSaveError.VersionConflict(currentVersion = 0L))

        case Some(existing) =>
          if existing.version == expectedVersion then
            val updated = existing.copy(content = content, version = existing.version + 1L)
            notes.update(id.value, updated)
            Right(updated)
          else Left(NoteSaveError.VersionConflict(currentVersion = existing.version))
    }
