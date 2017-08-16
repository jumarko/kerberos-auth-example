package net.curiousprogrammer.auth.kerberos.example;

import com.github.markusbernhardt.proxy.ProxySearch;
import com.github.markusbernhardt.proxy.selector.fixed.FixedProxySelector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.ProxySelector;
import java.security.Principal;
import java.security.Security;

/**
 * See "HttpClient set credentials for Kerberos authentication":
 * https://stackoverflow.com/questions/21629132/httpclient-set-credentials-for-kerberos-authentication
 *
 * The idea is to show how to authenticate against Kerberos-enabled proxy server with apache HTTP client.
 * The tricky part is to make sure that provided credentials can be used to obtain Kerberot TGT ticket
 * in case no ticket is already available in ticket OS cache.
 * We want to avoid entering password manually via stdin and instead want to use pre-configured password
 *
 * Notes:
 * - You need to have valid /etc/krb5.conf in place
 * - Make sure to change principal in login.conf
 * - Set proper username/password in KerberosCallbackHandler
 *
 * @see KerberosCallBackHandler
 */
public class KerberosAuthExample {

    private static final String PROXY_HOST = "CHANGEME";
    private static final int PROXY_PORT = 3128;

    public static void callServer(String url) throws IOException {
         HttpClient httpclient = getHttpClient();

        try {

            HttpUriRequest request = new HttpGet(url);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            System.out.println("----------------------------------------");

            System.out.println("STATUS >> " + response.getStatusLine());

            if (entity != null) {
                System.out.println("RESULT >> " + EntityUtils.toString(entity));
            }

            System.out.println("----------------------------------------");

            EntityUtils.consume(entity);

        } finally {
            httpclient.getConnectionManager().shutdown();
        }

    }

    private static HttpClient getHttpClient() {

        Credentials use_jaas_creds = new Credentials() {
            public String getPassword() {
                return null;
            }

            public Principal getUserPrincipal() {
                return null;
            }
        };

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(null, -1, null), use_jaas_creds);
        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create().register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true)).build();
        CloseableHttpClient httpclient = HttpClients.custom()
                // set our proxy - httpclient doesn't use ProxySelector
                .setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost(PROXY_HOST, PROXY_PORT)))
                .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                .setDefaultCredentialsProvider(credsProvider).build();

        return httpclient;
    }


    public static void autoconfigureProxy() {
        final ProxySearch proxySearch = new ProxySearch();
        proxySearch.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
        proxySearch.addStrategy(ProxySearch.Strategy.JAVA);
        proxySearch.addStrategy(ProxySearch.Strategy.ENV_VAR);
//        ProxySelector.setDefault(proxySearch.getProxySelector());
        ProxySelector.setDefault(new FixedProxySelector(PROXY_HOST, PROXY_PORT));
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.security.krb5.conf", "/etc/krb5.conf");
        System.setProperty("java.security.auth.login.config", "login.conf");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("sun.security.jgss.debug", "true");

        // Setting default callback handler to avoid prompting for password on command line
        // check https://github.com/frohoff/jdk8u-dev-jdk/blob/master/src/share/classes/sun/security/jgss/GSSUtil.java#L241
        Security.setProperty("auth.login.defaultCallbackHandler", "net.curiousprogrammer.auth.kerberos.example.KerberosCallBackHandler");

        autoconfigureProxy();

        callServer("http://example.com");
        callServer("https://example.com");
    }
}
