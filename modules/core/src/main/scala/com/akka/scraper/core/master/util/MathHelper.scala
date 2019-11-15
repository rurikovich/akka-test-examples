package com.akka.scraper.core.master.util

trait MathHelper {

  def listsDiff(set1: List[String], set2: List[String]): List[String] = set1.filterNot(set2.toSet)

  def filterDublicates(linksToAdd: List[String]): List[String] = linksToAdd.distinct

}
