package com.maxharlow.magimix

import org.scalatra.{ScalatraServlet, Accepted}

class Dispatcher extends ScalatraServlet {

  put("/index/*") {
    val contentId = params("splat")
    Indexer.index(contentId)
    Accepted()
  }

  post("/index") {
    val query = params.getOrElse("query", "")
    Indexer.indexAll(query)
    Accepted()
  }

}