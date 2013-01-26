package com.maxharlow.magimix

import org.scalatra.ScalatraServlet

class Dispatcher extends ScalatraServlet {

  get("/") {
    "hello"
  }

}