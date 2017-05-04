package com.overhere.liz.overhere.http;

import java.util.Map;

public class HttpResponseInterface {

    // the request url
    public String url;

    // the requester ip
    public String origin;

    // all headers that have been sent
    public Map headers;

    // url arguments
    public Map args;

    // post form parameters
    public Map form;

    // post body json
    public Map json;

    @Override
    public String toString() {
        return "HttpResponseInterface{" +
                "url='" + url + '\'' +
                ", origin='" + origin + '\'' +
                ", headers=" + headers +
                ", args=" + args +
                ", form=" + form +
                ", json=" + json +
                '}';
    }
}
