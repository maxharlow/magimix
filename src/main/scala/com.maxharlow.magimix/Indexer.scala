package com.maxharlow.magimix

import dispatch._
import net.liftweb.json._
import org.jsoup.Jsoup

object Indexer {

  val guardianContentApiKey = "techdev-internal"

  case class Content(uri: String, body: String)

  def guardianContent = host("content.guardianapis.com")
  def dbpediaSpotlight = host("spotlight.dbpedia.org")

  def index(contentId: String) {
    for {
      contentResponse <- retrieveContent(contentId)
      content = parseContent(contentResponse)
      entitiesResponse <- retrieveEntities(content.body)
      entities = parseEntities(entitiesResponse)
    }
    yield entities
  }

  private def retrieveContent(contentId: String): Promise[Either[Throwable, String]] = {
    val parameters = Map(("api-key" -> guardianContentApiKey), ("show-fields" -> "body"))
    val request = guardianContent / contentId <<? parameters
    Http(request OK as.String).either
  }

  private def parseContent(contentResponse: Either[Throwable, String]): Content = {
    implicit val formats = DefaultFormats
    contentResponse match {
      case Right(json) => {
        val parsedJson = parse(json)
        val uri = (parsedJson \\ "webUrl").extract[String]
        val body = (parsedJson \\ "body").extract[String]
        val bodyText = Jsoup.parseBodyFragment(body).text()
        Content(uri, bodyText)
      }
      case Left(e) => throw e
    }
  }

  private def retrieveEntities(text: String): Promise[Either[Throwable, xml.Elem]] = {
    val parameters = Map(("text" -> text),
        ("confidence" -> "0.2"),
        ("support" -> "20"),
        ("spotter" -> "Default"),
        ("disambiguator" -> "Default"),
        ("policy" -> "whitelist"),
        ("types" -> "Person,Organisation,Place"))
    val headers = Map(("content-type" -> "application/x-www-form-urlencoded"))
    val request = dbpediaSpotlight / "rest" / "annotate" << parameters <:< headers
    Http(request OK as.xml.Elem).either
  }

  private def parseEntities(entitiesResponse: Either[Throwable, xml.Elem]): Set[String] = {
    entitiesResponse match {
      case Right(xml) => {
        (xml \\ "Resource").map(_ \ "@URI" text).toSet
      }
      case Left(e) => throw e
    }
  }

}