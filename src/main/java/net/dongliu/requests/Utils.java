package net.dongliu.requests;

import net.dongliu.requests.encode.URIBuilder;
import net.dongliu.requests.struct.Parameter;
import net.dongliu.requests.struct.Parameters;
import net.dongliu.requests.struct.Proxy;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Util methods
 *
 * @author Dong Liu dongliu@wandoujia.com
 */
class Utils {
    static Registry<ConnectionSocketFactory> getConnectionSocketFactoryRegistry(
            Proxy proxy, boolean verify) {
        SSLContext sslContext;

        // trust all http certificate
        if (!verify) {
            try {
                sslContext = SSLContexts.custom().useTLS().build();
                sslContext.init(new KeyManager[0], new TrustManager[]{new AllTrustManager()},
                        new SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        } else {
            sslContext = SSLContexts.createSystemDefault();
        }

        SSLConnectionSocketFactory sslsf = new CustomSSLConnectionSocketFactory(sslContext,
                proxy, verify);
        PlainConnectionSocketFactory psf = new CustomConnectionSocketFactory(proxy);
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", psf)
                .register("https", sslsf)
                .build();
    }

    // build full url with parameters
    static URI fullUrl(URI url, Charset charset, Parameters parameters) {
        try {
            if (parameters == null || parameters.isEmpty()) {
                return url;
            }
            URIBuilder urlBuilder = new URIBuilder(url).setCharset(charset);
            for (Parameter param : parameters) {
                urlBuilder.addParameter(param.getName(), param.getValue());
            }
            return urlBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * make sure only one http body was set
     */
    static void checkHttpBody(Request request) {
        int bodyCount = 0;
        if (request.getStrBody() != null) bodyCount++;
        if (request.getBody() != null) bodyCount++;
        if (request.getIn() != null) bodyCount++;
        if (request.getParamBody() != null) bodyCount++;
        if (request.getMultiParts() != null) bodyCount++;
        if (bodyCount > 1) {
            //can not set both
            throw new RuntimeException("More than one http request body have been set");
        }
    }
}
