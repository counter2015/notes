package notes.frontend

import notes.shared.NoteSnapshot

object Main:
  def main(args: Array[String]): Unit =
    val demo = NoteSnapshot("ab12", "hello from frontend", 0)
    println(s"[frontend] scaffold ready: ${demo.id}")
