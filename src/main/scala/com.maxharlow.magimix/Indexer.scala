package com.maxharlow.magimix

import com.maxharlow.magimix.Configuration._
import dispatch._
import net.liftweb.json._
import org.jsoup.Jsoup
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
import com.hp.hpl.jena.vocabulary.DC_11
import java.io.StringWriter
import scala.actors.Futures.future

object Indexer {

  case class SourceContent(uri: String, body: String)
  case class AnnotatedContent(subjects: Set[String])

  def guardianContent = host("content.guardianapis.com")
  def dbpediaSpotlight = host("spotlight.dbpedia.org")
  def triplestore = url(triplestoreUri)

  def index(contentId: String) {
    future {
      val result = for {
        content <- retrieveContent(contentId).right
        annotation <- retrieveAnnotation(content.body).right
        _ <- deleteModel(content.uri).right
      }
      yield {
        val model = createModel(content, annotation)
        storeModel(model)
      }
      for (r <- result) r match {
        case Right(_) => println("Indexed content: " + contentId)
        case Left(e) => println("Failed to index content: " + contentId + " -- " + e.getMessage)
      }
    }
  }

  private def retrieveContent(contentId: String): Promise[Either[Throwable, SourceContent]] = {
    val parameters = Map("api-key" -> guardianContentApiKey, "show-fields" -> "body")
    val request = guardianContent / contentId <<? parameters
    Http(request OK as.String).either.right.map(parseContent)
  }

  private def parseContent(contentResponse: String): SourceContent = {
    implicit val formats = DefaultFormats
    val json = parse(contentResponse)
    val uri = (json \\ "webUrl").extract[String]
    val body = (json \\ "body").extract[String]
    val bodyText = Jsoup.parseBodyFragment(body).text()
    SourceContent(uri, bodyText)
  }

  private def retrieveAnnotation(text: String): Promise[Either[Throwable, AnnotatedContent]] = {
    val parameters = Map("text" -> text,
        "confidence" -> dbpediaSpotlightConfidence.toString,
        "support" -> dbpediaSpotlightSupport.toString,
        "spotter" -> dbpediaSpotlightSpotter,
        "disambiguator" -> dbpediaSpotlightDisambiguator,
        "policy" -> dbpediaSpotlightPolicy,
        "types" -> dbpediaSpotlightTypes)
    val headers = Map("content-type" -> "application/x-www-form-urlencoded")
    val request = dbpediaSpotlight / "rest" / "annotate" << parameters <:< headers
    Http(request OK as.xml.Elem).either.right.map(parseAnnotation)
  }

  private def parseAnnotation(annotationResponse: xml.Elem): AnnotatedContent = {
    val subjects = (annotationResponse \\ "Resource").map(_ \ "@URI" text).toSet
    AnnotatedContent(subjects)
  }

  private def createModel(sourceContent: SourceContent, annotatedContent: AnnotatedContent): Model = {
    val model = ModelFactory.createDefaultModel
    val contentResource = model.createResource(sourceContent.uri)
    for (subject <- annotatedContent.subjects) {
      contentResource.addProperty(DC_11.subject, model.createResource(subject))
    }
    model
  }

  private def deleteModel(contentUri: String): Promise[Either[Throwable, String]] = {
    val sparql = "DELETE { <%s> ?p ?o } { ?s ?p ?o }" format contentUri
    val parameters = Map("update" -> sparql)
    val request = triplestore / "update/" << parameters
    Http(request OK as.String).either
  }

  private def storeModel(model: Model): Promise[Either[Throwable, String]] = {
    val out = new StringWriter
    model write out
    val graph = "http://www.guardian.co.uk/"
    val parameters = Map("graph" -> graph, "data" -> out.toString)
    val request = triplestore / "data/" << parameters
    Http(request OK as.String).either
  }

}