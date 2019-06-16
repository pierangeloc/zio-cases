package io.tuliplogic.repository

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import io.tuliplogic.model.user.{User, UserId}

trait UserRepository[F[_]] {
  def create(user: User): F[Unit]
  def find(id: UserId): F[Option[User]]
  def delete(id: UserId): F[Unit]
}

class MemUserRepository[F[_]: Sync](state: Ref[F, Map[UserId, User]]) extends UserRepository[F] {
  override def create(user: User): F[Unit] = state.update(s => s.updated(user.id, user))

  override def find(id: UserId): F[Option[User]] = state.get.map(_.get(id))

  override def delete(id: UserId): F[Unit] = state.update(s => s - id)
}
