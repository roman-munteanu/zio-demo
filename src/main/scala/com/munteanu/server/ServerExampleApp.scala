package com.munteanu.server

import com.munteanu.server.api.UserRoute
import com.munteanu.server.service.UserService
import com.munteanu.server.service.UserService.UserServiceEnv
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{ExitCode, RIO, URIO, ZEnv, ZIO}

object ServerExampleApp extends zio.App {

  val userRoutes: HttpRoutes[RIO[UserServiceEnv with Clock with Blocking, *]] =
    UserRoute().getRoutes

  val server: ZIO[ZEnv with UserServiceEnv, Throwable, Unit] =
    ZIO.runtime[ZEnv with UserServiceEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[RIO[UserServiceEnv with Clock with Blocking, *]]
        .withExecutionContext(runtime.platform.executor.asEC)
        .bindHttp(8080, "localhost")
        .withHttpApp(Router("/" -> userRoutes).orNotFound)
        .serve
        .compile
        .drain
    }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    server.provideCustomLayer(UserService.live).exitCode
}
