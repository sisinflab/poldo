package r2rq;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

public class SPARQLQueryAnalyzer {

    List<TriplePattern> patternList = new ArrayList<TriplePattern>();
    TriplePattern triplePattern;

    public List<TriplePattern> getPattern() {
        return patternList;
    }

    public void analyze(String queryString){


        Query query = QueryFactory.create(queryString);


        // This will walk through all parts of the query
        ElementWalker.walk(query.getQueryPattern(),
                // For each element...
                new ElementVisitorBase() {
                    // ...when it's a block of triples...
                    public void visit(ElementPathBlock el) {
                        // ...go through all the triples...
                        Iterator<TriplePath> triples = el.patternElts();
                        while (triples.hasNext()) {

                            TriplePath t = triples.next();

                            String subject, property, object;

                            subject = t.getSubject().toString();
                            property = t.getPredicate().toString();
                            object = t.getObject().toString();

                            triplePattern = new TriplePattern (subject, property, object);
                            patternList.add(triplePattern);
                        }
                    }
                }
        );


    }

}
