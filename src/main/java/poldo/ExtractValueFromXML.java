package poldo;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExtractValueFromXML {

    public HashMap<Integer, ArrayList<String>> getValueList(Model model, Document xmlDocument, String resourceURI, String antenatoComuneURI) {

        HashMap<Integer, ArrayList<String>> values = new HashMap<Integer, ArrayList<String>>();

        String expression = getExpression(model, resourceURI);

        String antenatoComuneLabel = getLabelOfResource(antenatoComuneURI, model);

        int i = 1;

        // replace first part of xpath
        // /a/b/c become /a/b/c[1]
        // if b is the common ancestor: /a/b/c become /a/b[1]/c

        expression = expression.replaceFirst(getExpression(model, antenatoComuneLabel) + antenatoComuneLabel, getExpression(model, antenatoComuneLabel) + antenatoComuneLabel + "[" + i + "]");

        ArrayList<String> valueList = new ArrayList<String>();

        Boolean isNewRow = true;

        do {

            try {

                valueList = new ArrayList<String>();

                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

                if (nodeList.getLength()!=0) {
                    for (int contNode = 0; contNode < nodeList.getLength(); contNode++) {
                        if (nodeList.item(contNode).getFirstChild() != null) {
                            valueList.add(nodeList.item(contNode).getFirstChild().getNodeValue());
                        }
                    }
                }

                values.put(i, valueList);
                i++;
                expression = expression.substring(0, expression.indexOf("[")+1) + i + expression.substring(expression.indexOf("]"));

                //check if there is another row
                String parentExpression = expression.substring(0, expression.indexOf("[")+1) + i + "]";
                NodeList rowList = (NodeList) xPath.compile(parentExpression).evaluate(xmlDocument, XPathConstants.NODESET);
                if (rowList.getLength()==0) {
                    isNewRow = false;
                }

            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }



        } while (isNewRow);

        return values;
    }


    public List<String> getValueList(Model model, Document xmlDocument, String resourceURI) {

        List<String> valueList = new ArrayList<String>();

        String expression = getExpression(model, resourceURI);

        try {

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {

                //handle void nodes  (<nodename/>)
                if (nodeList.item(i).getFirstChild() != null) {
                    valueList.add(nodeList.item(i).getFirstChild().getNodeValue());
                }
            }

        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return valueList;
    }


    /**
     * Return the xPath of the resource
     *
     * @param model       mapping model
     * @param resourceURI Uri of the resource in mapping model
     * @return
     */
    public static String getExpression(Model model, String resourceURI) {
        if (!resourceURI.startsWith("<")) {
            resourceURI = "<" + resourceURI + ">";
        }

        String expression = "";

        //check if resource is an xml attribute
        String queryString = "select ?parent ?parentlabel ?label where {?parent " +
                "<" + Endpoint.ATTRIBUTE_PROPERTY + ">" +
                resourceURI + " . " +
                "?parent <http://www.w3.org/2000/01/rdf-schema#label> ?parentlabel . " +
                resourceURI + " <http://www.w3.org/2000/01/rdf-schema#label> ?label }";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet result = qexec.execSelect();

        //if it is not an attribute, search for the parent
        if (!result.hasNext()) {

            queryString = "select ?parent ?parentlabel ?label where {?parent " +
                    "<" + Endpoint.LI_PROPERTY + ">" +
                    resourceURI + " . " +
                    "?parent <http://www.w3.org/2000/01/rdf-schema#label> ?parentlabel . " +
                    resourceURI + " <http://www.w3.org/2000/01/rdf-schema#label> ?label }";
            query = QueryFactory.create(queryString);
            qexec = QueryExecutionFactory.create(query, model);
            result = qexec.execSelect();
            if (result.hasNext()) {
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

            expression = parentLabel.toString() + "/@" + label.toString();

        }

        Boolean isChild = true;

        while (isChild) {
            queryString = "select ?parent ?parentlabel where {?parent " +
                    "<" + Endpoint.LI_PROPERTY + ">" +
                    resourceURI + " . " +
                    "?parent <http://www.w3.org/2000/01/rdf-schema#label> ?parentlabel }";
            query = QueryFactory.create(queryString);
            qexec = QueryExecutionFactory.create(query, model);
            result = qexec.execSelect();
            if (!result.hasNext()) {
                isChild = false;
                expression = "/" + expression;
            } else {
                QuerySolution solution = result.nextSolution();
                Resource parent = solution.getResource("parent");
                Literal parentLabel = solution.getLiteral("parentlabel");

                resourceURI = "<" + parent.toString() + ">";

                expression = parentLabel.toString() + "/" + expression;

            }
        }
        return expression;
    }


    public static String getLabelOfResource(String resourceURI, Model model) {
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

}
