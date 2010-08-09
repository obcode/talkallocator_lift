/**
 * Copyright (c) 2010 Oliver Braun
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the author nor the names of his contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.obraun.talkallocator
package snippet

import scala.xml._

import net.liftweb._
import mapper._
import util.Helpers._
import common._
import http.S._
import http.SHtml._

import model._

object Talks {

  def available = talksAsTable(true)

  def allocated = talksAsTable(false)

  def talksAsTable(available: Boolean) = {
    def speaker(speakerID: MappedLong[Talk]) = {
      val speaker = User.find(
        By(User.id,speakerID)
      ).get
      Text(speaker.firstName+" "+speaker.lastName)
    }

    val talks = Talk.findAll(
      if (available) NullRef(Talk.speaker)
      else NotNullRef(Talk.speaker)
    )

    <table>
    { talks.map{
        talk =>
          <tr>
            <th>{talk.title}</th>
            {if (!available)
               <th width="50%">
                 ({speaker(talk.speaker)})
               </th>
            }
          </tr>
      }
    }
    </table>
  }

  def add(html: NodeSeq) = {
    var title = ""
    def addTalk(title: String) = {
      if (title!="" &&
          Talk.find(By(Talk.title,title)).isEmpty) {
        Talk.create.title(title).save
      }
    }
    bind("talk",html,
      "title" -> text("",
                      t => title = t.trim),
      "add" -> submit("Hinzufügen",
                      () => addTalk(title))
    )
  }

  def delete = {
    import scala.collection.mutable.Set
    val toDelete = Set[Talk]()
    val talks = Talk.findAll

    def deleteTalks(toDelete: Set[Talk]) {
      toDelete.foreach {
        talk  =>
          if (!talk.delete_!)
            error("Could not delete :"+talk.toString)
      }
    }

    val checkboxes = talks.flatMap(talk =>
      checkbox(
        false,
        if (_) toDelete += talk
      ) :+
      Text(talk.title) :+
      <br />
    )
    val delete = submit(
      "Löschen",
      () => deleteTalks(toDelete)
    )
    checkboxes ++ delete
  }

  def choose = {
    val user = User.currentUser.open_!
    val chosen = Talk.findAll(By(Talk.speaker,user.id))
    val available = Talk.findAll(NullRef(Talk.speaker))
    var newTitle: Option[String] = None

    def chooseTalk(maybeTitle: Option[String]) = {
      val hasOld = !chosen.isEmpty
      maybeTitle match {
        case None if hasOld =>
          chosen.head.speaker(Empty).save
        case Some(title) =>
          Talk.find(By(Talk.title,title)) match {
            case Full(talk) =>
              if (hasOld) {
                val old = chosen.head
                if (old.title != talk.title) {
                  old.speaker(Empty).save
                  talk.speaker(user.id).save
                }
              } else talk.speaker(user.id).save
            case _ => error("Talk "+ title+"not found")
            }
        case _ =>
      }
      redirectTo("/")
    }

    val talks = radio(
      (chosen:::available).map{ _.title.toString },
      if (chosen.isEmpty)
        Empty
      else
        Full(chosen.head.title),
      title => newTitle = Some(title)
    ).toForm
    val choose = submit(
      "Auswählen",
      () => chooseTalk(newTitle)
    )
    val chooseNone = submit(
      "Keinen Talk übernehmen",
      () => chooseTalk(None)
    )
    talks :+ choose :+ chooseNone
  }
}

// vim: set ts=2 sw=4 et:
