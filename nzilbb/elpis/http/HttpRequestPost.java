//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//

package nzilbb.elpis.http;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.InputStream;
import java.util.Random;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.Iterator;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * POST HTTP request.
 * <p>Originally com.myjavatools.web.ClientHttpRequest (version 1.0) by Vlad Patryshev</p>
 * <p>Adapted for LaBB-CAT by Robert Fromont</p>
 */
public class HttpRequestPost {
   
   protected HttpURLConnection connection;
   protected OutputStream os = null;
   protected Map<String,String> cookies = new HashMap<String,String>();
   
   /** Parameters so far flag */
   protected boolean bNoParametersYet = true;

   /** JSON body */
   protected Map<String,String> jsonBody = null;

   private String url = "?"; 
   private StringBuilder body = new StringBuilder(); 
   
   /**
    * Sets a request parameter value
    * @param sKey
    * @param sValue
    */
   public HttpRequestPost setHeader(String sKey, String sValue) {
      
      connection.setRequestProperty(sKey, sValue);
      return this;
   } // end of setHeader()
   
   protected void connect() throws IOException {
      
      if (os == null) {
         os = connection.getOutputStream();
      }
   }
   
   protected void write(char c) throws IOException {
      
      connect();
      os.write(c);
   }
   
   protected void write(String s) throws IOException {
      
      body.append(s);
      connect();
      os.write(s.getBytes());
   }
   
   protected void newline() throws IOException {
      
      body.append("\n");
      connect();
      write("\r\n");
   }
   
   protected void writeln(String s) throws IOException {
      
      connect();
      write(s);
      newline();
   }
   
   /**
    * Creates a new POST HTTP request on a freshly opened URLConnection
    *
    * @param connection an already open URL connection
    * @param sAuthorization Authorisation string or null if none is required
    * @throws IOException
    */
   public HttpRequestPost(HttpURLConnection connection, String sAuthorization) throws IOException {
      
      this.connection = connection;
      connection.setUseCaches(false);
      if (sAuthorization != null) {
         connection.setRequestProperty(
            "Authorization", sAuthorization);
      }
      connection.setDoOutput(true);
      connection.setChunkedStreamingMode(1024);
   }
   
   /**
    * Creates a new  POST HTTP request for a specified URL
    *
    * @param url the URL to send request to
    * @param sAuthorization Authorisation string or null if none is required
    * @throws IOException
    */
   public HttpRequestPost(URL url, String sAuthorization) throws IOException {
      
      this((HttpURLConnection)url.openConnection(), sAuthorization);
      this.url = url.toString();
   }
   
   /**
    * Creates a new  POST HTTP request for a specified URL
    *
    * @param url the URL to send request to
    * @throws IOException
    */
   public HttpRequestPost(URL url) throws IOException {
      
      this((HttpURLConnection)url.openConnection(), null);
      this.url = url.toString();
   }
   
   /**
    * Creates a new POST HTTP request for a specified URL string
    *
    * @param urlString the string representation of the URL to send request to
    * @throws IOException
    */
   public HttpRequestPost(String urlString, String sAuthorization) throws IOException {
      
      this(new URL(urlString), sAuthorization);
      url = urlString;
   }
      
   @SuppressWarnings("rawtypes")
   private void postCookies() {
      
      StringBuffer cookieList = new StringBuffer();
      
      for (Iterator i = cookies.entrySet().iterator(); i.hasNext();) {
         Map.Entry entry = (Map.Entry)(i.next());
         cookieList.append(entry.getKey().toString() + "=" + entry.getValue());
	 
         if (i.hasNext()) {
            cookieList.append("; ");
         }
      }
      if (cookieList.length() > 0) {
         connection.setRequestProperty("Cookie", cookieList.toString());
      }
   }
   
   /**
    * adds a cookie to the requst
    * @param name cookie name
    * @param value cookie value
    * @throws IOException
    */
   public HttpRequestPost setCookie(String name, String value) throws IOException {
      
      cookies.put(name, value);
      return this;
   }
   
   /**
    * adds cookies to the request
    * @param cookies the cookie "name-to-value" map
    * @throws IOException
    */
   public HttpRequestPost setCookies(Map<String,String> cookies) throws IOException {
      
      if (cookies == null) return this;
      this.cookies.putAll(cookies);
      return this;
   }
   
   /**
    * adds cookies to the request
    * @param cookies array of cookie names and values (cookies[2*i] is a name, cookies[2*i
    * + 1] is a value) 
    * @throws IOException
    */
   public HttpRequestPost setCookies(String[] cookies) throws IOException {
      
      if (cookies == null) return this;
      for (int i = 0; i < cookies.length - 1; i+=2) {
         setCookie(cookies[i], cookies[i+1]);
      }
      return this;
   }
   
