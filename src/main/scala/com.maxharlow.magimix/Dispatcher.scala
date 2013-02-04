package com.maxharlow.magimix

import org.scalatra.ScalatraServlet

class Dispatcher extends ScalatraServlet {

  get("/index/*") {
    val contentId = params("splat")
    Indexer.index(contentId)
    "ok!"
  }

}