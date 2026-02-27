package notes.backend.service

import munit.FunSuite

import scala.util.Random

class RandomIdAllocatorSuite extends FunSuite:
  test("allocate returns 4-char lowercase alnum id") {
    val allocator = new RandomIdAllocator(new Random(42L))
    val id = allocator.allocate()

    assertEquals(id.length, 4)
    assert(id.forall(ch => ch.isLower || ch.isDigit))
  }

  test("allocate generates varied ids") {
    val allocator = new RandomIdAllocator(new Random(7L))
    val ids = (1 to 50).map(_ => allocator.allocate()).toSet

    assert(ids.size > 1, "allocator should not return the same id every time")
  }
