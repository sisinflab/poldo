package poldo;


import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;

public class ParserJSON {


    private JsonFactory factory;
    private ObjectMapper mapper;

    private String r2rqPrefix;

    private Model model;
    private int count;
    private Property propStruct;
    private Property prop_li;

    private Boolean isRoot;

    public ParserJSON(){

        isRoot=true;

        factory = new JsonFactory();
        mapper = new ObjectMapper(factory);

        model = ModelFactory.createDefaultModel();
        r2rqPrefix = "";
        propStruct = model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.HAS_STRUCTURE_PROPERTY);
        prop_li = model.createProperty(RDF.uri + "li");

        count = 1;

    }

    public Model getOutput(String json, String prefix) throws IOException{

        r2rqPrefix = prefix;

        mapper = new ObjectMapper(factory);
        JsonNode rootNode = mapper.readTree(json);

        Iterator<Map.Entry<String,JsonNode>> fieldsIterator = rootNode.getFields();

        while (fieldsIterator.hasNext()) {

            Map.Entry<String,JsonNode> field = fieldsIterator.next();
            //System.out.println("Key: " + field.getKey() + "\tValue:" + field.getValue());

            checkStructure(field);
            isRoot = true;


        }

        return model;
    }


    /**
     * Handle the three data rapresentation in a JSON
     * @param field
     */
    public void checkStructure(Map.Entry<String,JsonNode> field){


        if(field.getValue().isObject()){

			/*handle JSON Objest*/

            Resource outAPI = model.createResource(r2rqPrefix + Endpoint.OUTPUT_URI_STRING+count);

            if (isRoot) {
                model.createResource(r2rqPrefix).addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.OUTPUT_PROPERTY), outAPI);
                isRoot=false;
            }

            outAPI.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.IS_RELATED_TO_SERVICE), model.createResource(r2rqPrefix));

            outAPI.addProperty(RDF.type, RDF.Bag);
            outAPI.addProperty(RDFS.label, field.getKey());
            outAPI.addProperty(propStruct, "JSON_Object");

            count = count + 1;

            Iterator<Map.Entry<String,JsonNode>> fieldsOBJ = field.getValue().getFields();

            while (fieldsOBJ.hasNext()) {

                Map.Entry<String,JsonNode> field_obj = fieldsOBJ.next();

                Resource outAPI2 = model.createResource(r2rqPrefix + Endpoint.OUTPUT_URI_STRING+count);
                outAPI.addProperty(prop_li, outAPI2);


                if(!field_obj.getValue().isArray()&&!field_obj.getValue().isObject()){

                    outAPI2.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.IS_RELATED_TO_SERVICE), model.createResource(r2rqPrefix));

                    outAPI2.addProperty(RDFS.label, field_obj.getKey());
                    outAPI2.addProperty(propStruct, "JSON_Data");
                    outAPI2.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.ISDATA_PROPERTY), true);
                    checkType(field_obj.getValue().toString(),outAPI2);
                }else{
                    //recursive call for nested structures
                    checkStructure(field_obj);
                }

                count = count + 1;
            }

        }else if(field.getValue().isArray()){

			/*handle JSON Array*/

            Resource outAPI = model.createResource(r2rqPrefix + Endpoint.OUTPUT_URI_STRING+count);

            if (isRoot) {
                model.createResource(r2rqPrefix).addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.OUTPUT_PROPERTY), outAPI);
                isRoot = false;
            }

            outAPI.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.IS_RELATED_TO_SERVICE), model.createResource(r2rqPrefix));

            outAPI.addProperty(RDF.type, RDF.Bag);
            outAPI.addProperty(RDFS.label, field.getKey());
            outAPI.addProperty(propStruct, "JSON_Array");

            // Check if there are elements in the array
            if(field.getValue().getElements().hasNext()){

                JsonNode sub_field = field.getValue().getElements().next();

                Iterator<Map.Entry<String,JsonNode>> fields = sub_field.getFields();

                count = count + 1;

                while (fields.hasNext()) {

                    Map.Entry<String,JsonNode> field_arr = fields.next();
                    //System.out.println("Key: " + fiel.getKey() + "\tValue:" + fiel.getValue());
                    if(!isCheckField(model,field.getKey(), field_arr.getKey())){

                        Resource outAPI2 = model.createResource(r2rqPrefix + Endpoint.OUTPUT_URI_STRING+count);
                        outAPI.addProperty(prop_li, outAPI2);


                        if(!field_arr.getValue().isArray()&&!field_arr.getValue().isObject()){

                            outAPI2.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.IS_RELATED_TO_SERVICE), model.createResource(r2rqPrefix));

                            outAPI2.addProperty(RDFS.label, field_arr.getKey());
                            outAPI2.addProperty(propStruct, "JSON_Data");
                            outAPI2.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.ISDATA_PROPERTY), true);
                            checkType(field_arr.getValue().toString(),outAPI2);
                        }else{
                            //recursive call for nested structures
                            checkStructure(field_arr);
                        }

                        count = count + 1;

                    }
                }

            }
        }else{

			/*handle JSON Data*/

            if(!isCheckField(model,field.getKey(), field.getKey())){

                Resource outAPI = model.createResource(r2rqPrefix + Endpoint.OUTPUT_URI_STRING+count);

                if (isRoot) {
                    model.createResource(r2rqPrefix).addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.OUTPUT_PROPERTY), outAPI);
                    isRoot=false;
                }

                outAPI.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE+Endpoint.IS_RELATED_TO_SERVICE), model.createResource(r2rqPrefix));

                outAPI.addProperty(RDFS.label, field.getKey());
                outAPI.addProperty(propStruct, "JSON_Data");
                outAPI.addLiteral(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.ISDATA_PROPERTY), true);
                checkType(field.getValue().toString(),outAPI);
                count = count + 1;
            }
        }

    }


    public boolean isCheckField(Model model,String fatherURI, String label){

        boolean flag;

        String queryString = "select ?o "
                + "where { <" +
                fatherURI+"> <" + prop_li + "> ?o . " +
                "?o <" + RDFS.label + "> \"" + label + "\" }";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        ResultSet results = qexec.execSelect();

        flag = results.hasNext();


        return flag;
    }

    public void checkType(String value,Resource attributeResource){

        if (value.matches("[-]?+[0-9]+[\\.,]?[0-9]*")) {
            attributeResource.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.CONTENT_PROPERTY), Endpoint.NUMBER_CONTENT);

        } else {

            if(value.equalsIgnoreCase("true")){
                attributeResource.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.CONTENT_PROPERTY), Endpoint.BOOLEAN_CONTENT);


            }else if(value.equalsIgnoreCase("false")){
                attributeResource.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.CONTENT_PROPERTY), Endpoint.BOOLEAN_CONTENT);


            }else{
                attributeResource.addProperty(model.createProperty(Endpoint.DEFAULT_NAMESPACE + Endpoint.CONTENT_PROPERTY), Endpoint.STRING_CONTENT);


            }

        }

    }

}
