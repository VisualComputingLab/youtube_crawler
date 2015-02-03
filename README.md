# youtube_crawler
A java web crawler for the ‘YouTube’ video sharing platform API

##About this project 

Project name: YouTubeCrawler
Architecture: Restfull application
Programming language: java 
Structuring and output format: json
Application server: Apache Tomcat
Messaging system: RabbitMQ, based on the AMQP standard

A java wrapper for the ‘YouTube’ video sharing platform API, a platform that allows users to view information, rate and comment on videos posted by other users.
With the YouTubeCrawler we try to gather video information.
Every video and its relevant metadata form a separate message that is delivered to the RabbitMQ.
The process is initiated by posting (POST request) our request to the Tomcat using a rest client (i.e. Advanced Rest Client for Google Chrome browser) followed by the .json file containing the request payload. The result of the request is written to the RabbitMQ and we get a server response about the operation status.

#Users --REST calls  

The user in order to search the YouTube social network for videos over a specific topic has to post a request with a specific payload to indicate the search parameters. 

i.e.
POST http://localhost:8084/YouTubeCrawler/resources/crawl
Content-Type: "application/json"

Payload
{
"youtube": {
		"apiKey": "AIzaSyBbCIyKaGOv98XZ9IGIlQQIMP6_hCWQl7w",
		"topic":"fashion",
		"free_limit": 3,
		"publishedAfter":"2014-01-01T01:01:01Z",
		"publishedBefore":"yyyy-MM-dd'T'HH:mm:ss'Z'"
 },
"rabbit": {
		"host": "localhost",
		"queue": "YOUTUBE_CRAWLER_IN_QUEUE"
}
}

•	The url defines where the service runs
•	The content-type defines what type is the request payload we are about to send to the application server
•	youtube object:
o	API key primitive ‘apiKey’ is the consumer key provided to us when we register a new application when a new application exploiting the YOUTUBE  v3 API is registered to the site.
o	Topic primitive ‘topic’ is the parameter we want to search for. With this search parameter the crawler searches to the Google’s FREEBASE API as well to the YOUTUBE API.
o	Free limit primitive ‘free_limit’ (optional) limits the search results returned from the FREEBASE API request.
o	Published before primitive ‘publishedBefore’ (optional)  defines the date before which the video should be posted
o	Published after primitive ‘publishedAfter’ (optional)  defines the date after which the video should be posted
•	rabbit object:
o	Host primitive ‘host’ defines where the RabbitMQ server is hosted 
o	Queue primitive ‘queue’ defined how the queue that will hold the messages should be named.


Since the server returns a 200, OK message the json objects that have been created can be accessed through the RabbitMQ server platform (localhost:15672…guest,guest)
	 

#Developers 

Package: gr.iti.vcl.youtubecrawl.impl

YouTubeCrawl.java methods documentation

The output of this class is zero or more messages written to a specific RabbitMQ queue containing information about videos posted on ToyTube and a json object over the operation status.

parseOut

Responsible method calling the request and parsing the responses from the GET requests to the GOOGLE FREEBASE and YOUTUBE API. Opens and closes connection, creates queues to the RabbitMQ service and writes multiple objects to the queues. Returns a json object that notifies the user over the operation status. The operation time depends on the amount of the resulting topics and videos per topic of the responses that the calls to APIs return.

@param jsonObject 	The paylod of the initial POST request that the user provides and. defines the parameters to form the GET request to the YOUTUBE and FREEBASE API. 
@return 		The json object containing information about process status.
@throws IOException 	If an input or output exception occurred.
@throws Exception 		If an input or output exception occurred.

 
parseVideos

Responsible to gather video information over a specific video id to a single json object that forms the final message.

@param video_id 		The video id parameter of the request.
@param apikey_val		The Google Developer API key.
@return 		The json object containing the movie and cast info. 
@throws JSONException If json objects exceptions occur 

freecallGET 

Responsible for passing the user defined parameters to the GET request to the FREEBASE API containing and passing the response back so that processing is initiated.

@param topic 		The topic parameter of the request.
@param free_limit		Limits the results fro relevancy.
@param apikey_val		The Google Developer API key.
@return 		The response of the GET request as String. 

SearchListCallGET 

