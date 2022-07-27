package burp;


import com.google.gson.*;
import com.sun.net.httpserver.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BurpExtender implements IBurpExtender, IHttpListener, ITab, IExtensionStateListener{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;
    private JPanel panel;
    // private JSplitPane splitPane1;
    private JButton button;
    private JTextArea textArea = new JTextArea("",10,10);

    private JsonObject obj;
    private HttpServer server;
    private JsonHelper jsonHelper;
    private Map<String, JsonHelper> jsonHelpers;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JsonArray htmlHolder = new JsonArray();


    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.callbacks.registerHttpListener(this);
        this.callbacks.registerExtensionStateListener(this);


        callbacks.setExtensionName("Test extension Java 18");
        this.stdout = new PrintWriter(callbacks.getStdout(), true);

        //UI tab
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                panel = new JPanel();
                button = new JButton("dump json");



                //JLabel label = new JLabel("Requested URLs:");
                JTextArea textArea = new JTextArea("",10,10);
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        textArea.setText(jsonHelper.dump());

                    }
                });
                //panel.setBounds(100,100,250,100);
                //panel.add(label);
                panel.add(button);
                panel.add(textArea);
                textArea.setVisible(true);
                callbacks.customizeUiComponent(panel);


//                // second variant
//                // setup panels
//                JLabel label1 = new JLabel("Requested URLs:");
//                JLabel label2 = new JLabel("Label 2:");
//                JLabel label3 = new JLabel("Label 3:");
//                JPanel panelTop = new JPanel();
//                JPanel panelMiddle = new JPanel();
//                JPanel panelBottom = new JPanel();
//                JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTop,panelMiddle);
//                splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, panelBottom);
//                panelTop.add(label1);
//                panelMiddle.add(label2);
//                panelBottom.add(label3);
//                splitPane.setVisible(true);
//                splitPane1.setVisible(true);
//                callbacks.customizeUiComponent(splitPane);
//                callbacks.customizeUiComponent(splitPane1);


                callbacks.addSuiteTab(BurpExtender.this);
            }
        });

        stdout.println(System.getProperty("user.dir"));

        File f = new File("burp2swagger_out/index.html");
        if (!f.exists() && f.getParentFile().mkdirs()){
            // TODO FIX
            //dropHtml();
        }



        server = SimpleFileServer.createFileServer(new InetSocketAddress(8090),
                Path.of(System.getProperty("user.dir") + "/burp2swagger_out/"), SimpleFileServer.OutputLevel.VERBOSE);
        try {
            server.start();
            stdout.println("launching server for swagger on port 8090");
        } catch (Exception e) {
            stdout.println("port 8090 in use");
        }

        jsonHelpers = new HashMap<>();

        //jsonHelper = new JsonHelper();

        stdout.println("This is a Hello World app!");
        //stdout.println(jsonHelper.dump());
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        var requestUrl = helpers.analyzeRequest(messageInfo).getUrl();
        var request = helpers.analyzeRequest(messageInfo.getRequest());
        var domain = requestUrl.getProtocol() + "://" + requestUrl.getHost();
        if (messageIsRequest&& callbacks.isInScope(requestUrl)){ //&& callbacks.isInScope(requestUrl)
            // debug
//            stdout.println("messGot in scope request to " + messageInfo.getHttpService());
//            stdout.println("mess2Got in scope request to " + messageInfo.getHttpService().toString());
//            stdout.println("help "+ requestUrl);
//            stdout.println("help "+ requestUrl.getProtocol() + "://" + requestUrl.getHost());
//            stdout.println("help "+ requestUrl.getPath());
//            stdout.println("help "+ helpers.analyzeRequest(messageInfo).getMethod());
            //stdout.println("1" + helpers.analyzeResponse(messageInfo.getResponse()).getStatusCode());
            //var a = helpers.bytesToString(messageInfo.getRequest()).substring(request.getBodyOffset());
            //stdout.println("asd"+a);
            //stdout.println(JsonParser.parseString(a).isJsonArray() + " " + JsonParser.parseString(a).isJsonObject());


            //if (!jsonHelper.isDomainSet){
            // checking for known ports
            // TODO : probably don't include localhost:8090 as domain

            if (jsonHelpers.containsKey(domain)){
                jsonHelper = jsonHelpers.get(domain);
            }
            else {
                jsonHelper = new JsonHelper();
                jsonHelpers.put(domain,jsonHelper);
            }

            if (request.getMethod() == "OPTIONS"){
                stdout.println("got options");
                stdout.println(request.getHeaders());
            }

            if (requestUrl.getPort() == 80 || requestUrl.getPort() == 443){
                jsonHelper.addDomain(domain);
            } else {
                jsonHelper.addDomain(domain + ":" + requestUrl.getPort());
            }

            if (request.getHeaders().contains("Referer: http://localhost:8090/")){
                addRequestHeaders(messageInfo);
            }

            //}

            // THIS WAS UNCOMMENTED
            //jsonHelper.add(helpers.analyzeRequest(messageInfo));
            //textArea.append(helpers.analyzeRequest(messageInfo).getUrl().getHost() + "\n");
        }
        else if (!messageIsRequest && callbacks.isInScope(requestUrl)){
            // debug
//            var responseParams = helpers.analyzeRequest(messageInfo.getResponse()).getParameters();
//            var requestParams = helpers.analyzeRequest(messageInfo.getRequest()).getParameters();
//
//            stdout.println("response ----------");
//            for (IParameter responseParam : responseParams) {
//                stdout.println(responseParam.getName() + " = " + responseParam.getValue() + " pb " + responseParam.getType());
//            }
//            //stdout.println(helpers.analyzeResponse(messageInfo.getResponse()));
//            stdout.println("----------");
//            stdout.println("request ----------");
//            for (IParameter requestParam : requestParams) {
//                stdout.println(requestParam.getName() + " = " + requestParam.getValue() + " pb " + requestParam.getType());
//            }
//
//            stdout.println("----------");
//
//            stdout.println("got params in response " + Arrays.toString(helpers.analyzeRequest(messageInfo.getResponse()).getParameters().toArray()));
//            stdout.println("got params in request " + helpers.analyzeRequest(messageInfo.getRequest()).getParameters());
//            stdout.println("response "+ helpers.analyzeResponse(messageInfo.getResponse()).getStatusCode());


            // TODO: CORS bypass doesn't work with preflights aka OPTIONS (test if works)
            // TODO: JSON Breaks at yandex.ru/ads/meta/265882
            // TODO: file upload

            // TODO: bug airbnb returns bad content type on preflight
            if (request.getHeaders().contains("Referer: http://localhost:8090/")){
                addResponseHeaders(messageInfo);
            }
            else if (!requestUrl.getPath().contains(".")){
                if (jsonHelpers.containsKey(domain)){
                    jsonHelper = jsonHelpers.get(domain);
                }
                else {
                    jsonHelper = new JsonHelper();
                    jsonHelpers.put(domain,jsonHelper);
                }
                jsonHelper.addRequest(messageInfo,helpers);
                saveToFiles(domain);
            }


        }

    }

    private void saveToFiles(String domain) {
        var entry = jsonHelpers.get(domain);

        try{

            JsonObject htmlPart = new JsonObject();
            htmlPart.addProperty("url", "http://localhost:8090/" + domain.replace("://","-" ) + ".json");
            htmlPart.addProperty("name", domain);
            if (!htmlHolder.contains(htmlPart)){
                htmlHolder.add(htmlPart);
            }

            System.out.println("Writing to file");
            Writer writer = Files.newBufferedWriter(Paths.get("burp2swagger_out/"+ domain.replace("://","-") +".json"));
            gson.toJson(entry.dumpAsJsonObject(), writer);
            writer.close();
        }
        catch (IOException e){
            System.out.println("Failed writing");
            throw new RuntimeException(e);
        }
        dropHtml(htmlHolder);
    }

    @Override
    public String getTabCaption() {
        return "Test Tab";
    }

    @Override
    public Component getUiComponent() {
        //return splitPane1;
        return panel;
    }

    public IExtensionHelpers getHelpers(){
        return helpers;
    }

    public void addRequestHeaders(IHttpRequestResponse messageInfo){
        var request = messageInfo.getRequest();
        var requestStr = helpers.bytesToString(request);
        var requestParsed = helpers.analyzeRequest(messageInfo);
        var body = requestStr.substring(requestParsed.getBodyOffset());
        var headers = requestParsed.getHeaders();
        headers.remove("Origin: http://localhost:8090");
        headers.add("Origin: "+ requestParsed.getUrl().getProtocol() + "://" + requestParsed.getUrl().getHost());

        var newRequest = helpers.buildHttpMessage(headers,body.getBytes());
        messageInfo.setRequest(newRequest);
    }
    public void addResponseHeaders(IHttpRequestResponse messageInfo){
        var response = messageInfo.getResponse();
        var responseStr = helpers.bytesToString(response);
        var responseParsed = helpers.analyzeResponse(response);
        var body = responseStr.substring(responseParsed.getBodyOffset());
        var headers = responseParsed.getHeaders();
        for(String header : headers){
            if (header.startsWith("Access-Control-Allow-Origin:")){
                headers.remove(header);
                headers.add("Access-Control-Allow-Origin: http://localhost:8090");
                break;
            }
        }
        // Will run if we don't find any ACAO headers in response
        if (!headers.contains("Access-Control-Allow-Origin: http://localhost:8090")){
            headers.add("Access-Control-Allow-Origin: http://localhost:8090");
        }
        var newResponse = helpers.buildHttpMessage(headers,body.getBytes());
        messageInfo.setResponse(newResponse);
    }
    public void dropHtml(JsonArray htmlHolder) {
        Writer writer;
        try {
            writer = Files.newBufferedWriter(Paths.get("burp2swagger_out/index.html"));
            writer.write("""
                    <!DOCTYPE html>
                    <html lang="en">
                      <head>
                        <meta charset="UTF-8">
                        <title>Swagger UI</title>
                        <link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/4.12.0/swagger-ui.css" />
                        <style>
                        html {
                            box-sizing: border-box;
                            overflow: -moz-scrollbars-vertical;
                            overflow-y: scroll;
                        }
                        
                        *,
                        *:before,
                        *:after {
                            box-sizing: inherit;
                        }
                        
                        body {
                            margin: 0;
                            background: #fafafa;
                        }
                        </style>
                        <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAAAkFBMVEUAAAAQM0QWNUYWNkYXNkYALjoWNUYYOEUXN0YaPEUPMUAUM0QVNUYWNkYWNUYWNUUWNUYVNEYWNkYWNUYWM0eF6i0XNkchR0OB5SwzZj9wyTEvXkA3az5apTZ+4C5DgDt31C9frjU5bz5uxTI/eDxzzjAmT0IsWUEeQkVltzR62S6D6CxIhzpKijpJiDpOkDl4b43lAAAAFXRSTlMAFc304QeZ/vj+ECB3xKlGilPXvS2Ka/h0AAABfklEQVR42oVT2XaCMBAdJRAi7pYJa2QHxbb//3ctSSAUPfa+THLmzj4DBvZpvyauS9b7kw3PWDkWsrD6fFQhQ9dZLfVbC5M88CWCPERr+8fLZodJ5M8QJbjbGL1H2M1fIGfEm+wJN+bGCSc6EXtNS/8FSrq2VX6YDv++XLpJ8SgDWMnwqznGo6alcTbIxB2CHKn8VFikk2mMV2lEnV+CJd9+jJlxXmMr5dW14YCqwgbFpO8FNvJxwwM4TPWPo5QalEsRMAcusXpi58/QUEWPL0AK1ThM5oQCUyXPoPINkdd922VBw4XgTV9zDGWWFrgjIQs4vwvOg6xr+6gbCTqE+DYhlMGX0CF2OknK5gQ2JrkDh/W6TOEbYDeVecKbJtyNXiCfGmW7V93J2hDus1bDfhxWbIZVYDXITA7Lo6E0Ktgg9eB4KWuR44aj7ppBVPazhQH7/M/KgWe9X1qAg8XypT6nxIMJH+T94QCsLvj29IYwZxyO9/F8vCbO9tX5/wDGjEZ7vrgFZwAAAABJRU5ErkJggg==" sizes="32x32" />
                        <link rel="icon" type="image/png" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAABNVBMVEVisTRhsTReqzVbpTVXoDdVnTdSlzhRljgvXkAuXUAtWkErV0EzZj40Zj85bz0lTkMkTUMkT0MmTUIkS0IjTEIhSUMkS0IkTEIkTUIlTUIkTkMlTkMcQUQcP0UfQ0QdQ0QfREQgRUMiSUMiSUMjSkInU0EkTEMmUEEiR0IiSEMpVkErWT8kTUElTUIUNkYVNEQVMkcRM0QSNUYQMUIMMUkVK0AAJEkAM00AMzMAAAAAAACF6i2E6SyD6CyC5i2B5Sx/4i6A4S593S583S520jB00DByyjFxyTFwyDFvxjJtxTFtxDFswzJrwDJqvzJpvjNouzNoujNnuDNLjTlKijpKiTpEfztDfzxAeT0+dz05bj44bT44bj82aj81aD8zZT8bPUUbPkUcP0UcPUUeQ0UfREQgRkRgJREvAAAAO3RSTlP09PX19vX39u7u7/Dq6ufh4eDg4+Pf3Nvb2tnY2NvPv7y6rKupqaGZlpSOiYWETDEkHh0fFQwHCgUBAAcHrskAAADYSURBVHjaPc/ZLkNRGIbhz26KjVJpqSKGtjHPc9a7W7OEEhtBjDWUO3XghqQSwVrNTp+j///OXhlrLpdJdg9MLblbxqwPd5RLUDpOjK66YWMwTqRpaM0OhZbo3dskljea9+HyAevxHtoWVAjhfQtr5w3CSfUE8BrgvEDQpxRc3eyfH5wenlQuIO39Sb9x/8uv+bXvmPSjbABPRZznIkGvxkOo7mJtV+FsQsutcFvBuruG9kWZMY+G5pzxlMp/KPKZSUs2cLrzyMWVEyP1OGtlNpvs6p+p5/8DzUo5hMDku9EAAAAASUVORK5CYII=" sizes="16x16" />
                      </head>
                                        
                      <body>
                        <div id="swagger-ui"></div>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/4.12.0/swagger-ui-bundle.js" charset="UTF-8"> </script>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/4.12.0/swagger-ui-standalone-preset.js" charset="UTF-8"> </script>
                                        
                    <script>
                    window.onload = function() {
                      window.ui = SwaggerUIBundle({
                        urls: """+ gson.toJson(htmlHolder) + """
                        ,
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        plugins: [
                          SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                      });
                    };
                    </script>
                    </body>
                    </html>
                    """);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    @Override
    public void extensionUnloaded() {
        server.stop(0);
    }
}
