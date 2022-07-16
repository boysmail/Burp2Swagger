package burp;

import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class BurpExtender implements IBurpExtender, IHttpListener, ITab, IExtensionStateListener{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;
    private JPanel panel;
    // private JSplitPane splitPane1;
    private JButton button;
    private JTextArea textArea = new JTextArea("",10,10);

    private JsonObject obj;
    private HttpServer server = SimpleFileServer.createFileServer(new InetSocketAddress(8090),
            Path.of(System.getProperty("user.dir") + "/dist/"), SimpleFileServer.OutputLevel.VERBOSE);
    private JsonHelper jsonHelper;


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

        try {
            server.start();
            stdout.println("launching server for swagger on port 8090");
        } catch (Exception e) {
            stdout.println("port 8090 in use");
        }

        jsonHelper = new JsonHelper();

        stdout.println("This is a Hello World app!");
        stdout.println(jsonHelper.dump());
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        var RequestUrl = helpers.analyzeRequest(messageInfo).getUrl();
        if (messageIsRequest && callbacks.isInScope(RequestUrl)){ //&& callbacks.isInScope(RequestUrl)
            // debug
            stdout.println("messGot in scope request to " + messageInfo.getHttpService());
            stdout.println("mess2Got in scope request to " + messageInfo.getHttpService().toString());
            stdout.println("help "+ RequestUrl);
            stdout.println("help "+ RequestUrl.getProtocol() + "://" + RequestUrl.getHost());
            stdout.println("help "+ RequestUrl.getPath());
            stdout.println("help "+ helpers.analyzeRequest(messageInfo).getMethod());
            //stdout.println("1" + helpers.analyzeResponse(messageInfo.getResponse()).getStatusCode());
            stdout.println();


            //if (!jsonHelper.isDomainSet){
            // checking for known ports
            if (RequestUrl.getPort() == 80 || RequestUrl.getPort() == 443){
                jsonHelper.setDomain(RequestUrl.getProtocol() + "://" + RequestUrl.getHost());
            } else {
                jsonHelper.setDomain(RequestUrl.getProtocol() + "://" + RequestUrl.getHost() + ":" + RequestUrl.getPort());
            }

            //}

            // THIS WAS UNCOMMENTED
            //jsonHelper.add(helpers.analyzeRequest(messageInfo));
            //textArea.append(helpers.analyzeRequest(messageInfo).getUrl().getHost() + "\n");
        }
        else if (!messageIsRequest && callbacks.isInScope(RequestUrl)){
            // debug
            var responseParams = helpers.analyzeRequest(messageInfo.getResponse()).getParameters();
            var requestParams = helpers.analyzeRequest(messageInfo.getRequest()).getParameters();
            stdout.println("response ----------");
            for (IParameter responseParam : responseParams) {
                stdout.println(responseParam.getName() + " = " + responseParam.getValue() + " pb " + responseParam.getType());
            }
            //stdout.println(helpers.analyzeResponse(messageInfo.getResponse()));
            stdout.println("----------");
            stdout.println("request ----------");
            for (IParameter requestParam : requestParams) {
                stdout.println(requestParam.getName() + " = " + requestParam.getValue() + " pb " + requestParam.getType());
            }

            stdout.println("----------");

            stdout.println("got params in response " + Arrays.toString(helpers.analyzeRequest(messageInfo.getResponse()).getParameters().toArray()));
            stdout.println("got params in request " + helpers.analyzeRequest(messageInfo.getRequest()).getParameters());
            stdout.println("response "+ helpers.analyzeResponse(messageInfo.getResponse()).getStatusCode());

            // Todo: check ending of each endpoint to blacklist .js .css etc ()
            jsonHelper.add2(messageInfo,helpers);
        }

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
    @Override
    public void extensionUnloaded() {
        server.stop(0);
    }
}
