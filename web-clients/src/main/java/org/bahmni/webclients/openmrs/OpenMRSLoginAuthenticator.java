package org.bahmni.webclients.openmrs;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bahmni.webclients.*;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.URI;

public class OpenMRSLoginAuthenticator implements Authenticator {
    private static Logger logger = Logger.getLogger(OpenMRSLoginAuthenticator.class);
    private final String SESSION_ID_KEY = "JSESSIONID";


    private ConnectionDetails authenticationDetails;
    private HttpRequestDetails requestDetails;

    public OpenMRSLoginAuthenticator(ConnectionDetails authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
    }

    @Override
    public HttpRequestDetails getRequestDetails(URI uri) {
        if (requestDetails == null) {
            return refreshRequestDetails(uri);
        }
        return requestDetails;
    }

    @Override
    public HttpRequestDetails refreshRequestDetails(URI uri) {
        String responseText = null;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, authenticationDetails.getReadTimeout());
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, authenticationDetails.getConnectionTimeout());

            HttpGet httpGet = new HttpGet(authenticationDetails.getAuthUrl());

            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authenticationDetails.getUserId(), authenticationDetails.getPassword());
            BasicScheme scheme = new BasicScheme();
            Header authorizationHeader = scheme.authenticate(credentials, httpGet);
            httpGet.setHeader(authorizationHeader);

            logger.info(String.format("Executing request: %s", httpGet.getRequestLine()));

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream content = entity.getContent();
                responseText = IOUtils.toString(content);
            }
            logger.info(String.format("Authentication response: %s", responseText));
            EntityUtils.consume(entity);
            OpenMRSAuthenticationResponse openMRSResponse = new ObjectMapper().readValue(responseText, OpenMRSAuthenticationResponse.class);
            confirmAuthenticated(openMRSResponse);

            ClientCookies clientCookies = new ClientCookies();
            clientCookies.put(SESSION_ID_KEY, openMRSResponse.getSessionId());

            requestDetails = new HttpRequestDetails(uri, clientCookies, new HttpHeaders());
            HttpRequestDetails httpRequestDetails = new HttpRequestDetails(uri, clientCookies, new HttpHeaders());
            return httpRequestDetails;

        } catch (Exception e) {
            throw new WebClientsException(e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private void confirmAuthenticated(OpenMRSAuthenticationResponse openMRSResponse) {
        if (!openMRSResponse.isAuthenticated()) {
            logger.error("Could not authenticate with OpenMRS. ");
            throw new WebClientsException("Could not authenticate with OpenMRS");
        }
    }
}