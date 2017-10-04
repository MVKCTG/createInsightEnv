

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.xml.internal.messaging.saaj.util.Base64;
import jdk.nashorn.internal.parser.JSONParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class main {
    public static void main(String[] args) {
        System.out.println("V 0.3 - 20170908927");
        int errorCount = 0;
        List<String> ltc = new ArrayList<>();
        String url = "";
        String objectSchemaId = "";
        String objectSchemaName = "";
        String user = "";
        List<String> afterwork = new ArrayList<>();
        String file = "";
        for(int i = 0; i < args.length; i++){
            switch (args[i]){
                case "-u":
                    i++;
                    url = args[i];
                    break;
                case "-oid":
                    i++;
                    objectSchemaId = args[i];
                    break;
                case "-o":
                    i++;
                    objectSchemaName = args[i];
                    break;
                case "-l":
                    i++;
                    user = args[i];
                    break;
                case "-f":
                    i++;
                    file = args[i];
                    break;
                case "-h":
                    System.out.println("-u URL");
                    System.out.println("http://localhost:8080");
                    System.out.println("-o objectSchemaName");
                    System.out.println("objectSchemaName is casse sensitive");
                    System.out.println("-oid objectSchemaID");
                    System.out.println("The id of the objectSchema");
                    System.out.println("-l user:password");
                    break;
                case "-d":
                    url = "http://192.168.99.100:32772";
                    objectSchemaId = "";
                    objectSchemaName = "QA";
                    user = "mvankuyk:mvankuyk";
                    file ="c:\\Temp\\abc.csv";
                    break;
            }
        }



        if(url==""||user==""){
            System.out.println("-u URL");
            System.out.println("http://localhost:8080");
            System.out.println("-o objectSchemaName");
            System.out.println("objectSchemaName is casse sensitive");
            System.out.println("-oid objectSchemaID");
            System.out.println("The id of the objectSchema");
            System.out.println("-l user:password");
            System.exit(0);
        }

        Map<String,String> ObjectSchemas = new Hashtable();

        try {
            ObjectSchemas = mapObjectSchemas(url+"/rest/insight/1.0/objectschema/list",user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(objectSchemaName!=""){
            objectSchemaId = ObjectSchemas.get(objectSchemaName);
        }
        System.out.println("URL: "+url);
        System.out.println("SchemaName: "+objectSchemaName);
        System.out.println("SchemaID: "+ objectSchemaId);
        System.out.println("User: "+user);
        System.out.println("File: "+file);


        Map Status = new Hashtable();
        try {
            Status =mapStatus(url+"/rest/insight/1.0/config/statustype?",user);
            for (String key : ObjectSchemas.keySet()){
                Status.putAll(mapStatus(url + "/rest/insight/1.0/config/statustype?objectSchemaId=" + ObjectSchemas.get(key), user));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Map Referrences = new Hashtable();
        try {

            Referrences = mapStatus(url+"/rest/insight/1.0/config/referencetype?",user);
            for (String key : ObjectSchemas.keySet()){
                Referrences.putAll(mapStatus(url+"/rest/insight/1.0/config/referencetype?objectSchemaId="+ObjectSchemas.get(key),user));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        Map<String,String> ObjectTypes = new Hashtable();
        Map<String,String> Objects = new Hashtable();
        String ObjectTypeID = "";
        String ObjectID = "";
        String csvFile = file;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ";";


        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                ObjectTypeID = "";
                ObjectID = "";
                // use comma as separator
                String[] objectLine = line.split(cvsSplitBy);
                if(!ObjectTypes.containsKey(objectLine[0])){
                    ObjectTypeID = sendPost2(url+"/rest/insight/1.0/objecttype/create","{\"inherited\":false,\"abstractObjectType\":false,\"objectSchemaId\":\""+objectSchemaId+"\",\"parentObjectTypeId\":\"0\",\"iconId\":91,\"name\":\""+objectLine[0]+"\",\"description\":\"\"}",user);
                    ObjectTypes.put(objectLine[0],ObjectTypeID);
                }else{
                    ObjectTypeID = ObjectTypes.get(objectLine[0]);
                }
                if(!objectLine[1].isEmpty() && objectLine[1]!= null) {
                    if (!Objects.containsKey(objectLine[1])) {
                        ObjectID = sendPost2(url+"/rest/insight/1.0/objecttype/create", "{\"inherited\":false,\"abstractObjectType\":false,\"objectSchemaId\":\"2\",\"parentObjectTypeId\":\"" + ObjectTypeID + "\",\"iconId\":91,\"name\":\"" + objectLine[1] + "\",\"description\":\"\"}",user);
                        Objects.put(objectLine[1], ObjectID);
                    } else {
                        ObjectID = Objects.get(objectLine[1]);
                    }
                }else{
                    ObjectID=ObjectTypeID;
                }
                String json = "";
                String field = objectLine[2];
                boolean empty = false;
                try{
                    if(objectLine[3].isEmpty()){empty = true;}
                }catch(Exception e){
                    empty = true;
                }
                if(empty) {
                    switch (objectLine[2].toLowerCase()) {
                        case "owner":
                            json = "{\"name\":\"" + objectLine[2] + "\",\"type\":\"4\",\"additionalValue\":\"SHOW_PROFILE\",\"expand\":\"operations\"}";
                            break;
                       case "status":
                           String[] multi = objectLine[4].split(",");
                           String tvm = "";
                           for(String s:multi){
                               s=s.trim();
                               if(tvm!=""){
                                   tvm += ",";
                               }
                               //System.out.println("Stat: "+s);
                               if(Status.get(s)==null){

                                   String temp = "{\"name\":\""+s+"\",\"objectSchemaId\":\""+objectSchemaId+"\",\"category\":\"1\"}";
                                    System.out.println("ADD Statustype: "+url+"/rest/insight/1.0/config/statustype"+" "+temp);
                                   String ID = sendPost2(url+"/rest/insight/1.0/config/statustype",temp,user);
                                   Status.put(s,ID);
                                   tvm += "\"" + ID + "\"";

                               }else {
                                   tvm += "\"" + Status.get(s) + "\"";
                               }
                           }

                           //System.out.println("TVM: "+tvm);
                           json = "{\"name\":\""+ objectLine[2] +"\",\"type\":\"7\",\"typeValueMulti\":["+tvm+"],\"expand\":\"operations\"}";


                            break;
                        case "category":
                            String key = "";
                            boolean updateRef = false;
                            if (Referrences.containsKey("category")) {
                                key = Referrences.get("category").toString();
                            } else {
                                key = "CREATE:" + "category";
                                updateRef = true;
                            }
                            json = "{\"name\":\""+objectLine[2]+"\",\"type\":\"1\",\"typeValue\":\""+ObjectTypes.get(objectLine[2])+"\",\"additionalValue\":\""+key+"\",\"expand\":\"operations\"}";
                            if (updateRef) {
                                Referrences.clear();
                                Referrences = mapStatus(url + "/rest/insight/1.0/config/referencetype?", user);
                                for (String key1 : ObjectSchemas.keySet()) {
                                    Referrences.putAll(mapStatus(url + "/rest/insight/1.0/config/referencetype?objectSchemaId=" + ObjectSchemas.get(key1), user));
                                }
                            }
                            System.out.println("CAT: "+json);
                            break;
                        default:
                            json = "{\"name\":\"" + objectLine[2] + "\",\"description\":\"\",\"type\":\"0\",\"defaultTypeId\":\"0\",\"expand\":\"operations\"}";
                    }

                }else{
                    //ignore = true;
                    String key = "";
                    String ref = objectLine[3].toUpperCase().replace(" "+objectLine[2].toUpperCase(), "");

                    /*if(Referrences.containsKey(ref)){
                        key = Referrences.get(ref).toString();
                    }else{
                        key = "CREATE:"+ref;

                    }*/
                    afterwork.add(ObjectID+";"+objectLine[2] + ";" + ref);
                    //json = "{\"name\":\""+objectLine[2]+"\",\"type\":\"1\",\"typeValue\":\""+Objects.get(objectLine[2])+"\",\"additionalValue\":\""+key+"\",\"expand\":\"operations\"}";

                }
                try {
                    String a = sendPost2(url+"/rest/insight/1.0/objecttypeattribute/" + ObjectID, json,user);
                   /* if(!ignore) {
                        if (!objectLine[3].isEmpty()) {
                            afterwork.add(objectLine[1] + ";" + a + ";" + objectLine[2] + ";" + objectLine[3]);
                        }
                    }*/
                }catch (Exception e){}

                System.out.println(objectLine[0]+" "+objectLine[1]+" "+objectLine[2]);


            }

            //update
            System.out.println("Count of not linked items: "+afterwork.size());
            for (String s:afterwork) {
                System.out.println("AW: " + s);
                String[] split = s.split(";");
                String key = "";
                Boolean updateRef = false;
                if (Referrences.containsKey(split[2])) {
                    key = Referrences.get(split[2]).toString();
                } else {
                    key = "CREATE:" + split[2];
                    updateRef = true;
                }
                String json = "{\"name\":\"" + split[1] + "\",\"type\":\"1\",\"typeValue\":\"" + Objects.get(split[1]) + "\",\"additionalValue\":\"" + key + "\",\"expand\":\"operations\"}";
                String output = url + "/rest/insight/1.0/objecttypeattribute/" + split[0] + " " + json;
                System.out.println("AW URL: " + url + "/rest/insight/1.0/objecttypeattribute/" + split[0] + " " + json);
                try {
                    sendPost2(url + "/rest/insight/1.0/objecttypeattribute/" + split[0], json, user);
                    if (updateRef) {
                        Referrences.clear();
                        Referrences = mapStatus(url + "/rest/insight/1.0/config/referencetype?", user);
                        for (String key1 : ObjectSchemas.keySet()) {
                            Referrences.putAll(mapStatus(url + "/rest/insight/1.0/config/referencetype?objectSchemaId=" + ObjectSchemas.get(key1), user));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: OCCURRED FOR LINE ABOVE ");
                    ltc.add(output);
                    errorCount++;
                }


            }
                System.out.println(errorCount + " link errors");
            for (String s:ltc) {
                System.out.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String sendPost2(String urlStr, String dataJSON, String user) throws Exception {


        URL url = new URL(urlStr);
        String userCredentials = user;
        String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty ("Authorization", basicAuth);
        conn.setRequestProperty("X-Atlassian-Token", "nocheck");
        conn.setRequestProperty("User-Agent", "xx");
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");

        OutputStream os = conn.getOutputStream();
        os.write(dataJSON.getBytes("UTF-8"));
        os.close();

        // read the response
        InputStream in = new BufferedInputStream(conn.getInputStream());
        String result = new BufferedReader(new InputStreamReader(in)) .lines().collect(Collectors.joining("\n"));

        JSONObject obj = new JSONObject(result);
       // JSONArray geodata = obj.getJSONArray("geodata");


        String[] json = result.split(",");
        result = json[0].replace("\"id\":","").replace("{","");;


        in.close();
        conn.disconnect();

        return result;

    }

    private static String sendGet2(String urlStr, String user) throws Exception {


        URL url = new URL(urlStr);
        String userCredentials = user;
        String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty ("Authorization", basicAuth);
        conn.setRequestProperty("X-Atlassian-Token", "nocheck");
        conn.setRequestProperty("User-Agent", "xx");
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        conn.setRequestMethod("GET");


        // read the response
        InputStream in = new BufferedInputStream(conn.getInputStream());
        String result = new BufferedReader(new InputStreamReader(in)) .lines().collect(Collectors.joining("\n"));


        in.close();
        conn.disconnect();

        return result;

    }



    private static Map mapStatus(String url, String user){
        Map result = new Hashtable();
        String outcome = "";
        try {
            outcome = sendGet2(url,user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject obj = new JSONObject("{\"a\":"+ outcome +"}");
        JSONArray geodata = obj.getJSONArray("a");
        int is = geodata.length();
        for(int i=0;i<geodata.length();i++){
            JSONObject o = geodata.getJSONObject(i);
            result.put(o.getString("name"),o.getInt("id"));

        }


        return result;
    }

    private static Map mapObjectSchemas(String url, String user){
        Map<String,String> result = new Hashtable();
        String outcome = "";
        try {
            outcome = sendGet2(url,user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject obj = new JSONObject(outcome);
        JSONArray geodata = obj.getJSONArray("objectschemas");
        int is = geodata.length();
        for(int i=0;i<geodata.length();i++){
            JSONObject o = geodata.getJSONObject(i);
            result.put(o.getString("name"),""+o.getInt("id"));

        }


        return result;
    }
}
