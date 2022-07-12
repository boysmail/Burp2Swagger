package burp;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Locale;


public class JsonHelper {
    JsonObject output;
    JsonArray servers;
    JsonObject server;
    JsonObject paths;
    Gson gson;
    //BurpExtender burp = new BurpExtender();
    //IExtensionHelpers helpers = burp.getHelpers();
    public boolean isDomainSet;
    public JsonHelper() {

        output = new JsonObject();
        gson = new Gson();
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
        String endpoint = req.getUrl().getPath();
        String method = req.getMethod();

        JsonObject methodJson = new JsonObject();
        methodJson.addProperty("summary", method + ' ' + endpoint);

        if (req.getParameters().size() !=0){
            var pars = req.getParameters();
            JsonArray parameters = new JsonArray();
            JsonObject parameter = new JsonObject();
            for (var i = 0; i < pars.size(); i++){

                // add params for request and response
                parameter.addProperty("name", pars.get(i).getName());
                // get from GetType also check required tag
                parameter.addProperty("in", "query");
                parameter.addProperty("description", "parameter" + pars.get(i).getName());
                // add schema by detecting value


                //parameter.addProperty("required", "false");
            }
            parameters.add(parameter);
            methodJson.add("parameters",parameters);
        }



        JsonObject response = new JsonObject();
        JsonObject responseContent = new JsonObject();
        String statusCode = String.valueOf(res2.getStatusCode());
        responseContent.addProperty("description","Example Description");


        response.add(statusCode, responseContent);

        methodJson.add("responses", response);



        JsonObject path = new JsonObject();
        path.add(method.toLowerCase(), methodJson);
        // not tested - check if path already exists
        //if (!paths.has(String.valueOf(path))) {
            paths.add(endpoint, path);
        //}

        // TODO:
        // support for 2 methods on one path
        // support for 2 response codes on 1 method
        // support for parameters
        // support multiple servers
    }
}
