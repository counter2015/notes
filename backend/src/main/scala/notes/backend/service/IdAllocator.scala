package notes.backend.service

import scala.util.Random

trait IdAllocator:
  def allocate(): String

final class RandomIdAllocator(
  random: Random = new Random()
) extends IdAllocator:
  private val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
  private val idLength = 4

  override def allocate(): String =
    val builder = new StringBuilder(idLength)
    var i = 0
    while i < idLength do
      builder.append(alphabet.charAt(random.nextInt(alphabet.length)))
      i += 1
    builder.result()
