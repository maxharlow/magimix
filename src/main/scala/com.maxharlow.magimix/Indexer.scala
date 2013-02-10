package com.maxharlow.magimix

import dispatch._
import net.liftweb.json._
import org.jsoup.Jsoup
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
import com.hp.hpl.jena.vocabulary.DC_11
import java.io.StringWriter

object Indexer {

  val guardianContentApiKey = "techdev-internal"

  case class Content(uri: String, body: String)

  def guardianContent = host("content.guardianapis.com")
  def dbpediaSpotlight = host("spotlight.dbpedia.org")
  def triplestore = host("localhost", 8000)

  def index(contentId: String) {
    for {
      contentResponse <- retrieveContent(contentId)
      content = parseContent(contentResponse)
      annotationResponse <- retrieveAnnotation(content.body)
      entityUris = parseAnnotation(annotationResponse)
      model = createModel(content, entityUris)
      _ <- deleteModel(content.uri)
    }
    yield storeModel(model)
  }

  private def retrieveContent(contentId: String): Promise[Either[Throwable, String]] = {
    val parameters = Map("api-key" -> guardianContentApiKey, "show-fields" -> "body")
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

  private def retrieveAnnotation(text: String): Promise[Either[Throwable, xml.Elem]] = {
    val parameters = Map("text" -> text,
        "confidence" -> "0.2",
        "support" -> "20",
        "spotter" -> "Default",
        "disambiguator" -> "Default",
        "policy" -> "whitelist",
        "types" -> "Person,Organisation,Place")
    val headers = Map("content-type" -> "application/x-www-form-urlencoded")
    val request = dbpediaSpotlight / "rest" / "annotate" << parameters <:< headers
    Http(request OK as.xml.Elem).either
  }

  private def parseAnnotation(annotationResponse: Either[Throwable, xml.Elem]): Set[String] = {
    annotationResponse match {
      case Right(xml) => {
        (xml \\ "Resource").map(_ \ "@URI" text).toSet
      }
      case Left(e) => throw e
    }
  }

  private def createModel(content: Content, entityUris: Set[String]): Model = {
    val model = ModelFactory.createDefaultModel
    val contentResource = model.createResource(content.uri)
    for (entityUri <- entityUris) {
      contentResource.addProperty(DC_11.subject, model.createResource(entityUri))
    }
    model
  }

  private def deleteModel(contentUri: String): Promise[String] = {
    val sparql = "DELETE { <%s> ?p ?o } { ?s ?p ?o }" format contentUri
    val parameters = Map("update" -> sparql)
    val request = triplestore / "update/" << parameters
    Http(request OK as.String) onFailure { case e => throw e }
  }

  private def storeModel(model: Model) {
    val out = new StringWriter
    model write out
    val graph = "http://www.guardian.co.uk/"
    val parameters = Map("graph" -> graph, "data" -> out.toString)
    val request = triplestore / "data/" << parameters
    Http(request OK as.String).either onComplete {
      case Right(_) => println("Stored model for: " + model.listSubjects.nextResource.getURI)
      case Left(e) => throw e
    }
  }

}