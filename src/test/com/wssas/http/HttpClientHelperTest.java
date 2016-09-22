package com.wssas.http;

import org.junit.Test;

/**
 * TODO
 * <p/>
 *
 * @author Sunny
 * @version 1.0 2016/9/21
 * @Description: TODO
 */
public class HttpClientHelperTest
{
    @Test
    public void testGet()
    {
        String url="http://www.baidu.com";

        HttpClientHelper httpClient=new HttpClientHelper();
        httpClient.doGet(url);

        System.out.println(httpClient.getResponseHelper().getHtmlContent());
    }


    @Test
    public void testTargetUrl()
    {
        String url="http://www.baidu.com/link?url=1JaoXP2tuKkGaOATwz3kDa99hgVhoEXI3gf8FX_GZn1igKixHO4tY2_N4y52sKQxwhnhC_3lbY-3cRCjkVYEh-RHoMkiWf89Or3RcYv63o_";
        HttpClientHelper httpClient=new HttpClientHelper();
        httpClient.doGet(url,null,"GBK",null);

        System.out.println(httpClient.getResponseHelper().getHtmlContent());

        System.out.println(httpClient.getResponseHelper().getTargetUrl());
        System.out.println(httpClient.getResponseHelper().getRequestUrl());

    }

    @Test
    public void testHttps()
    {
        String url= "https://www.medpex.de/testbericht/schnupfen-nebenhoehlen/nasenspray-ratiopharm-erwachsene-p999848/#reviews";
        HttpClientHelper httpClient=new HttpClientHelper();
        httpClient.doGet(url,null,"ISO-8859-1",null);

        System.out.println(httpClient.getResponseHelper().getHtmlContent());

        System.out.println(httpClient.getResponseHelper().getTargetUrl());
        System.out.println(httpClient.getResponseHelper().getRequestUrl());

    }

    @Test
    public void testProxy()
    {
        String url="http://www.baidu.com/link?url=1JaoXP2tuKkGaOATwz3kDa99hgVhoEXI3gf8FX_GZn1igKixHO4tY2_N4y52sKQxwhnhC_3lbY-3cRCjkVYEh-RHoMkiWf89Or3RcYv63o_";
        HttpClientHelper httpClient=new HttpClientHelper();
        httpClient.setCookie("aaa","bbbb","/");
        httpClient.doGet(url,null,"GBK",null,"127.0.0.1",8888);

        System.out.println(httpClient.getResponseHelper().getHtmlContent());

        System.out.println(httpClient.getResponseHelper().getTargetUrl());
        System.out.println(httpClient.getResponseHelper().getRequestUrl());
        System.out.println(httpClient.getCookie("X-Ser"));

        //BDSVRTM=0; path=/
        System.out.println(httpClient.getCookie("BDSVRTM"));
        System.out.println(httpClient.getCookie("path"));
        System.out.println(httpClient.getCookie("aaa"));

        //X-Ser	BC5_dx-sichuan-deyang-1-cache-1
    }



}
