package poldo;


import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Map;

public class HttpURLConnectionAPI {

    /**
     * Get connection with params
     * @param url base url
     * @param params get params
     * @throws Exception
     */
    String sendGet(String url,Map<String, String> params) throws Exception {

        //add "?" in case is not part of the URL
        if(url.indexOf('?') == -1){

            url = url.concat("?");

        }

		/*add params to GET call*/
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String,String> pair = (Map.Entry<String,String>)it.next();

            url = url.concat(pair.getKey() + "=" + pair.getValue().replaceAll(" ", "%20")+"&");

            //it.remove();
        }

        url = url.substring(0, url.lastIndexOf("&"));

        System.out.println(url);

		/*Server connection*/
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        if(responseCode == 200){

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();

        }else{

            return ""+responseCode;

        }
    }

    /**
     * Get connection with no params (free sources)
     * @param url location of the file
     * @return
     * @throws Exception
     */
    String sendGet(String url) throws Exception {

        System.out.println(url);

		/*Server connection*/
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        if(responseCode == 200){

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();

        }else{

            return ""+responseCode;

        }
    }
}
