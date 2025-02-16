package com.linagora.gatling.imap.action

import akka.actor.Props
import com.linagora.gatling.imap.check.ImapCheck
import com.linagora.gatling.imap.protocol.{Command, UserId}
import io.gatling.commons.validation.Validation
import io.gatling.core.action.ValidatedActionActor
import io.gatling.core.session._

import scala.collection.immutable.Seq

object RenameFolderAction {
  def props(imapContext: ImapActionContext, requestName: String, checks: Seq[ImapCheck], oldFolder: Expression[String], newFolder: Expression[String]) =
    Props(new RenameFolderAction(imapContext, requestName, checks, oldFolder, newFolder))
}

class RenameFolderAction(val imapContext: ImapActionContext,
                         val requestName: String,
                         override val checks: Seq[ImapCheck],
                         oldFolder: Expression[String],
                         newFolder: Expression[String]) extends ValidatedActionActor with ImapActionActor {

  override protected def executeOrFail(session: Session): Validation[_] = {
    for {
      oldFolder <- oldFolder(session)
      newFolder <- newFolder(session)
    } yield {
      val id: Long = session.userId
      val handler = handleResponse(session, imapContext.clock.nowMillis)
      sessions.tell(Command.RenameFolder(UserId(id), oldFolder, newFolder), handler)
    }
  }
}
