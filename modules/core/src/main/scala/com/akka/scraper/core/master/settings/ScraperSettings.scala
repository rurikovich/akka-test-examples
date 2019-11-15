package com.akka.scraper.core.master.settings

import java.util.Date

import scala.concurrent.duration.{FiniteDuration, _}

object ScraperSettings {

  val visitLinksPeriod: FiniteDuration = 10 days

  val allocationReservePeriod: FiniteDuration = 10 minutes

  def nextVisiteDate: Date = nextDate(visitLinksPeriod)

  def nextAllocateDate: Date = nextDate(allocationReservePeriod)

  private def nextDate(period: FiniteDuration) = {
    new Date(now.getTime + period.toMillis)
  }

  private def now = new Date

}
