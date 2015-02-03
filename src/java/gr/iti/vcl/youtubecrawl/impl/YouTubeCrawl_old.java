/*
 * 1. Search google's Freebase API for topic Ids using input freetext paramenter
 * 2. Aply topic Ids recovered to a search on the youtube API
 * 3. Get video info applying the retrieved video ids with a second search on the youtube API
 * 
 * 4. Search directly usiong the youtube API and the input freetext paramenter
 * 5. Get video info applying the retrieved video ids with a forth search on the youtube API
 * 
 */
package gr.iti.vcl.youtubecrawl.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author dimitris.samaras@iti.gr
 */
public class YouTubeCrawl_old {

    private static final String FREEBASE_API_SITE = "https://www.googleapis.com/freebase/v1/search";
    private static final String YOUTUBE_API_SITE = "https://www.googleapis.com/youtube/v3/search";
    // DO NOT FORGET THE " ? " AFTER SEARCH!!!
    private static final String API_KEY = "&key=";
    private static final String PREFIX_QUERY = "query=";
    private static final String PREFIX_LIMIT = "&limit=";
    private static final String PREFIX_VIDEO_SEARCH_PART = "part=id";
    private static final String PREFIX_TOPIC_ID = "&topicId=";
    // Results are always too many to get less than max
    private static final String PREFIX_MAX_RESULTS = "&maxResults=50";
    private static final String PREFIX_PART = "part=";
    private static final String PREFIX_TYPE_VIDEO = "&type=video";
    private static final String PREFIX_SEARCH_TERM = "&q=";
    private static final String PREFIX_AFTER = "&publishedAfter=";
    private static final String PREFIX_BEFORE = "&publishedBefore=";
    public static String YOUTUBE_STREAM_COMMAND_CREATE = "create";
    public static String YOUTUBE_STREAM_COMMAND_REMOVE = "remove";
    public Connection connection = null;
    public Channel channel = null;

    public YouTubeCrawl_old() {
    }

    @SuppressWarnings("empty-statement")
    //public JSONObject parseOut() throws Exception, IOException {
    public JSONObject parseOut(JSONObject jsonObject) throws Exception, IOException {

        DateFormat formatter = null;
        // Create the JSONObject to construct the response that will be saved to RabbitMQ
        JSONObject resultObject = new JSONObject();
        // Create Array to be filled with the id values 
        ArrayList<String> freeTopicIds = new ArrayList<String>();
        Set <String> vidIds = new HashSet<String>();

        String command;

        try {

            command = jsonObject.getString("command");

            if (command.equals(YOUTUBE_STREAM_COMMAND_CREATE)) {
                // credential for Tumblr configuration
                String apiKey_val = jsonObject.getJSONObject("youtube").getString("apiKey");
                String host = jsonObject.getJSONObject("rabbit").getString("host");
                String qName = jsonObject.getJSONObject("rabbit").getString("queue");

                // check for certain crusial params 
                //keywords
                String topic = jsonObject.getJSONObject("youtube").getString("topic").replaceAll(" ", "+");

                if (topic == null || topic.isEmpty()) {
                    err("No topic given to explore, aborting");
                    resultObject.put("Status", "Error");
                    resultObject.put("Message", "No topic given");
                    //return resultObject;
                }

                //limit -- num of results
                // if no page_limit is defined set 20...as defined by default on Tumblr API
                String free_limit = jsonObject.getJSONObject("youtube").optString("free_limit", "20");

                String rsp = freeCallGET(topic, free_limit, apiKey_val);
                System.out.println(rsp);

                // Create the JSONObject to be parsed
                JSONObject jobj = new JSONObject(rsp);

                JSONArray topicArr = jobj.getJSONArray("result");

                //Add topic Ids to String Array

                for (int i = 0; i < topicArr.length(); i++) {
                    // get separate results from freebase response
                    JSONObject topicResp = new JSONObject(topicArr.getString(i));

                    //Retrieve topic responses 
                    String freeTopic = topicResp.getString("mid");
                    freeTopicIds.add(freeTopic);

                }
                // Start searching in the YOUTUBE API using the topics retrieved
                try {
                    if (freeTopicIds == null) {
                        err("No topics retrieved, aborting");
                        resultObject.put("Status", "Error");
                        resultObject.put("Message", "No topics retrieved");
                        //return resultObject;
                    } else {

                        for (String topicId : freeTopicIds) {
                            //https://www.googleapis.com/youtube/v3/search?part=id%2C+snippet
                            //&publishedAfter=2013-01-01T00%3A00%3A00Z&publishedBefore=2014-04-01T00%3A00%3A00Z
                            //&topicId=%2Fm%2F0947l&type=video&key={YOUR_API_KEY}
                            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

                            String publishedAfter = jsonObject.getJSONObject("youtube").optString("publishedAfter", "1970-01-01T00:00:00Z");
                            //get current date and format to pass to publishedBefore
                            Date date_now = new Date();
                            String conv_date_now = formatter.format(date_now);                            
                            String publishedBefore = jsonObject.getJSONObject("youtube").optString("publishedBefore", conv_date_now);

                            String videos_rsp = SearchListCallGET(publishedAfter, publishedBefore, topicId, apiKey_val);
                            //System.out.println(videos_rsp);
                            // Create the JSONObject to be parsed
                            JSONObject vids_jobj = new JSONObject(videos_rsp);
                            
                            JSONArray vidsArr = vids_jobj.getJSONArray("items");
                            while (vidsArr != null){
                                for (int i = 0; i < vidsArr.length(); i++) {
                                    JSONObject vidItem = new JSONObject(vidsArr.getString(i));

                                    //Retrieve video id
                                    String id = vidItem.getJSONObject("id").getString("videoId");
                                    vidIds.add(id);
                                }
                            }                      
                        }
                    }
                    for (String v : vidIds){
                        System.out.println("ViDeO" + v);
                    }
                } catch (JSONException e) {
                    err("JSONException parsing freebase API topics: " + e);
                }

//                    openRMQ(host, qName);
//                    writeToRMQ(parseVideos(video_id, apiKey_val), qName);



            } else if (command.equals(YOUTUBE_STREAM_COMMAND_REMOVE)) {
                closeRMQ();
            }
        } catch (JSONException e) {
            err("JSONException parsing initial response: " + e);
        }

        return resultObject;

    }

