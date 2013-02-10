package com.maxharlow.magimix

import com.maxharlow.magimix.Configuration._
import dispatch._
import net.liftweb.json._
import org.jsoup.Jsoup
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
import com.hp.hpl.jena.vocabulary.DC_11
import java.io.StringWriter

object Indexer {

  case class Content(uri: String, body: String)

  def guardianContent = host("content.guardianapis.com")
  def dbpediaSpotlight = host("spotlight.dbpedia.org")
  def triplestore = url(triplestoreUri)

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

  private def retrieveContent(contentId: String): Promise[String] = {
    val parameters = Map("api-key" -> guardianContentApiKey, "show-fields" -> "body")
    val request = guardianContent / contentId <<? parameters
    Http(request OK as.String) onFailure { case e => throw e }
  }

  private def parseContent(contentResponse: String): Content = {
    implicit val formats = DefaultFormats
    val json = parse(contentResponse)
    val uri = (json \\ "webUrl").extract[String]
    val body = (json \\ "body").extract[String]
    val bodyText = Jsoup.parseBodyFragment(body).text()
    Content(uri, bodyText)
  }

  private def retrieveAnnotation(text: String): Promise[xml.Elem] = {
    val parameters = Map("text" -> text,
        "confidence" -> dbpediaSpotlightConfidence.toString,
        "support" -> dbpediaSpotlightSupport.toString,
        "spotter" -> dbpediaSpotlightSpotter,
        "disambiguator" -> dbpediaSpotlightDisambiguator,
        "policy" -> dbpediaSpotlightPolicy,
        "types" -> dbpediaSpotlightTypes)
    val headers = Map("content-type" -> "application/x-www-form-urlencoded")
    val request = dbpediaSpotlight / "rest" / "annotate" << parameters <:< headers
    Http(request OK as.xml.Elem) onFailure { case e => throw e }
  }

  private def parseAnnotation(annotationResponse: xml.Elem): Set[String] = {
    (annotationResponse \\ "Resource").map(_ \ "@URI" text).toSet
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

  private def storeModel(model: Model): Promise[String] = {
    val out = new StringWriter
    model write out
    val graph = "http://www.guardian.co.uk/"
    val parameters = Map("graph" -> graph, "data" -> out.toString)
    val request = triplestore / "data/" << parameters
    Http(request OK as.String) onComplete {
      case Right(_) => println("Stored model for: " + model.listSubjects.nextResource.getURI)
      case Left(e) => throw e
    }
  }

}