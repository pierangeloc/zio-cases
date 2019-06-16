package io.tuliplogic.model

import io.tuliplogic.model.user.UserId


object user {
  case class UserId(value: String) extends AnyVal
  case class UserName(value: String) extends AnyVal
  case class User(id: UserId, name: UserName)
}

object tracing {
  case class TraceId(value: String) extends AnyVal
  case class ClientId(value: String) extends AnyVal
}

object events {

  import io.tuliplogic.model.user.{User, UserId}
  case class Timestamp(fromEpochMillis: Long) extends AnyVal

  sealed trait UserEvent {
    def timestamp: Timestamp
  }
  object UserEvent {
    case class UserCreated(user: User, timestamp: Timestamp) extends UserEvent
    case class UserDeleted(userId: UserId, timestamp: Timestamp) extends UserEvent
    case class UserSearched(userId: UserId, timestamp: Timestamp) extends UserEvent
  }
}

object errors {
  sealed trait Error extends Exception
  object Error {
    case class UserNotFound(userId: UserId) extends Error
    case class UserAlreadyExists(userId: UserId) extends Error
    case class InternalError(reason: String) extends Error
  }
}

