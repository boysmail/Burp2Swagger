package burp;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;


public class JsonHelper {
    JsonObject output;
    JsonArray servers;
    JsonObject server;
    JsonObject paths;
    JsonObject path;
    JsonObject methodJson;
    JsonObject methods;
    JsonObject responses;
    JsonObject responseContent;
    Gson gson;
    String endpoint;
    String method;
    //BurpExtender burp = new BurpExtender();
    //IExtensionHelpers helpers = burp.getHelpers();
    public boolean isDomainSet;

    public JsonHelper() {

        output = new JsonObject();
        gson = new GsonBuilder().setPrettyPrinting().create();
        //info part
        output.addProperty("openapi","3.0.0");
        JsonObject info = new JsonObject();
        info.addProperty("title","test");
        info.addProperty("description","test234");
        info.addProperty("version","1.0.0");
        output.add("info", info);

        //server part
        servers = new JsonArray();
        //server = new JsonObject();
        //server.addProperty("url","http://test-domain.com");

        //server.addProperty("description", "Example Description");
        //servers.add(server);
        output.add("servers", servers);

        paths = new JsonObject();
        output.add("paths",paths);
    }

    public void add(IRequestInfo messageInfo){
        String endpoint = messageInfo.getUrl().getPath();
        String method = messageInfo.getMethod();

        JsonObject methodJson = new JsonObject();
        methodJson.addProperty("summary", method + ' ' + endpoint);
        // somehow add response
        JsonObject path = new JsonObject();
        path.add(method.toLowerCase(), methodJson);

        paths.add(endpoint,path);

        //String endpoint = helpers.analyzeRequest(messageInfo).getUrl().getPath();
    }

    public String dump(){
        return gson.toJson(output);
    }

    public void setDomain(String requestUrlString) {
        // Or maybe it's better to just have a list of servers and check without creation of "server"
        server = new JsonObject();
        server.addProperty("url", requestUrlString);
        if (!servers.contains(server)){
            servers.add(server);
        }

        //isDomainSet = true;
    }

    public void add2(IHttpRequestResponse messageInfo, IExtensionHelpers helpers) {
        var req = helpers.analyzeRequest(messageInfo);
        var res = helpers.analyzeRequest(messageInfo.getResponse());
        var res2 = helpers.analyzeResponse(messageInfo.getResponse());
        endpoint = req.getUrl().getPath();
        method = req.getMethod();

        if (paths.has(endpoint)) {
            path = paths.get(endpoint).getAsJsonObject();
        }
        else {
            path = new JsonObject();
        }

        if (path.has(method.toLowerCase())){
            methodJson = path.get(method.toLowerCase()).getAsJsonObject();
        }
        else{
            methodJson = new JsonObject();
        }



        methodJson.addProperty("summary", method + ' ' + endpoint);

        if (req.getParameters().size() !=0){
            AddParameters(req,methodJson, true);
        }



        String statusCode = String.valueOf(res2.getStatusCode());
        if (methodJson.has("responses")){
            responses = methodJson.get("responses").getAsJsonObject();
        }
        else{
            responses = new JsonObject();
        }

        if (responses.has(statusCode)){
            responseContent = responses.get(statusCode).getAsJsonObject();
        }
        else {
            responseContent = new JsonObject();
        }


        responseContent.addProperty("description","Example Description");

        if(res.getParameters().size() != 0){
            AddParameters(res,responseContent, false);
        }


        // check here
        // if (!responses.has(statusCode)){
            responses.add(statusCode, responseContent);
        // }
        //responses.add(statusCode, responseContent);

        methodJson.add("responses", responses);



        //JsonObject path = new JsonObject();
        // check if method already exists
//        if (path.has(method.toLowerCase())){
//            path = path.get(method.toLowerCase());
//        }
//        else{
//
//        }
        JsonObject methods = new JsonObject();
        path.add(method.toLowerCase(),methodJson);
        //path.add("test", methods);

        // seems like we need to check if something exists, then we should grab it from paths, otherwise create new
        // not tested - check if path already exists

//        if (paths.has(endpoint)) {
//            paths.get(endpoint);
//        }
//        else {
//            paths.add(endpoint, path);
//        }

        paths.add(endpoint, path);
        SaveToFile();
        // TODO:
        // support for 2 methods on one path
        // support for 2 response codes on 1 method
        // support for parameters
        // support multiple servers
    }

