package r2rq;


import java.util.ArrayList;


public class AnalizerPath {

    private String root;
    private int index_root;
    private int index;
    private String single_elem;
    private ArrayList<String> elems = new ArrayList<String>();

    public ArrayList<String> analizer(String path){

        index_root = path.indexOf("$.");

        path = path.substring(index_root + 2, path.length());

        if(path.indexOf(".") == -1){

            elems.add(getArray(path));
        }else{
            root = path.substring(0,path.indexOf("."));

            String subpath = path.substring(root.length() + 1);

            elems.add(getArray(root));

            index = subpath.indexOf(".");

            while (index >= 0) {

                single_elem = subpath.substring(0, index);

                elems.add(getArray(single_elem));

                subpath = subpath.substring(index + 1, subpath.length());


                index = subpath.indexOf(".");
            }

            single_elem = path.substring(path.lastIndexOf(".") + 1, path.length());

            elems.add(single_elem);
        }
        return elems;
    }

    public String getArray(String item){

        if(item.indexOf("[") != -1){

            item = item.substring(0, item.indexOf("["));
            return item;
        }

        return item;
    }

}
