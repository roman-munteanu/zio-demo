package com.munteanu.server.service

import com.munteanu.server.model.User
import zio.{Has, Task, UIO, ZIO, ZLayer}

object UserService {

  type UserServiceEnv = Has[UserService.Service]

  trait Service {
    def find(id: Int): Task[User]
  }

  val live = ZLayer.succeed(new Service {
    override def find(id: Int): Task[User] =
      Task {
        User("TestUserName")
      }
  })

  def find(id: Int): ZIO[UserServiceEnv, Throwable, User] =
    ZIO.accessM(_.get.find(id))
}
