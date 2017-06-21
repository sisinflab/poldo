package poldo;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.query.*;


public class JsonResults {

    private String path;

    private ArrayList<String> elems;
    private List<String> valueList;

    public JsonResults(){

        path = "";
        elems = new ArrayList<String>();
        valueList = new ArrayList<String>();

    }

    public String getResults(Model model, String nodo) throws IOException{

        String hasStructure = "<"+ Endpoint.HAS_STRUCTURE_PROPERTY+">";
        String hasOutput = Endpoint.OUTPUT_PROPERTY;

        String check = "http://www.w3.org/1999/02/22-rdf-syntax-ns#li";
        String pred = "";
        String name;
        String struct;

        do{

            String queryString =
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
                            "SELECT ?nodo_padre ?struct ?name ?predicato  WHERE { " +
                            "?nodo_padre ?predicato " + "<" + nodo +">"+". " +
                            "<" + nodo +">"  + hasStructure +" ?struct. " +
                            "<" + nodo +">" + " rdfs:label ?name  " +
                            "}";

            Query query = QueryFactory.create(queryString);

            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

                ResultSet results = qexec.execSelect();


                while ( results.hasNext() ) {
                    /**
                     * Ottengo i singoli risultatio della query SPARQL,
                     * nello specifico ottengo label e URI
                     */

                    QuerySolution soln = results.nextSolution();

                    if(soln.get("predicato").toString().equals(hasOutput)||
                            soln.get("predicato").toString().equals(check)){

                        name = soln.getLiteral("name").toString();
                        struct = soln.getLiteral("struct").toString();
                        nodo = soln.get("nodo_padre").toString();
                        pred = soln.get("predicato").toString();



                        if(struct.equals("JSON_Array")){

                            path = "." + name.concat("[*]").concat(path);

                        }else if(struct.equals("JSON_Object")){

                            path = "." + name.concat(path);

                        }else{
                            if(path.isEmpty()){

                                path = "." + path.concat(name);

                            }else{

                                path = "." + name.concat(path);

                            }

                        }

                    }
                }
            }

        }while(check.equals(pred));

        path = "$" + path;

        return path;
    }


}
