package poldo;


import java.io.IOException;
import java.util.*;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;


public class ValuesJSON {

    private JsonFactory factory;
    private ObjectMapper mapper;
    private int count;
    private int prec_count;
    private List<String> valueList;

    public ValuesJSON(){

        factory  = new JsonFactory();
        mapper   = new ObjectMapper(factory);
        count = 0;
        prec_count = 0;
        valueList = new ArrayList<String>();

    }

    public List<String> getValuesData(String json,ArrayList<String> path) throws IOException{

        //input: json string
        mapper = new ObjectMapper(factory);
        JsonNode rootNode = mapper.readTree(json);

        Iterator<Map.Entry<String,JsonNode>> fieldsIterator = rootNode.getFields();

        while (fieldsIterator.hasNext()) {

            Map.Entry<String,JsonNode> field = fieldsIterator.next();

            checkStructure(field,path);
        }


        return valueList;
    }


    public void checkStructure(Map.Entry<String,JsonNode> field,ArrayList<String> path){

        if(field.getValue().isObject()){

              /*handle JSON Object*/


            if(field.getKey().equals(path.get(count).toString()) == true){

                count = count + 1;

                Iterator<Map.Entry<String,JsonNode>> fieldsOBJ = field.getValue().getFields();

                while (fieldsOBJ.hasNext()) {

                    Map.Entry<String,JsonNode> fielOBJ = fieldsOBJ.next();

                    if(!fielOBJ.getValue().isArray()&&!fielOBJ.getValue().isObject()){

                        String k1 = fielOBJ.getKey();

                        if(k1.equals(path.get(count).toString()) == true){

                            count = count + 1;

                            String v1 = fielOBJ.getValue().toString();
                            valueList.add(v1);

                            count = count - 1;
                        }
                    }else{
                        //recursive call for nested structures
                        checkStructure(fielOBJ,path);
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

                            String k2 = arrayfield.getKey();

                            if(k2.equals(path.get(count).toString()) == true){

                                count = count + 1;

                                String v2 = arrayfield.getValue().toString();
                                valueList.add(v2);

                                count = count - 1;
                            }
                        }else{
                            //recursive call for nested structures
                            checkStructure(arrayfield,path);

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
                //System.out.println(v3);
                valueList.add(v3);
            }
        }
    }
}