    private JSONObject parseVideos(String video_id, String apiKey_val) throws JSONException {

        String rsp = callGET(video_id, apiKey_val);

        // Create the JSONObject to be parsed
        JSONObject resultObject = new JSONObject(rsp);

        return resultObject;

    }

    public String freeCallGET(String topic, String free_limit, String apiKey_val) {
        String output;
        int code = 0;
        String msg = null;

        try {
            URL url = new URL(FREEBASE_API_SITE + "?" + PREFIX_QUERY + topic + PREFIX_LIMIT + free_limit + API_KEY + apiKey_val);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            // you need the following if you pass server credentials
            // httpCon.setRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode(servercredentials.getBytes()));
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("GET");
            output = convertStreamToString(httpCon.getInputStream());
            code = httpCon.getResponseCode();
            msg = httpCon.getResponseMessage();
            //output = "" + httpCon.getResponseCode() + "\n" + httpCon.getResponseMessage() + "\n" + output;

        } catch (IOException e) {
            output = "IOException during GET: " + e;
            err(output);
        }
        // Check for Response 
        if ((code != 200 || code != 201) && !("OK".equals(msg))) {
            //output = "NOT OK RESPONSE";
            err("Failed : HTTP error code : " + code);

        }

        return output;

    }

    private String SearchListCallGET(String publishedAfter,String publishedBefore, String topicId, String apiKey_val) {
            String output;
        int code = 0;
        String msg = null;

        try {
            URL url = new URL(YOUTUBE_API_SITE + "?" + PREFIX_VIDEO_SEARCH_PART + PREFIX_MAX_RESULTS + PREFIX_AFTER + publishedAfter + PREFIX_BEFORE + publishedBefore + PREFIX_TOPIC_ID + topicId + PREFIX_TYPE_VIDEO + API_KEY + apiKey_val);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            // you need the following if you pass server credentials
            // httpCon.setRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode(servercredentials.getBytes()));
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("GET");
            output = convertStreamToString(httpCon.getInputStream());
            code = httpCon.getResponseCode();
            msg = httpCon.getResponseMessage();
            //output = "" + httpCon.getResponseCode() + "\n" + httpCon.getResponseMessage() + "\n" + output;

        } catch (IOException e) {
            output = "IOException during GET: " + e;
            err(output);
        }
        // Check for Response 
        if ((code != 200 || code != 201) && !("OK".equals(msg))) {
            //output = "NOT OK RESPONSE";
            err("Failed : HTTP error code : " + code);

        }

        return output;
    }

    public String callGET(String id, String apiKey_val) {
        String output;
        int code = 0;
        String msg = null;


        try {
            URL url = new URL(FREEBASE_API_SITE + "/movies/" + id + ".json?" + API_KEY + apiKey_val);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            // you need the following if you pass server credentials
            // httpCon.setRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode(servercredentials.getBytes()));
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("GET");
            output = convertStreamToString(httpCon.getInputStream());
            code = httpCon.getResponseCode();
            msg = httpCon.getResponseMessage();
            //output = "" + httpCon.getResponseCode() + "\n" + httpCon.getResponseMessage() + "\n" + output;

        } catch (IOException e) {
            output = "IOException during GET: " + e;
            err(output);
        }
        // Check for Response 
        if ((code != 200 || code != 201) && !("OK".equals(msg))) {
            //output = "NOT OK RESPONSE";
            err("Failed : HTTP error code : " + code);

        }

        return output;

    }

    private static String convertStreamToString(InputStream is) throws IOException {
        //
        // To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.
        //
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }

            return writer.toString();
        } else {
            return "";
        }
    }

    public void writeToRMQ(JSONObject json, String qName) throws IOException {

        channel.basicPublish("", qName,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                json.toString().getBytes());
        log(" [x] Sent to queue '" + json + "'");
    }

    public void openRMQ(String host, String qName) throws IOException {
        //Pass the queue name here from the RESQUEST JSON

        //Create queue, connect and write to rabbitmq
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);

        log("connected to rabbitMQ on localhost ...");

        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare(qName, true, false, false, null);
        } catch (IOException ex) {
            err("IOException during queue creation: " + ex);
        }
    }

    public void closeRMQ() throws IOException {
        // PREPEI NA PANE STO TUMBLR_STREAM_COMMAND_REMOVE

        if (connection != null) {
            log("Closing rabbitmq connection and channels");
            try {
                connection.close();
                connection = null;
            } catch (IOException ex) {
                err("IOException during closing rabbitmq connection and channels: " + ex);

            }
        }
    }

    private void log(String message) {
        System.out.println("YoutubeCrawler:INFO: " + message);
    }

    private void err(String message) {
        System.err.println("YoutubeCrawler:ERROR:" + message);
    }
}
