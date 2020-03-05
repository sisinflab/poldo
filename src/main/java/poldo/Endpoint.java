package poldo;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;


@Path("/endpoint")
public class Endpoint {

    public static final String PREFIX = "poldo";
    public static final String CUSTOM_NS_PREFIX = "customns";

    public static String CUSTOM_NAMESPACE = "http://sisinflab.poliba.it/semanticweb/lod/poldo/";

    public static final String DEFAULT_NAMESPACE = "http://sisinflab.poliba.it/semanticweb/lod/poldo/";
    public static final String URL_PROPERTY = DEFAULT_NAMESPACE + "hasUrl";
    public static final String METHOD_PROPERTY = DEFAULT_NAMESPACE + "hasMethod";
    public static final String INPUT_PROPERTY = DEFAULT_NAMESPACE + "hasInput";
    public static final String LANGUAGE_PROPERTY = DEFAULT_NAMESPACE + "hasLanguage";
    public static final String OUTPUT_PROPERTY = DEFAULT_NAMESPACE + "hasOutput";
    public static final String ATTRIBUTE_PROPERTY = DEFAULT_NAMESPACE + "hasAttribute";
    public static final String CONTENT_PROPERTY = DEFAULT_NAMESPACE + "content";
    public static final String ISDATA_PROPERTY = DEFAULT_NAMESPACE + "isData";
    public static final String ISREQUIRED_PROPERTY = DEFAULT_NAMESPACE + "isRequired";
    public static final String HAS_STRUCTURE_PROPERTY = DEFAULT_NAMESPACE + "hasStructure";
    public static final String IS_RELATED_TO_SERVICE = DEFAULT_NAMESPACE + "isRelatedToService";
    public static final String HAS_FIXED_VALUE = DEFAULT_NAMESPACE + "hasFixedValue";
    public static final String ADDED_RESOURCE = DEFAULT_NAMESPACE + "customResource";
    public static final String PARAM_NAME = DEFAULT_NAMESPACE + "paramName";
    public static final String FIND_URI = DEFAULT_NAMESPACE + "findURI";

    public static final String STRING_CONTENT = "String";
    public static final String NUMBER_CONTENT = "Number";
    public static final String BOOLEAN_CONTENT = "Boolean";

    public static final String OUTPUT_URI_STRING = "-output";
    public static final String INPUT_URI_STRING = "-input";
    public static final String RESOURCE_URI_STRING = "resource";

    public static final String LI_PROPERTY = RDF.uri + "li";
    public static final String SAME_PROPERTY_AS = OWL.NS + "equivalentProperty";

    public static final long TIMEOUT_DELAY = 5000;	//5000 = 5s

    //put files in the web directory
    public static final String MAPPING_PATH = "apollon-uv-dailyavg.ttl";
    public static final String LOV_PATH = "lov.ttl";
    public static final String DBO_PATH = "dbpedia_2016-04-range-domain.nt";

    public static final String JSON_ARRAY_ROOT = "jsonArrayRoot";

    public static final String LANG_DBPEDIA = "en";


    @javax.ws.rs.core.Context
    public ServletContext context;



    @POST
    @Path("/querywithmapping")
    @Produces (MediaType.APPLICATION_JSON)
    public String executeQueryWithMapping(@Context HttpHeaders header,
                                          @Context HttpServletResponse response,
                                          @FormParam("query") String queryString,
                                          @FormParam("model") String modelString) throws Exception {

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        Model model;
        model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(modelString.getBytes()), null, "TTL");

        QueryPlanner queryPlanner = new QueryPlanner();
        //JSONObject jsonResponse = queryPlanner.solveQuery(model, queryString);

        //return jsonResponse.toString();

