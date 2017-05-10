package r2rq;


import java.util.ArrayList;
import java.util.HashMap;

public interface FindURI {

    /**
     *
     * @param par  par[0] = label, par[1] = class
     * @param propertyListResIsSub list of properties with customResource as subject, null if resource is not customResource.
     * @param propertyListResIsOb list of properties with customResource as object, null if resource is not customResource.
     * @param valuesArrayResIsSub hashMap array, key: index of property (from propertyListResIsSub) and value extract from xml or json, null if resource is not customResource.
     * @param valuesArrayResIsOb hashMap array, key: index of property (from propertyListResIsOb) and value extract from xml or json, null if resource is not customResource.
     * @param inputIsSubject hashMap, key: property uri and input value (input subject of the triple), null if resource is not customResource.
     * @param inputIsObject hashMap, key: property uri and input value (input object of the triple), null if resource is not customResource.
     * @return Uri of the resource
     */
    String getResourceURI (ArrayList<String> par,
                           ArrayList<String> propertyListResIsSub,
                           ArrayList<String> propertyListResIsOb,
                           ArrayList <HashMap <Integer, ArrayList<String>>> valuesArrayResIsSub,
                           ArrayList <HashMap <Integer, ArrayList<String>>> valuesArrayResIsOb,
                           HashMap<String,String> inputIsSubject,
                           HashMap<String,String> inputIsObject);

}
