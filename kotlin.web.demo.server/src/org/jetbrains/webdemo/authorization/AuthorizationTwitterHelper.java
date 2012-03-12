/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.webdemo.authorization;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.webdemo.ErrorWriter;
import org.jetbrains.webdemo.ResponseUtils;
import org.jetbrains.webdemo.server.ApplicationSettings;
import org.jetbrains.webdemo.session.SessionInfo;
import org.jetbrains.webdemo.session.UserInfo;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by IntelliJ IDEA.
 * User: Natalia.Ukhorskaya
 * Date: 12/30/11
 * Time: 3:54 PM
 */

public class AuthorizationTwitterHelper extends AuthorizationHelper {
    private final String TYPE = "twitter";

    private static OAuthService twitterService;
    private static Token requestToken;
    private static final String PROTECTED_RESOURCE_URL = "http://api.twitter.com/1/account/verify_credentials.xml";

    public String authorize() {
        try {
            twitterService = new ServiceBuilder()
                    .provider(TwitterApi.class)
                    .apiKey("g0dAeSZpnxTHxRKV2UZFGg")
                    .apiSecret("NSfUf8o3BhyT96U6hcCarWIUEwz6Le4FY6Em7WBPtuw")
                    .callback("http://" + ApplicationSettings.AUTH_REDIRECT + ResponseUtils.generateRequestString("authorization", "twitter"))
                    .build();
            requestToken = twitterService.getRequestToken();
            return twitterService.getAuthorizationUrl(requestToken);
        } catch (Throwable e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, SessionInfo.TypeOfRequest.AUTHORIZATION.name(), "twitter");
        }
        return "";
    }

    @Override
    @Nullable
    public UserInfo verify(String url) {
        UserInfo userInfo = null;
        try {
            String authUrl = ResponseUtils.substringAfter(url, "oauth_verifier=");
            Verifier verifier = new Verifier(authUrl);
            Token accessToken = twitterService.getAccessToken(requestToken, verifier);
            OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
            twitterService.signRequest(accessToken, request); // the access token from step 4
            Response response = request.send();
            //System.out.println(response.getBody());

            Document document = ResponseUtils.getXmlDocument(response.getStream());
            if (document == null) {
                return userInfo;
            }

            userInfo = new UserInfo();
            String name = null;
            String id = null;
            NodeList nodeList = document.getElementsByTagName("user");
            for (int i = 0; i < nodeList.getLength(); i++) {
                NodeList children = nodeList.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node node = children.item(j);
                    if (node.getNodeName().equals("name")) {
                        name = node.getTextContent();
                    } else if (node.getNodeName().equals("id")) {
                        id = node.getTextContent();
                    }
                }
            }
            if (name != null && id != null) {
                userInfo.login(name, id, TYPE);
            }


        } catch (Throwable e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, SessionInfo.TypeOfRequest.AUTHORIZATION.name(), "twitter: " + url);
        }
        return userInfo;
    }
}
