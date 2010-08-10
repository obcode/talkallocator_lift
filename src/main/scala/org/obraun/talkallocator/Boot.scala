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

import net.liftweb._
import util._
import common._
import http._
import sitemap._
import Loc._
import mapper._

import org.obraun.talkallocator.model._

class Boot extends Bootable {
  def boot {
    val vendor =
      new StandardDBVendor(
        "org.h2.Driver",
        "jdbc:h2:talkallocator.db;AUTO_SERVER=TRUE",
        Empty, Empty)

    LiftRules.unloadHooks.append(
      vendor.closeAllConnections_! _)

    DB.defineConnectionManager(
      DefaultConnectionIdentifier, vendor)

    Schemifier.schemify(true, Schemifier.infoF _, User, Talk)

    LiftRules.addToPackages("org.obraun.talkallocator")

    val ifLoggedIn = If(() => User.loggedIn_?, () => RedirectResponse("/index"))
    val ifAdmin = If(() => User.superUser_?, () => RedirectResponse("/index"))
    val entries = List(
      Menu.i("Home") / "index",
      Menu(Loc("Add", List("add"), "Talk hinzufügen / löschen", ifAdmin)),
      Menu(Loc("Choose", List("choose"), "Talk auswählen", ifLoggedIn))
      ) ::: User.sitemap

    LiftRules.setSiteMap(SiteMap(entries:_*))

    User.createExampleUsers()
    Talk.createExampleTalks()

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
    LiftRules.loggedInTest = Full(() => User.loggedIn_?)
    S.addAround(DB.buildLoanWrapper)
  }
}
// vim: set ts=2 sw=4 et:
