package com.wssas.http;

import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * For resolve some Function
 * HttpClient is NOT a browser. It is a client side HTTP transport library. HttpClient's purpose is to transmit and receive HTTP messages.
 * HttpClient will not attempt to process content, execute javascript embedded in HTML pages, try to guess content type, if not explicitly set,
 * or reformat request / rewrite location URIs, or other functionality unrelated to the HTTP transport.
 * <p/>
 *
 * @author Sunny
 * @version 1.0 2016/9/21
 * @Description:
 * HttpClientHelper
 * Wrapper for HttpClient & HttpResponse etc.
 */
public class HttpClientHelper
{
    //logger
    private static Logger logger = Logger.getLogger(HttpClientHelper.class.getName());

    //http error, IO error retry times
    private static final int retry_times = 3;

    // connectionTimeout（ms）
    private static int connectionTimeout = 30000;

    // read Data soTimeout（ms）
    private static int soTimeout = 3000;

    // maxConnectionsPerHost
    private static int maxConnectionsPerHost = 5;

    // maxTotalConnections
    private static int maxTotalConnections = 500;

    //httpClient
    private CloseableHttpClient httpClient = null;

    //responseHelper
    private HttpResponseHelper responseHelper = null;

    //httpClientContext
    private HttpClientContext httpClientContext = HttpClientContext.create();

    {
        httpClientContext.setCookieStore(new BasicCookieStore());
    }

