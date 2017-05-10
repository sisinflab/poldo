package r2rq;


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
                                 HashMap<String, String> inputIsObject) {

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

        System.out.println("param3");
        for (int indexLista=0; indexLista<valuesArrayResIsSub.size(); indexLista++){
            System.out.println("HashMap" + indexLista);
            Iterator<Integer> iteratorHashMap = valuesArrayResIsSub.get(indexLista).keySet().iterator();
            while (iteratorHashMap.hasNext()){

                int key = iteratorHashMap.next();
                System.out.println("key: " + key);
                for (int indexListaInterna = 0; indexListaInterna < valuesArrayResIsSub.get(indexLista).get(key).size(); indexListaInterna++){
                    System.out.println("Str: " + valuesArrayResIsSub.get(indexLista).get(key).get(indexListaInterna));
                }
            }
        }

        System.out.println("param4");
        for (int indexLista=0; indexLista<valuesArrayResIsOb.size(); indexLista++){
            System.out.println("HashMap" + indexLista);
            Iterator<Integer> iteratorHashMap = valuesArrayResIsOb.get(indexLista).keySet().iterator();
            while (iteratorHashMap.hasNext()){

                int key = iteratorHashMap.next();
                System.out.println("key: " + key);
                for (int indexListaInterna = 0; indexListaInterna < valuesArrayResIsOb.get(indexLista).get(key).size(); indexListaInterna++){
                    System.out.println("Str: " + valuesArrayResIsOb.get(indexLista).get(key).get(indexListaInterna));
                }
            }
        }

        System.out.println("param5");
        Iterator<String> iteratorHashMapSub = inputIsSubject.keySet().iterator();
        while (iteratorHashMapSub.hasNext()){
            String key = iteratorHashMapSub.next();
            System.out.println("key: " + key + " value: " + inputIsSubject.get(key));
        }

        System.out.println("param6");
        Iterator<String> iteratorHashMapOb = inputIsObject.keySet().iterator();
        while (iteratorHashMapOb.hasNext()){
            String key = iteratorHashMapOb.next();
            System.out.println("key: " + key + " value: " + inputIsObject.get(key));
        }

        return uri;
    }

}
