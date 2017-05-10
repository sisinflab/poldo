package poldo;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class ExtractValueFromXML {

    public HashMap<Integer, ArrayList<String>> getValueList(Model model, Document xmlDocument, String resourceURI, String antenatoComuneURI){

        HashMap <Integer, ArrayList<String>> values = new HashMap <Integer, ArrayList<String>>();

        String expression = getExpression (model, resourceURI);

        String antenatoComuneLabel = getLabelOfResource(antenatoComuneURI, model);

        int i = 1;

        // replace first part of xpath
        // /a/b/c become /a/b/c[1]

        expression = expression.replaceFirst(getExpression(model, antenatoComuneLabel)+antenatoComuneLabel, getExpression(model, antenatoComuneLabel)+antenatoComuneLabel+"[" + i + "]");

        ArrayList<String> valueList = new ArrayList<String>();

        do {

            try {

                valueList = new ArrayList<String>();

                XPath xPath =  XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

                for (int contNode = 0; contNode < nodeList.getLength(); contNode++) {
                    valueList.add(nodeList.item(contNode).getFirstChild().getNodeValue());
                }

                if (valueList.size()!=0){
                    values.put(i, valueList);
                    i++;
                    expression = expression.replaceAll("[" + (i-1) + "]", ""+i );
                }

            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }

        } while (valueList.size()!=0);

        return values;
    }


    public List<String> getValueList(Model model, Document xmlDocument, String resourceURI){

        List<String> valueList = new ArrayList<String>();

        String expression = getExpression(model, resourceURI);

        try {

            XPath xPath =  XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                valueList.add(nodeList.item(i).getFirstChild().getNodeValue());
            }

        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return valueList;
    }


    public static String getExpression (Model model, String resourceURI) {
        if (!resourceURI.startsWith("<")){
            resourceURI = "<"+ resourceURI +">";
        }

        String expression = "";

        //check if resource is an xml attribute
        String queryString = "select ?parent ?parentlabel ?label where {?parent "+
                "<" +Endpoint.DEFAULT_NAMESPACE + Endpoint.ATTRIBUTE_PROPERTY + ">" +
                resourceURI + " . " +
                "?parent <http://www.w3.org/2000/01/rdf-schema#label> ?parentlabel . " +
                resourceURI + " <http://www.w3.org/2000/01/rdf-schema#label> ?label }";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet result = qexec.execSelect();

        //if it is not an attribute, search for the parent
        if (!result.hasNext()){

            queryString = "select ?parent ?parentlabel ?label where {?parent "+
                    "<" +Endpoint.LI_PROPERTY + ">" +
                    resourceURI + " . " +
                    "?parent <http://www.w3.org/2000/01/rdf-schema#label> ?parentlabel . " +
                    resourceURI + " <http://www.w3.org/2000/01/rdf-schema#label> ?label }";
            query = QueryFactory.create(queryString);
            qexec = QueryExecutionFactory.create(query,model);
            result = qexec.execSelect();
            if (result.hasNext()){
                QuerySolution solution = result.nextSolution();
                Resource parent = solution.getResource("parent");
                Literal label = solution.getLiteral("label");
                Literal parentLabel = solution.getLiteral("parentlabel");

                resourceURI = "<" + parent.toString() + ">";

                expression = parentLabel.toString() + "/" + label.toString();
            }
        } else {
            QuerySolution solution = result.nextSolution();
            Resource parent = solution.getResource("parent");
            Literal label = solution.getLiteral("label");
            Literal parentLabel = solution.getLiteral("parentlabel");

            resourceURI = "<" + parent.toString() + ">";

            expression = parentLabel.toString() +"/@" + label.toString();

        }

        Boolean isChild=true;

        while (isChild){
            queryString = "select ?parent ?parentlabel where {?parent "+
                    "<" + Endpoint.LI_PROPERTY + ">" +
                    resourceURI + " . " +
                    "?parent <http://www.w3.org/2000/01/rdf-schema#label> ?parentlabel }";
            query = QueryFactory.create(queryString);
            qexec = QueryExecutionFactory.create(query,model);
            result = qexec.execSelect();
            if (!result.hasNext()){
                isChild=false;
                expression = "/"+expression;
            } else {
                QuerySolution solution = result.nextSolution();
                Resource parent = solution.getResource("parent");
                Literal parentLabel = solution.getLiteral("parentlabel");

                resourceURI = "<" + parent.toString() + ">";

                expression = parentLabel.toString() +"/"+ expression;

            }
        }
        return expression;
    }


    public static String getLabelOfResource(String resourceURI, Model model){
        String queryStr = "select ?label where { "
                + "<" + resourceURI + "> <" + RDFS.label + "> ?label }";

        Query query = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet result = qexec.execSelect();

        if (result.hasNext()){
            QuerySolution solution = result.nextSolution();
            Literal label = solution.getLiteral("label");
            return label.toString();
        } else {
            return null;
        }
    }

}
