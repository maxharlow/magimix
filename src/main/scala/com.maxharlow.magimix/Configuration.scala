package com.maxharlow.magimix

import com.typesafe.config.ConfigFactory

object Configuration {

  private val config = ConfigFactory.load
  config.checkValid(ConfigFactory.defaultReference)

  val guardianContentApiKey = config getString "guardian-content.api-key"

  val dbpediaSpotlightConfidence = config getDouble "dbpedia-spotlight.confidence"
  val dbpediaSpotlightSupport = config getInt "dbpedia-spotlight.support"
  val dbpediaSpotlightSpotter = config getString "dbpedia-spotlight.spotter"
  val dbpediaSpotlightDisambiguator = config getString "dbpedia-spotlight.disambiguator"
  val dbpediaSpotlightPolicy = config getString "dbpedia-spotlight.policy"
  val dbpediaSpotlightTypes = config getString "dbpedia-spotlight.types"

  val triplestoreUri = config getString "triplestore.uri"

}
