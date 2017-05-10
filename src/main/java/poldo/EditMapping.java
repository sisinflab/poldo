package poldo;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.OWL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class EditMapping {

    Model model;
    JSONObject jsonObject;

    /**
     * Constructor - Read JSON string and create the model
     * @param s Json String cointaining all informations
     */
    public EditMapping (String s) {
        try {
            jsonObject = new JSONObject (s);
            model = ModelFactory.createDefaultModel();
            model.read(new ByteArrayInputStream(jsonObject.getString("model").getBytes()), null, "TTL");

            Endpoint.DEFAULT_NAMESPACE = model.getNsPrefixURI(Endpoint.PREFIX);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Call the function analizeJSON() and return the edited model
     * @return String representing the TURTLE of the new model
     */
    public String editAndGetTurtleModel(){

        if (model.isEmpty()) {
            return "Error: Unable to read RDF model!";
        }

        analizeJson();

        //Write the model to Output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos, "turtle");

        return baos.toString();
    }

    /**
     * Read the json and call functions to edit/add input/output
     */
    public void analizeJson(){
        //For each object "editInput" in the json, call editInput()
        if (jsonObject.has("editInput")) {
            try {
                JSONArray editInputArray = jsonObject.getJSONArray("editInput");
                for (int i=0; i < editInputArray.length(); i++) {

                    String inputURI;
                    Boolean isRequired;
                    Boolean DatatypeProperty;
                    String classRDF;
                    String fixedValue;

                    //inputURI is required, the others are optional (can be null)
                    if (editInputArray.getJSONObject(i).has("inputURI")){

                        inputURI=editInputArray.getJSONObject(i).getString("inputURI");

                        if (editInputArray.getJSONObject(i).has("isRequired")){
                            isRequired=editInputArray.getJSONObject(i).getBoolean("isRequired");
                        } else {
                            isRequired=null;
                        }

						/*Aggiunto per la modifica DatatypeProperty*/
                        if(editInputArray.getJSONObject(i).has("DatatypeProperty")){
                            DatatypeProperty=editInputArray.getJSONObject(i).getBoolean("DatatypeProperty");
                        } else {
                            DatatypeProperty=null;
                        }

                        if (editInputArray.getJSONObject(i).has("classRDF")){
                            classRDF=editInputArray.getJSONObject(i).getString("classRDF");
                        } else {
                            classRDF=null;
                        }

                        if (editInputArray.getJSONObject(i).has("fixedValue")){
                            fixedValue=editInputArray.getJSONObject(i).getString("fixedValue");
                        } else {
                            fixedValue=null;
                        }

                        editInput(inputURI, isRequired, DatatypeProperty, classRDF, fixedValue);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //For each object "addInput" in the json, call addInput()
        if (jsonObject.has("addInput")) {
            try {
                JSONArray addInputArray = jsonObject.getJSONArray("addInput");
                for (int i=0; i < addInputArray.length(); i++) {

                    String serviceURI;
                    String label;
                    Boolean isRequired;
                    Boolean DatatypeProperty;
                    String classRDF;
                    String fixedValue;

                    //serviceURI, label and numberURI are required, the others are optional
                    if (addInputArray.getJSONObject(i).has("serviceURI") &
                            addInputArray.getJSONObject(i).has("label")){

                        serviceURI = addInputArray.getJSONObject(i).getString("serviceURI");
                        label = addInputArray.getJSONObject(i).getString("label");

                        if (addInputArray.getJSONObject(i).has("isRequired")){
                            isRequired = addInputArray.getJSONObject(i).getBoolean("isRequired");
                        } else {
                            isRequired = null;
                        }

						/*handle DatatypeProperty*/
                        if(addInputArray.getJSONObject(i).has("DatatypeProperty")){
                            DatatypeProperty=addInputArray.getJSONObject(i).getBoolean("DatatypeProperty");
                        } else {
                            DatatypeProperty=null;
                        }

                        if (addInputArray.getJSONObject(i).has("classRDF")){
                            classRDF = addInputArray.getJSONObject(i).getString("classRDF");
                        } else {
                            classRDF = null;
                        }

                        if (addInputArray.getJSONObject(i).has("fixedValue")){
                            fixedValue = addInputArray.getJSONObject(i).getString("fixedValue");
                        } else {
                            fixedValue = null;
                        }

                        addInput(serviceURI, label, isRequired, DatatypeProperty, classRDF, fixedValue);

                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //For each object "editOutput" in the json, call editOutput()
        if (jsonObject.has("editOutput")) {
            try {
                JSONArray editOutputArray = jsonObject.getJSONArray("editOutput");
                for (int i=0; i < editOutputArray.length(); i++) {

                    String outputURI;
                    Boolean isData;
                    Boolean DatatypeProperty;
                    String content;
                    String classRDF;

                    //System.out.println(editOutputArray.getJSONObject(i).toString());

                    //outputURI is required, the others are optional
                    if (editOutputArray.getJSONObject(i).has("outputURI")) {
                        outputURI=editOutputArray.getJSONObject(i).getString("outputURI");

                        if(editOutputArray.getJSONObject(i).has("isData")){
                            isData=editOutputArray.getJSONObject(i).getBoolean("isData");
                        } else {
                            isData=null;
                        }

						/*handle DatatypeProperty*/
                        if(editOutputArray.getJSONObject(i).has("DatatypeProperty")){
                            DatatypeProperty=editOutputArray.getJSONObject(i).getBoolean("DatatypeProperty");
                        } else {
                            DatatypeProperty=null;
                        }

                        if(editOutputArray.getJSONObject(i).has("content")){
                            content=editOutputArray.getJSONObject(i).getString("content");
                        } else {
                            content=null;
                        }

                        if(editOutputArray.getJSONObject(i).has("classRDF")){
                            classRDF=editOutputArray.getJSONObject(i).getString("classRDF");
                        } else {
                            classRDF=null;
                        }

                        editOutput (outputURI, isData, DatatypeProperty, content, classRDF);

                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //For each object "addOutput" in the json, call addOutput()
        if (jsonObject.has("addOutput")) {
            try {
                JSONArray addOutputArray = jsonObject.getJSONArray("addOutput");
                for (int i=0; i < addOutputArray.length(); i++) {

                    String serviceURI;
                    String label;
                    Boolean isData;
                    Boolean DatatypeProperty;
                    String content;
                    String classRDF;
                    String parentURI;

                    //serviceURI, label, numberURI, parentURI are required, the others are optional
                    if (addOutputArray.getJSONObject(i).has("serviceURI") &
                            addOutputArray.getJSONObject(i).has("label") &
                            addOutputArray.getJSONObject(i).has("parentURI")){

                        serviceURI=addOutputArray.getJSONObject(i).getString("serviceURI");
                        label=addOutputArray.getJSONObject(i).getString("label");
                        parentURI=addOutputArray.getJSONObject(i).getString("parentURI");

                        if (addOutputArray.getJSONObject(i).has("isData")){
                            isData = addOutputArray.getJSONObject(i).getBoolean("isData");
                        } else {
                            isData = null;
                        }

						/*handle DatatypeProperty*/
                        if(addOutputArray.getJSONObject(i).has("DatatypeProperty")){
                            DatatypeProperty=addOutputArray.getJSONObject(i).getBoolean("DatatypeProperty");
                        } else {
                            DatatypeProperty=null;
                        }

                        if (addOutputArray.getJSONObject(i).has("content")){
                            content = addOutputArray.getJSONObject(i).getString("content");
                        } else {
                            content = null;
                        }

                        if (addOutputArray.getJSONObject(i).has("classRDF")){
                            classRDF = addOutputArray.getJSONObject(i).getString("classRDF");
                        } else {
                            classRDF = null;
                        }

                        addOutput(serviceURI,label,isData,DatatypeProperty,content,classRDF,parentURI);

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //For each object "addProperty" in the json, call addProperty()
        if (jsonObject.has("addProperty")) {
            try {
                JSONArray addPropertyArray = jsonObject.getJSONArray("addProperty");
                for (int i=0; i < addPropertyArray.length(); i++) {
                    if (addPropertyArray.getJSONObject(i).has("subjectURI") &
                            addPropertyArray.getJSONObject(i).has("propertyURI") &
                            addPropertyArray.getJSONObject(i).has("objectURI")){
                        addProperty(addPropertyArray.getJSONObject(i).getString("subjectURI"),
                                addPropertyArray.getJSONObject(i).getString("propertyURI"),
                                addPropertyArray.getJSONObject(i).getString("objectURI"));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //For each object "addResource" in the json, call addProperty() for each property to be attributed to the new resource
        if (jsonObject.has("addResource")) {
            try {
                JSONArray addResourceArray = jsonObject.getJSONArray("addResource");
                for (int i=0; i < addResourceArray.length(); i++) {

                    //cerco l'uri del servizio - si potrebbe anche evitare mettendolo come campo nel json
                    //String serviceURI = addResourceArray.getJSONObject(i).getString("serviceURI");
                    String serviceURI = getService();

                    //Create a new URI for the new resource
                    int numberURI = firstURIAvailable(serviceURI + "-" + Endpoint.RESOURCE_URI_STRING);
                    String newResourceURI = serviceURI + "-" + Endpoint.RESOURCE_URI_STRING + numberURI;

                    //add type
                    model.createResource(newResourceURI).addProperty(
                            RDF.type,
                            model.createResource(Endpoint.DEFAULT_NAMESPACE + Endpoint.ADDED_RESOURCE));

                    //if the new resource is subject of some property, i add them.
                    if (addResourceArray.getJSONObject(i).has("resourceIsSubjectOf")){
                        JSONArray resourceIsSubjectOfArray = addResourceArray.getJSONObject(i).getJSONArray("resourceIsSubjectOf");
                        for (int resIsSubOfIndex = 0; resIsSubOfIndex < resourceIsSubjectOfArray.length(); resIsSubOfIndex++){
                            if (resourceIsSubjectOfArray.getJSONObject(resIsSubOfIndex).has("propertyURI") &
                                    resourceIsSubjectOfArray.getJSONObject(resIsSubOfIndex).has("objectURI")){
                                addProperty (newResourceURI,
                                        resourceIsSubjectOfArray.getJSONObject(resIsSubOfIndex).getString("propertyURI"),
                                        resourceIsSubjectOfArray.getJSONObject(resIsSubOfIndex).getString("objectURI"));
                            }
                        }
                    }
                    //if the new resource is object of some property, i add them.
                    if (addResourceArray.getJSONObject(i).has("resourceIsObjectOf")){
                        JSONArray resourceIsObjectOfArray = addResourceArray.getJSONObject(i).getJSONArray("resourceIsObjectOf");
                        for (int resIsObjOfIndex = 0; resIsObjOfIndex < resourceIsObjectOfArray.length(); resIsObjOfIndex++){
                            if (resourceIsObjectOfArray.getJSONObject(resIsObjOfIndex).has("subjectURI") &
                                    resourceIsObjectOfArray.getJSONObject(resIsObjOfIndex).has("propertyURI")){
                                addProperty (resourceIsObjectOfArray.getJSONObject(resIsObjOfIndex).getString("subjectURI"),
                                        resourceIsObjectOfArray.getJSONObject(resIsObjOfIndex).getString("propertyURI"),
                                        newResourceURI);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove old properties and add new properties (RDF:type and IS_REQUIRED)
     * @param inputURI String representing the URI of the subject resource
     * @param isRequired Boolean telling if the input is required or optional
     * @param classRDF String representing the URI of the class associated to the resource
     */
    public void editInput(String inputURI, Boolean isRequired, Boolean DatatypeProperty, String classRDF, String fixedValue){
        if(isRequired!=null){
            model.createResource(inputURI).removeAll(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.ISREQUIRED_PROPERTY));
            model.createResource(inputURI).addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.ISREQUIRED_PROPERTY), isRequired);
        }
		/*handle DatatypeProperty*/
        if (DatatypeProperty!=null){
            if (DatatypeProperty!=false){
                model.createResource(inputURI).removeAll(RDF.type);
                model.createResource(inputURI).addProperty(RDF.type, OWL.DatatypeProperty);
                if(classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    model.createResource(inputURI).addProperty(model.createProperty(Endpoint.SAME_PROPERTY_AS), model.createResource(classRDF));
                }
            }else{

                if(classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    model.createResource(inputURI).removeAll(RDF.type);
                    model.createResource(inputURI).addProperty(RDF.type, model.createResource(classRDF));
                }

            }
        } else{

            if(classRDF!=null && !classRDF.equalsIgnoreCase("")){
                model.createResource(inputURI).removeAll(RDF.type);
                model.createResource(inputURI).addProperty(RDF.type, model.createResource(classRDF));
            }

        }
        if(fixedValue!=null){
            if (!fixedValue.equalsIgnoreCase("")) {
                Property hasFixedValueProperty = model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.HAS_FIXED_VALUE);
                model.createResource(inputURI).removeAll(hasFixedValueProperty);
                model.createResource(inputURI).addLiteral(hasFixedValueProperty, fixedValue);
                model.createResource(inputURI).removeAll(RDF.type);
                model.createResource(inputURI).addProperty(RDF.type, model.createResource(inputURI+"key"));
            }
        }
    }

    /**
     * Create new input resource
     * @param serviceURI String representing the URI of the Service associated to the new input
     * @param label String representing the label of the input (in the url ...?label=value&otherlabel=othervalue)
     * @param isRequired Boolean telling if the input is required or optional
     * @param classRDF String representing the URI of the class associated to the resource
     */
    public void addInput(String serviceURI, String label, Boolean isRequired, Boolean DatatypeProperty, String classRDF, String fixedValue){
        int numberURI = firstURIAvailable(serviceURI + Endpoint.INPUT_URI_STRING);
        Resource input = model.createResource(serviceURI + Endpoint.INPUT_URI_STRING + numberURI);
        model.createResource(serviceURI).addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.INPUT_PROPERTY), input);
        input.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.PARAM_NAME), label);
        if (isRequired!=null){
            input.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.ISREQUIRED_PROPERTY), isRequired);
        }


		/*handle DatatypeProperty*/
        if (DatatypeProperty!=null){
            if (DatatypeProperty){
                input.removeAll(RDF.type);
                input.addProperty(RDF.type, OWL.DatatypeProperty);
                if(classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    input.addProperty(model.createProperty(Endpoint.SAME_PROPERTY_AS), model.createResource(classRDF));
                }
            }else{

                if(classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    input.removeAll(RDF.type);
                    input.addProperty(RDF.type, model.createResource(classRDF));
                }

            }
        } else {

            if (classRDF != null && !classRDF.equalsIgnoreCase("")) {
                input.removeAll(RDF.type);
                input.addProperty(RDF.type, model.createResource(classRDF));
            }
        }

        if(fixedValue!=null & !fixedValue.equalsIgnoreCase("")){
            input.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.HAS_FIXED_VALUE), fixedValue);
            input.addProperty(RDF.type, model.createResource(input.toString()+"key"));
        }
    }

    /**
     * Remove old properties and add new properties (RDF:type, content, isData)
     * @param outputURI String representing the URI of the subject resource
     * @param isData Boolean telling if the output contains data or other nodes
     * @param content String representing the content (String or Number)
     * @param classRDF String representing the URI of the class associated to the resource
     */
    public void editOutput(String outputURI, Boolean isData, Boolean DatatypeProperty, String content, String classRDF){
        Resource output = model.createResource(outputURI);
        Property isDataProperty = model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.ISDATA_PROPERTY);
        Property contentProperty = model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.CONTENT_PROPERTY);

        if (isData!=null){
            output.removeAll(isDataProperty);
            output.addLiteral(isDataProperty, isData);
        }

        if (content!=null){
            output.removeAll(contentProperty);
            output.addLiteral(contentProperty, content);
        }

		/*handle DatatypeProperty*/
        if (DatatypeProperty!=null){
            if (DatatypeProperty!=false){
                output.removeAll(RDF.type);
                output.addProperty(RDF.type, OWL.DatatypeProperty);
                if (classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    output.addProperty(model.createProperty(Endpoint.SAME_PROPERTY_AS), model.createResource(classRDF));
                }
            }else{

                if (classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    output.removeAll(RDF.type);
                    output.addProperty(RDF.type, model.createResource(classRDF));
                }

            }
        } else {

            if (classRDF != null && !classRDF.equalsIgnoreCase("")) {
                output.removeAll(RDF.type);
                output.addProperty(RDF.type, model.createResource(classRDF));
            }
        }
    }

    /**
     * Create new output resource
     * @param serviceURI serviceURI String representing the URI of the Service associated to the new output
     * @param label String representing the label of the output
     * @param isData Boolean telling if the output contains data or other nodes
     * @param content String representing the content (String or Number)
     * @param classRDF String representing the URI of the class associated to the resource
     * @param parentURI String representing the URI of the parent of the new output
     */
    public void addOutput(String serviceURI, String label, Boolean isData, Boolean DatatypeProperty,String content, String classRDF, String parentURI){
        Resource parent = model.createResource(parentURI);
        int numberURI = firstURIAvailable(serviceURI+ Endpoint.OUTPUT_URI_STRING);
        Resource output = model.createResource(serviceURI+ Endpoint.OUTPUT_URI_STRING +numberURI);
        parent.addProperty(model.createProperty(Endpoint.LI_PROPERTY), output);
        output.addProperty(RDFS.label, label);
        if (isData!=null){
            output.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.ISDATA_PROPERTY), isData);
        }
        if (content!=null){
            output.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.CONTENT_PROPERTY), content);
        }

		/*handle DatatypeProperty*/
        if (DatatypeProperty!=null){
            if (DatatypeProperty){
                output.removeAll(RDF.type);
                output.addProperty(RDF.type, OWL.DatatypeProperty);
                if (classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    output.addProperty(model.createProperty(Endpoint.SAME_PROPERTY_AS), model.createResource(classRDF));
                }
            }else{

                if (classRDF!=null && !classRDF.equalsIgnoreCase("")){
                    output.removeAll(RDF.type);
                    output.addProperty(RDF.type, model.createResource(classRDF));
                }

            }
        } else{

            if (classRDF!=null && !classRDF.equalsIgnoreCase("")){
                output.removeAll(RDF.type);
                output.addProperty(RDF.type, model.createResource(classRDF));
            }

        }
    }

    /**
     * Add triple to the model, used to link two resources with a property
     * @param subjectURI Uri of the subject
     * @param propertyURI Uri of the property
     * @param objectURI URI of the object
     */
    public void addProperty (String subjectURI, String propertyURI, String objectURI){
        model.createResource(subjectURI).addProperty(model.createProperty(propertyURI), model.createResource(objectURI));
    }

    public int firstURIAvailable (String baseURI){
        int number = 0;
        boolean isAvailable=false;

        do {
            number++;
            String queryString = "select * "
                    + "where { <" +
                    baseURI+number + "> ?p ?o }";
            //System.out.println(queryString);
            Query query = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.create(query,model);
            ResultSet result = qexec.execSelect();
            if (!result.hasNext()){
                isAvailable=true;
            }
        } while (isAvailable==false);

        return number;

    }

    public String getService (){

        String queryStr = "select ?service where { "
                + "?service <" + Endpoint.DEFAULT_NAMESPACE + Endpoint.URL_PROPERTY + "> ?url }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()){
            QuerySolution solution = result.nextSolution();
            Resource service = solution.getResource("service");
            return service.toString();
        } else {
            return null;
        }
    }

}