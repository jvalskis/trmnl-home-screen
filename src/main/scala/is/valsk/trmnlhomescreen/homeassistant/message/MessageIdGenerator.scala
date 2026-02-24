package is.valsk.trmnlhomescreen.homeassistant.message

import zio.*

trait MessageIdGenerator {

  def generate(): UIO[Int]
}

object MessageIdGenerator {

  class SequentialMessageIdGenerator(messageId: Ref[Int]) extends MessageIdGenerator {

    def generate(): UIO[Int] = messageId.getAndUpdate(_ + 1)
  }

  object SequentialMessageIdGenerator {

    val layer: ULayer[MessageIdGenerator] = ZLayer {
      for {
        ref <- Ref.make[Int](1)
      } yield SequentialMessageIdGenerator(ref)
    }

  }

}
