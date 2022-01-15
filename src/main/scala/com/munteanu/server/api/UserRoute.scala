package com.munteanu.server.api

import com.munteanu.server.model.User
import com.munteanu.server.service.UserService.UserServiceEnv
import sttp.tapir.{PublicEndpoint, endpoint}
import sttp.tapir.ztapir._
import com.munteanu.server.service.UserService
import io.circe.generic.auto._
import org.http4s._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{RIO, ZIO}

import zio.interop.catz._

class UserRoute {

  type Env = UserServiceEnv

  type UserEndpoint = ZServerEndpoint[Env, Any]

  val userEndpoint: PublicEndpoint[Int, String, User, Any] =
    endpoint.get
      .in("users" / path[Int]("userId"))
      .errorOut(stringBody)
      .out(jsonBody[User])

  private def handler(userId: Int): ZIO[Env, String, User] =
    UserService
      .find(userId)
      .mapError(ex => ex.getMessage)

  def getRoutes: HttpRoutes[RIO[Env with Clock with Blocking, *]] =
    ZHttp4sServerInterpreter()
      .from(userEndpoint.zServerLogic(handler).widen[Env])
      .toRoutes
}

object UserRoute {
  def apply(): UserRoute = new UserRoute()
}
