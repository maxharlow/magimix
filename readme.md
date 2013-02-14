Magimix
=======

Setup
-----

Requires SBT and 4store, so install those. It would probably work with any triplestore, though I haven't tried.

Set up a new database:

	$ mkdir /var/lib/4store
	$ 4s-backend-setup content

Running
-------

Start the database server and Sparql endpoint:

	$ 4s-backend content
	$ 4s-httpd -p 8000 content

You can check it's running by going to `localhost:8000/status`.

Start Magimix itself:

	$ sbt
	> container:start

New content can be indexed by issuing a PUT to `localhost:8080/index/<ID>` with the content's ID. If that content is already in the index, existing triples will be removed first.
