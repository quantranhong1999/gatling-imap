package com.linagora.gatling.imap.protocol.command

import java.util.function.Consumer

import akka.actor.{ActorRef, Props}
import com.linagora.gatling.imap.protocol._
import com.yahoo.imapnio.async.client.ImapAsyncSession
import com.yahoo.imapnio.async.request.CreateFolderCommand
import com.yahoo.imapnio.async.response.ImapAsyncResponse
import io.gatling.core.akka.BaseActor

import scala.collection.immutable.Seq

object CreateFolderHandler {
  def props(session: ImapAsyncSession) = Props(new CreateFolderHandler(session))
}

class CreateFolderHandler(session: ImapAsyncSession) extends BaseActor {
  override def receive: Receive = {
    case Command.CreateFolder(userId, mailbox) =>
      logger.trace(s"CreateFolderHandler for user : ${userId.value}, on actor ${self.path} responding to ${sender.path}")
      context.become(waitCallback(sender()))

      val responseCallback: Consumer[ImapAsyncResponse] = responses => {
        import collection.JavaConverters._

        val responsesList = ImapResponses(responses.getResponseLines.asScala.to[Seq])
        logger.trace(s"On response for $userId :\n ${responsesList.mkString("\n")}")
        self !  Response.CreateFolderResponse(responsesList)
      }
      val errorCallback: Consumer[Exception] = e => {
        logger.trace(s"${getClass.getSimpleName} command failed", e)
        logger.error(s"${getClass.getSimpleName} command failed")
        sender ! e
        context.stop(self)
      }

      val future = session.execute(new CreateFolderCommand(mailbox))
      future.setDoneCallback(responseCallback)
      future.setExceptionCallback(errorCallback)
  }

  def waitCallback(sender: ActorRef): Receive = {
    case msg@Response.CreateFolderResponse(_) =>
      logger.trace(s"CreateFolderHandler respond to ${sender.path} with $msg")
      sender ! msg
      context.stop(self)
  }
}