   /**
    * adds a string parameter to the request
    * @param name parameter name
    * @param value parameter value
    * @throws IOException
    */
   public HttpRequestPost setParameter(String name, String value) throws IOException {
      
      if (value == null) return this; //20100520 robert.fromont@canterbury.ac.nz 
      if (!bNoParametersYet) write("&");
      write(URLEncoder.encode(name, "UTF8") 
            + "=" + URLEncoder.encode(value, "UTF8"));
      bNoParametersYet = false;
      return this;
   }
   
   /**
    * adds a string parameter to the JSON body object.
    * @param name parameter name
    * @param value parameter value
    * @throws IOException
    */
   public HttpRequestPost setJsonParameter(String name, String value) throws IOException {
      
      if (value == null) return this;
      if (jsonBody == null) {
         setHeader("Content-Type", "application/json;charset=utf-8");
         jsonBody = new HashMap<String,String>();
      }
      jsonBody.put(name, value);
      return this;
   }
   
   /**
    * adds a parameter to the request; if the parameter is a File, the file is uploaded,
    * otherwise the string value of the parameter is passed in the request 
    * @param name parameter name
    * @param object parameter value, or a collection of values
    * @throws IOException
    */
   public HttpRequestPost setParameter(String name, Object object) throws IOException {
      
      if (object == null) return this;
      if (object.getClass().isArray()) {
         object = Arrays.asList((Object[])object);
      }
      if (object instanceof Iterable) {
         @SuppressWarnings("rawtypes")
	    Iterator i = ((Iterable)object).iterator();
         while (i.hasNext()) {
            Object o = i.next();
            if (o != null)
               setParameter(name, o.toString());
         }
      } else {
         setParameter(name, object.toString());
      }
      return this;
   }
   
