package notes.backend.service

import notes.shared.NoteId
import notes.shared.NoteSnapshot

import scala.collection.concurrent.TrieMap

trait NoteService:
  def getOrEmpty(id: NoteId): NoteSnapshot

final class InMemoryNoteService extends NoteService:
  private val notes = TrieMap.empty[String, NoteSnapshot]

  override def getOrEmpty(id: NoteId): NoteSnapshot =
    notes.getOrElse(id.value, NoteSnapshot(id = id.value, content = "", version = 0L))
