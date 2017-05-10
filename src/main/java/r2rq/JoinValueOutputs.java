package r2rq;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;


public class JoinValueOutputs {

    private JsonFactory factory;
    private ObjectMapper mapper;
    private int count;
    private HashMap<Integer,String> subjectList;
    private HashMap<Integer,String> objectList;
    private int level;
    HashMap<Integer,String> valueList;

    public JoinValueOutputs(){

        factory  = new JsonFactory();
        count = 0;
        subjectList = new HashMap<Integer,String>();
        objectList = new HashMap<Integer,String>();
        level = 0;


    }

    public List< HashMap<Integer,String>> joinValuesData(String json,ArrayList pathOut1,ArrayList pathOut2) throws IOException{
        List< HashMap<Integer,String>> tableList = new ArrayList< HashMap<Integer,String>>();
        mapper = new ObjectMapper(factory);
        JsonNode rootNode = mapper.readTree(json);
        System.out.println(rootNode.size());
        Iterator<Map.Entry<String,JsonNode>> fieldsIterator = rootNode.getFields();

        while (fieldsIterator.hasNext()) {

            Map.Entry<String,JsonNode> field = fieldsIterator.next();


            if(pathOut1.get(0).toString().equals(pathOut2.get(0).toString())){


                valueList = new HashMap<Integer,String>();
                subjectList = getValueJSON(field,pathOut1);

                tableList.add(subjectList);

                level = 0;
                valueList = new HashMap<Integer,String>();
                objectList = getValueJSON(field,pathOut2);

                tableList.add(objectList);


            }else{

                //TODO handle the case there is not a root for outputs  (if it exists)

            }
        }

        return tableList;
    }


    public HashMap<Integer,String> getValueJSON(Map.Entry<String,JsonNode> field,ArrayList path){


        if(field.getValue().isObject()){

              /*handle JSON Objest*/

            if(field.getKey().equals(path.get(count).toString()) == true){

                count = count + 1;

                Iterator<Map.Entry<String,JsonNode>> fieldsOBJ = field.getValue().getFields();

                while (fieldsOBJ.hasNext()) {

                    Map.Entry<String,JsonNode> fielOBJ = fieldsOBJ.next();

                    if(!fielOBJ.getValue().isArray()&&!fielOBJ.getValue().isObject()){

                        String k1 = fielOBJ.getKey();

                        if(k1.equals(path.get(count).toString()) == true){
                            level++;

                            count = count + 1;

                            String v1 = fielOBJ.getValue().toString();
                            valueList.put((level+1), v1);

                            count = count - 1;
                        }
                    }else{
                        //recursive call for nested structures
                        getValueJSON(fielOBJ,path);
                    }
                }
                count = count - 1;
            }

        }else if(field.getValue().isArray()){

			/*Check if the name is already present in the path */
            if(field.getKey().equals(path.get(count).toString()) == true){

                count = count + 1;

                int j = 0;

		/*check how many elements there are*/
                while(j < field.getValue().size()){

                    JsonNode arrayFields = field.getValue().get(j);
                    Iterator<Map.Entry<String,JsonNode>> fieldsIterator = arrayFields.getFields();

                    while (fieldsIterator.hasNext()) {

                        Map.Entry<String,JsonNode> arrayfield = fieldsIterator.next();

                        if(!arrayfield.getValue().isArray()&&!arrayfield.getValue().isObject()){
                            //System.out.println("field:" + field.getKey());
                            String k2 = arrayfield.getKey();

                            if(k2.equals(path.get(count).toString()) == true){

                                count = count + 1;
                                level++;

                                String v2 = arrayfield.getValue().toString();

                                valueList.put((level+1), v2);

                                count = count - 1;
                            }
                        }else{
                            //recursive call for nested structures
                            getValueJSON(arrayfield,path);

                        }

                    }

                    j = j + 1;
                }
                count = count - 1;
            }

        }else{

            /*handle JSON Data*/
            String k3 = field.getKey();
            String v3 = field.getValue().toString();

            if(k3.equals(path.get(count).toString()) == true){
                count = count + 1;
                level++;
                //System.out.println(v3);
                valueList.put((level+1),v3);

                count = count - 1;
            }
        }

        return valueList;
    }

}