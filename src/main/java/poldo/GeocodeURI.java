package poldo;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * It Use latitude and longitude to call geocode API which return the name of the closest city.
 * Then we use the city name to call DBPedia Lookup service.
 */
public class GeocodeURI implements FindURI {

    @Override
    public String getResourceURI (ArrayList<String> par,
                                  ArrayList<String> propertyListResIsSub,
                                  ArrayList<String> propertyListResIsOb,
                                  ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsSub,
                                  ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsOb,
                                  HashMap<String,String> inputIsSubject,
                                  HashMap<String,String> inputIsObject,
                                  Integer key,
                                  Model model) {


        String latitude="";
        String longitude="";


        // .get(0) and .get(1) contain latitude and longitude values
        String propertyString0 = selectSamePropertyAs(propertyListResIsSub.get(0), model);
        String propertyString1 = selectSamePropertyAs(propertyListResIsSub.get(1), model);

        //System.out.println(propertyString);

        //I know that there is only one value, so .get(0)
        String objectValue0 = valuesArrayResIsSub.get(0).get(key).get(0);
        String objectValue1 = valuesArrayResIsSub.get(1).get(key).get(0);

        if (propertyString0.contains("lat")) {
            latitude = objectValue0;
        } else if (propertyString0.contains("long")){
            longitude = objectValue0;
        }

        if (propertyString1.contains("lat")) {
            latitude = objectValue1;
        } else if (propertyString1.contains("long")){
            longitude = objectValue1;
        }



        String serviceString = "http://geocode.xyz/"+latitude.replaceAll("\"","")+","+longitude.replaceAll("\"","");

        //call service
        HttpURLConnectionAPI http = new HttpURLConnectionAPI();

        String response;

        String cityLabel="";

        HashMap<String, String> params = new HashMap<String, String>();

        params.put("geoit", "xml");
        try {
            response = http.sendGet(serviceString, params);

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder =  builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/geodata/city");

            NodeList nodeList = (NodeList) expr.evaluate(xmlDocument, XPathConstants.NODESET);
            cityLabel = nodeList.item(0).getFirstChild().getNodeValue();

            //System.out.println("city: " + cityLabel);

        } catch (Exception e) {
            e.printStackTrace();
            //cityLabel = Endpoint.CUSTOM_NAMESPACE + par.get(1).substring(par.get(1).lastIndexOf("/")+1) + "#" + par.get(0);	//TODO handle this case
        }

        ArrayList<String> param = new ArrayList<>();

        param.add(0, cityLabel);
        param.add(1, "http://dbpedia.org/ontology/Place");

        DbpediaURI dbpediaURI = new DbpediaURI();


        return dbpediaURI.getResourceURI(param, null,null,null,null,null,null,0, null);
    }

    public String selectSamePropertyAs(String property, Model model) {

        String samePropertyString = null;

        String queryStr = "select ?sameProp where { "
                + "<" + property + "> <" + Endpoint.SAME_PROPERTY_AS + "> ?sameProp "
                + " }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()) {
            QuerySolution solution = result.nextSolution();
            Resource sameProp = solution.getResource("sameProp");
            samePropertyString = sameProp.toString();
        }


        return samePropertyString;
    }

}
