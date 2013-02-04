package com.maxharlow.magimix

import dispatch._
import net.liftweb.json._

object Indexer {

  val guardianContentApiKey = "techdev-internal"

  case class Content(id: String, headline: String, body: String)

  def guardianContentApi = host("content.guardianapis.com")

  def index(contentId: String) {
    val content = for (response <- retrieveContent(contentId)) parseContent(response)
    println(content)
  }

  private def retrieveContent(contentId: String): Promise[Either[Throwable, String]] = {
    val parameters = Map(("api-key" -> guardianContentApiKey), ("show-fields" -> "headline,body"))
    val request = guardianContentApi / contentId <<? parameters
    Http(request OK as.String).either
  }

  private def parseContent(contentResponse: Either[Throwable, String]): Content = {
    implicit val formats = DefaultFormats
    contentResponse match {
      case Right(json) => {
        val parsedJson = parse(json)
        val id = (parsedJson \\ "contentId").extract[String]
        val headline = (parsedJson \\ "headline").extract[String]
        val body = (parsedJson \\ "body").extract[String]
        Content(id, headline, body)
      }
      case Left(e) => throw e
    }
  }

}