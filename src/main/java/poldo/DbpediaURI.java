package poldo;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class DbpediaURI implements FindURI {

    @Override
    public String getResourceURI (ArrayList<String> par,
                                  ArrayList<String> propertyListResIsSub,
                                  ArrayList<String> propertyListResIsOb,
                                  ArrayList <HashMap <Integer, ArrayList<String>>> valuesArrayResIsSub,
                                  ArrayList <HashMap <Integer, ArrayList<String>>> valuesArrayResIsOb,
                                  HashMap<String,String> inputIsSubject,
                                  HashMap<String,String> inputIsObject) {

        String uriString = "";

        String serviceString = "http://lookup.dbpedia.org/api/search.asmx/KeywordSearch";

        HashMap<String, String> params = new HashMap<String, String>();

        params.put("QueryString", par.get(0));
        params.put("QueryClass", par.get(1));

        //call service
        HttpURLConnectionAPI http = new HttpURLConnectionAPI();

        String response;

        try {
            response = http.sendGet(serviceString,params);

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder =  builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/ArrayOfResult/Result[1]/URI");

            NodeList nodeList = (NodeList) expr.evaluate(xmlDocument, XPathConstants.NODESET);
            uriString = nodeList.item(0).getFirstChild().getNodeValue();

            System.out.println("URI DBPEDIA: " + uriString);

        } catch (Exception e) {
            e.printStackTrace();
            uriString = Endpoint.DEFAULT_NAMESPACE + par.get(1).substring(par.get(1).lastIndexOf("/")+1) + "#" + par.get(0);	//TODO handle this case
        }


        return uriString;
    }

}
