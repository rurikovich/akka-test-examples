package com.akka.scraper.core.model

import java.util.Date

case class PageLink(link: String, nextVisitDate: Option[Date] = Some(new Date), nextAllocateDate: Option[Date] = Some(new Date))

object PageLink {

  val linkFieldName = "link"

  val nextVisitDateFieldName = "nextVisitDate"

  val nextAllocateDateFieldName = "nextAllocateDate"

}
