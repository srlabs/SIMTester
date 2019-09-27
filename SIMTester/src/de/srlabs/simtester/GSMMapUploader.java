package de.srlabs.simtester;

import de.srlabs.simlib.LoggingUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;

public class GSMMapUploader {

    public static boolean uploadFile(String filename) {

        if (null == filename || "".equals(filename)) {
            throw new IllegalArgumentException(LoggingUtils.formatDebugMessage("filename cannot be empty nor null."));
        }

        File file = new File(filename);

        if (!file.exists()) {
            throw new IllegalArgumentException(LoggingUtils.formatDebugMessage("file " + filename + " does not exists!"));
        }

        System.out.println("Trying to upload data to gsmmap.org ..");
        
        try {
            System.setProperty("jsse.enableSNIExtension", "false");
            
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

            InputStream keystoreStream = SIMTester.class.getResourceAsStream("gsmmap.srlabs.de_truststore");

            keystore.load(keystoreStream, "123456".toCharArray());

            trustManagerFactory.init(keystore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagers, null);

            HttpClientBuilder client_builder = HttpClientBuilder.create();
            client_builder.setSslcontext(sc);
            HttpClient client = client_builder.build();

            MultipartEntityBuilder request_builder = MultipartEntityBuilder.create();
            request_builder.addPart("bursts", new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, filename));

            String filename_without_ext = file.getName();
            int index = file.getName().lastIndexOf('.');
            if (index > 0) {
                filename_without_ext = file.getName().substring(0, index);
            }
            
            request_builder.addTextBody("ident", filename_without_ext);
            request_builder.addTextBody("submit", "batch");
            request_builder.addTextBody("opaque", "1");

            HttpPost post = new HttpPost("https://gsmmap.srlabs.de:4433/cgi-bin/dat_upload.cgi");
            post.setEntity(request_builder.build());

            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() != 200) {
                System.err.println("Upload has failed, status is: " + response.getStatusLine());
                return false;
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String inputLine;
                String buffer = "";
                while ((inputLine = rd.readLine()) != null) {
                    buffer += inputLine;
                }
                return "OK".equals(buffer); // there's "OK" in the body of response if upload went fine.
            }
        } catch (IOException | IllegalStateException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            System.err.println("There was a problem during upload: ");
            e.printStackTrace(System.err);
            return false;
        }
    }

}
