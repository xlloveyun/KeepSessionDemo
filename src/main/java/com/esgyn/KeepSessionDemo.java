package com.esgyn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class KeepSessionDemo {

    CookieStore cookieStore = null;
    ObjectMapper mapper;

    private  String HOST = "";

//    private  String DBMGR_BASE_URL = "https://" + HOST + ":4206/resources/";
    private  String DBMGR_BASE_URL = "";

    private  String USER_NAME = "";

    private  String USER_PWD = "";

    private  String TENANT_NAME = "tenant1";

    public KeepSessionDemo() {
        mapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("host"));
        String sQuery = "";
        String sControlStmts = "";
        System.out.println("请输入sql:");
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            sQuery = scanner.nextLine();
        }
        System.out.println("请输入controlQuerystmt:");
//        Scanner scan = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            sControlStmts = scanner.nextLine();
        }
        scanner.close();
        KeepSessionDemo demo = new KeepSessionDemo();
        demo.HOST = System.getProperty("host");
        demo.USER_NAME = System.getProperty("username");
        demo.USER_PWD = System.getProperty("userPwd");
        demo.DBMGR_BASE_URL ="https://" + demo.HOST + ":4206/resources/";
        demo.login(demo.USER_NAME, demo.USER_PWD, demo.TENANT_NAME);
        demo.executeQuery(sQuery, sControlStmts);

    }

    private void executeQuery(String sQuery, String sControlStmts) {
        long timestamp = new Date().getTime();
        Map<String, Object> param = new HashMap<>();
        param.put("timeStamp", timestamp);
        param.put("sQuery", sQuery);
        param.put("sControlStmts", sControlStmts);
        String str = httpsPost("queries/execute", param);
//        try {
//            Thread.sleep(10000000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.out.println(str);

    }

    private void login(String userName, String password, String tenantName) {
        Map<String, String> param = new HashMap<>();
        param.put("username", userName);
        param.put("password", password);
        param.put("tenantName", tenantName);
        String str = httpsPost("server/login", param);
        System.out.println(str);
    }


    public CloseableHttpClient getIgnoreSslCertificateHttpClient() throws HttpException {

        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(final X509Certificate[] arg0, final String arg1)
                    throws CertificateException {

                    return true;
                }
            }).build();
        } catch (Exception e) {
            throw new HttpException("can not create http client.", e);
        }
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
            new NoopHostnameVerifier());
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
            .<ConnectionSocketFactory> create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslSocketFactory).build();
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(
            socketFactoryRegistry);
        return HttpClientBuilder.create().setDefaultCookieStore(cookieStore).setSslcontext(sslContext).setConnectionManager(connMgr).build();
    }


    public void setCookieStore(HttpResponse httpResponse, String str) throws Exception{
        cookieStore = new BasicCookieStore();
        String setCookie = httpResponse.getFirstHeader("Set-Cookie").getValue();
        String JSESSIONID = setCookie.substring("JSESSIONID=".length(),setCookie.indexOf(";"));
        BasicClientCookie cookie = new BasicClientCookie("JSESSIONID",JSESSIONID);
        cookie.setVersion(0);
        cookie.setDomain(HOST);
        BasicClientCookie cookie0 = new BasicClientCookie("locale","en-US");
        cookie0.setVersion(0);
        cookie0.setDomain(HOST);
        BasicClientCookie cookie1 = new BasicClientCookie("token",
            String.valueOf(mapper.readValue(str, Map.class).get("key")));
        cookie1.setVersion(0);
        cookie1.setDomain(HOST);
        cookieStore.addCookie(cookie);
        cookieStore.addCookie(cookie0);
        cookieStore.addCookie(cookie1);
    }

    private String httpsGet(String url) {
        HttpGet httpGet = new HttpGet(DBMGR_BASE_URL + url);
        CloseableHttpClient httpsClient = null;
        CloseableHttpResponse response = null;
        String str = null;
        try {
            httpsClient = getIgnoreSslCertificateHttpClient();
            httpGet.addHeader("Content-Type", "application/json");
            response = httpsClient.execute(httpGet);
            str = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e2) {
            }
            try {
                if (httpsClient != null) {
                    httpsClient.close();
                }
            } catch (Exception e2) {
            }
        }
        return str;
    }

    public String httpsPost(String url, Map param) {
        String str = null;
        HttpPost httpPost = new HttpPost(DBMGR_BASE_URL + url);
        CloseableHttpClient httpsClient = null;
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(param), ContentType.APPLICATION_JSON));
            httpPost.addHeader("Content-Type", "application/json");
            httpsClient = getIgnoreSslCertificateHttpClient();
            response = httpsClient.execute(httpPost);
            str = EntityUtils.toString(response.getEntity());
            if(url.contains("login")) {
                setCookieStore(response, str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e2) {
            }
            try {
                if (httpsClient != null) {
                    httpsClient.close();
                }
            } catch (Exception e2) {
            }
        }
        return str;
    }


    private String httpsDelete(String url) {
        HttpDelete httpDelete = new HttpDelete(DBMGR_BASE_URL + url);
        CloseableHttpClient httpsClient = null;
        CloseableHttpResponse response = null;
        String str = null;
        try {
            httpsClient = getIgnoreSslCertificateHttpClient();
            httpDelete.addHeader("Content-Type", "application/json");
            response = httpsClient.execute(httpDelete);
            str = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e2) {
            }
            try {
                if (httpsClient != null) {
                    httpsClient.close();
                }
            } catch (Exception e2) {
            }
        }
        return str;
    }
}

