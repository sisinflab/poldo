package poldo;


import java.util.ArrayList;

public class URLAnalizer {

    /**
     * example URL passed to the function that extract parameters
     */
    private String exampleURL;
    /**
     * beginning of the URL, up to "?"
     */
    private String endpoint;
    /**
     * List of the extracted inputs
     */
    private ArrayList<String> inputArray = new ArrayList<String>();

    /**
     * Return the complete example URL
     * @return exampleURL Complete URL to be used as example to extract input parameters
     */
    public String getExampleURL() {
        return exampleURL;
    }

    /**
     * Set the value of the complete URL
     * @param exampleURL Complete URL to be used as example to extract input parameters
     */
    public void setExampleURL(String exampleURL) {
        this.exampleURL = exampleURL;
    }

    /**
     * Return the endpoint of the service (first part of the submitted URL)
     * @return endpoint First part of the submitted URL, without parameters
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Set the value of the endpoint
     * @param endpoint First part of the URL, without parameters
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Return the ArrayList containing inputs
     * @return inputArray Array containing inputs
     */
    public ArrayList<String> getInputArray() {
        return inputArray;
    }

    /**
     * Set the ArrayList containing inputs
     * @param inputArray Array containing inputs
     */
    public void setInputArray(ArrayList<String> inputArray) {
        this.inputArray = inputArray;
    }

    /**
     * Constructor
     * @param url Example URL to be used as example to extract inputs
     */
    public URLAnalizer(String url){
        setExampleURL (url);
    }

    /**
     * Empty constructor
     */
    public URLAnalizer(){

    }

    /**
     * Analyze the URL and extract inputs
     * @param url Complete URL to be used to extracting inputs
     * @return -1 if errors occurs, 0 in the event of correct execution
     */
    public int analizeURL(String url){
        int start = url.indexOf("?") + 1;
        //if i don't find "?", return -1 (error)
        if(start==-1){
            return -1;
        }
        //if i don't find "=", return -1 (error)
        int end = url.indexOf("=", start);
        if(end==-1){
            return -1;
        }
        setEndpoint(url.substring(0, start-1));
        while(start!=0){
            inputArray.add(url.substring(start, end));
            start = url.indexOf("&", end) + 1;
            end = url.indexOf("=", start + 1);
        }
        return 0;
    }

}
