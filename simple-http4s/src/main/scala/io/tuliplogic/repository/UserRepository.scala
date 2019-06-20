package io.tuliplogic.repository

import io.tuliplogic.model.errors.Error.{UserAlreadyExists, UserNotFound}
import io.tuliplogic.model.user.{User, UserId}
import zio.{Ref, UIO, ZIO}

trait UserRepository {
  def userRepository: UserRepository.Service[Any]
}

object UserRepository {

  trait Service[R] {
    def create(user: User): ZIO[R, UserAlreadyExists , Unit]
    def find(id: UserId): ZIO[R, Nothing, Option[User]]
    def delete(id: UserId): ZIO[R, UserNotFound, Unit]
  }

  trait MemUserRepository extends UserRepository {
    val state: zio.Ref[Map[UserId, User]]
    def userRepository: Service[Any] = new Service[Any] {
      override def create(user: User): ZIO[Any, UserAlreadyExists, Unit] =
        for {
          userExists <- state.get.map(_.contains(user.id))
          _ <-
          if (userExists)
            ZIO.fail(UserAlreadyExists(user.id))
          else
            state.update(s => s.updated(user.id, user))
        } yield ()

      override def find(id: UserId): ZIO[Any, Nothing, Option[User]] =
        state.get.map(_.get(id))

      override def delete(id: UserId): ZIO[Any, UserNotFound, Unit] =
        for {
          userExists <- state.get.map(_.contains(id))
          _ <-
          if (userExists)
            ZIO.fail(UserNotFound(id))
          else state.update(s => s - id)
        } yield ()
    }
  }

  object MemUserRepository {
    def create(map: Map[UserId, User]): UIO[MemUserRepository] =
      Ref.make(map).map(m => new MemUserRepository {
        override val state: Ref[Map[UserId, User]] = m
      }
    )
  }

}

object userRepo extends UserRepository.Service[UserRepository] {

  override def create(user: User): ZIO[UserRepository, UserAlreadyExists, Unit] = ZIO.accessM(_.userRepository.create(user))

  override def find(id: UserId): ZIO[UserRepository, Nothing, Option[User]] = ZIO.accessM(_.userRepository.find(id))

  override def delete(id: UserId): ZIO[UserRepository, UserNotFound, Unit] = ZIO.accessM(_.userRepository.delete(id))
}