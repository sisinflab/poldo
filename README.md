# PoLDo
A tool for exposing the deep Web in the Linked Data cloud.

With PoLDo you can run SPARQL query over non-RDF data; 
in order to do this, you have to provide a mapping file for describing one or more external API services.

PoLDo mapping file allows the designer to create a link between URIs contained in the SPARQL queries 
processed by the engine and, at the same time, to enrich their semantics by explicitly 
adding information about the corresponding OWL class or property that can be defined also 
in an external vocabulary (e.g. DBpedia).

Once the mapping file has been generated, users can run SPARQL queries. 
PoLDo engine will extract constants from the query graph pattern 
(literal values or labels obtained from resource URIs), 
than it will call all the available API services and it will build an RDF-cache 
with the exctracted data observing the rules contained in the mapping file.
Finally the user SPARQL query will be evaluated over the cache.

## PoLDo client
In order to consume PoLDo engine, a client view has been deployed; its source code is available here: https://github.com/sisinflab/poldo-client
