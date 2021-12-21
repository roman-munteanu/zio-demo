package com.munteanu.storage

import zio.{BootstrapRuntime, ExitCode, IO, RIO, Ref, Task, URIO, ZIO}

object StorageDemo extends zio.App {

  case class Hero(id: Int, name: String)

  // module
  object HeroStorage {
    trait Service {
      def findById(id: Int): Task[Option[Hero]]
      def findAll(): Task[Seq[Hero]]
      def save(hero: Hero): Task[Unit]
    }
  }

  trait HeroStorage {
    def storage: HeroStorage.Service
  }

  object db {
    def findById(id: Int): RIO[HeroStorage, Option[Hero]] =
      ZIO.accessM(_.storage.findById(id))

    def findAll(): RIO[HeroStorage, Seq[Hero]] =
      ZIO.accessM(_.storage.findAll())

    def save(hero: Hero): RIO[HeroStorage, Unit] =
      ZIO.accessM(_.storage.save(hero))
  }

  // not fiber-safe: usage of var
/*
  trait InMemoryHeroStorage extends HeroStorage {

    val storage = new HeroStorage.Service {

      private var map: Map[Int, Hero] = Map(1 -> Hero(1, "Craig Hack"), 2 -> Hero(2, "Solmyr"))

      override def findById(id: Int): Task[Option[Hero]] =
        Task(map.get(id))

      override def findAll(): Task[Seq[Hero]] =
        Task(map.values.toSeq)
    }
  }
*/

  // fiber-safe: usage of Ref
  trait InMemoryHeroStorage extends HeroStorage with BootstrapRuntime {

    val storage = new HeroStorage.Service {

      val ref: Ref[InMemoryHeroStorage.State] = unsafeRun(
        Ref.make(
          InMemoryHeroStorage.State(
            Map(
              1 -> Hero(1, "Craig Hack"),
              2 -> Hero(2, "Solmyr")
            )
          )
        )
      )

      override def findById(id: Int): Task[Option[Hero]] =
        ref.modify(_.get(id))

      override def findAll(): Task[Seq[Hero]] =
        ref.modify(_.getAll)

      override def save(hero: Hero): Task[Unit] =
        ref.update(_.put(hero))
    }
  }

  object InMemoryHeroStorage extends InMemoryHeroStorage {

    final case class State(storage: Map[Int, Hero]) {

      def get(id: Int): (Option[Hero], State) =
        (storage.get(id), this)

      def getAll: (Seq[Hero], State) =
        (storage.values.toSeq, this)

      def put(hero: Hero): State =
        copy(storage = storage.updated(hero.id, hero))
    }
  }

  def main: RIO[HeroStorage, Unit] =
    for {
      _ <- db.save(Hero(3, "Gelu"))
      heroes <- db.findAll()
      _ = heroes.foreach(println)
    } yield ()

  def mainInMemory: Task[Unit] = main.provide(InMemoryHeroStorage)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    mainInMemory.exitCode
}
