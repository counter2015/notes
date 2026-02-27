package notes.backend

import notes.shared.NoteSnapshot

object Main:
  def main(args: Array[String]): Unit =
    val demo = NoteSnapshot("ab12", "hello from backend", 0)
    println(s"[backend] scaffold ready: ${demo.id}")
