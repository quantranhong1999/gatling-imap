package com.linagora.gatling.imap.protocol.command

import java.util.function.Consumer

import akka.actor.{ActorRef, Props}
import com.linagora.gatling.imap.protocol._
import com.yahoo.imapnio.async.client.ImapAsyncSession
import com.yahoo.imapnio.async.request.UnsubscribeFolderCommand
import com.yahoo.imapnio.async.response.ImapAsyncResponse
import io.gatling.core.akka.BaseActor

import scala.collection.immutable.Seq

object UnsubscribeHandler {
  def props(session: ImapAsyncSession) = Props(new UnsubscribeHandler(session))
}

class UnsubscribeHandler(session: ImapAsyncSession) extends BaseActor {
  override def receive: Receive = {
    case Command.Unsubscribe(userId, mailbox) =>
      logger.trace(s"UnsubscribeHandler for user : ${userId.value}, on actor ${self.path} responding to ${sender.path}")
      context.become(waitCallback(sender()))

      val responseCallback: Consumer[ImapAsyncResponse] = responses => {
        import collection.JavaConverters._

        val responsesList = ImapResponses(responses.getResponseLines.asScala.to[Seq])
        logger.trace(s"On response for $userId :\n ${responsesList.mkString("\n")}")
        self !  Response.UnsubscribeResponse(responsesList)
      }
      val errorCallback: Consumer[Exception] = e => {
        logger.trace(s"${getClass.getSimpleName} command failed", e)
        logger.error(s"${getClass.getSimpleName} command failed")
        sender ! e
        context.stop(self)
      }

      val future = session.execute(new UnsubscribeFolderCommand(mailbox))
      future.setDoneCallback(responseCallback)
      future.setExceptionCallback(errorCallback)
  }

  def waitCallback(sender: ActorRef): Receive = {
    case msg@Response.UnsubscribeResponse(_) =>
      logger.trace(s"UnsubscribeHandler respond to ${sender.path} with $msg")
      sender ! msg
      context.stop(self)
  }
}
