#!/usr/bin/env groovy
import groovy.json.JsonSlurperClassic;


HttpResponse doGetHttpRequest(String requestUrl){
    URL url = new URL(requestUrl);
    HttpURLConnection connection = url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    HttpResponse resp = new HttpResponse(connection);

    if(resp.isFailure()){
        error("\nGET from URL: $requestUrl\n  HTTP Status: $resp.statusCode\n  Message: $resp.message\n  Response Body: $resp.body");
    }
    return resp;
}

HttpResponse doPostHttpRequestWithJson(String json, String requestUrl){
    return doHttpRequestWithJson(json, requestUrl, "POST");
}

HttpResponse doPutHttpRequestWithJson(String json, String requestUrl){
    return doHttpRequestWithJson(json, requestUrl, "PUT");
}

HttpResponse doHttpRequestWithJson(String json, String requestUrl, String verb){
    URL url = new URL(requestUrl);
    HttpURLConnection connection = url.openConnection();

    connection.setRequestMethod(verb);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.doOutput = true;

    def writer = new OutputStreamWriter(connection.outputStream);
    writer.write(json);
    writer.flush();
    writer.close();

    connection.connect();

    //parse the response
    HttpResponse resp = new HttpResponse(connection);

    if(resp.isFailure()){
        error("\n$verb to URL: $requestUrl\n    JSON: $json\n    HTTP Status: $resp.statusCode\n    Message: $resp.message\n    Response Body: $resp.body");
    }

    return resp;
}

class HttpResponse {
    String body;
    String message;
    Integer statusCode;
    boolean failure = false;

    public HttpResponse(HttpURLConnection connection){
        this.statusCode = connection.responseCode;
        this.message = connection.responseMessage;

        if(statusCode == 200 || statusCode == 201){
            this.body = connection.content.text;//this would fail the pipeline if there was a 400
        }else{
            this.failure = true;
            this.body = connection.getErrorStream().text;
        }

        connection = null; //set connection to null for good measure, since we are done with it
    }
}


// Servicebook iterator to get operational tests for a given project
Object getProjectTests(String name) {
    resp = doGetHttpRequest("http://servicebook.dev.mozaws.net/api/api/project").body;
    def jsonSlurper = new JsonSlurperClassic();
    def projects = jsonSlurper.parseText(resp);

    for (project in projects.data) {
        if (project.name == name) {
            def jenkin_tests = [];
                for (test in project.tests) {
                    if (test.jenkins_pipeline) {
                        jenkin_tests << test;
                    }
                }
            return jenkin_tests;
        }
    }
    return null;
}


allowedOrgs = ['Kinto', 'mozilla', 'mozilla-services']


def validURL(url) {
    for (allowedOrg in allowedOrgs) {
        if (url.startsWith('https://github.com/' + allowedOrg + '/')) {
            return true
        }
    }
    return false
}


def runStage(test) {
    stage(test.name) {
        if (validURL(test.url)) {
            echo "checking out " + test.url + ".git"
            node {
                checkout([$class: 'GitSCM',
                branches: [[name: '*/master']],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'CleanCheckout']],
                submoduleCfg: [], 
                userRemoteConfigs: [[url: test.url + '.git']]]
                )
            }
            echo "checked out"
            node {
                sh "chmod +x run"
                sh "./run"
            }
        } else {
            echo test.url + " is not allowed"
        }
    }
}


def testProject(name) {
    def tests = getProjectTests(name)

    for (test in tests) {
        timeout(time: 300, unit: 'SECONDS') {
            try {
                runStage(test)
            } catch (exc) {
                echo test.name + " failed"
                throw exc
            }
        }
    }
}


return this;
