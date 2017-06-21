package poldo;



import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


public class XMLAnalizer {

    String outputPrefix;


    int outputNumber = 1;

    Model model = ModelFactory.createDefaultModel();


    /**
     * Class for building the RDF model of the xml document
     * @param document xml
     */
    public Model modelBuilder(Document document, String prefix) {

        outputPrefix = prefix;

        // get first element (root) of xml
        Node root = document.getFirstChild();

        //analize the content of the node
        analizeNode(root, null);

		/*
		//output of the method (turtle)
		try {
			FileOutputStream fileOutputStream = new FileOutputStream("file.ttl");
			model.write(fileOutputStream, "Turtle");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/

        return model;
    }

    /**
     * Analize the content of an xml node
     * @param node Node to analize
     * @param fatherURI URI of the father node (null for root)
     */
    public void analizeNode(Node node, String fatherURI){

        String resourceURI = null;

        //if node is not #text, create a RDF resource
        if(node.getNodeType() != Node.TEXT_NODE){
            //check if resource already exists
            if (fatherURI!=null){
                if(isSonAlreadyPresent(fatherURI, node.getNodeName())){
                    return;
                }
            }

            //create an rdf resource for the node under analysis
            resourceURI = outputPrefix + Endpoint.OUTPUT_URI_STRING + outputNumber;
            Resource resource = model.createResource(resourceURI);
            resource.addProperty(RDFS.label, node.getNodeName());
            //increase counter
            outputNumber++;

            //link new resource to his father. Father became a rdf:bag
            if (fatherURI!=null){
                model.createResource(fatherURI).addProperty(model.createProperty(Endpoint.LI_PROPERTY), resource);
                model.createResource(fatherURI).addProperty(RDF.type, RDF.Bag);
            } else {
                //if there is no father, we are analyzing the root.
                //We link the root to the service
                model.createResource(outputPrefix).addProperty(model.createProperty(Endpoint.OUTPUT_PROPERTY), resource);
            }

            //link the new output resource to the service
            resource.addProperty(model.createProperty(Endpoint.IS_RELATED_TO_SERVICE), model.createResource(outputPrefix));

            //if there are attributes, i read them
            if (node.hasAttributes()) {
                for (int k = 0; k < node.getAttributes().getLength(); k++) {
                    Node attribute = node.getAttributes().item(k);
                    //add resource for the attribute
                    Resource attributeResource = model.createResource(outputPrefix + Endpoint.OUTPUT_URI_STRING + outputNumber);

                    //link the new attribute to the service
                    attributeResource.addProperty(model.createProperty(Endpoint.IS_RELATED_TO_SERVICE), model.createResource(outputPrefix));

                    attributeResource.addProperty(RDFS.label, attribute.getNodeName());
                    attributeResource.addLiteral(model.createProperty(Endpoint.ISDATA_PROPERTY), true);
                    if (attribute.getTextContent().matches("[-]?+[0-9]+[\\.,]?[0-9]*")) {
                        attributeResource.addProperty(model.createProperty(Endpoint.CONTENT_PROPERTY), Endpoint.NUMBER_CONTENT);
                    } else if (node.getTextContent().equalsIgnoreCase("true")||node.getTextContent().equalsIgnoreCase("false")) {
                        model.createResource(fatherURI).addProperty((model.createProperty(Endpoint.CONTENT_PROPERTY)), Endpoint.BOOLEAN_CONTENT);
                    } else {
                        attributeResource.addProperty(model.createProperty(Endpoint.CONTENT_PROPERTY), Endpoint.STRING_CONTENT);
                    }
                    outputNumber++;
                    //add property between node and attribute
                    resource.addProperty(model.createProperty(Endpoint.ATTRIBUTE_PROPERTY), attributeResource);

                }
            }
        }



        //if node hasn't children, it may contains #text
        if(!node.hasChildNodes()){
            //check if text contains only spaces
            if (node.getTextContent().trim().length()>0) {
                model.createResource(fatherURI).addLiteral(model.createProperty(Endpoint.ISDATA_PROPERTY), true);
                if (node.getTextContent().matches("[-]?+[0-9]+[\\.,]?[0-9]*")){
                    model.createResource(fatherURI).addProperty((model.createProperty(Endpoint.CONTENT_PROPERTY)), Endpoint.NUMBER_CONTENT);
                } else if (node.getTextContent().equalsIgnoreCase("true")||node.getTextContent().equalsIgnoreCase("false")) {
                    model.createResource(fatherURI).addProperty((model.createProperty(Endpoint.CONTENT_PROPERTY)), Endpoint.BOOLEAN_CONTENT);
                } else {
                    model.createResource(fatherURI).addProperty((model.createProperty(Endpoint.CONTENT_PROPERTY)), Endpoint.STRING_CONTENT);
                }
            }
        } else {
            //if node has children -> recursive call
            for (int i=0; i<node.getChildNodes().getLength(); i++) {
                Node son = node.getChildNodes().item(i);
                analizeNode(son, resourceURI);
            }
        }
    }

    /**
     * Check if another resource with same parent and same label is already present in the RDF model
     * @param fatherURI URI of the father (whitout angle brackets)
     * @param label String representing the label of the child
     * @return true if resource is already present, false if it is not present
     */
    public boolean isSonAlreadyPresent (String fatherURI, String label){
        String queryString = "select ?o "
                + "where { <" +
                fatherURI+"> <" + Endpoint.LI_PROPERTY + "> ?o . " +
                "?o <" + RDFS.label + "> \"" + label + "\" }";
        //System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet result = qexec.execSelect();
        if (!result.hasNext()){
            return false;
        } else {
            return true;
        }
    }

}
