package poldo;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public class ExtractStructuredParams {

    private JSONObject obj;
    private JSONArray input;
    private JSONArray output;
    private JSONArray outputList;

    public ExtractStructuredParams() {

        obj = new JSONObject();
        input = new JSONArray();
        output = new JSONArray();
        outputList = new JSONArray();

    }


    public String extraction(String modelStringTTL) throws IOException, JSONException {


        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(modelStringTTL.getBytes()), null, "TTL");

        System.out.println(modelStringTTL);

        obj.put("model", modelStringTTL);

        extractInput(model);

        extractOutput(model);

        obj.put("outputList", outputList);
        return obj.toString();

    }

    /**
     * @param model
     * @throws JSONException
     */

    public void extractInput(Model model) throws JSONException {


        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
                        "SELECT ?name ?input WHERE { " +
                        "    ?service <" + Endpoint.INPUT_PROPERTY + "> ?input . " +
                        "    ?input  <" + Endpoint.PARAM_NAME + "> ?name  " +
                        "}";

        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {

                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("name");
                String input_URI = soln.get("input").toString();

                JSONObject elem_obj = new JSONObject();
                elem_obj.put("label", name);
                elem_obj.put("uri", input_URI);
                elem_obj.put("path", "/inputs/");


                input.put(elem_obj);
            }

            obj.put("Input", input);
        }
    }


    /**
     * @param model
     * @throws JSONException
     */
    public void extractOutput(Model model) throws JSONException {

        //select outputs
        String queryString = "select ?output ?label ?hasValue ?contentType ?childURI "
                +"where { " +
                "?service <"+Endpoint.OUTPUT_PROPERTY+"> ?output . " +
                "?output <" + RDFS.label + "> ?label " +
                "OPTIONAL { " +
                "?output <" + Endpoint.ISDATA_PROPERTY + "> ?hasValue  ."+
                "?output <" + Endpoint.CONTENT_PROPERTY + "> ?contentType " +
                " } " +
                " } ";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()){
            JSONObject jsonObject = new JSONObject();

            QuerySolution solution = result.nextSolution();
            Resource uri = solution.getResource("output");
            Literal label = solution.getLiteral("label");
            jsonObject.put("uri", uri.toString());
            jsonObject.put("label", label.toString());
            jsonObject.put("path", ExtractValueFromXML.getExpression(model, uri.toString()));


            if (solution.get("hasValue")!=null && solution.get("contentType")!=null){
                Boolean hasValue = solution.getLiteral("hasValue").getBoolean();
                String contentType = solution.get("contentType").toString();
                jsonObject.put("hasValue", hasValue);
                jsonObject.put("contentType", contentType);
            }


            jsonObject.put("children", extractChildren(model, uri.toString()));


            output.put(jsonObject);
            outputList.put(jsonObject);
        }


        obj.put("Output", output);


    }


    public JSONArray extractChildren (Model model, String parentURI){
        JSONArray childrenArray = new JSONArray();

        //select attributes
        String queryStringAttribute = "select ?attributeURI ?label ?hasValue ?contentType "
                + "where { " +
                "<" + parentURI+"> <" + Endpoint.ATTRIBUTE_PROPERTY + "> ?attributeURI . " +
                "?attributeURI <" + RDFS.label + "> ?label " +
                "OPTIONAL { " +
                "?attributeURI <" + Endpoint.ISDATA_PROPERTY + "> ?hasValue  ."+
                "?attributeURI <" + Endpoint.CONTENT_PROPERTY + "> ?contentType " +
                " } " +
                " } ";

        Query query = QueryFactory.create(queryStringAttribute);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet result = qexec.execSelect();
        while (result.hasNext()){
            JSONObject jsonObject = new JSONObject();

            QuerySolution solution = result.nextSolution();
            Resource uri = solution.getResource("attributeURI");
            Literal label = solution.getLiteral("label");
            jsonObject.put("uri", uri.toString());
            jsonObject.put("label", label.toString());
            jsonObject.put("isAttribute", true);
            jsonObject.put("path", ExtractValueFromXML.getExpression(model, uri.toString()));


            if (solution.get("hasValue")!=null && solution.get("contentType")!=null){
                Boolean hasValue = solution.getLiteral("hasValue").getBoolean();
                String contentType = solution.get("contentType").toString();
                jsonObject.put("hasValue", hasValue);
                jsonObject.put("contentType", contentType);
            }

            childrenArray.put(jsonObject);
            outputList.put(jsonObject);

        }

        //select rdf:li
        String queryStringLi = "select ?childURI ?label ?hasValue ?contentType "
                + "where { " +
                "<" + parentURI+"> <" + Endpoint.LI_PROPERTY + "> ?childURI . " +
                "?childURI <" + RDFS.label + "> ?label " +
                "OPTIONAL { " +
                "?childURI <" + Endpoint.ISDATA_PROPERTY + "> ?hasValue  ."+
                "?childURI <" + Endpoint.CONTENT_PROPERTY + "> ?contentType " +
                " } " +
                " } ";

        Query queryLi = QueryFactory.create(queryStringLi);
        QueryExecution qexecLi = QueryExecutionFactory.create(queryLi,model);
        ResultSet resultLi = qexecLi.execSelect();
        while (resultLi.hasNext()){
            JSONObject jsonObject = new JSONObject();

            QuerySolution solution = resultLi.nextSolution();

            Resource uri = solution.getResource("childURI");
            Literal label = solution.getLiteral("label");
            jsonObject.put("uri", uri.toString());
            jsonObject.put("label", label.toString());
            jsonObject.put("path", ExtractValueFromXML.getExpression(model, uri.toString()));


            if (solution.get("hasValue")!=null && solution.get("contentType")!=null){
                Boolean hasValue = solution.getLiteral("hasValue").getBoolean();
                String contentType = solution.get("contentType").toString();
                jsonObject.put("hasValue", hasValue);
                jsonObject.put("contentType", contentType);
            }

            jsonObject.put("children", extractChildren(model, uri.toString()));


            childrenArray.put(jsonObject);
            outputList.put(jsonObject);

        }

        return childrenArray;
    }

}