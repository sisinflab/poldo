package poldo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExtractParams {

    private JSONObject obj;
    private JSONArray output;

    public ExtractParams(){

        obj = new JSONObject();
        output = new JSONArray();
    }


    public String extraction(String modelStringTTL) throws IOException, JSONException{


        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(modelStringTTL.getBytes()), null, "TTL");

        obj.put("model", modelStringTTL);

        extractInput(model);

        extractOutput(model);

        return obj.toString();

    }

    /**
     *
     * @param model
     * @throws JSONException
     */

    public void extractInput(Model model) throws JSONException{

        JSONArray input = new JSONArray();


        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
                        "SELECT ?name ?input WHERE { " +
                        "    ?service <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.INPUT_PROPERTY+"> ?input . " +
                        "    ?input  <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.PARAM_NAME +"> ?name  " +
                        "}";

        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

            ResultSet results = qexec.execSelect();

            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Literal name = soln.getLiteral("name");
                String input_URI = soln.get("input").toString();


                JSONObject elem_obj = new JSONObject();
                elem_obj.put("label", name);
                elem_obj.put("uri",input_URI);
                elem_obj.put("path", "/inputs/");

                input.put(elem_obj);
            }

            obj.put("Input", input);
        }

    }


    /**
     *
     * @param model
     * @throws JSONException
     */
    public void extractOutput(Model model) throws JSONException{

        List<Literal> list = new ArrayList<Literal>();

        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  " +
                        "SELECT ?name ?service ?output ?elem_struct ?flag ?type ?attr_elem WHERE{ " +
                        "    ?service <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.OUTPUT_PROPERTY+"> ?output . " +
                        "    ?output rdfs:label ?name  ." +
                        "    OPTIONAL{"+
                        "       ?output rdf:li ?elem_struct"+
                        "    }" +
                        "    OPTIONAL{"+
                        "       ?output <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.ATTRIBUTE_PROPERTY+"> ?attr_elem"+
                        "    }" +
                        "    OPTIONAL{"+
                        "       ?output <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.ISDATA_PROPERTY+"> ?flag  ."+
                        "       ?output <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.CONTENT_PROPERTY+"> ?type"+
                        "    }" +
                        "}";

        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

            ResultSet results = qexec.execSelect();


            while ( results.hasNext() ) {

                QuerySolution soln = results.nextSolution();

                Literal name = soln.getLiteral("name");

                String output_URI = soln.get("output").toString();
                String service_URI = soln.get("service").toString();


                if(!list.contains(name)){

                    list.add(name);

                    JSONObject elem_obj = new JSONObject();
                    elem_obj.put("label", name);
                    elem_obj.put("uri",output_URI);
                    elem_obj.put("parent",service_URI);
                    elem_obj.put("path", ExtractValueFromXML.getExpression(model, output_URI));

                    if(soln.get("flag")!= null){

                        String data = soln.get("flag").toString();
                        String type = soln.get("type").toString();
                        elem_obj.put("isData",data);
                        elem_obj.put("type",type);
                    }

                    output.put(elem_obj);

                }



                if(soln.get("elem_struct")!= null){
                    String elem_URI = soln.get("elem_struct").toString();
                    extractElem(model, elem_URI,output_URI);

                }else if(soln.get("?attr_elem")!= null){

                    String elem_URI = soln.get("attr_elem").toString();
                    extractElem(model, elem_URI,output_URI);
                }

            }

            obj.put("Output", output);
        }

    }


    public void extractElem(Model model, String element,String parent) throws JSONException{

        List<Literal> list = new ArrayList<Literal>();

        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  " +
                        "SELECT ?name ?elem_struct ?flag ?type ?attr_elem WHERE{ " +
                        "<" + element + ">" + " rdfs:label ?name  ." +
                        "    OPTIONAL{"+
                        "<" + element + ">" + " <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.ATTRIBUTE_PROPERTY+"> ?attr_elem"+
                        "    }" +
                        "    OPTIONAL{"+
                        "<" + element + ">" + " <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.ISDATA_PROPERTY+"> ?flag ."+
                        "<" + element + ">" + " <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.CONTENT_PROPERTY+"> ?type "+
                        "    }" +
                        "}";

        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

            ResultSet results = qexec.execSelect();


            while ( results.hasNext() ) {

                QuerySolution soln = results.nextSolution();

                Literal name = soln.getLiteral("name");

                if(!list.contains(name)){

                    list.add(name);
                    JSONObject elem_obj = new JSONObject();
                    elem_obj.put("label", name);
                    elem_obj.put("uri",element);
                    elem_obj.put("parent",parent);
                    elem_obj.put("path", ExtractValueFromXML.getExpression(model, element));


                    if(soln.get("flag")!= null){

                        String data = soln.get("flag").toString();
                        String type = soln.get("type").toString();
                        elem_obj.put("isData",data);
                        elem_obj.put("type",type);
                    }

                    output.put(elem_obj);
                }

                if(soln.get("?attr_elem")!= null){

                    String elem_URI = soln.get("attr_elem").toString();
                    extractElem(model, elem_URI,element);
                }

            }


        }

        queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  " +
                        "SELECT ?name ?elem_struct ?flag ?type ?attr_elem WHERE{ " +
                        "<" + element + ">" + " rdfs:label ?name  ." +
                        "    OPTIONAL{"+
                        "<" + element + ">" + " rdf:li ?elem_struct"+
                        "    }" +
                        "    OPTIONAL{"+
                        "<" + element + ">" + " <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.ISDATA_PROPERTY+"> ?flag ."+
                        "<" + element + ">" + " <"+Endpoint.DEFAULT_NAMESPACE+Endpoint.CONTENT_PROPERTY+"> ?type "+
                        "    }" +
                        "}";

        query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

            ResultSet results = qexec.execSelect();


            while ( results.hasNext() ) {

                QuerySolution soln = results.nextSolution();

                Literal name = soln.getLiteral("name");

                if(!list.contains(name)){

                    list.add(name);

                    JSONObject elem_obj = new JSONObject();
                    elem_obj.put("label", name);
                    elem_obj.put("uri",element);
                    elem_obj.put("parent",parent);
                    elem_obj.put("path", ExtractValueFromXML.getExpression(model, element));


                    if(soln.get("flag")!= null){

                        String data = soln.get("flag").toString();
                        String type = soln.get("type").toString();
                        elem_obj.put("isData",data);
                        elem_obj.put("type",type);
                    }

                    output.put(elem_obj);
                }

                if(soln.get("elem_struct")!= null){

                    String elem_URI = soln.get("elem_struct").toString();
                    extractElem(model, elem_URI,element);

                }

            }


        }

    }


}
