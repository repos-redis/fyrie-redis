package net.fyrie
package redis

import actors.{RedisClientSession}
import messages.{Request}

import se.scalablesolutions.akka.dispatch.{Future, FutureTimeoutException}
import se.scalablesolutions.akka.actor.{Actor,ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.dispatch._

class RedisClient(address: String = "localhost", port: Int = 6379) {
  val actor = actorOf(new RedisClientSession(address, port)).start

  def ![A](command: Command[A])(implicit sender: Option[ActorRef] = None): Unit =
    actor ! Request(command.toBytes, command.handler)

  def !![A](command: Command[A]): Option[Result[A]] = {
    val future = this !!! command
    try {
      future.await
    } catch {
      case e: FutureTimeoutException => None
    }
    if (future.exception.isDefined) throw future.exception.get
    else future.result
  }

  def !!![A](command: Command[A]): Future[Result[A]] =
    actor !!! Request(command.toBytes, command.handler)

  def send[A](command: Command[A]): A = {
    val future = this !!! command
    future.await
    if (future.exception.isDefined) throw future.exception.get
    else future.result.get.get
  }

  def stop = actor.stop

  def disconnect = stop
}

case class RedisErrorException(message: String) extends RuntimeException(message)
case class RedisProtocolException(message: String) extends RuntimeException(message)
case class RedisConnectionException(message: String) extends RuntimeException(message)

trait SortOrder
object SortOrder {
  case object ASC extends SortOrder
  case object DESC extends SortOrder
}

trait AggregateScore {
  def getBytes: Array[Byte]
}
object AggregateScore {
  case object SUM extends AggregateScore {
    val getBytes = "SUM".getBytes
  }
  case object MIN extends AggregateScore {
    val getBytes = "MIN".getBytes
  }
  case object MAX extends AggregateScore {
    val getBytes = "MAX".getBytes
  }
}