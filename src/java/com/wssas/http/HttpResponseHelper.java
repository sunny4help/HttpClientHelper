package com.wssas.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * TODO ������
 * <p/>
 *
 * @author Sunny
 * @version 1.0 2016/9/21
 * @Description: TODO ��ϸ����
 */
public class HttpResponseHelper
{
    // read once max_bytes
    private final static int max_bytes = 40960;

    private HttpResponse resp;

    private String targetUrl;

    private String htmlContent;
    
    private String requestUrl;
    
    private String requestEncode="UTF-8";
    
    private String responseEncode="UTF-8";

    public HttpResponseHelper(HttpResponse response)
    {
       this(response,null,null);
    }

    public HttpResponseHelper(HttpResponse response,String reqEncode,String respEncode)
    {
        this.resp=response;

        if(reqEncode!=null)
        {
            requestEncode = reqEncode;
        }

        if(respEncode!=null)
        {
            responseEncode = respEncode;
        }

        if(this.resp!=null)
        {
            InputStream in=null;
            try
            {
                in=resp.getEntity().getContent();
                htmlContent = readInputStream(in,responseEncode);
            }
            catch (Exception e)
            {

            }
            finally
            {
                if(in!=null)
                {
                    try
                    {
                        in.close();
                        if(resp instanceof CloseableHttpResponse)
                        {
                            CloseableHttpResponse t=(CloseableHttpResponse)resp;
                            t.close();
                        }
                    }catch (Exception e)
                    {

                    }
                }
            }

        }
    }

    public HttpResponse getResp()
    {
        return resp;
    }

    public void setResp(HttpResponse resp)
    {
        this.resp = resp;
    }

    public String getTargetUrl()
    {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl)
    {
        this.targetUrl = targetUrl;
    }

    public String getHtmlContent()
    {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent)
    {
        this.htmlContent = htmlContent;
    }

    public String getRequestUrl()
    {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl)
    {
        this.requestUrl = requestUrl;
    }

    public String getRequestEncode()
    {
        return requestEncode;
    }

    public void setRequestEncode(String requestEncode)
    {
        this.requestEncode = requestEncode;
    }

    public String getResponseEncode()
    {
        return responseEncode;
    }

    public void setResponseEncode(String responseEncode)
    {
        this.responseEncode = responseEncode;
    }

    private String readInputStream(InputStream is, String encoding) throws IOException
    {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] b = new byte[max_bytes];
        StringBuilder builder = new StringBuilder();
        int bytesRead = 0;
        while (true)
        {
            bytesRead = is.read(b, 0, max_bytes);
            if (bytesRead == -1)
            {
                builder.append(new String(swapStream.toByteArray(), encoding));
                return builder.toString();
            }
            swapStream.write(b, 0, bytesRead);
        }
    }
}