    //globalConfig
    private static RequestConfig globalConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.DEFAULT).setSocketTimeout(soTimeout).setConnectTimeout(connectionTimeout+soTimeout).setConnectionRequestTimeout(connectionTimeout)
            .build();

    //localConfig
    private static RequestConfig localConfig = RequestConfig.copy(globalConfig)
            .setCookieSpec(CookieSpecs.STANDARD_STRICT)
            .build();

    //sslContext
    private static SSLContext sslContext = SSLContexts.createSystemDefault();

    //sslsf
    private static SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
            sslContext,
            NoopHostnameVerifier.INSTANCE);

    private static ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.INSTANCE;

    private static Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", plainsf)
            .register("https", sslsf)
            .build();

    private static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);

    static {
        // 将最大连接数增加到200
        cm.setMaxTotal(maxTotalConnections);
        // 将每个路由基础的连接增加到20
        cm.setDefaultMaxPerRoute(maxConnectionsPerHost);
    }

    public HttpClientHelper()
    {
        httpClient = HttpClients.custom()
                .addInterceptorLast(myHttpRequestInterceptor).setRetryHandler(myRetryHandler)
                .setRedirectStrategy(redirectStrategy).setConnectionManager(
                        cm).setKeepAliveStrategy(myKeepAliveStrategy).setDefaultRequestConfig(globalConfig)
                .build();
    }

    public void finalize() throws Throwable
    {
        if (httpClient != null)
        {
            try
            {
                httpClient.close();
                httpClient = null;
            }
            catch (Exception e)
            {

            }
        }
    }

    public HttpResponseHelper doGet(String url)
    {
        return doGet(url,null,null,null);
    }

    public HttpResponseHelper doGet(String url,String reqEncode,String respEncode,Map headerMap)
    {
        return doGet(url,reqEncode,respEncode,headerMap,null,0);
    }

    public HttpResponseHelper doGet(String url,String reqEncode,String respEncode,Map headerMap,String proxyHost,int port)
    {
        CloseableHttpResponse response = null;

        HttpGet httpget = null;
        try
        {
            httpget = getHttpGet(url,headerMap,proxyHost,port);
            response = httpClient.execute(httpget, httpClientContext);
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "get url:" + url + " error:" + e.getMessage());
        }

        responseHelper = new HttpResponseHelper(response,reqEncode,respEncode);

        responseHelper.setRequestUrl(url);

        try
        {
            HttpHost target = httpClientContext.getTargetHost();
            List<URI> redirectLocations = httpClientContext.getRedirectLocations();
            URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
            responseHelper.setTargetUrl(location.toASCIIString());
            // System.out.println("Final HTTP location: " + location.toASCIIString());
        }
        catch (Exception e)
        {
            responseHelper.setTargetUrl(url);
        }

        /*
        <META http-equiv="refresh" content="0;URL='http://www.manmankan.com/dy2013/dianying/'">
         */

        return responseHelper;
    }

    private static List<Header> getHeaders(Map<String, String> headersMap)
    {
        List<Header> headers = new ArrayList<Header>();

        Map<String, String> headersMapTmp = new HashMap<String, String>();
        headersMapTmp.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headersMapTmp.put("Accept-Language", "zh-CN,zh;q=0.8");
        headersMapTmp.put("User-Agent",
                "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36");

        if (null != headersMap && false == headersMap.isEmpty())
        {
            headersMapTmp.putAll(headersMap);
        }

        Set<Map.Entry<String, String>> entrySet = headersMapTmp.entrySet();
        for (Map.Entry<String, String> entry : entrySet)
        {
            headers.add(new BasicHeader(entry.getKey(), entry.getValue()));
        }

        return headers;
    }

    public HttpGet getHttpGet(String url,Map headerMap)
    {
        return getHttpGet(url, headerMap, null, 0);
    }

    public HttpGet getHttpGet(String url,Map headerMap, String proxyHost, int port)
    {
        HttpGet httpget = new HttpGet(url);

        if(!("127.0.0.1".equals(proxyHost)&&port==80))
        {
            RequestConfig.Builder requestConfigBuilder = RequestConfig.copy(globalConfig);
            requestConfigBuilder.setProxy(new HttpHost(proxyHost, port));
            RequestConfig requestConfig = requestConfigBuilder.setExpectContinueEnabled(false).build();

            httpget.setConfig(requestConfig);
        }
        else
        {
            httpget.setConfig(localConfig);
        }

        List<Header> headers = getHeaders(headerMap);

        headers.add(new BasicHeader("Accept-Charset", "utf-8"));

        Header[] headersArray = new Header[headers.size()];
        headersArray = headers.toArray(headersArray);
        httpget.setHeaders(headersArray);

        return httpget;
    }

    public void setCookie(final String cookieName, final String cookieValue, final String domain)
    {
        CookieStore cookieStore = httpClientContext.getCookieStore();
        Cookie c = new Cookie()
        {
            @Override
            public String getName()
            {
                return cookieName;
            }

            @Override
            public String getValue()
            {
                return cookieValue;
            }

            @Override
            public String getComment()
            {
                return null;
            }

            @Override
            public String getCommentURL()
            {
                return null;
            }

            @Override
            public Date getExpiryDate()
            {
                return new Date(new Date().getTime() + 20 * 60 * 1000);
            }

            @Override
            public boolean isPersistent()
            {
                return false;
            }

            @Override
            public String getDomain()
            {
                return domain;
            }

            @Override
            public String getPath()
            {
                return "/";
            }

            @Override
            public int[] getPorts()
            {
                return new int[0];
            }

            @Override
            public boolean isSecure()
            {
                return false;
            }

            @Override
            public int getVersion()
            {
                return 0;
            }

            @Override
            public boolean isExpired(Date date)
            {
                return false;
            }
        };

        cookieStore.addCookie(c);
    }

    public String getCookie(String cookieName)
    {
        CookieStore cookieStore = httpClientContext.getCookieStore();

        List<Cookie> list = cookieStore.getCookies();

        if (list != null && list.size() > 0)
        {
            for (Cookie c : list)
            {
                System.out.println(c + ";c.getName()=" + c.getName() + ";c.getValue()=" + c.getValue());

                if (cookieName.equalsIgnoreCase(c.getName()))
                {
                    return c.getValue();
                }
            }
        }

        return "";
    }

    public static SSLContext custom(String keyStorePath, String keyStorepass)
    {
        SSLContext sc = null;
        FileInputStream instream = null;
        KeyStore trustStore = null;
        try
        {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            instream = new FileInputStream(new File(keyStorePath));
            trustStore.load(instream, keyStorepass.toCharArray());
            sc = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                instream.close();
            }
            catch (IOException e)
            {
            }
        }
        return sc;
    }

    public void setSSLContext(String keyStorePath, String keyStorepass)
    {
        setSSLContext(custom(keyStorePath, keyStorepass));
    }

    public void setSSLContext(SSLContext ssl)
    {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(ssl))
                .build();

        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        // 将最大连接数增加到200
        cm.setMaxTotal(maxTotalConnections);
        // 将每个路由基础的连接增加到20
        cm.setDefaultMaxPerRoute(maxConnectionsPerHost);

        httpClient = HttpClients.custom()
                .addInterceptorLast(myHttpRequestInterceptor).setRetryHandler(myRetryHandler)
                .setRedirectStrategy(redirectStrategy).setConnectionManager(
                        cm).setKeepAliveStrategy(myKeepAliveStrategy).setDefaultRequestConfig(globalConfig)
                .build();
    }

    private static ConnectionKeepAliveStrategy myKeepAliveStrategy = new ConnectionKeepAliveStrategy()
    {
        public long getKeepAliveDuration(HttpResponse response, HttpContext context)
        {
            // Honor 'keep-alive' header
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext())
            {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout"))
                {
                    try
                    {
                        return Long.parseLong(value) * 1000;
                    }
                    catch (NumberFormatException ignore)
                    {
                    }
                }
            }
            HttpHost target = (HttpHost) context.getAttribute(
                    HttpClientContext.HTTP_TARGET_HOST);
            if ("www.naughty-server.com".equalsIgnoreCase(target.getHostName()))
            {
                // Keep alive for 5 seconds only
                return 5 * 1000;
            }
            else
            {
                // otherwise keep alive for 30 seconds
                return 8 * 1000;
            }
        }
    };

    private static HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler()
    {
        public boolean retryRequest(
                IOException exception,
                int executionCount,
                HttpContext context)
        {
            if (executionCount >= retry_times)
            {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException)
            {
                // Timeout
                return false;
            }
            if (exception instanceof UnknownHostException)
            {
                // Unknown host
                return false;
            }
            if (exception instanceof ConnectTimeoutException)
            {
                // Connection refused
                return false;
            }
            if (exception instanceof SSLException)
            {
                // SSL handshake exception
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent)
            {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        }
    };

    private HttpRequestInterceptor myHttpRequestInterceptor = new HttpRequestInterceptor()
    {
        public void process(
                final HttpRequest request,
                final HttpContext context) throws HttpException, IOException
        {
        }
    };

    private static LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();

    /**
     * 将Header[]转换为String
     *
     * @param headers Http头信息
     * @return String形式的Http头信息
     */
    private String getHeadersStr(Header[] headers)
    {
        StringBuilder builder = new StringBuilder();
        for (Header header : headers)
        {
            builder.append(header.getName()).append(": ").append(header.getValue()).append("\n");
        }
        return builder.toString();
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSoTimeout()
    {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout)
    {
        this.soTimeout = soTimeout;
    }

    public int getMaxConnectionsPerHost()
    {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost)
    {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getMaxTotalConnections()
    {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections)
    {
        this.maxTotalConnections = maxTotalConnections;
    }

    public HttpResponseHelper getResponseHelper()
    {
        return responseHelper;
    }

    public void setResponseHelper(HttpResponseHelper responseHelper)
    {
        this.responseHelper = responseHelper;
    }

    public CloseableHttpClient getHttpClient()
    {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }
}
