package poldo;


import org.apache.jena.rdf.model.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class TestURI implements FindURI {

    static int contatore = 1;

    @Override
    public String getResourceURI(ArrayList<String> par,
                                 ArrayList<String> propertyListResIsSub,
                                 ArrayList<String> propertyListResIsOb,
                                 ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsSub,
                                 ArrayList<HashMap<Integer, ArrayList<String>>> valuesArrayResIsOb,
                                 HashMap<String, String> inputIsSubject,
                                 HashMap<String, String> inputIsObject,
                                 Integer key,
                                 Model model) {

        String uri = "http://resource.com/asd#" + contatore;

        contatore ++;

        System.out.println("param0");
        System.out.println(".get(0): " + par.get(0));
        System.out.println(".get(1): " + par.get(1));



        System.out.println("param1");
        for (int i=0; i<propertyListResIsSub.size(); i++){
            System.out.println(propertyListResIsSub.get(i));
        }

        System.out.println("param2");
        for (int i=0; i<propertyListResIsOb.size(); i++){
            System.out.println(propertyListResIsOb.get(i));
        }

        /*System.out.println("param3");
        //System.out.println("HashMap" + indexLista);
        Iterator<Integer> iteratorHashMapSub = valuesArrayResIsSub.keySet().iterator();
        while (iteratorHashMapSub.hasNext()){

            int key3 = iteratorHashMapSub.next();
            System.out.println("key: " + key3);
            for (int indexListaInterna = 0; indexListaInterna < valuesArrayResIsSub.get(key3).size(); indexListaInterna++){
                System.out.println("Str: " + valuesArrayResIsSub.get(key3).get(indexListaInterna));
            }
        }

        System.out.println("param4");
        //System.out.println("HashMap" + indexLista);
        Iterator<Integer> iteratorHashMapOb = valuesArrayResIsOb.keySet().iterator();
        while (iteratorHashMapOb.hasNext()){

            int key4 = iteratorHashMapOb.next();
            System.out.println("key: " + key4);
            for (int indexListaInterna = 0; indexListaInterna < valuesArrayResIsOb.get(key4).size(); indexListaInterna++){
                System.out.println("Str: " + valuesArrayResIsOb.get(key4).get(indexListaInterna));
            }
        }

        System.out.println("param5");
        Iterator<String> iteratorHashMapSubIn = inputIsSubject.keySet().iterator();
        while (iteratorHashMapSub.hasNext()){
            String key5 = iteratorHashMapSubIn.next();
            System.out.println("key: " + key5 + " value: " + inputIsSubject.get(key5));
        }

        System.out.println("param6");
        Iterator<String> iteratorHashMapObIn = inputIsObject.keySet().iterator();
        while (iteratorHashMapOb.hasNext()){
            String key6 = iteratorHashMapObIn.next();
            System.out.println("key: " + key6 + " value: " + inputIsObject.get(key6));
        }
*/
        return uri;
    }

}