Responsible for passing the topic id parameter to the GET request to the YOUTUBE API to retrieve videos per topic id.

@param pageToken		Variable to iterate to results next pages.
@param publishedAfter Defines lower time limit.
@param publishedBefore Defines upper time limit.
@param topicId		The topic id parameter.
@param apikey_val		The Google Developer API key.
@return 		The string containing the GET response. 

qSearchListCallGET

Responsible for passing the user defined topic parameter to the GET request to the YOUTUBE API to retrieve relevant videos.

@param pageToken		Variable to iterate to results next pages.
@param publishedAfter Defines lower time limit.
@param publishedBefore Defines upper time limit.
@param q		The topic search parameter.
@param apikey_val		The Google Developer API key.
@return 		The string containing the GET response. 

callGET 

Responsible for passing the video id parameter to the GET request to the YOUTUBE API to retrieve video metadata.

@param video_id 		The video id parameter of the request.
@param apikey_val		The Google Developer API key.
@return 		The string containing the GET response.



convertStreamToString

Responsible for parsing the inputstream created by the GET request to a String 

@param is 		The inputStream.
@return 		The String. 
@throws IOException 	If an input or output exception occurred.

writeToRMQ

Responsible for writing messages to the queue.

@param json 		The json object that will be stored to the messages queue (bytes).
@param qName		The qName that the message will be stored to.
@throws IOException 	If an input or output exception occurred.

openRMQ

Responsible for creating a connection with the RabbitMQ server and creating a queue 

@param host		The host of the RabitMQ.
@param qName		The qName that the message will be stored to.
@throws IOException 	If an input or output exception occurred.

closeRMQ

Responsible for closing the connection and channels to the RabbitMQ queue

log & err

Logging and error messaging methods

Package: gr.iti.vcl.youtubecrawl.rest

YouTubeCrawl_Rest.java methods documentation

@POST
@Consumes("application/json")
@Produces("application/json")

postJson

The rest implementation for the Tumblr crawler.
@param json 	The json object containing the payload for the Post request provided by the user.
@return json	The json object containing the operation status.
@throws Exception	if json object not provided to method 


#Problems met

While it is possible to search directly for videos over a topic only by using the YOUTUBE API the returned results are of low relevancy (broad search with “q”), therefore an initial call to the FREEBASE API has to be made so that relevant topic ids to the topic search term are obtained. Requesting to the FREEBASE API, the relevancy to the topic terms fades as topic id results increase. Therefore the later results have to be restricted.  Yet, in order to get the best possible results apart both the YOUTUBE and the FREEBASE API are employed for extensive search.

Video results are limited to 500 videos per topic id although the total videos count results per topic is provided via the response, i.e. only 500 videos are returned from the total 130000!. To overcome that ‘publishedBefore’ and ‘publishedAfter’ primitives usage is of high interest. 

Putting directly a video URL as a search term (‘topic’) works but also other video containing among their metadata this url (A common reference practise among YOUTUBE users) will show up in the results, usually the requested one is the first. The provided URL could be parsed is order to obtain the exact video id (…watch?{video-id}) but the URL format differs, as it could be a single video URL, a playlist URL or a channel URL. Furthermore an additional method is needed so that in case the topic is a URL the FREEBASE and YOUTUBE search-by-topic methods are escaped, otherwise the results would be the same as described earlier.

Comments and comments’ Authors cannot be obtained via the YOUTUBE API v.3 only through the v.2 witch is currently deprecated because of changes to the commenting system (integration with the Google+ social network).

Because of the sequential and multiple get requests as resulting videos increase the process slows down.

#Future work

Search with video url.

Parse the URL for video id and construct new methods for direct video search.

Operation status as json to server .

Multitheading? Change set for Video Ids to HashMap for parallel processing? 

Display all the messages created as a single object as a server log message. Total Videos count displayed instead.
 
Try Catch statements surrounding all object parsing methods to prevent user from malformed and erroneous input. (Maybe! restrict it from the UI).

ETags  primitive stored in the messages while seeming irrelevant  can be of use for search results Optimization and Quota usage limitation because  ETags will change whenever the underlying data changes.

Get comments and comment authors (on later implementation of the API).

Search not only for videos (youtube#video), but also for video playlists/lists (youtube#list).
