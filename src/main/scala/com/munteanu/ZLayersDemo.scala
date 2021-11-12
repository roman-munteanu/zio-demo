package com.munteanu

import zio.{ExitCode, Has, Task, URIO, ZIO, ZLayer}
import zio.console._

object ZLayersDemo extends zio.App {

  // ZIO[-R, +E, +A] = effects
  // R => Either[E, A]

  val succeedExample = ZIO.succeed(37)
  val failExample = ZIO.fail("Error occurred")

  val greeting = for {
    _ <- putStrLn("What's your age?")
    age <- getStrLn
    _ <- putStrLn(s"Your age: $age")
  } yield ()

  /*
  Creating heavy apps which involve services
  - interacting with storage layer
  - business logic
  - front-facing APIs (over HTTP)
  - communicating with other services
   */

  case class User(name: String, email: String)

  object UserEmailer {
    type UserEmailerEnv = Has[UserEmailer.Service]

    trait Service {
      def notify(user: User, message: String): Task[Unit] // ZIO[Any, Throwable, Unit]
    }

    val live: ZLayer[Any, Nothing, UserEmailerEnv] = ZLayer.succeed(new Service {
      override def notify(user: User, message: String): Task[Unit] = Task {
        println(s"[UserEmailer] Sending message '$message' to ${user.email}")
      }
    })

    // front-facing API
    def notify(user: User, message: String): ZIO[UserEmailerEnv, Throwable, Unit] =
      ZIO.accessM(hasService => hasService.get.notify(user, message))
  }

  object UserDb {
    type UserDbEnv = Has[UserDb.Service]

    trait Service {
      def insert(user: User): Task[Unit]
    }

    val live = ZLayer.succeed(new Service {
      override def insert(user: User): Task[Unit] = Task {
        println(s"[UserDb] Insert into users values ('${user.email}')")
      }
    })

    def insert(user: User): ZIO[UserDbEnv, Throwable, Unit] =
      ZIO.accessM(_.get.insert(user))
  }

  // horizontal composition
  // Zlayer[In1, E1, Out1] ++ Zlayer[In2, E2, Out2] => Zlayer[In1 with In2, super(E1, E2), Out1 with Out2]

  import UserDb._
  import UserEmailer._

  val userBackendLayer: ZLayer[Any, Nothing, UserDbEnv with UserEmailerEnv] = UserDb.live ++ UserEmailer.live


  // vertical composition
  object UserSubscription {
    type UserSubscriptionEnv = Has[UserSubscription.Service]

    class Service(userDb: UserDb.Service, notifier: UserEmailer.Service) {
      def subscribe(user: User): Task[User] =
        for {
          _ <- userDb.insert(user)
          _ <- notifier.notify(user, s"[UserSubscription] Welcome to RMHighlander on Spotify, ${user.name}! I have some nice music for you!")
        } yield user
    }

    val live: ZLayer[UserEmailerEnv with UserDbEnv, Nothing, UserSubscriptionEnv] =
      ZLayer.fromServices[UserEmailer.Service, UserDb.Service, UserSubscription.Service] { (userEmailer, userDb) =>
        new Service(userDb, userEmailer)
      }

    // front-facing API
    def subscribe(user: User): ZIO[UserSubscriptionEnv, Throwable, User] =
      ZIO.accessM(_.get.subscribe(user))
  }

  import UserSubscription._
  val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscriptionEnv] = userBackendLayer >>> UserSubscription.live

  val roman = User("Roman", "roman@test.com")
  val message = "Sparkling Stars"

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
//    greeting.exitCode
//    UserEmailer.notify(roman, message)
//      .provideLayer(UserEmailer.live)
//      .exitCode
    UserSubscription.subscribe(roman)
      .provideLayer(userSubscriptionLayer)
      .exitCode
}