   /**
    * adds parameters to the request
    * @param parameters "name-to-value" map of parameters; if a value is a file, the file
    * is uploaded, otherwise it is stringified and sent in the request 
    * @throws IOException
    */
   @SuppressWarnings("rawtypes")
   public HttpRequestPost setParameters(Map<String,String> parameters) throws IOException {
      
      if (parameters == null) return this;
      for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
         Map.Entry entry = (Map.Entry)i.next();
         setParameter(entry.getKey().toString(), entry.getValue());
      }
      return this;
   }
   
   /**
    * adds parameters to the request
    * @param parameters array of parameter names and values (parameters[2*i] is a name,
    * parameters[2*i + 1] is a value); if a value is a file, the file is uploaded,
    * otherwise it is stringified and sent in the request 
    * @throws IOException
    */
   public HttpRequestPost setParameters(Object[] parameters) throws IOException {
      
      if (parameters == null) return this;
      for (int i = 0; i < parameters.length - 1; i+=2) {
         setParameter(parameters[i].toString(), parameters[i+1]);
      }
      return this;
   }
   
   /**
    * Generates the JSON body.
    * @return A JSON string.
    */
   public String generateJson()
   {
      if (jsonBody != null) {
         JsonObjectBuilder builder = Json.createObjectBuilder();
         for (String parameter : jsonBody.keySet()) {
            builder.add(parameter, jsonBody.get(parameter));
         }
         return builder.build().toString();
      } else {       
         return null;
      }
   } // end of generateJson()
   
   /**
    * posts the requests to the server, with all the cookies and parameters that were added
    * @return input stream with the server response
    * @throws IOException
    */
   public HttpURLConnection post() throws IOException {

      if (jsonBody != null) {
         write(generateJson());
      }
      if (os != null) os.close();
      return connection;
   }
   
   /**
    * posts the requests to the server, with all the cookies and parameters that were
    * added before (if any), and with parameters that are passed in the argument 
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameters
    */
   public HttpURLConnection post(Map<String,String> parameters) throws IOException {
      
      setParameters(parameters);
      return post();
   }
   
   /**
    * posts the requests to the server, with all the cookies and parameters that were
    * added before (if any), and with parameters that are passed in the argument 
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameters
    */
   public HttpURLConnection post(Object[] parameters) throws IOException {
      
      setParameters(parameters);
      return post();
   }
   
   /**
    * posts the requests to the server, with all the cookies and parameters that were
    * added before (if any), and with cookies and parameters that are passed in the
    * arguments 
    * @param cookies request cookies
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameters
    * @see #setCookies
    */
   public HttpURLConnection post(Map<String,String> cookies, Map<String,String> parameters) throws IOException {
      
      setCookies(cookies);
      setParameters(parameters);
      return post();
   }
   
   /**
    * posts the requests to the server, with all the cookies and parameters that were
    * added before (if any), and with cookies and parameters that are passed in the
    * arguments 
    * @param cookies request cookies
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameters
    * @see #setCookies
    */
   public HttpURLConnection post(String[] cookies, Object[] parameters) throws IOException {
      
      setCookies(cookies);
      setParameters(parameters);
      return post();
   }
   
   /**
    * post the POST request to the server, with the specified parameter
    * @param name parameter name
    * @param value parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public HttpURLConnection post(String name, Object value) throws IOException {
      
      setParameter(name, value);
      return post();
   }
   
   /**
    * post the POST request to the server, with the specified parameters
    * @param name1 first parameter name
    * @param value1 first parameter value
    * @param name2 second parameter name
    * @param value2 second parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public HttpURLConnection post(String name1, Object value1, String name2, Object value2) throws IOException {
      
      setParameter(name1, value1);
      return post(name2, value2);
   }
   
   /**
    * post the POST request to the server, with the specified parameters
    * @param name1 first parameter name
    * @param value1 first parameter value
    * @param name2 second parameter name
    * @param value2 second parameter value
    * @param name3 third parameter name
    * @param value3 third parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public HttpURLConnection post(String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException {
      
      setParameter(name1, value1);
      return post(name2, value2, name3, value3);
   }
   
   /**
    * post the POST request to the server, with the specified parameters
    * @param name1 first parameter name
    * @param value1 first parameter value
    * @param name2 second parameter name
    * @param value2 second parameter value
    * @param name3 third parameter name
    * @param value3 third parameter value
    * @param name4 fourth parameter name
    * @param value4 fourth parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public HttpURLConnection post(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException {
      
      setParameter(name1, value1);
      return post(name2, value2, name3, value3, name4, value4);
   }
   
   /**
    * posts a new request to specified URL, with parameters that are passed in the argument
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameters
    */
   public static HttpURLConnection post(URL url, String sAuthorization, Map<String,String> parameters) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(parameters);
   }
   
   /**
    * posts a new request to specified URL, with parameters that are passed in the argument
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameters
    */
   public static HttpURLConnection post(URL url, String sAuthorization, Object[] parameters) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(parameters);
   }
   
   /**
    * posts a new request to specified URL, with cookies and parameters that are passed in the argument
    * @param cookies request cookies
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setCookies
    * @see #setParameters
    */
   public static HttpURLConnection post(URL url, String sAuthorization, Map<String,String> cookies, Map<String,String> parameters) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(cookies, parameters);
   }
   
   /**
    * posts a new request to specified URL, with cookies and parameters that are passed in the argument
    * @param cookies request cookies
    * @param parameters request parameters
    * @return input stream with the server response
    * @throws IOException
    * @see #setCookies
    * @see #setParameters
    */
   public static HttpURLConnection post(URL url, String sAuthorization, String[] cookies, Object[] parameters) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(cookies, parameters);
   }
   
   /**
    * post the POST request specified URL, with the specified parameter
    * @param name1 parameter name
    * @param value1 parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public static HttpURLConnection post(URL url, String sAuthorization, String name1, Object value1) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(name1, value1);
   }
   
   /**
    * post the POST request to specified URL, with the specified parameters
    * @param name1 first parameter name
    * @param value1 first parameter value
    * @param name2 second parameter name
    * @param value2 second parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public static HttpURLConnection post(URL url, String sAuthorization, String name1, Object value1, String name2, Object value2) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(name1, value1, name2, value2);
   }
   
   /**
    * post the POST request to specified URL, with the specified parameters
    * @param name1 first parameter name
    * @param value1 first parameter value
    * @param name2 second parameter name
    * @param value2 second parameter value
    * @param name3 third parameter name
    * @param value3 third parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public static HttpURLConnection post(URL url, String sAuthorization, String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(name1, value1, name2, value2, name3, value3);
   }
   
   /**
    * post the POST request to specified URL, with the specified parameters
    * @param name1 first parameter name
    * @param value1 first parameter value
    * @param name2 second parameter name
    * @param value2 second parameter value
    * @param name3 third parameter name
    * @param value3 third parameter value
    * @param name4 fourth parameter name
    * @param value4 fourth parameter value
    * @return input stream with the server response
    * @throws IOException
    * @see #setParameter
    */
   public static HttpURLConnection post(URL url, String sAuthorization, String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException {
      
      return new HttpRequestPost(url, sAuthorization).post(name1, value1, name2, value2, name3, value3, name4, value4);
   }

   /**
    * String representation of the request, for logging.
    * @return A String representation of the request, for logging.
    */
   public String toString() {
      if (jsonBody != null) {
         return "POST " + url + " : " + generateJson();
      } else {
         return "POST " + url + " : " + body;
      }
   } // end of toString()
}