        String jsonResponse = queryPlanner.solveQuery(model, queryString, context);
        return jsonResponse;
    }

    @POST
    @Path("/querywithmappingfile")
    @Produces (MediaType.APPLICATION_JSON)
    public String executeQueryWithMappingFile(@Context HttpHeaders header,
                                          @Context HttpServletResponse response,
                                          @FormParam("query") String queryString,
                                          @FormParam("modelName") String modelFileName) throws Exception {

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");


        File fileModel = new File(context.getRealPath(modelFileName+".ttl"));
        byte[] encModel = Files.readAllBytes(Paths.get(fileModel.getPath()));
        String modelString = new String (encModel);

        Model model;
        model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(modelString.getBytes()), null, "TTL");

        QueryPlanner queryPlanner = new QueryPlanner();
        //JSONObject jsonResponse = queryPlanner.solveQuery(model, queryString);

        //return jsonResponse.toString();

        String jsonResponse = queryPlanner.solveQuery(model, queryString, context);
        return jsonResponse;
    }

    @POST
    @Path("/query")
    @Produces (MediaType.APPLICATION_JSON)
    public String executeQuery(@Context HttpHeaders header,
                               @Context HttpServletResponse response,
                               @FormParam("query") String queryString) throws Exception {

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");


        File fileModel = new File(context.getRealPath(MAPPING_PATH));
        byte[] encModel = Files.readAllBytes(Paths.get(fileModel.getPath()));
        String modelString = new String (encModel);

        Model model;
        model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(modelString.getBytes()), null, "TTL");

        QueryPlanner queryPlanner = new QueryPlanner();
        //JSONObject jsonResponse = queryPlanner.solveQuery(model, queryString);

        //return jsonResponse.toString();

        String jsonResponse = queryPlanner.solveQuery(model, queryString, context);
        return jsonResponse;
    }

    @POST
    @Path("/suggestproperties")
    @Produces (MediaType.APPLICATION_JSON)
    public String suggestProperties(@Context HttpHeaders header,
                                    @Context HttpServletResponse response,
                                    @FormParam("model") String modelString) throws Exception {

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        Model model;
        model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(modelString.getBytes()), null, "TTL");

        PropertiesFinder propertiesFinder = new PropertiesFinder();

        String jsonResponse = propertiesFinder.suggest(model, context);
        return jsonResponse;
    }

    @POST
    @Path("/merge")
    @Produces ("text/turtle")
    public String mergeModels (@Context HttpHeaders header,
                               @Context HttpServletResponse response,
                               @FormParam("models") String jsonModelsString) throws JSONException{

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        Model newModel = mergeModelsFromJson(jsonModelsString);

        //Write the model to Output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newModel.write(baos, "turtle");

        //Send the model to client
        return baos.toString();
    }

    public Model mergeModelsFromJson(String jsonString) throws JSONException{
        Model model;
        model = ModelFactory.createDefaultModel();

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray jsonArray = jsonObject.getJSONArray("models");

        for (int i=0; i<jsonArray.length(); i++){
            model.read(new ByteArrayInputStream(jsonArray.getString(i).getBytes()), null, "TTL");
        }

        return model;
    }


    /**
     *
     * @param header
     * @param response
     * @param serviceURL example of a URL of an API request
     * @ param serviceMethod http method of the API (GET)
     * @param serviceOutputLanguage Output language (json, xml)
     * @param serviceOutput example of an output of the API
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path("/getmodelandparam")
    @Produces (MediaType.APPLICATION_JSON)
    public String modelGeneratorReturnParam(@Context HttpHeaders header,
                                            @Context HttpServletResponse response,
                                            @DefaultValue("http://") @FormParam("url") String serviceURL,
                                            //@DefaultValue("GET") @FormParam("method") String serviceMethod,
                                            @DefaultValue("xml") @FormParam("language") String serviceOutputLanguage,
                                            @FormParam("output") String serviceOutput,
                                            @DefaultValue("undefined") @FormParam("namespace") String namespace) throws IOException, JSONException{

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        //call the main function (modelGenerator)
        //String modelString = modelGenerator(header, response, serviceURL, serviceMethod, serviceOutputLanguage, serviceOutput);
        String modelString = modelGenerator(header, response, serviceURL, serviceOutputLanguage, serviceOutput,namespace);
        //call the function for extracting parameters from the model - return a JSON object containing model, inputs and outputs
        ExtractParams extractParam = new ExtractParams();
        return extractParam.extraction(modelString);

    }

    /**
     *
     * @param header
     * @param response
     * @param model turtle model
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @POST
    @Path("/getparam")
    @Produces (MediaType.APPLICATION_JSON)
    public String getParamsOfModel(@Context HttpHeaders header,
                                            @Context HttpServletResponse response,
                                            @FormParam("model") String model) throws IOException, JSONException{

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        //call the function for extracting parameters from the model - return a JSON object containing model, inputs and outputs
        ExtractParams extractParam = new ExtractParams();
        return extractParam.extraction(model);
    }


    @POST
    @Path("/getmodelandstructuredparams")
    @Produces (MediaType.APPLICATION_JSON)
    public String modelGeneratorReturnStructuredParam(@Context HttpHeaders header,
                                                      @Context HttpServletResponse response,
                                                      @DefaultValue("http://") @FormParam("url") String serviceURL,
                                                      //@DefaultValue("GET") @FormParam("method") String serviceMethod,
                                                      @DefaultValue("xml") @FormParam("language") String serviceOutputLanguage,
                                                      @FormParam("output") String serviceOutput,
                                                      @DefaultValue("undefined") @FormParam("namespace") String namespace) throws IOException, JSONException{

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        //call the main function (modelGenerator)
        //String modelString = modelGenerator(header, response, serviceURL, serviceMethod, serviceOutputLanguage, serviceOutput);
        String modelString = modelGenerator(header, response, serviceURL, serviceOutputLanguage, serviceOutput,namespace);
        //call the function for extracting parameters from the model - return a JSON object containing model, inputs and outputs
        ExtractStructuredParams extractParam = new ExtractStructuredParams();
        return extractParam.extraction(modelString);

    }

    /**
     *
     * @param header
     * @param response
     * @param json jsonString that describes model and changes to do
     * @return
     */
    @POST
    @Path("/edit")
    @Produces ("text/turtle")
    public String edit(@Context HttpHeaders header,
                       @Context HttpServletResponse response,
                       @FormParam("json") String json){

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        //Call editAndGetTurtleModel() from the class EditMapping
        EditMapping editMapping = new EditMapping(json);
        String newModel = editMapping.editAndGetTurtleModel();
        if (newModel.startsWith("Error")) {
            //response.sendError(400, newModel);
            throw new WebApplicationException(Response.status(400).entity(newModel).build());
        }
        return newModel;
    }

    /**
     *
     * @param header
     * @param response
     * @param serviceURL example of a URL of an API request
     * @ param serviceMethod http method of the API (GET) - obsolete, no more used
     * @param serviceOutputLanguage Output language (json, xml)
     * @param serviceOutput example of an output of the API
     * @return
     */
    @POST
    @Path("/model")
    @Produces ("text/turtle")
    public String modelGenerator(@Context HttpHeaders header,
                                 @Context HttpServletResponse response,
                                 @DefaultValue("http://") @FormParam("url") String serviceURL,
                                 //@DefaultValue("GET") @FormParam("method") String serviceMethod,
                                 @DefaultValue("xml") @FormParam("language") String serviceOutputLanguage,
                                 @FormParam("output") String serviceOutput,
                                 @DefaultValue("undefined") @FormParam("namespace") String namespace){

        //Add header to allow cross-domain requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        //Create new RDF model
        Model model = ModelFactory.createDefaultModel();

        /*
        user can pass a namespace to use for creating resources in mapping file,
        in this case we add a NS prefix to the model, otherwise we will use PoLDo namespace.
         */
        if(!namespace.equals("undefined") && !namespace.equals("")){
            //namespace must be a valid URI
            String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
            if (!namespace.matches(regex)){
                throw new WebApplicationException(Response.status(400).entity("Custom namespace must be a valid URI").build());
            }
            CUSTOM_NAMESPACE = namespace;
            model.setNsPrefix(CUSTOM_NS_PREFIX, namespace);
        } else {
            CUSTOM_NAMESPACE = DEFAULT_NAMESPACE;
        }

        //Analyze URL extracting endpoint and inputs
        URLAnalizer urlAnalizer = new URLAnalizer(serviceURL);
        int result = urlAnalizer.analizeURL(urlAnalizer.getExampleURL());

        //return ERROR if urlAnalizer doesn't work
        if (result == -1){
            throw new WebApplicationException(Response.status(400).entity("Error: Impossible to extract parameters from the submitted URL!").build());
        }

        //Remove "http://" from the endpoint string (to create the URI of the service)
        String endpointNoHttp;

        if (urlAnalizer.getEndpoint().startsWith("http://")) {
            endpointNoHttp = urlAnalizer.getEndpoint().substring(7);
        } else if (urlAnalizer.getEndpoint().startsWith("https://")) {
            endpointNoHttp = urlAnalizer.getEndpoint().substring(8);
        } else {
            endpointNoHttp = urlAnalizer.getEndpoint();
        }

        //replace "/", ":" and "." from the URL to create the resource URI
        //if there are "/" and ".", prefix doesn't work
        endpointNoHttp = endpointNoHttp.replace("/", "-").replace(".", "-").replace(":", "-");

        //Add namespace prefixes
        //PoLDo
        model.setNsPrefix(PREFIX, DEFAULT_NAMESPACE);
        //Standard
        model.setNsPrefix("rdfs", RDFS.uri);
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("owl", OWL.getURI());

        //Create Service resource
        Resource service = model.createResource(CUSTOM_NAMESPACE + endpointNoHttp);

        //Create properties
        Property urlProperty = model.createProperty(URL_PROPERTY);
        //Property methodProperty = model.createProperty(METHOD_PROPERTY);
        Property inputProperty = model.createProperty(INPUT_PROPERTY);
        Property languageProperty = model.createProperty(LANGUAGE_PROPERTY);

        //Add properties to service resource
        service.addProperty(urlProperty, urlAnalizer.getEndpoint());
        //service.addProperty(methodProperty, serviceMethod);

        //Add input resources
        int i=1;
        for (String inputName : urlAnalizer.getInputArray()){
            //Create new resource for input
            Resource inputResource = model.createResource(CUSTOM_NAMESPACE + endpointNoHttp + INPUT_URI_STRING + i );
            //Add property "hasInput" to the service resource
            service.addProperty(inputProperty, inputResource);
            //Add label to the input resource
            inputResource.addProperty(model.createProperty(PARAM_NAME), inputName);
            i++;
        }

        //Output analysis
        if(serviceOutput!=null){
            if (serviceOutputLanguage.equalsIgnoreCase("xml")){

                //Add language property to the service
                service.addProperty(languageProperty, "xml");

                //create new Document from String containing xml output
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                Document doc;
                try {
                    builder = factory.newDocumentBuilder();
                    doc = builder.parse(new InputSource(new StringReader(serviceOutput.replaceAll("&", "&amp;"))));
                    //Call xmlAnalizer.modelbuilder and add to existing model
                    XMLAnalizer xmlAnalizer = new XMLAnalizer();
                    model.add(xmlAnalizer.modelBuilder(doc, CUSTOM_NAMESPACE + endpointNoHttp));
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    e.printStackTrace();
                }
            } else if (serviceOutputLanguage.equalsIgnoreCase("json")){

                //Add language property to the service
                service.addProperty(languageProperty, "json");

                //Call getOutput from ParserJSON that analyze the output string and generate the model for the outputs
                ParserJSON parserJSON = new ParserJSON();
                try {
                    model.add(parserJSON.getOutput(serviceOutput, CUSTOM_NAMESPACE + endpointNoHttp));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //changes URI of resources, from -outputN to -path/of/resource
        model = editURI(model);

        //Write the model to Output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos, "turtle");

        //Send the model to client
        return baos.toString();

    }

    //Function to check if the server works
    @GET
    @Produces (MediaType.TEXT_PLAIN)
    public String get(@Context HttpHeaders header, @Context HttpServletResponse response, @DefaultValue("http://") @QueryParam("url") String param){
        response.setHeader("Access-Control-Allow-Origin", "*");
        System.out.println("get: "+param);
        return "ok get";
    }

    /**
     * Method for replacing uri of inputs and outputs.
     * input old uri: prefix:input-1
     * input new uri: prefix:paramName
     * output old uri: prefix:output-1
     * output new uri: prefix:outputPath
     * @param model
     * @return
     */
    public Model editURI (Model model) {

        //select inputs
        String queryStrIn = "select distinct ?res ?paramName where "
                + "{ " +
                "?res <" + Endpoint.PARAM_NAME + "> ?paramName "
                + "}";

        Query queryIn = QueryFactory.create(queryStrIn);
        QueryExecution qexecIn = QueryExecutionFactory.create(queryIn, model);
        ResultSet resultIn = qexecIn.execSelect();

        ArrayList<Resource> inputrResources = new ArrayList<>();
        ArrayList<String> paramNames = new ArrayList<>();

        while (resultIn.hasNext()) {
            QuerySolution solution = resultIn.nextSolution();
            Resource res = solution.getResource("res");
            String paramName = solution.getLiteral("paramName").toString();
            inputrResources.add(res);
            paramNames.add(paramName);
        }

        for (int i = 0; i < inputrResources.size(); i++) {
            String newUri = getNewInputResourceUri(inputrResources.get(i).toString(), paramNames.get(i));
            ResourceUtils.renameResource(inputrResources.get(i), newUri);
        }

        ArrayList<Resource> outputResources = new ArrayList<>();

        //select outputs
        String queryStrOut = "select distinct ?res where "
                + "{ " +
                "?res <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service "
                + "}";

        Query queryOut = QueryFactory.create(queryStrOut);
        QueryExecution qexecOut = QueryExecutionFactory.create(queryOut, model);
        ResultSet resultOut = qexecOut.execSelect();

        while (resultOut.hasNext()) {
            QuerySolution solution = resultOut.nextSolution();
            Resource res = solution.getResource("res");
            outputResources.add(res);
        }

        for (int i = 0; i < outputResources.size(); i++) {
            String newUri = getNewOutputResourceUri(outputResources.get(i).toString(), model);
            ResourceUtils.renameResource(outputResources.get(i), newUri);
        }

        //TODO forzare il modello ad utilizzare il prefisso se possibile (prefix:resURI e non http://extendedPrefix.com/resURI)

        return model;
    }

    public String getNewInputResourceUri(String uri, String name) {
        String newUri = uri.substring(0, uri.lastIndexOf("-"));

        if (newUri.endsWith("-")){
            newUri = newUri.substring(0, newUri.length()-1);
        }

        newUri += "-" + name;

        return newUri;
    }

    public String getNewOutputResourceUri(String uri, Model model) {
        String newUri = uri.substring(0, uri.lastIndexOf("-"));

        if (!newUri.endsWith("-")){
            newUri += "-";
        }
        newUri = newUri + ExtractValueFromXML.getExpression(model, uri).substring(1)
                .replaceAll("/","-")
                .replaceAll("@","");

        if (newUri.endsWith("-")){
            newUri += "root";
        }

        return newUri;
    }

}
