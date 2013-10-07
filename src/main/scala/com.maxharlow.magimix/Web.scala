package com.maxharlow.magimix

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

object Web extends App {

  val server = new Server(8080)
  val context = new ServletContextHandler(server, "/")

  context.addServlet(classOf[Dispatcher], "/*")

  server.start()
  server.join()

}