    public void SaveToFile(){
        try{
            //System.out.println("Writing to file");
            Writer writer = Files.newBufferedWriter(Paths.get("burp2swagger_out/output.json"));
            gson.toJson(output, writer);
            writer.close();
        }
        catch (IOException e){
            //System.out.println("Failed writing");
            throw new RuntimeException(e);
        }


    }

    public void AddParameters(IRequestInfo req, JsonObject holder, boolean isRequest){

        // крч body запроса и ответ генерируются схожим образом
        // поэтому возможно стоит перенести это все в отдельную функцию
        var pars = req.getParameters();
        JsonArray parameters;
        if(holder.has("parameters")){
            parameters = holder.get("parameters").getAsJsonArray();
        }
        else {
            parameters = new JsonArray();
        }

        // for body
        JsonObject requestBody = new JsonObject();
        JsonObject content = new JsonObject();
        JsonObject mimeType = new JsonObject();
        JsonObject schemaBody = new JsonObject();
        JsonObject properties = new JsonObject();
        schemaBody.addProperty("type","object");
        schemaBody.add("properties",properties);


        for (IParameter par : pars) {

            // TODO: Ask if we should track cookies (probably not)
            if (par.getType() == IParameter.PARAM_URL){
                JsonObject parameter = new JsonObject();
                parameter.addProperty("name", par.getName());
                parameter.addProperty("in", "query");
                parameter.addProperty("description", "parameter " + par.getName());

                JsonObject schema = new JsonObject();
                // TODO ask about other stuff
                try{
                    Integer.valueOf(par.getValue());
                    schema.addProperty("type","integer");
                }catch (NumberFormatException e){
                    schema.addProperty("type","string");
                }

                parameter.add("schema",schema);
                parameter.addProperty("example", par.getValue());

                // checks if parameter already exists
                // can add parameter with same name but different schema
                // probably won't happen in real life
                if (!parameters.contains(parameter)) {
                    parameters.add(parameter);
                }


            }
            // body content
            if (par.getType() == IParameter.PARAM_BODY || par.getType() == IParameter.PARAM_JSON){

                // text in body?

                // Json in body
                if (par.getType() == IParameter.PARAM_JSON){
                    JsonObject property = new JsonObject();
                    try{
                        Integer.valueOf(par.getValue());
                        property.addProperty("type","integer");

                    }catch (NumberFormatException e){
                        property.addProperty("type","string");
                    }
                    // TODO ask if we need to add examples to to every property
                    property.addProperty("example",par.getValue());

                    properties.add(par.getName(),property);
                    schemaBody.add("properties",properties);
                    mimeType.add("schema",schemaBody);
                    content.add("application/json",mimeType);
                    requestBody.addProperty("description", "Body content for " + method + " " + endpoint);
                    requestBody.add("content",content);

                }

            }


            // add params for request and response
            //parameter.addProperty("name", par.getName());
            // get from GetType also check required tag
            //parameter.addProperty("in", "query");
            //parameter.addProperty("description", "parameter" + par.getName());
            // add schema by detecting value
//                if (!parameters.contains(parameter)) {
//                    parameters.add(parameter);
//                }


            //parameter.addProperty("required", "false");
        }
        //parameters.add(parameter);
        if (parameters.size() != 0){
            holder.add("parameters", parameters);
        }
        if (requestBody.size() != 0 && isRequest){
            holder.add("requestBody", requestBody);
        }
        else if(requestBody.size() != 0)
            holder.add("content",content);


        // TODO known bugs: json arrays https://yandex.ru/bell/api/v1/get-ticker

    }
}
