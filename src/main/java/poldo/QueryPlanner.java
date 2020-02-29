package poldo;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class QueryPlanner {

    //mapping model
    Model model;
    //user sparql query
    Query query;

    //rdf cache to be used for the evaluation of the query
    Model rdfCache;

    //values used for calling services
    List<String> calledServicesList;

    //classes of inputs of every service
    HashMap<String, List<String>> serviceInputTypeTable;

    HashMap<String, String> outputServiceTable;
    HashMap<String, List<String>> serviceInputsURITable;

    //triples extract from query (graph pattern)
    List<TriplePattern> patternList;

    //All the extracted costants
    HashMap<String, List<String>> constantsTable;

    HashMap<String, List<String>> labelsTable;
    HashMap<String, List<String>> selectTable;

    List<String> analyzedResourceURIList;

    //to build the answer
    JSONObject jsonObject;
    JSONObject jsonObjectHead;
    JSONObject jsonObjectResults;
    JSONArray jsonArrayVars;
    JSONArray jsonArrayBindings;

    //used for building URI for rdf cache
    int resourceIndex;
    int customResourceIndex;

    boolean timeout;
    boolean limitReached;

    ServletContext context;


    public String solveQuery(Model m, String queryString, ServletContext c) throws Exception {

        context = c;

        resourceIndex = 0;
        customResourceIndex = 0;

        calledServicesList = new ArrayList<String>();

        jsonObject = new JSONObject();
        jsonObjectHead = new JSONObject();
        jsonObjectResults = new JSONObject();

        jsonArrayVars = new JSONArray();
        jsonArrayBindings = new JSONArray();

        model = m;
        query = QueryFactory.create(queryString);

        outputServiceTable = new HashMap<String, String>();
        serviceInputsURITable = new HashMap<String, List<String>>();
        serviceInputTypeTable = new HashMap<String, List<String>>();

        patternList = new ArrayList<TriplePattern>();

        constantsTable = new HashMap<String, List<String>>();

        labelsTable = new HashMap<String, List<String>>();

        selectTable = new HashMap<String, List<String>>();

        analyzedResourceURIList = new ArrayList<String>();

        rdfCache = ModelFactory.createDefaultModel();

        //extract triples from "where" of query
        SPARQLQueryAnalyzer sparqlQueryAnalyzer = new SPARQLQueryAnalyzer();
        sparqlQueryAnalyzer.analyze(queryString);
        patternList = sparqlQueryAnalyzer.getPattern();

        //TODO insert here a dataset of constants

        //load configuration files
        Properties prop = new Properties();
        InputStream input = null;
        String[] services = null;
        ArrayList<LabelTypeService> labelTypeServices = new ArrayList<>();
        try {

            input = new FileInputStream(context.getRealPath("/config.properties"));

            // load properties file
            prop.load(input);

            services = prop.getProperty("services").split(",");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (services != null) {
            for (int i = 0; i < services.length; i++) {
                Properties serviceProp = new Properties();
                InputStream serviceInput = null;

                try {

                    serviceInput = new FileInputStream(context.getRealPath("/"+services[i] + ".properties"));

                    // load a properties file
                    serviceProp.load(serviceInput);

                    LabelTypeService labelTypeService = new LabelTypeService();
                    labelTypeService.setEndpoint(serviceProp.getProperty("endpoint"));
                    labelTypeService.setContains(serviceProp.getProperty("contains"));
                    labelTypeService.setPlaceholder(serviceProp.getProperty("placeholder"));
                    labelTypeService.setQueryString(serviceProp.getProperty("query"));

                    labelTypeServices.add(labelTypeService);

                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


        //extract constants from graph pattern
        for (int index = 0; index < patternList.size(); index++) {


            //get labels from config services
            for (int indexService = 0; indexService < labelTypeServices.size(); indexService++) {
                if (patternList.get(index).getSubject().contains(labelTypeServices.get(indexService).getContains())){
                    writeConstantsLabelAndTypeFromService(patternList.get(index).getSubject(), labelTypeServices.get(indexService));
                }
                if (patternList.get(index).getObject().contains(labelTypeServices.get(indexService).getContains())){
                    writeConstantsLabelAndTypeFromService(patternList.get(index).getObject(), labelTypeServices.get(indexService));
                }
            }


            //if starts with " is a LITERAL
            if (patternList.get(index).getObject().startsWith("\"")) {
                List<String> typeList = new ArrayList<String>();

                if (patternList.get(index).getProperty().equalsIgnoreCase(RDFS.label.toString())) {

                    typeList = typeFromGraphPattern(patternList.get(index).getSubject(), patternList);

                    //if the class is not specified in the graph pattern, search in mapping file
                    if (typeList.isEmpty()) {
                        typeList = typeFromMappingFile(patternList.get(index).getSubject(), patternList);
                    }

                    //add constant to constantsTable
                    for (int indexTypeList = 0; indexTypeList < typeList.size(); indexTypeList++) {
                        if (constantsTable.containsKey(typeList.get(indexTypeList))) {
                            constantsTable.get(typeList.get(indexTypeList)).add(patternList.get(index).getObject());
                        } else {
                            List<String> valueList = new ArrayList<String>();
                            valueList.add(patternList.get(index).getObject());
                            constantsTable.put(typeList.get(indexTypeList), valueList);
                        }
                    }

                } else {

                    //if the constant is not related to rdfs:label, the property has a "samePropertyAs" or the property is object of "samePropertyAs". We use it as key

                    if (constantsTable.containsKey(patternList.get(index).getProperty())) {
                        constantsTable.get(patternList.get(index).getProperty()).add(patternList.get(index).getObject());
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        valueList.add(patternList.get(index).getObject());
                        constantsTable.put(patternList.get(index).getProperty(), valueList);
                    }


                }

                //labelsTable could be deleted - no more used
                if (labelsTable.containsKey(patternList.get(index).getSubject())) {
                    labelsTable.get(patternList.get(index).getSubject()).add(patternList.get(index).getObject());
                } else {
                    List<String> labelList = new ArrayList<String>();
                    labelList.add(patternList.get(index).getObject());
                    labelsTable.put(patternList.get(index).getSubject(), labelList);
                }

            }
        }

        //add constants to rdfCache
        Iterator<String> iteratorCostantsTable = constantsTable.keySet().iterator();
        while (iteratorCostantsTable.hasNext()) {
            String keyClasse = iteratorCostantsTable.next();
            List<String> valueList = constantsTable.get(keyClasse);
            //add only if it is not datatypeProperty
            if (!isSamePropertyAs(keyClasse)) {
                for (int index = 0; index < valueList.size(); index++) {
                    addResourceToRdfCache(valueList.get(index).replaceAll("\"", ""), keyClasse, null, null, null, null, null, null, null, -1);
                }
            }
        }

        //write class og inputs of every service.
        writeServicesAndInputClass();
        writeServicesAndInputs();

        //add to constantstabele values of keys of API (fixed values)
        addFixedValueToConstantsTable();

        boolean nuoviValori = true;

        timeout = false;

        limitReached = false;

        //start timer
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //After Endpoint.TIMEOUT_DELAY timeout becomes true.
                timeout = true;
            }
        }, Endpoint.TIMEOUT_DELAY);

        //call free sources (services with no inputs)
        callFreeSources();

        //TODO change stop condition if necessary. Stop after 10 times.
        for (int i = 0; i < 10; i++) {
            if (nuoviValori & !timeout & !limitReached) {
                nuoviValori = getNewValuesFromApi();

                if (query.getLimit() > 0) {
                    //if there is a LIMIT, stop the process if limit is reached.
                    ExecuteQuery executeQuery = new ExecuteQuery();
                    Thread t = new Thread(executeQuery);
                    t.start();
                }
            } else {
                break;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        rdfCache.write(baos, "turtle");

        System.out.println(baos.toString());


        QueryExecution qexec = QueryExecutionFactory.create(query, rdfCache);
        ResultSet results = qexec.execSelect();

        // write to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ResultSetFormatter.outputAsJSON(outputStream, results);

        // and turn that into a String
        String jsonString = new String(outputStream.toByteArray());

        return jsonString;

    }

    public class ExecuteQuery implements Runnable {

        int resultsIndex;

        @Override
        public void run() {

            resultsIndex = 0;

            QueryExecution qexec = QueryExecutionFactory.create(query, rdfCache);
            ResultSet results = qexec.execSelect();

            //System.out.println("row numb: " + results.getRowNumber() );

            //number of results
            while (results.hasNext()) {
                results.nextSolution();
                resultsIndex++;
            }

            //System.out.println("results: " + resultsIndex );

            //check if results number is greater than the limit
            if (resultsIndex == query.getLimit()) {
                limitReached = true;
            }

        }

    }

    public void callFreeSources() {

        ArrayList<String> freeSources = getFreeSources();

        for (int indexSource = 0; indexSource < freeSources.size(); indexSource++) {

            String serviceString = freeSources.get(indexSource);

            String url = getUrlOfService(serviceString);

            //call service
            HttpURLConnectionAPI http = new HttpURLConnectionAPI();

            String response;

            List<String> result = new ArrayList<String>();
            ArrayList<String> elems = new ArrayList<String>();

            try {
                response = http.sendGet(url);

                if (response.startsWith("[")) {
                    response = "{ \"" + Endpoint.JSON_ARRAY_ROOT + "\" : " + response + " }";
                }

                ArrayList<String> outputURIList = getOutputsURIOfService(serviceString);

                String languageOfService = getLanguageOfService(serviceString);

                // XML
                if (languageOfService.equalsIgnoreCase("xml")) {

                    //System.out.println("response: " + response);

                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    InputSource is = new InputSource(new StringReader(response));
                    Document xmlDocument = builder.parse(is);

                    ExtractValueFromXML extractValueFromXML = new ExtractValueFromXML();

                    //extract outputs
                    for (int i = 0; i < outputURIList.size(); i++) {
                        result = extractValueFromXML.getValueList(model, xmlDocument, outputURIList.get(i));
                        String classe = getTypeOfResource(outputURIList.get(i));

                        if (classe != null) {
                            //if output = datatypeprop and there is a "samePropertyAs", use that as key
                            if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && isSamePropertyAs(classe)) {
                                classe = selectSamePropertyAs(classe).get(0);
                            }
                            // if there is not a samePropertyAs, ignore results (used only for RDFCache)
                            if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && !isSamePropertyAs(classe)) {
                                result.clear();
                            }

                            //every output can have more values
                            for (int j = 0; j < result.size(); j++) {
                                //add to rdfCache
                                addResourceToRdfCache(result.get(j), classe, outputURIList.get(i), null, null, null, null, null, null, 0);
                                //add to costantsTable
                                if (constantsTable.containsKey(classe)) {
                                    constantsTable.get(classe).add(result.get(j));
                                } else {
                                    List<String> valueList = new ArrayList<String>();
                                    valueList.add(result.get(j));
                                    constantsTable.put(classe, valueList);
                                }
                            }
                        }
                    }


                    // JSON
                } else if (languageOfService.equalsIgnoreCase("json")) {

                    //extract outputs
                    for (int i = 0; i < outputURIList.size(); i++) {
                        JsonResults jsonResults = new JsonResults();
                        String path_resource = jsonResults.getResults(model, outputURIList.get(i));
                        AnalizerPath end_path = new AnalizerPath();
                        elems = end_path.analizer(path_resource);
                        ValuesJSON values = new ValuesJSON();
                        result = values.getValuesData(response, elems);
                        String classe = getTypeOfResource(outputURIList.get(i));

                        if (classe != null) {
                            //if output = datatypeprop and there is a "samePropertyAs", use that as key
                            if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && isSamePropertyAs(classe)) {
                                classe = selectSamePropertyAs(classe).get(0);
                            }
                            // if there is not a samePropertyAs, ignore results (used only for RDFCache)
                            if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && !isSamePropertyAs(classe)) {
                                result.clear();
                            }

                            //every output can have more values
                            for (int j = 0; j < result.size(); j++) {
                                //add to rdfCache
                                addResourceToRdfCache(result.get(j), classe, outputURIList.get(i), null, null, null, null, null, null, 0);
                                //add to costantsTable
                                if (constantsTable.containsKey(classe)) {
                                    constantsTable.get(classe).add(result.get(j));
                                } else {
                                    List<String> valueList = new ArrayList<String>();
                                    valueList.add(result.get(j));
                                    constantsTable.put(classe, valueList);
                                }
                            }
                        }
                    }
                }

                //search for properties to link resources in rdfCache
                //search customResource related to the service

                ArrayList<String> addedResourcesList = new ArrayList<String>();
                addedResourcesList = getAddedResourcesOfService(serviceString);

                //add to rdfCache
                for (int i = 0; i < addedResourcesList.size(); i++) {
                    addAddedResourceWithPropertiesToRdfCache(addedResourcesList.get(i),
                            response,
                            languageOfService,
                            null);
                }

                //search for properties between outputs
                ArrayList<TriplePattern> tripleList = new ArrayList<TriplePattern>();
                tripleList = getPropertyBetweenOutputs(serviceString);

                //add to rdfCache for every property
                for (int i = 0; i < tripleList.size(); i++) {
                    addPropertyBetweenOutputsToRdfCache(tripleList.get(i).getSubject(),
                            tripleList.get(i).getProperty(),
                            tripleList.get(i).getObject(),
                            response,
                            languageOfService);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public boolean getNewValuesFromApi() {

        boolean newValues = false;

        // select services with all inputs available in constants table
        Iterator<String> iteratorServiceInputTypeTable = serviceInputTypeTable.keySet().iterator();
        while (iteratorServiceInputTypeTable.hasNext()) {


            boolean allInputsAvailable = true;
            String keyService = iteratorServiceInputTypeTable.next();
            List<String> typeList = serviceInputTypeTable.get(keyService);
            for (int index = 0; index < typeList.size(); index++) {

                if (!constantsTable.containsKey(typeList.get(index))) {

                    allInputsAvailable = false;
                }
            }

            if (allInputsAvailable) {
                Boolean newValuesSingleService = callService(keyService);

                if (newValues == false) {
                    newValues = newValuesSingleService;
                }

            }

        }

        return newValues;

    }


    public boolean callService(String serviceString) {

        boolean nuoviValori = false;


        //list of input uri in mapping file
        List<String> inputList = new ArrayList<String>();
        inputList = serviceInputsURITable.get(serviceString);

        //list of class of every input
        List<String> inputTypeList = new ArrayList<String>();
        inputTypeList = serviceInputTypeTable.get(serviceString);

        //list of inputs values
        List<String> inputValueList = new ArrayList<String>();


        int inputIndex = 0;
        //size: number of required parameters
        int[] valueIndex = new int[inputTypeList.size()];
        for (int i = 0; i < valueIndex.length; i++) {
            valueIndex[i] = 0;
        }
        int[] numberOfValues = new int[inputTypeList.size()];

        HashMap<String, String> params = new HashMap<String, String>();

        // put in numberOfValues
        for (int i = 0; i < inputTypeList.size(); i++) {
            inputValueList = constantsTable.get(inputTypeList.get(i));
            numberOfValues[i] = inputValueList.size();
        }

        //variable used to stop while
        Boolean increased = true;

        if (inputValueList.size() == 0) {
            increased = false;
        }


        //cartesian product of inputs
        while (increased == true) {

            //to check if parameters are already used
            String url = new String();
            url = getUrlOfService(serviceString) + "?";

            //put values in params
            for (inputIndex = 0; inputIndex < valueIndex.length; inputIndex++) {
                //input type = class (constants table)
                String inputType = inputTypeList.get(inputIndex);
                //inputValueList = list of values related to class (constants table)
                inputValueList = constantsTable.get(inputType);

                //System.out.println(getLabelOfResource(serviceInputsURITable.get(outputServiceTable.get(outputResourceList.get(index))).get(inputIndex)));
                //System.out.println(inputType);

                //for (int k=0; k<inputValueList.size(); k++){
                //System.out.println(inputValueList.get(k));
                //}

                //put variable and value in params
                params.put(getParamNameOfResource(inputList.get(inputIndex)),
                        inputValueList.get(valueIndex[inputIndex]).replaceAll("\"", ""));

                //add values to url, only for control and for avoiding identical calls
                url = url + getParamNameOfResource(inputList.get(inputIndex)) + "=" + inputValueList.get(valueIndex[inputIndex]).replaceAll("\"", "") + "&";
            }

            url = url.substring(0, url.length() - 1);


            //call the service only if we haven't already done it
            if (!calledServicesList.contains(url)) {

                nuoviValori = true;

                //System.out.println("url: " + url);

                //add url to the list
                calledServicesList.add(url);

                //call service
                HttpURLConnectionAPI http = new HttpURLConnectionAPI();

                String response;

                List<String> result = new ArrayList<String>();
                ArrayList<String> elems = new ArrayList<String>();

                try {

                    response = http.sendGet(getUrlOfService(serviceString), params);

                    if (response.startsWith("[")) {
                        response = "{ \"" + Endpoint.JSON_ARRAY_ROOT + "\" : " + response + " }";
                    }

                    ArrayList<String> outputURIList = getOutputsURIOfService(serviceString);

                    String languageOfService = getLanguageOfService(serviceString);

                    // XML
                    if (languageOfService.equalsIgnoreCase("xml")) {

                        //System.out.println("response: " + response);

                        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = builderFactory.newDocumentBuilder();
                        InputSource is = new InputSource(new StringReader(response));
                        Document xmlDocument = builder.parse(is);

                        ExtractValueFromXML extractValueFromXML = new ExtractValueFromXML();

                        //extract outputs
                        for (int i = 0; i < outputURIList.size(); i++) {
                            result = extractValueFromXML.getValueList(model, xmlDocument, outputURIList.get(i));
                            String classe = getTypeOfResource(outputURIList.get(i));

                            if (classe != null) {
                                //if output = datatypeprop and there is a "samePropertyAs", use that as key
                                if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && isSamePropertyAs(classe)) {
                                    classe = selectSamePropertyAs(classe).get(0);
                                }
                                // if there is not a samePropertyAs, ignore results (used only for RDFCache)
                                if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && !isSamePropertyAs(classe)) {
                                    result.clear();
                                }

                                //every output can have more values
                                for (int j = 0; j < result.size(); j++) {
                                    //add to rdfCache
                                    addResourceToRdfCache(result.get(j), classe, outputURIList.get(i), null, null, null, null, null, null, 0);
                                    //add to costantsTable
                                    if (constantsTable.containsKey(classe)) {
                                        constantsTable.get(classe).add(result.get(j));
                                    } else {
                                        List<String> valueList = new ArrayList<String>();
                                        valueList.add(result.get(j));
                                        constantsTable.put(classe, valueList);
                                    }
                                }
                            }
                        }


                        // JSON
                    } else if (languageOfService.equalsIgnoreCase("json")) {

                        //extract outputs
                        for (int i = 0; i < outputURIList.size(); i++) {
                            JsonResults jsonResults = new JsonResults();
                            String path_resource = jsonResults.getResults(model, outputURIList.get(i));
                            AnalizerPath end_path = new AnalizerPath();
                            elems = end_path.analizer(path_resource);
                            ValuesJSON values = new ValuesJSON();
                            result = values.getValuesData(response, elems);
                            String classe = getTypeOfResource(outputURIList.get(i));

                            if (classe != null) {
                                //if output = datatypeprop and there is a "samePropertyAs", use that as key
                                if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && isSamePropertyAs(classe)) {
                                    classe = selectSamePropertyAs(classe).get(0);
                                }
                                // if there is not a samePropertyAs, ignore results (used only for RDFCache)
                                if (classe.equalsIgnoreCase(OWL.DatatypeProperty.toString()) && !isSamePropertyAs(classe)) {
                                    result.clear();
                                }

                                //every output can have more values
                                for (int j = 0; j < result.size(); j++) {
                                    //add to rdfCache
                                    addResourceToRdfCache(result.get(j), classe, outputURIList.get(i), null, null, null, null, null, null, 0);
                                    //add to costantsTable
                                    if (constantsTable.containsKey(classe)) {
                                        constantsTable.get(classe).add(result.get(j));
                                    } else {
                                        List<String> valueList = new ArrayList<String>();
                                        valueList.add(result.get(j));
                                        constantsTable.put(classe, valueList);
                                    }
                                }
                            }
                        }
                    }

                    //search for properties to link resources in rdfCache
                    //search customResource related to the service

                    ArrayList<String> addedResourcesList = new ArrayList<String>();
                    addedResourcesList = getAddedResourcesOfService(serviceString);

                    //add to rdfCache
                    for (int i = 0; i < addedResourcesList.size(); i++) {
                        addAddedResourceWithPropertiesToRdfCache(addedResourcesList.get(i),
                                response,
                                languageOfService,
                                params);
                    }


                    // analyze properties linked to inputs
                    for (int indexInput = 0; indexInput < serviceInputsURITable.get(serviceString).size(); indexInput++) {

                        String inputURI = serviceInputsURITable.get(serviceString).get(indexInput);

                        //input as subject
                        ArrayList<String> propertyOfInput = new ArrayList<String>();
                        propertyOfInput = getPropertyOfResource(inputURI);
                        for (int indexProperty = 0; indexProperty < propertyOfInput.size(); indexProperty++) {
                            String property = propertyOfInput.get(indexProperty);

                            if (isDatatypeProperty(property)) {
                                if (isSamePropertyAs(property)) {

									/*
                                     *  argoments di addPropertyToRdfCache:
									 *  1: input label in rdfCache
									 *  2: input class in rdfCache
									 *  3: uri of property  ->  selectSameAs
									 *  4: object uri in mapping file -> property uri in case of datatypeProp
									 *  5: reply of service
									 *  6: language (XML or JSON)
									 */

                                    addPropertyToRdfCacheInputAsSubject(params.get(getParamNameOfResource(inputURI)),
                                            getTypeOfResource(inputURI),
                                            selectSamePropertyAs(property).get(0),
                                            property,
                                            response,
                                            languageOfService);
                                }
                            } else {
                                ArrayList<String> objectOfProperty = new ArrayList<String>();
                                objectOfProperty = getObjectURIOfPropertyOfResource(inputURI, property);
                                for (int indexObject = 0; indexObject < objectOfProperty.size(); indexObject++) {
                                    String object = objectOfProperty.get(indexObject);
									/*
									 *  argoments of addPropertyToRdfCache:
									 *  1: input label in rdfCache
									 *  2: input class in rdfCache
									 *  3: uri of property  ->  selectSameAs
									 *  4: object uri in mapping file -> property uri in case of datatypeProp
									 *  5: reply of service
									 *  6: language (XML or JSON)
									 */

                                    addPropertyToRdfCacheInputAsSubject(params.get(getParamNameOfResource(inputURI)),
                                            getTypeOfResource(inputURI),
                                            property,
                                            object,
                                            response,
                                            languageOfService);
                                }
                            }


                        }
                        //input as object
                        propertyOfInput = new ArrayList<String>();
                        propertyOfInput = getPropertyOfResourceAsObject(inputURI);
                        for (int indexProperty = 0; indexProperty < propertyOfInput.size(); indexProperty++) {
                            String property = propertyOfInput.get(indexProperty);
                            ArrayList<String> subjectOfProperty = new ArrayList<String>();
                            subjectOfProperty = getSubjectURIOfPropertyOfResource(inputURI, property);
                            for (int indexSubject = 0; indexSubject < subjectOfProperty.size(); indexSubject++) {
                                String subject = subjectOfProperty.get(indexSubject);
								/*
								 *  argoments of addPropertyToRdfCacheAsObject:
								 *  1: subject uri in mapping file
								 *  2: property uri
								 *  3: input label in rdfCache
								 *  4: input class in rdfCache
							 	 *  5: reply of service
								 *  6: language (XML or JSON)
								 */

                                addPropertyToRdfCacheInputAsObject(subject,
                                        property,
                                        params.get(getParamNameOfResource(inputURI)),
                                        getTypeOfResource(inputURI),
                                        response,
                                        languageOfService);
                            }
                        }

                        //input as property (datatypeProperty)
                        ArrayList<String> subjectOfProperty = new ArrayList<String>();
                        subjectOfProperty = getSubjectURIOfProperty(inputURI);
                        for (int indexSubject = 0; indexSubject < subjectOfProperty.size(); indexSubject++) {

                            String subject = subjectOfProperty.get(indexSubject);
								/*
								 *  argoments of addPropertyToRdfCacheAsObject:
								 *  1: subject uri in mapping file
								 *  2: property uri
								 *  3: input label in rdfCache
								 *  4: input class in rdfCache
							 	 *  5: reply of service
								 *  6: language (XML or JSON)
								 */

                            addPropertyToRdfCacheInputAsObject(subject,
                                    selectSamePropertyAs(inputURI).get(0),
                                    params.get(getParamNameOfResource(inputURI)),
                                    getTypeOfResource(inputURI),
                                    response,
                                    languageOfService);
                        }


                    }

                    //search for properties between outputs
                    ArrayList<TriplePattern> tripleList = new ArrayList<TriplePattern>();
                    tripleList = getPropertyBetweenOutputs(serviceString);

                    //add to rdfCache for every property
                    for (int i = 0; i < tripleList.size(); i++) {
                        addPropertyBetweenOutputsToRdfCache(tripleList.get(i).getSubject(),
                                tripleList.get(i).getProperty(),
                                tripleList.get(i).getObject(),
                                response,
                                languageOfService);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            //increment indexes
            increased = false;

            inputIndex = 0;


            while (increased == false & inputIndex < valueIndex.length) {
                if (valueIndex[inputIndex] < numberOfValues[inputIndex] - 1) {
                    valueIndex[inputIndex]++;
                    increased = true;
                } else {
                    inputIndex++;
                }
            }

            //reset values
            for (int i = 0; i < inputIndex; i++) {
                valueIndex[i] = 0;
            }


        }

        return nuoviValori;

    }


    public void addPropertyToRdfCacheInputAsSubject(String labelOfSubject,
                                                    String typeOfSubject,
                                                    String propertyURI,
                                                    String objectURI,
                                                    String response,
                                                    String languageOfService) throws ParserConfigurationException, SAXException, IOException {

        String subjectURIrdfCache = getUriInRdfCache(labelOfSubject, typeOfSubject);

        String typeOfObject = getTypeOfResource(objectURI);
        List<String> result = new ArrayList<String>();

        //list with single value to use the method for extracting values (which requires a list)
        List<String> objectURIList = new ArrayList<String>();
        objectURIList.add(objectURI);

        if (languageOfService.equalsIgnoreCase("xml")) {

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            ExtractValueFromXML extractValueFromXML = new ExtractValueFromXML();

            result = extractValueFromXML.getValueList(model, xmlDocument, objectURIList.get(0));


        } else if (languageOfService.equalsIgnoreCase("json")) {
            ArrayList<String> elems = new ArrayList<String>();

            JsonResults jsonResults = new JsonResults();
            String path_resource;
            path_resource = jsonResults.getResults(model, objectURIList.get(0));

            AnalizerPath end_path = new AnalizerPath();
            elems = end_path.analizer(path_resource);

            ValuesJSON values = new ValuesJSON();
            result = values.getValuesData(response, elems);
        }

        for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {

            //custom resources already added
            if (typeOfObject == null) {
                // datatype
                if (isSamePropertyAs(propertyURI)) {
                    Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                    Property property = rdfCache.createProperty(propertyURI);
                    resourceSubj.addLiteral(property, result.get(resultIndex).replaceAll("\"", ""));
                }
            } else {
                if (!typeOfObject.equalsIgnoreCase(Endpoint.ADDED_RESOURCE)) {
                    // datatype
                    if (isSamePropertyAs(propertyURI)) {
                        //maybe this is useless
                        Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                        Property property = rdfCache.createProperty(propertyURI);
                        resourceSubj.addLiteral(property, result.get(resultIndex).replaceAll("\"", ""));
                    } else {
                        //link two resources - no datatype
                        String objectURIrdfCache = getUriInRdfCache(result.get(resultIndex), typeOfObject);
                        Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                        Resource resourceObj = rdfCache.createResource(objectURIrdfCache);
                        Property property = rdfCache.createProperty(propertyURI);
                        resourceSubj.addProperty(property, resourceObj);
                    }
                }
            }
        }

    }


    public void addPropertyToRdfCacheInputAsObject(
            String subjectURI,
            String propertyURI,
            String labelOfObject,
            String typeOfObject,
            String response,
            String languageOfService) throws ParserConfigurationException, SAXException, IOException {

        String objectURIrdfCache = getUriInRdfCache(labelOfObject, typeOfObject);

        String typeOfSubject = getTypeOfResource(subjectURI);
        List<String> result = new ArrayList<String>();

        //list with single value to use the method for extracting values (which requires a list)
        List<String> subjectURIList = new ArrayList<String>();
        subjectURIList.add(subjectURI);

        if (languageOfService.equalsIgnoreCase("xml")) {

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            ExtractValueFromXML extractValueFromXML = new ExtractValueFromXML();

            result = extractValueFromXML.getValueList(model, xmlDocument, subjectURIList.get(0));


        } else if (languageOfService.equalsIgnoreCase("json")) {
            ArrayList<String> elems = new ArrayList<String>();

            JsonResults jsonResults = new JsonResults();
            String path_resource;
            path_resource = jsonResults.getResults(model, subjectURIList.get(0));

            AnalizerPath end_path = new AnalizerPath();
            elems = end_path.analizer(path_resource);

            ValuesJSON values = new ValuesJSON();
            result = values.getValuesData(response, elems);
        }

        for (int resultIndex = 0; resultIndex < result.size(); resultIndex++) {

            if (isSamePropertyAs(propertyURI)) {                // datatypeProperty
                String subjectURIrdfCache = getUriInRdfCache(result.get(resultIndex), typeOfSubject);
                Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                Property property = rdfCache.createProperty(propertyURI);
                resourceSubj.addLiteral(property, labelOfObject);

            } else {                //link between two resources (no datatype)
                String subjectURIrdfCache = getUriInRdfCache(result.get(resultIndex), typeOfSubject);
                Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                Resource resourceObj = rdfCache.createResource(objectURIrdfCache);
                Property property = rdfCache.createProperty(propertyURI);
                resourceSubj.addProperty(property, resourceObj);
            }
        }

    }


    public void addAddedResourceWithPropertiesToRdfCache(String addedResourceURIModel, String response, String languageOfService, HashMap<String, String> params) throws ParserConfigurationException, SAXException, IOException {

        customResourceIndex++;

        //output related to customResource
        ArrayList<String> linkedOutputsList = new ArrayList<String>();
        linkedOutputsList = getOutputRelatedToResource(addedResourceURIModel);

        String antenatoComuneURI = getCommonAncestor(linkedOutputsList);

        ArrayList<String> propertyListResIsSub = new ArrayList<String>();
        //property with custom resource as subject
        propertyListResIsSub = getPropertyOfResource(addedResourceURIModel);

        ArrayList<String> propertyListResIsOb = new ArrayList<String>();
        //property with custom resource as object
        propertyListResIsOb = getPropertyOfResourceAsObject(addedResourceURIModel);

        //values extracted from xml or json
        ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsSub = new ArrayList<HashMap<Integer, ArrayList<String>>>();
        ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsOb = new ArrayList<HashMap<Integer, ArrayList<String>>>();

        //property-value in case subject or object is input
        HashMap<String, String> inputIsSubject = new HashMap<String, String>();
        HashMap<String, String> inputIsObject = new HashMap<String, String>();

        if (languageOfService.equalsIgnoreCase("xml")) {

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            ExtractValueFromXML extractValueFromXML = new ExtractValueFromXML();


            for (int propertyIndex = 0; propertyIndex < propertyListResIsSub.size(); propertyIndex++) {
                HashMap<Integer, ArrayList<String>> valuesMap = new HashMap<Integer, ArrayList<String>>();

                String objectURI = "";

                if (isDatatypeProperty(propertyListResIsSub.get(propertyIndex))) {
                    if (isSamePropertyAs(propertyListResIsSub.get(propertyIndex))) {
                        String prop = propertyListResIsSub.get(propertyIndex);
                        objectURI = prop;
                        //propertyListResIsSub.set(propertyIndex, selectSamePropertyAs(prop).get(0));
                        propertyListResIsSub.set(propertyIndex, prop);
                    }

                } else {
                    objectURI = getObjectURIOfPropertyOfResource(addedResourceURIModel, propertyListResIsSub.get(propertyIndex)).get(0);
                }


                //resource is input
                if (resourceIsInput(objectURI)) {

                    ArrayList<String> list = new ArrayList<String>();

                    //list.add(params.get(getLabelOfResource(objectURI)));

                    list.add("inputIsObject");
                    valuesMap.put(-1, list);

                    inputIsObject.put(propertyListResIsSub.get(propertyIndex), params.get(getParamNameOfResource(objectURI)));

                    //resource is output
                } else {
                    valuesMap = extractValueFromXML.getValueList(model, xmlDocument, objectURI, antenatoComuneURI);
                }

                valuesArrayResIsSub.add(valuesMap);
            }


            for (int propertyIndex = 0; propertyIndex < propertyListResIsOb.size(); propertyIndex++) {
                HashMap<Integer, ArrayList<String>> valuesMap = new HashMap<Integer, ArrayList<String>>();

                String subjectURI = getSubjectURIOfPropertyOfResource(addedResourceURIModel, propertyListResIsOb.get(propertyIndex)).get(0);

                if (resourceIsInput(subjectURI)) {

                    ArrayList<String> list = new ArrayList<String>();

                    //list.add(params.get(getLabelOfResource(subjectURI)));

                    valuesMap.put(-1, list);

                    inputIsSubject.put(propertyListResIsOb.get(propertyIndex), params.get(getParamNameOfResource(subjectURI)));

                } else {
                    valuesMap = extractValueFromXML.getValueList(model, xmlDocument, subjectURI, antenatoComuneURI);
                }

                valuesArrayResIsOb.add(valuesMap);
            }


        } else if (languageOfService.equalsIgnoreCase("json")) {

            for (int propertyIndex = 0; propertyIndex < propertyListResIsSub.size(); propertyIndex++) {
                HashMap<Integer, ArrayList<String>> valuesMap = new HashMap<Integer, ArrayList<String>>();

                String objectURI = "";

                if (isDatatypeProperty(propertyListResIsSub.get(propertyIndex))) {
                    if (isSamePropertyAs(propertyListResIsSub.get(propertyIndex))) {
                        String prop = propertyListResIsSub.get(propertyIndex);
                        objectURI = prop;
                        //propertyListResIsSub.set(propertyIndex, selectSamePropertyAs(prop).get(0));
                        propertyListResIsSub.set(propertyIndex, prop);
                    }

                } else {
                    objectURI = getObjectURIOfPropertyOfResource(addedResourceURIModel, propertyListResIsSub.get(propertyIndex)).get(0);
                }


                //resource is input
                if (resourceIsInput(objectURI)) {

                    ArrayList<String> list = new ArrayList<String>();

                    list.add("inputIsObject");

                    //insert an empty list - use inputIsObject to store value
                    valuesMap.put(-1, list);

                    inputIsObject.put(propertyListResIsSub.get(propertyIndex), params.get(getParamNameOfResource(objectURI)));

                    //risource is output
                } else {


                    ArrayList<String> elems = new ArrayList<String>();

                    String path_resource;

                    JsonResults jsonResults = new JsonResults();
                    path_resource = jsonResults.getResults(model, objectURI);

                    AnalizerPath end_path = new AnalizerPath();
                    elems = end_path.analizer(path_resource);

                    JoinResource valuesRes = new JoinResource();
                    valuesMap = valuesRes.getValuesData(response, elems);

                }

                valuesArrayResIsSub.add(valuesMap);
            }

            for (int propertyIndex = 0; propertyIndex < propertyListResIsOb.size(); propertyIndex++) {
                HashMap<Integer, ArrayList<String>> valuesMap = new HashMap<Integer, ArrayList<String>>();

                String subjectURI = getSubjectURIOfPropertyOfResource(addedResourceURIModel, propertyListResIsOb.get(propertyIndex)).get(0);

                if (resourceIsInput(subjectURI)) {

                    ArrayList<String> list = new ArrayList<String>();

                    //list.add(params.get(getLabelOfResource(subjectURI)));

                    valuesMap.put(-1, list);

                    inputIsSubject.put(propertyListResIsOb.get(propertyIndex), params.get(getParamNameOfResource(subjectURI)));

                } else {
                    ArrayList<String> elems = new ArrayList<String>();

                    String path_resource;

                    JsonResults jsonResults = new JsonResults();
                    path_resource = jsonResults.getResults(model, subjectURI);

                    AnalizerPath end_path = new AnalizerPath();
                    elems = end_path.analizer(path_resource);

                    JoinResource valuesRes = new JoinResource();
                    valuesMap = valuesRes.getValuesData(response, elems);
                }

                valuesArrayResIsOb.add(valuesMap);
            }

        }

        //scan hashmap list and add resources and propertires to rdfCache
        //res is subject
        for (int indexLista = 0; indexLista < valuesArrayResIsSub.size(); indexLista++) {

            Property property = rdfCache.createProperty(propertyListResIsSub.get(indexLista));

            //iterator on hashmap
            Iterator<Integer> iteratorHashMap = valuesArrayResIsSub.get(indexLista).keySet().iterator();
            while (iteratorHashMap.hasNext()) {

                int key = iteratorHashMap.next();

                String addedResourceLabel = addedResourceURIModel + "R" + key + "-" + customResourceIndex;

                //if (key != -1) {
                    addResourceToRdfCache(addedResourceLabel, Endpoint.ADDED_RESOURCE, addedResourceURIModel,
                            propertyListResIsSub,    // List of properties with customResource as subject
                            propertyListResIsOb,    // List of properties with customResource as object
                            valuesArrayResIsSub,    //key: index of property (in propertyListResIsSub) and value extracted from xml or json
                            valuesArrayResIsOb,        //key: index of property (in propertyListResIsOb) and value extracted from xml or json
                            inputIsSubject,            // key: uri of property and value of input
                            inputIsObject,            // key: uri of property and value of input
                            key
                    );
                //}

                List<String> objectValues = valuesArrayResIsSub.get(indexLista).get(key);

                String subjectURIrdfCache = getUriInRdfCache(addedResourceLabel, Endpoint.ADDED_RESOURCE);

                for (int i = 0; i < objectValues.size(); i++) {

                    //String typeOfObject = getTypeOfResource(getObjectURIOfPropertyOfResource(addedResourceURIModel, propertyListResIsSub.get(key)).get(0));

                    String typeOfObject = getTypeOfResource(getObjectURIOfPropertyOfResource(addedResourceURIModel, property.getURI()).get(0));


                    String objectURIrdfCache = getUriInRdfCache(objectValues.get(i), typeOfObject);

                    Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                    Resource resourceObj = rdfCache.createResource(objectURIrdfCache);

                    if (isSamePropertyAs(property.getURI())) {
                        String objectString = "";
                        // DatatypeProperty case
                        if (key == -1 && inputIsObject.size() > i && objectValues.get(i).equals("inputIsObject")) {
                            objectString = inputIsObject.get(property.getURI()).replaceAll("\"", "");
                        } else {
                            objectString = objectValues.get(i).replaceAll("\"", "");
                        }
                        resourceSubj.addLiteral(rdfCache.createProperty(selectSamePropertyAs(property.getURI()).get(0)), objectString);
                    } else {
                        // normal case
                        resourceSubj.addProperty(property, resourceObj);
                    }


                    //add properties related to inputs
                    Iterator<String> iteratorInputObj = inputIsObject.keySet().iterator();
                    while (iteratorInputObj.hasNext()) {
                        String propertyInputKey = iteratorInputObj.next();
                        String typeOfInput = getTypeOfResource(getObjectURIOfPropertyOfResource(addedResourceURIModel, propertyInputKey).get(0));
                        String objectURIrdfCacheInput = getUriInRdfCache(inputIsObject.get(propertyInputKey), typeOfInput);
                        Resource resourceObjInput = rdfCache.createResource(objectURIrdfCacheInput);
                        Property propertyInput = rdfCache.createProperty(propertyInputKey);
                        if (isSamePropertyAs(propertyInputKey)) {
                            resourceSubj.addLiteral(rdfCache.createProperty(selectSamePropertyAs(propertyInputKey).get(0)), inputIsObject.get(propertyInputKey).replaceAll("\"", ""));
                        } else {
                            resourceSubj.addProperty(propertyInput, resourceObjInput);
                        }
                    }

                    Iterator<String> iteratorInputSubj = inputIsSubject.keySet().iterator();
                    while (iteratorInputSubj.hasNext()) {
                        String propertyInputKey = iteratorInputSubj.next();
                        String typeOfInput = getTypeOfResource(getSubjectURIOfPropertyOfResource(addedResourceURIModel, propertyInputKey).get(0));
                        String subjectURIrdfCacheInput = getUriInRdfCache(inputIsSubject.get(propertyInputKey), typeOfInput);
                        Resource resourceSubjInput = rdfCache.createResource(subjectURIrdfCacheInput);
                        Property propertyInput = rdfCache.createProperty(propertyInputKey);
                        resourceSubjInput.addProperty(propertyInput, resourceSubj);
                    }

                }

            }
        }

        //res is object
        for (int indexLista = 0; indexLista < valuesArrayResIsOb.size(); indexLista++) {

            Property property = rdfCache.createProperty(propertyListResIsOb.get(indexLista));

            //iterator on hashmap
            Iterator<Integer> iteratorHashMap = valuesArrayResIsOb.get(indexLista).keySet().iterator();
            while (iteratorHashMap.hasNext()) {
                int key = iteratorHashMap.next();

                String addedResourceLabel = addedResourceURIModel + "R" + key + "-" + customResourceIndex;
                if (key != -1) {
                    addResourceToRdfCache(addedResourceLabel, Endpoint.ADDED_RESOURCE, addedResourceURIModel,
                            propertyListResIsSub,    // List of properties with customResource as subject
                            propertyListResIsOb,    // List of properties with customResource as object
                            valuesArrayResIsSub,    //key: index of property (in propertyListResIsSub) and value extracted from xml or json
                            valuesArrayResIsOb,        ///key: index of property (in propertyListResIsOb) and value extracted from xml or json
                            inputIsSubject,            // key: uri of property and value of input
                            inputIsObject,            // key: uri of property and value of input
                            key
                    );
                }

                List<String> subjectValues = valuesArrayResIsOb.get(indexLista).get(key);

                String objectURIrdfCache = getUriInRdfCache(addedResourceLabel, Endpoint.ADDED_RESOURCE);

                for (int i = 0; i < subjectValues.size(); i++) {

                    //String typeOfSubject = getTypeOfResource(getSubjectURIOfPropertyOfResource(addedResourceURIModel, propertyListResIsOb.get(key)).get(0));
                    String typeOfSubject = getTypeOfResource(getSubjectURIOfPropertyOfResource(addedResourceURIModel, property.getURI()).get(0));

                    String subjectURIrdfCache = getUriInRdfCache(subjectValues.get(i), typeOfSubject);

                    Resource resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                    Resource resourceObj = rdfCache.createResource(objectURIrdfCache);


                    resourceSubj.addProperty(property, resourceObj);


                    //add properties related to inputs
                    Iterator<String> iteratorInputObj = inputIsObject.keySet().iterator();
                    while (iteratorInputObj.hasNext()) {
                        String propertyInputKey = iteratorInputObj.next();
                        String typeOfInput = getTypeOfResource(getObjectURIOfPropertyOfResource(addedResourceURIModel, propertyInputKey).get(0));
                        String objectURIrdfCacheInput = getUriInRdfCache(inputIsObject.get(propertyInputKey), typeOfInput);
                        Resource resourceObjInput = rdfCache.createResource(objectURIrdfCacheInput);
                        Property propertyInput = rdfCache.createProperty(propertyInputKey);
                        resourceObjInput.addProperty(propertyInput, resourceObj);
                    }

                    Iterator<String> iteratorInputSubj = inputIsSubject.keySet().iterator();
                    while (iteratorInputSubj.hasNext()) {
                        String propertyInputKey = iteratorInputSubj.next();
                        String typeOfInput = getTypeOfResource(getSubjectURIOfPropertyOfResource(addedResourceURIModel, propertyInputKey).get(0));
                        String subjectURIrdfCacheInput = getUriInRdfCache(inputIsSubject.get(propertyInputKey), typeOfInput);
                        Resource resourceSubjInput = rdfCache.createResource(subjectURIrdfCacheInput);
                        Property propertyInput = rdfCache.createProperty(propertyInputKey);
                        resourceSubjInput.addProperty(propertyInput, resourceObj);
                    }

                }

            }
        }

    }

    public String getCommonAncestor(ArrayList<String> outputList) {
        if (outputList.size() == 0) {
            return null;
        } else if (outputList.size() == 1) {
            return outputList.get(0);
        } else {
            String antenatoComuneURI = null;
            ArrayList<String> percorso = new ArrayList<String>();
            String resource = outputList.get(0);
            String father;
            do {
                father = getFatherURI(resource);
                percorso.add(father);
                resource = father;

            } while (father != null);

            for (int i = 1; i < outputList.size(); i++) {
                resource = outputList.get(i);
                do {
                    father = getFatherURI(resource);
                    if (percorso.contains(father)) {
                        antenatoComuneURI = father;
                    }
                    resource = father;
                } while (antenatoComuneURI == null);

                percorso = new ArrayList<String>();
                resource = antenatoComuneURI;
                percorso.add(resource);
                father = getFatherURI(resource);
                while (father != null) {
                    percorso.add(father);
                    resource = father;
                    father = getFatherURI(resource);
                }

                antenatoComuneURI = null;

            }


            return percorso.get(0);
        }
    }

    public void addPropertyBetweenOutputsToRdfCache(String subjectURIModel,
                                                    String propertyURI,
                                                    String objectURIModel,
                                                    String response,
                                                    String languageOfService) throws ParserConfigurationException, SAXException, IOException, JSONException {

        if (isSamePropertyAs(propertyURI)) {
            objectURIModel = propertyURI;
            propertyURI = selectSamePropertyAs(propertyURI).get(0);
        }


        String subjectURIrdfCache;
        String objectURIrdfCache;
        Resource resourceSubj;
        Resource resourceObj;
        Property property;

        String typeOfSubject = getTypeOfResource(subjectURIModel);
        String typeOfObject = getTypeOfResource(objectURIModel);

        if (languageOfService.equalsIgnoreCase("xml")) {

            String commonAncestorURI = null;
            ArrayList<String> subjectPath = new ArrayList<String>();
            String resource = subjectURIModel;
            String father;
            do {
                father = getFatherURI(resource);
                subjectPath.add(father);
                resource = father;
            } while (father != null);

            resource = objectURIModel;
            do {
                father = getFatherURI(resource);
                if (subjectPath.contains(father)) {
                    commonAncestorURI = father;
                }
                resource = father;
            } while (commonAncestorURI == null);

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            ExtractValueFromXML extractValueFromXML = new ExtractValueFromXML();

            HashMap<Integer, ArrayList<String>> subjectValuesMap = new HashMap<Integer, ArrayList<String>>();
            HashMap<Integer, ArrayList<String>> objectValuesMap = new HashMap<Integer, ArrayList<String>>();

            subjectValuesMap = extractValueFromXML.getValueList(model, xmlDocument, subjectURIModel, commonAncestorURI);
            objectValuesMap = extractValueFromXML.getValueList(model, xmlDocument, objectURIModel, commonAncestorURI);


            Iterator<Integer> iteratorSubject = subjectValuesMap.keySet().iterator();
            while (iteratorSubject.hasNext()) {
                int key = iteratorSubject.next();

                List<String> subjectValues = subjectValuesMap.get(key);
                List<String> objectValues = objectValuesMap.get(key);

                for (int indexSubject = 0; indexSubject < subjectValues.size(); indexSubject++) {
                    for (int indexObject = 0; indexObject < objectValues.size(); indexObject++) {

                        subjectURIrdfCache = getUriInRdfCache(subjectValues.get(indexSubject), typeOfSubject);

                        resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                        property = rdfCache.createProperty(propertyURI);

                        if (isSamePropertyAs(propertyURI)) {
                            //resourceSubj.addLiteral(property, objectValues.get(indexObject).replaceAll("\"", ""));
                            resourceSubj.addLiteral(property, objectValues.get(indexObject));
                        } else {
                            objectURIrdfCache = getUriInRdfCache(objectValues.get(indexObject), typeOfObject);
                            resourceObj = rdfCache.createResource(objectURIrdfCache);

                            resourceSubj.addProperty(property, resourceObj);
                        }

                    }
                }
            }


        } else if (languageOfService.equalsIgnoreCase("json")) {

            String path_subjResource, path_objResource;

            ArrayList<String> subjElems = new ArrayList<String>();
            ArrayList<String> objElems = new ArrayList<String>();

            JSONObject jsonObject = new JSONObject(response);
            JSONObject root = new JSONObject();
            JSONArray results = new JSONArray();
            results.put(jsonObject);
            root.put("results", results);


            JsonResults jsonResults = new JsonResults();
            path_subjResource = jsonResults.getResults(model, subjectURIModel);
            jsonResults = new JsonResults();
            path_objResource = jsonResults.getResults(model, objectURIModel);

            AnalizerPath end_path = new AnalizerPath();
            subjElems = end_path.analizer(path_subjResource);
            subjElems.add(0, "results");
            end_path = new AnalizerPath();
            objElems = end_path.analizer(path_objResource);
            objElems.add(0, "results");

            List<HashMap<Integer, String>> tableList = new ArrayList<HashMap<Integer, String>>();
            HashMap<Integer, String> subjectList = new HashMap<Integer, String>();
            HashMap<Integer, String> objectList = new HashMap<Integer, String>();

            JoinValueOutputs values = new JoinValueOutputs();
            tableList = values.joinValuesData(root.toString(), subjElems, objElems);

            subjectList = tableList.get(0);
            objectList = tableList.get(1);

            Iterator<Entry<Integer, String>> iterSubjList = subjectList.entrySet().iterator();


            while (iterSubjList.hasNext()) {

                Entry<Integer, String> pairSubj = iterSubjList.next();
                Iterator<Entry<Integer, String>> iterObjList = objectList.entrySet().iterator();

                while (iterObjList.hasNext()) {
                    Entry<Integer, String> pairObj = iterObjList.next();
                    if (pairSubj.getKey() == pairObj.getKey()) {

                        subjectURIrdfCache = getUriInRdfCache(pairSubj.getValue().toString(), typeOfSubject);
                        objectURIrdfCache = getUriInRdfCache(pairObj.getValue().toString(), typeOfObject);

                        resourceSubj = rdfCache.createResource(subjectURIrdfCache);
                        resourceObj = rdfCache.createResource(objectURIrdfCache);
                        property = rdfCache.createProperty(propertyURI);
                        if (isSamePropertyAs(propertyURI)) {
                            resourceSubj.addLiteral(property, pairObj.getValue().toString().replaceAll("\"", ""));
                        } else {
                            resourceSubj.addProperty(property, resourceObj);
                        }
                    }
                }
            }

        }
    }


    /**
     * Check if a resource with a label and class already exists, if not it add a new resource
     *
     * @param label
     * @param type
     * @param mappingURI
     * @param propertyListResIsSub List of property with customResource as subject
     * @param propertyListResIsOb  List of property with customResource as object
     * @param valuesArrayResIsSub  key: index of property (in propertyListResIsSub) and value extracted from xml or json
     * @param valuesArrayResIsOb   key: index of property (in propertyListResIsOb) and value extracted from xml or json
     * @param inputIsSubject       key: uri of property and value of input
     * @param inputIsObject        key: uri of property and value of input
     */
    public void addResourceToRdfCache(String label, String type, String mappingURI,
                                      ArrayList<String> propertyListResIsSub,
                                      ArrayList<String> propertyListResIsOb,
                                      ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsSub,
                                      ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsOb,
                                      HashMap<String, String> inputIsSubject,
                                      HashMap<String, String> inputIsObject,
                                      int key) {

        if (label.startsWith("\"")) {
            label = label.substring(1, label.length() - 1);
        }

        String queryStr = "select ?resourceURI where { "
                + "?resourceURI <" + RDFS.label + "> \"" + label + "\" ."
                + "?resourceURI <" + RDF.type + "> <" + type + "> }";


        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, rdfCache);
        ResultSet result = qexec.execSelect();

        //add resource only if the previous query get no results, else finish
        if (!result.hasNext()) {

            String resourceUriString = "";


            //mappingURI is null for resources extracted from query graph pattern
            if (mappingURI != null) {
                //check if there is findUri property
                String queryStrMapping = "select ?classFindURI where { "
                        + "<" + mappingURI + "> <" + Endpoint.FIND_URI + "> ?classFindURI "
                        + " }";


                Query queryMapping = QueryFactory.create(queryStrMapping);
                QueryExecution qexecMapping = QueryExecutionFactory.create(queryMapping, model);
                ResultSet resultMapping = qexecMapping.execSelect();

                if (resultMapping.hasNext()) {
                    //call the class specified by the user
                    QuerySolution solution = resultMapping.nextSolution();
                    String className = solution.getLiteral("classFindURI").toString();
                    //System.out.println(className);
                    try {
                        Class<?> cls = Class.forName(className);
                        Object obj = cls.newInstance();

                        //custom parameter
                        Class[] paramCustom = new Class[9];
                        paramCustom[0] = ArrayList.class;
                        paramCustom[1] = ArrayList.class;
                        paramCustom[2] = ArrayList.class;
                        paramCustom[3] = ArrayList.class;
                        paramCustom[4] = ArrayList.class;
                        paramCustom[5] = HashMap.class;
                        paramCustom[6] = HashMap.class;
                        paramCustom[7] = Integer.class;
                        paramCustom[8] = Model.class;


                        Method method = cls.getDeclaredMethod("getResourceURI", paramCustom);
                        ArrayList<String> arrayList = new ArrayList<String>();

                        arrayList.add(label);
                        arrayList.add(type);

                        resourceUriString = (String) method.invoke(obj, arrayList, propertyListResIsSub, propertyListResIsOb, valuesArrayResIsSub, valuesArrayResIsOb, inputIsSubject, inputIsObject, key, model);

                    } catch (Exception e) {
                        // Auto-generated catch block
                        e.printStackTrace();
                    }

                } else {
                    //if there isn't findURI property
                    resourceUriString = Endpoint.CUSTOM_NAMESPACE + Endpoint.RESOURCE_URI_STRING + resourceIndex;
                    resourceIndex++;
                }
            } else {
                //if mappingURI == null
                resourceUriString = Endpoint.CUSTOM_NAMESPACE + Endpoint.RESOURCE_URI_STRING + resourceIndex;
                resourceIndex++;
            }

            Resource resource = rdfCache.createResource(resourceUriString);
            Resource classeResource = rdfCache.createResource(type);
            resource.addProperty(RDF.type, classeResource);
            resource.addLiteral(RDFS.label, label.replaceAll("\"", ""));
        }

    }


    /**
     * Write in serviceInputsUriTable the uri of inputs for every service
     */
    public void writeServicesAndInputs() {

        String queryStr = "select ?service ?input where { "
                + "?service <" + Endpoint.INPUT_PROPERTY + "> ?input . }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource service = solution.getResource("service");
            Resource input = solution.getResource("input");

            if (serviceInputsURITable.containsKey(service.toString())) {
                serviceInputsURITable.get(service.toString()).add(input.toString());
            } else {
                List<String> valueList = new ArrayList<String>();
                valueList.add(input.toString());
                serviceInputsURITable.put(service.toString(), valueList);
            }

        }

    }

    /**
     * Write in serviceInputTypeTable the class of inputs for every service
     */
    public void writeServicesAndInputClass() {

        String queryStr = "select ?service ?class ?input where { "
                + "?service <" + Endpoint.INPUT_PROPERTY + "> ?input ."
                + "?input <" + RDF.type + "> ?class }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource service = solution.getResource("service");
            Resource classInput = solution.getResource("class");
            Resource input = solution.getResource("input");

            if (!classInput.toString().equalsIgnoreCase(OWL.DatatypeProperty.toString())) {
                if (serviceInputTypeTable.containsKey(service.toString())) {
                    serviceInputTypeTable.get(service.toString()).add(classInput.toString());
                } else {
                    List<String> valueList = new ArrayList<String>();
                    valueList.add(classInput.toString());
                    serviceInputTypeTable.put(service.toString(), valueList);
                }
            } else { //nel caso in cui class = owl:DatatypeProperty, devo cercare la proprietà sameAs
                String queryStr2 = "select ?sameProp where { "
                        + "<" + input.toString() + "> <" + Endpoint.SAME_PROPERTY_AS + "> ?sameProp "
                        + " }";

                Query query2 = QueryFactory.create(queryStr2);
                QueryExecution qexec2 = QueryExecutionFactory.create(query2, model);
                ResultSet result2 = qexec2.execSelect();

                if (result2.hasNext()) {
                    QuerySolution solution2 = result2.nextSolution();
                    Resource sameProp = solution2.getResource("sameProp");

                    if (serviceInputTypeTable.containsKey(service.toString())) {
                        serviceInputTypeTable.get(service.toString()).add(sameProp.toString());
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        valueList.add(sameProp.toString());
                        serviceInputTypeTable.put(service.toString(), valueList);
                    }
                }
            }


        }

    }


    public String getFatherURI(String resource) {

        String queryStr = "select ?subject where { "
                + "?subject <" + Endpoint.ATTRIBUTE_PROPERTY + "> <" + resource + "> }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource subject = solution.getResource("subject");

            return subject.toString();
        }

        queryStr = "select ?subject where { "
                + "?subject <" + Endpoint.LI_PROPERTY + "> <" + resource + "> }";

        query = QueryFactory.create(queryStr);
        qexec = QueryExecutionFactory.create(query, model);
        result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource subject = solution.getResource("subject");
            return subject.toString();
        } else {
            return null;
        }
    }

    public boolean resourceIsInput(String resource) {

        String queryStr = "select ?service where { "
                + "?service <" + Endpoint.INPUT_PROPERTY + "> <" + resource + "> }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<String> getOutputURIOfType(String type) {
        ArrayList<String> resourceURIList = new ArrayList<String>();

        String queryStr = "select ?resource where { "
                + "?resource <" + RDF.type + "> <" + type + "> ."
                + "?resource <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource resource = solution.getResource("resource");
            resourceURIList.add(resource.toString());
        }

        return resourceURIList;
    }

    /**
     * Select sources with no inputs
     *
     * @return
     */
    public ArrayList<String> getFreeSources() {
        ArrayList<String> freeSources = new ArrayList<String>();

        String queryStr = "select ?source where { " +
                "?source <" + Endpoint.URL_PROPERTY + "> ?url " +
                "MINUS " +
                "{ " +
                "?source <" + Endpoint.INPUT_PROPERTY + "> ?input " +
                "} " +
                "}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource resource = solution.getResource("source");
            freeSources.add(resource.toString());
        }

        return freeSources;
    }

    public ArrayList<String> getOutputsURIOfService(String serviceURI) {
        ArrayList<String> outputURIList = new ArrayList<String>();

        String queryStr = "select ?output where {"
                + "?output <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + serviceURI + "> . "
                + "?output <" + Endpoint.ISDATA_PROPERTY + "> true }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource output = solution.getResource("output");
            outputURIList.add(output.toString());
        }

        return outputURIList;
    }


    public ArrayList<String> getPropertyOfResource(String subjectString) {
        ArrayList<String> propertyList = new ArrayList<String>();

        String queryStr = "select ?prop where {"
                + "<" + subjectString + "> ?prop ?obj } ";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource property = solution.getResource("prop");
            String propertyString = property.toString();

            if (!propertyString.equalsIgnoreCase(Endpoint.PARAM_NAME) &
                    !propertyString.equalsIgnoreCase(Endpoint.FIND_URI) &
                    !propertyString.equalsIgnoreCase(Endpoint.HAS_FIXED_VALUE) &
                    !propertyString.equalsIgnoreCase(Endpoint.IS_RELATED_TO_SERVICE) &
                    !propertyString.equalsIgnoreCase(Endpoint.HAS_STRUCTURE_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ISREQUIRED_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ISDATA_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.CONTENT_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ATTRIBUTE_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.SAME_PROPERTY_AS) &
                    !propertyString.equalsIgnoreCase(RDFS.label.toString()) &
                    !propertyString.equalsIgnoreCase(RDF.type.toString()) &
                    !propertyString.equalsIgnoreCase(Endpoint.LI_PROPERTY)) {
                propertyList.add(propertyString);
            }
        }

        return propertyList;
    }


    public ArrayList<String> getPropertyOfResourceAsObject(String objectString) {
        ArrayList<String> propertyList = new ArrayList<String>();

        String queryStr = "select ?prop where {"
                + "?sub ?prop <" + objectString + "> } ";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource property = solution.getResource("prop");
            String propertyString = property.toString();
            if (!propertyString.equalsIgnoreCase(Endpoint.PARAM_NAME) &
                    !propertyString.equalsIgnoreCase(Endpoint.FIND_URI) &
                    !propertyString.equalsIgnoreCase(Endpoint.HAS_FIXED_VALUE) &
                    !propertyString.equalsIgnoreCase(Endpoint.IS_RELATED_TO_SERVICE) &
                    !propertyString.equalsIgnoreCase(Endpoint.HAS_STRUCTURE_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ISREQUIRED_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ISDATA_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.CONTENT_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ATTRIBUTE_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.INPUT_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.SAME_PROPERTY_AS) &
                    !propertyString.equalsIgnoreCase(RDFS.label.toString()) &
                    !propertyString.equalsIgnoreCase(RDF.type.toString()) &
                    !propertyString.equalsIgnoreCase(Endpoint.LI_PROPERTY)) {
                propertyList.add(propertyString);
            }
        }

        return propertyList;
    }

    public ArrayList<TriplePattern> getPropertyBetweenOutputs(String serviceString) {

        ArrayList<TriplePattern> tripleList = new ArrayList<TriplePattern>();

        String queryStr = "select ?sub ?prop ?obj where { "
                + "{ ?sub <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + serviceString + "> . "
                + "?obj <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + serviceString + "> . "
                + "?sub ?prop ?obj } "
                + "UNION"
                + "{ ?sub <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + serviceString + "> . "
                + "?prop <" + RDF.type + "> <" + OWL.DatatypeProperty + "> . "
                + "?sub ?prop ?obj } "
                + "}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource property = solution.getResource("prop");
            String propertyString = property.toString();
            if (!propertyString.equalsIgnoreCase(Endpoint.PARAM_NAME) &
                    !propertyString.equalsIgnoreCase(Endpoint.HAS_FIXED_VALUE) &
                    !propertyString.equalsIgnoreCase(Endpoint.IS_RELATED_TO_SERVICE) &
                    !propertyString.equalsIgnoreCase(Endpoint.HAS_STRUCTURE_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ISREQUIRED_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ISDATA_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.CONTENT_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.ATTRIBUTE_PROPERTY) &
                    !propertyString.equalsIgnoreCase(Endpoint.SAME_PROPERTY_AS) &
                    !propertyString.equalsIgnoreCase(RDFS.label.toString()) &
                    !propertyString.equalsIgnoreCase(RDF.type.toString()) &
                    !propertyString.equalsIgnoreCase(Endpoint.LI_PROPERTY)) {
                Resource subject = solution.getResource("sub");
                String subjectString = subject.toString();
                Resource object = solution.getResource("obj");
                String objectString = object.toString();
                tripleList.add(new TriplePattern(subjectString, propertyString, objectString));
            }
        }

        return tripleList;
    }

    public ArrayList<String> getObjectURIOfPropertyOfResource(String subjectString, String propertyString) {
        ArrayList<String> objectList = new ArrayList<String>();

        String queryStr = "select ?obj where {"
                + "<" + subjectString + "> <" + propertyString + "> ?obj }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource object = solution.getResource("obj");
            String objectString = object.toString();
            objectList.add(objectString);

        }


        return objectList;
    }

    public ArrayList<String> getSubjectURIOfPropertyOfResource(String objectString, String propertyString) {
        ArrayList<String> subjectList = new ArrayList<String>();

        String queryStr = "select ?sub where {"
                + " ?sub  <" + propertyString + "> <" + objectString + "> }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource object = solution.getResource("sub");
            String subjectString = object.toString();
            subjectList.add(subjectString);

        }

        return subjectList;
    }

    public ArrayList<String> getObjectURIOfProperty(String subject, String property) {
        ArrayList<String> objectURIList = new ArrayList<String>();

        String queryStr = "select ?obj where {"
                + "<" + subject + "> <" + property + "> ?obj }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource object = solution.getResource("obj");
            objectURIList.add(object.toString());
        }

        return objectURIList;
    }

    public ArrayList<String> getSubjectURIOfProperty(String property) {
        ArrayList<String> subjectURIList = new ArrayList<String>();

        String queryStr = "select ?subj where { "
                + "?subj <" + property + "> ?obj }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource subject = solution.getResource("subj");
            subjectURIList.add(subject.toString());
        }

        return subjectURIList;
    }

    public ArrayList<String> getOutputRelatedToResource(String addedResource) {
        ArrayList<String> resourceURIList = new ArrayList<String>();

        String queryStr = "select distinct ?output where { "
                + "{ <" + addedResource + "> ?prop ?output . "
                + "?output <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service } "
                + " UNION "
                + "{ ?output ?prop <" + addedResource + "> . "
                + "?output <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service } "
                + " UNION "
                + "{ <" + addedResource + "> ?output ?type . "  //caso datatypeProperty
                + "?output <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service } "
                + "}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource res = solution.getResource("output");
            resourceURIList.add(res.toString());
        }

        return resourceURIList;
    }

    public ArrayList<String> getAddedResourcesOfService(String service) {
        ArrayList<String> resourceURIList = new ArrayList<String>();

        String queryStr = "select distinct ?res where { "
                + "{ ?res ?prop ?obj . "
                + "?obj <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + service + "> . "
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?subj ?prop ?res . "
                + "?subj <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + service + "> ."
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?res ?prop ?obj . "
                + "?prop <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + service + "> . "
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?subj ?prop ?res . "
                + "?prop <" + Endpoint.IS_RELATED_TO_SERVICE + "> <" + service + "> ."
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?res ?prop ?obj . "
                + "<" + service + "> <" + Endpoint.INPUT_PROPERTY + "> ?obj . "
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?res ?prop ?obj . "
                + "<" + service + "> <" + Endpoint.INPUT_PROPERTY + "> ?prop . "
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?subj ?prop ?res . "
                + "<" + service + "> <" + Endpoint.INPUT_PROPERTY + "> ?prop . "
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + " UNION "
                + "{ ?subj ?prop ?res . "
                + "<" + service + "> <" + Endpoint.INPUT_PROPERTY + "> ?subj . "
                + "?res <" + RDF.type + "> <" + Endpoint.ADDED_RESOURCE + "> } "
                + "}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource res = solution.getResource("res");
            resourceURIList.add(res.toString());
        }

        return resourceURIList;
    }


    public String getUriInRdfCache(String label, String type) {

        String uri = null;

        if (label != null) {
            if (label.startsWith("\"")) {
                label = label.substring(1, label.length() - 1);
            }

            String queryStr = "select ?subject where { "
                    + "?subject <" + RDFS.label + "> \"" + label + "\" . "
                    + "?subject <" + RDF.type + "> <" + type + "> }";

            Query query = QueryFactory.create(queryStr);
            QueryExecution qexec = QueryExecutionFactory.create(query, rdfCache);
            ResultSet result = qexec.execSelect();

            if (result.hasNext()) {
                QuerySolution solution = result.nextSolution();
                Resource subject = solution.getResource("subject");
                uri = subject.toString();
            }
        }
        return uri;
    }

    public String getServiceOfInput(String inputURI) {

        String queryStr = "select ?service where { "
                + "?service <" + Endpoint.INPUT_PROPERTY + "> <" + inputURI + "> }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource service = solution.getResource("service");
            return service.toString();
        } else {
            return null;
        }
    }

    public String getServiceOfOutput(String outputURI) {
        String queryStr = "select ?service where { "
                + "<" + outputURI + "> <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource service = solution.getResource("service");
            return service.toString();
        } else {
            return null;
        }
    }

    public String getLanguageOfService(String service) {
        String queryStr = "select ?language where { "
                + "<" + service + "> <" + Endpoint.LANGUAGE_PROPERTY + "> ?language }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Literal language = solution.getLiteral("language");
            return language.toString();
        } else {
            return null;
        }
    }

    public String getUrlOfService(String service) {
        String queryStr = "select ?url where { "
                + "<" + service + "> <" + Endpoint.URL_PROPERTY + "> ?url }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Literal url = solution.getLiteral("url");
            return url.toString();
        } else {
            return null;
        }
    }

    public String getLabelOfResource(String resourceURI) {
        String queryStr = "select ?label where { "
                + "<" + resourceURI + "> <" + RDFS.label + "> ?label }";

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

    public String getParamNameOfResource(String resourceURI) {
        String queryStr = "select ?label where { "
                + "<" + resourceURI + "> <" + Endpoint.PARAM_NAME + "> ?label }";

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

    public String getTypeOfResource(String resourceURI) {
        String queryStr = "select ?type where { "
                + "<" + resourceURI + "> <" + RDF.type + "> ?type }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource type = solution.getResource("type");
            return type.toString();
        } else {
            return null;
        }
    }


    public void addFixedValueToConstantsTable() {
        String queryStr = "select ?value ?type where { "
                + "?resource <" + Endpoint.HAS_FIXED_VALUE + "> ?value . "
                + "?resource <" + RDF.type + "> ?type "
                + "}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Literal value = solution.getLiteral("value");
            Resource type = solution.getResource("type");
            String valueString = value.toString();
            String typeString = type.toString();
            if (constantsTable.containsKey(typeString)) {
                constantsTable.get(typeString).add(valueString);
            } else {
                List<String> valueList = new ArrayList<String>();
                valueList.add(valueString);
                constantsTable.put(typeString, valueList);
            }
        }
    }


    public void writeServiceAndInputsURI(String serviceURIString) {

        String queryStr = "select ?input where { "
                + "< " + serviceURIString + "> <" + Endpoint.INPUT_PROPERTY + "> ?input }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();

            String inputURIString = solution.getResource("input").toString();

            if (serviceInputsURITable.containsKey(serviceURIString)) {
                serviceInputsURITable.get(serviceURIString).add(inputURIString);
            } else {
                List<String> inputsList = new ArrayList<String>();
                inputsList.add(inputURIString);
                serviceInputsURITable.put(serviceURIString, inputsList);
            }

        }

    }

    public void writeOutputAndServicesAndInputsURI(String type) {

        String queryStr = "select ?output ?service ?input where { "
                + "?output <" + RDF.type + "> <" + type + "> ."
                + "?output <" + Endpoint.IS_RELATED_TO_SERVICE + "> ?service ."
                + "?service <" + Endpoint.INPUT_PROPERTY + "> ?input }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();

            String outputURIString = solution.getResource("output").toString();
            String serviceURIString = solution.getResource("service").toString();
            String inputURIString = solution.getResource("input").toString();

            outputServiceTable.put(outputURIString, serviceURIString);
            if (serviceInputsURITable.containsKey(serviceURIString)) {
                serviceInputsURITable.get(serviceURIString).add(inputURIString);
            } else {
                List<String> inputsList = new ArrayList<String>();
                inputsList.add(inputURIString);
                serviceInputsURITable.put(serviceURIString, inputsList);
            }

        }

    }

    public List<String> selectRange(String property) {
        List<String> typeList = new ArrayList<String>();

        String queryStr = "select ?range where { "
                + "?s <" + property + "> ?o ."
                + "?o <" + RDF.type + "> ?range}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource type = solution.getResource("range");
            typeList.add(type.toString());
        }

        return typeList;
    }

    public Boolean isDatatypeProperty(String property) {

        Boolean isDatatypeProperty = false;

        String queryStr = "select ?p where { "
                //+ property + " <" + RDF.type +"> <" + OWL.DatatypeProperty + "> "
                + "<" + property + "> ?p <" + OWL.DatatypeProperty + "> "
                + " }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            isDatatypeProperty = true;
        }

        return isDatatypeProperty;
    }

    public Boolean isSamePropertyAs(String property) {

        Boolean isSameProperty = false;

        String queryStr = "select ?sameProp where { "
                + "<" + property + "> <" + Endpoint.SAME_PROPERTY_AS + "> ?sameProp "
                + " }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            isSameProperty = true;
        } else {
            //nel caso sia property la proprietà segnata come equivalente (oggetto)

            queryStr = "select ?sameProp where { "
                    + "?sameProp <" + Endpoint.SAME_PROPERTY_AS + "> <" + property + ">"
                    + " }";

            query = QueryFactory.create(queryStr);
            qexec = QueryExecutionFactory.create(query, model);
            result = qexec.execSelect();

            if (result.hasNext()) {
                isSameProperty = true;
            }

        }

        return isSameProperty;
    }

    public List<String> selectSamePropertyAs(String property) {
        List<String> typeList = new ArrayList<String>();

        String queryStr = "select ?sameProp where { "
                + "<" + property + "> <" + Endpoint.SAME_PROPERTY_AS + "> ?sameProp "
                + " }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource sameProp = solution.getResource("sameProp");
            typeList.add(sameProp.toString());
        }

        // case property is samePropertyAs (object)
        if (typeList.isEmpty()) {
            queryStr = "select ?sameProp where { "
                    + "?sameProp <" + Endpoint.SAME_PROPERTY_AS + "> <" + property + ">"
                    + " }";

            query = QueryFactory.create(queryStr);
            qexec = QueryExecutionFactory.create(query, model);
            result = qexec.execSelect();

            while (result.hasNext()) {
                QuerySolution solution = result.nextSolution();
                Resource sameProp = solution.getResource("sameProp");
                typeList.add(sameProp.toString());
            }

        }

        return typeList;
    }

    public List<String> selectDomain(String property) {
        List<String> typeList = new ArrayList<String>();

        String queryStr = "select ?domain where { "
                + "?s <" + property + "> ?o ."
                + "?s <" + RDF.type + "> ?domain}";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        while (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource type = solution.getResource("domain");
            typeList.add(type.toString());
        }

        return typeList;
    }

    public List<String> typeFromGraphPattern(String subject, List<TriplePattern> patternList) {
        List<String> rdfTypeList = new ArrayList<String>();

        for (int index = 0; index < patternList.size(); index++) {
            if (patternList.get(index).getSubject().equalsIgnoreCase(subject) &
                    patternList.get(index).getProperty().equalsIgnoreCase(RDF.type.toString())) {
                rdfTypeList.add(patternList.get(index).getObject());
            }
        }

        return rdfTypeList;
    }

    public List<String> typeFromMappingFile(String resource, List<TriplePattern> patternList) {
        List<String> rdfTypeList = new ArrayList<String>();

        Boolean isFirst = true;

        for (int index = 0; index < patternList.size(); index++) {

            if (!patternList.get(index).getProperty().equalsIgnoreCase(RDFS.label.toString())) {

                //resource is object
                if (patternList.get(index).getSubject().equals(resource)) {

                    //add to domainList all the elements of domain of the first property
                    if (isFirst) {
                        List<String> domainList = selectDomain(patternList.get(index).getProperty());
                        for (int indexDomainList = 0; indexDomainList < domainList.size(); indexDomainList++) {
                            rdfTypeList.add(domainList.get(indexDomainList));
                        }
                        isFirst = false;
                    } else {
                        //if there are other properties related to the resource, do the intersection between domains
                        List<String> domainList = selectDomain(patternList.get(index).getProperty());
                        for (int indexTypeList = 0; indexTypeList < rdfTypeList.size(); indexTypeList++) {
                            if (!domainList.contains(rdfTypeList.get(indexTypeList))) {
                                rdfTypeList.remove(indexTypeList);
                            }
                        }
                    }

                } else if (patternList.get(index).getObject().equals(resource)) {
                    //resource is object
                    //like before but using range instead of domain
                    if (isFirst) {
                        List<String> rangeList = selectRange(patternList.get(index).getProperty());
                        for (int indexRangeList = 0; indexRangeList < rangeList.size(); indexRangeList++) {
                            rdfTypeList.add(rangeList.get(indexRangeList));
                        }
                        isFirst = false;
                    } else {
                        List<String> rangeList = selectRange(patternList.get(index).getProperty());
                        for (int indexTypeList = 0; indexTypeList < rdfTypeList.size(); indexTypeList++) {
                            if (!rangeList.contains(rdfTypeList.get(indexTypeList))) {
                                rdfTypeList.remove(indexTypeList);
                            }
                        }
                    }
                }
            }
        }

        return rdfTypeList;
    }


    public void writeConstantsLabelAndTypeFromService (String uri, LabelTypeService labelTypeService) {
        List<String> typeList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();

        String queryString = labelTypeService.getQueryString().replaceAll(labelTypeService.getPlaceholder(), uri);

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(labelTypeService.getEndpoint(), query);
        ResultSet result = qexec.execSelect();

        //add all results to values and types list
        for (; result.hasNext(); ) {
            QuerySolution solution = result.nextSolution();
            String label = solution.getLiteral("label").toString();
            String type = solution.getResource("type").toString();
            if (label.contains("@")) {
                label = label.substring(0, label.indexOf("@"));
            }
            if (!valueList.contains(label)) {
                valueList.add(label);
            }
            if (!typeList.contains(type)) {
                typeList.add(type);
            }
        }

        //add constant to constantsTable
        for (int indexTypeList = 0; indexTypeList < typeList.size(); indexTypeList++) {
            Boolean completeListAdded = false;
            for (int indexValueList = 0; indexValueList < valueList.size(); indexValueList++) {

                //add resource to rdf-cache
                Resource resource = rdfCache.createResource(uri);
                Resource typeResource = rdfCache.createResource(typeList.get(indexTypeList));
                resource.addProperty(RDF.type, typeResource);
                resource.addLiteral(RDFS.label, valueList.get(indexValueList));

                if (!completeListAdded) {
                    if (constantsTable.containsKey(typeList.get(indexTypeList))) {
                        //add only one element if this is not already present
                        if (!constantsTable.get(typeList.get(indexTypeList)).contains(valueList.get(indexValueList))) {
                            constantsTable.get(typeList.get(indexTypeList)).add(valueList.get(indexValueList));
                        }
                    } else {
                        //if constantsTable doesn't contain key:type -> add all the list (only once)
                        completeListAdded = true;
                        constantsTable.put(typeList.get(indexTypeList), valueList);
                    }
                }
            }
        }
    }

}
