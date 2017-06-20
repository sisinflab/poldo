package poldo;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class PropertiesFinder {

    Model model;

    ServletContext context;

    Model lovModel;
    Model dboModel;

    // key: uri - value: list of rdf class associated to that uri
    HashMap<String, ArrayList<String>> hashMapUriClass;

    public String suggest(Model m, ServletContext c) {

        hashMapUriClass = new HashMap<>();
        model = m;
        context = c;

        JSONObject jsonObject = new JSONObject();
        JSONArray suggestedPropertiesArray = new JSONArray();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos, "turtle");

        jsonObject.put("model", baos.toString());


        //read linked open vocabulary dump file
        File fileModelLov = new File(context.getRealPath(Endpoint.LOV_PATH));

        try {
            byte[] encModel = Files.readAllBytes(Paths.get(fileModelLov.getPath()));
            String modelString = new String (encModel);

            lovModel = ModelFactory.createDefaultModel();
            lovModel.read(new ByteArrayInputStream(modelString.getBytes()), null, "TTL");

        } catch (IOException e) {
            e.printStackTrace();
        }


        //read dbpedia ontology dump file
        File fileModelDbo = new File(context.getRealPath(Endpoint.DBO_PATH));

        try {
            byte[] encModel = Files.readAllBytes(Paths.get(fileModelDbo.getPath()));
            String modelString = new String (encModel);

            dboModel = ModelFactory.createDefaultModel();
            dboModel.read(new ByteArrayInputStream(modelString.getBytes()), null, "TTL");

        } catch (IOException e) {
            e.printStackTrace();
        }


        //write hashMapUriClass
        getUriAndClass();


        if (hashMapUriClass.size() > 1) {
            //iterator for subjects
            Iterator<String> iteratorUriClassSubject = hashMapUriClass.keySet().iterator();
            while (iteratorUriClassSubject.hasNext()) {
                String subjectUri = iteratorUriClassSubject.next();
                String subjectLabel = this.getParamNameOfResource(subjectUri, model);
                String subjectPath = "/inputs/";
                if (subjectLabel==null) {
                    subjectPath = ExtractValueFromXML.getExpression(model, subjectUri);
                    subjectLabel = ExtractValueFromXML.getLabelOfResource(subjectUri, model);
                }
                //iterator for objects
                Iterator<String> iteratorUriClassObject = hashMapUriClass.keySet().iterator();
                while (iteratorUriClassObject.hasNext()) {
                    String objectUri = iteratorUriClassObject.next();
                    String objectLabel = this.getParamNameOfResource(objectUri, model);
                    String objectPath = "/inputs/";
                    if (objectLabel==null) {
                        objectPath = ExtractValueFromXML.getExpression(model, objectUri);
                        objectLabel = ExtractValueFromXML.getLabelOfResource(objectUri, model);
                    }
                    //if subject!=object, search for possible properties to link the resources
                    if (!subjectUri.equalsIgnoreCase(objectUri)) {
                        ArrayList<String> propertiesArray = new ArrayList<>();
                        for (int indexClassSubject = 0; indexClassSubject < hashMapUriClass.get(subjectUri).size(); indexClassSubject++) {
                            for (int indexClassObject = 0; indexClassObject < hashMapUriClass.get(objectUri).size(); indexClassObject++) {
                                String subjectClass = hashMapUriClass.get(subjectUri).get(indexClassSubject);
                                String objectClass = hashMapUriClass.get(objectUri).get(indexClassObject);
                                propertiesArray.addAll(getProperties(subjectClass, objectClass));
                            }
                        }

                        //put resources into a json to return to the client
                        if (propertiesArray.size()>0) {
                            JSONObject suggestedPropertiesObject = new JSONObject();

                            suggestedPropertiesObject.put("subjectUri", subjectUri);
                            suggestedPropertiesObject.put("subjectPath", subjectPath);
                            suggestedPropertiesObject.put("subjectLabel", subjectLabel);
                            suggestedPropertiesObject.put("objectUri", objectUri);
                            suggestedPropertiesObject.put("objectPath", objectPath);
                            suggestedPropertiesObject.put("objectLabel", objectLabel);
                            suggestedPropertiesObject.put("properties", propertiesArray);

                            suggestedPropertiesArray.put(suggestedPropertiesObject);
                        }

                    }
                }
            }

            jsonObject.put("properties", suggestedPropertiesArray);


        }

        return jsonObject.toString();
    }

    //method to obtain all the classes (rdf:type) in the model
    public void getUriAndClass() {
        String queryStr = "select ?uri ?class where { "
                + "?uri <" + RDF.type + "> ?class }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            String uriString = solution.getResource("uri").toString();
            Resource classResource = solution.getResource("class");
            String classString = classResource.toString();

            //do not add to hashMap if class is owl:DatatypeProperty or RDF.Bag
            if (!classResource.equals(OWL.DatatypeProperty) &
                    !classResource.equals(RDF.Bag)) {
                if (hashMapUriClass.containsKey(uriString)) {
                    hashMapUriClass.get(uriString).add(classString);
                } else {
                    ArrayList<String> valueList = new ArrayList<>();
                    valueList.add(classString);
                    hashMapUriClass.put(uriString, valueList);
                }
            }

        }
    }

    public ArrayList<String> getProperties(String subjectClass, String objectClass) {
        ArrayList<String> propertiesArray = new ArrayList<>();

        //search for properties in DBpedia Ontology and write into propertiesArray
        propertiesArray.addAll(getPropertiesFromModel(subjectClass, objectClass, dboModel));

        //search for properties in LOV and add to propertiesArray
        for (String x: getPropertiesFromModel(subjectClass, objectClass, lovModel)){
            if (!propertiesArray.contains(x)){
                propertiesArray.add(x);
            }
        }
        //propertiesArray.addAll(getPropertiesLinkedOpenVocabulary(subjectClass, objectClass));

        //TODO using add it's possible to add other methods to find properties

        return propertiesArray;
    }

    /**
     * Not used because it's too slow
     * @param subjectClass
     * @param objectClass
     * @return
     */
    public ArrayList<String> getPropertiesDbpedia(String subjectClass, String objectClass) {
        ArrayList<String> propertiesArray = new ArrayList<>();

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT DISTINCT ?p " +
                "WHERE { " +
                "{ " +
                "?p rdfs:domain <" + subjectClass + "> . " +
                "?p rdfs:range <" + objectClass + "> " +
                "} " +

                /*
                "union " +
                "{ " +
                "<" + subjectClass + "> rdfs:subClassOf ?classA . " +
                "?p rdfs:domain ?classA . " +
                "?p rdfs:range <" + objectClass + "> " +
                "} " +
                "union " +
                "{ " +
                "<" + objectClass + "> rdfs:subClassOf ?classB . " +
                "?p rdfs:domain <" + subjectClass + "> . " +
                "?p rdfs:range ?classB " +
                "} " +
                "union " +
                "{ " +
                "<" + subjectClass + "> rdfs:subClassOf ?classA . " +
                "<" + objectClass + "> rdfs:subClassOf ?classB . " +
                "?p rdfs:domain ?classA . " +
                "?p rdfs:range ?classB " +
                "} " +
                */

               /*
               TOO SLOW
               "union " +
                "{ " +
                "?s a <" + subjectClass + "> . " +
                "?o a <" + objectClass + "> . " +
                "?s ?p ?o " +
                "} " +*/

                /*
               TOO SOLW
                "union " +
                "{ " +
                "<" + subjectClass + "> rdfs:subClassOf ?classA . " +
                "?s a ?classA . " +
                "?o a <" + objectClass + "> . " +
                "?s ?p ?o " +
                "} " +
                "union " +
                "{ " +
                "<" + objectClass + "> rdfs:subClassOf ?classB . " +
                "?s a <" + subjectClass + "> . " +
                "?o a ?classB . " +
                "?s ?p ?o " +
                "} " +
                "union " +
                "{ " +
                "<" + subjectClass + "> rdfs:subClassOf ?classA . " +
                "<" + objectClass + "> rdfs:subClassOf ?classB . " +
                "?s a ?classA . " +
                "?o a ?classB . " +
                "?s ?p ?o " +
                "} " +
                */
                "}";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        ResultSet result = qexec.execSelect();

        for (; result.hasNext(); ) {
            QuerySolution solution = result.nextSolution();
            Resource r = solution.getResource("p");
            propertiesArray.add(r.toString());
            System.out.println(r.toString());

        }
        return propertiesArray;
    }

    //use rdfs:domain and rdfs:range to obtain some possible properties for linking subject and object
    public ArrayList<String> getPropertiesFromModel(String subjectClass, String objectClass, Model model) {
        ArrayList<String> propertiesArray = new ArrayList<>();

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT DISTINCT ?p " +
                "WHERE { " +
                "?p rdfs:domain <" + subjectClass + "> . " +
                "?p rdfs:range <" + objectClass + "> " +
                "}";

        Query query = QueryFactory.create(queryString);

        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        for (; result.hasNext(); ) {
            QuerySolution solution = result.nextSolution();
            Resource r = solution.getResource("p");
            propertiesArray.add(r.toString());
            System.out.println(r.toString());

        }

        return propertiesArray;
    }

    public String getParamNameOfResource(String resourceURI, Model model) {
        String queryStr = "select ?label where { "
                + "<" + resourceURI + "> <" + Endpoint.DEFAULT_NAMESPACE + Endpoint.PARAM_NAME + "> ?label }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Literal label = solution.getLiteral("label");
            return label.toString();
        } else {
            return null;
        }
    }


}
