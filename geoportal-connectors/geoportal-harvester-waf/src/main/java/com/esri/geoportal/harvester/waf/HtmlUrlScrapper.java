/*
 * Copyright 2016 Esri, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.geoportal.harvester.waf;

import com.esri.geoportal.commons.http.BotsHttpClient;
import static com.esri.geoportal.commons.utils.Constants.DEFAULT_REQUEST_CONFIG;
import static com.esri.geoportal.commons.utils.HttpClientContextBuilder.createHttpClientContext;
import com.esri.geoportal.commons.utils.SimpleCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;

/**
 * HTML URL scrapper.
 */
/*package*/ class HtmlUrlScrapper {
  private final BotsHttpClient httpClient;
  private final SimpleCredentials creds;

  /**
   * Creates instance of the scrapper
   * @param httpClient HTTP client
   * @param creds credentials
   */
  public HtmlUrlScrapper(BotsHttpClient httpClient, SimpleCredentials creds) {
    this.httpClient = httpClient;
    this.creds = creds;
  }

  /**
   * Scrap HTML page for URL's
   * @param root root of the page
   * @return list of found URL's
   * @throws IOException if error reading data
   * @throws URISyntaxException if invalid URL
   */
  public List<URL> scrap(URL root) throws IOException, URISyntaxException {
    ContentAnalyzer analyzer = new ContentAnalyzer(root);
    HttpGet method = new HttpGet(root.toExternalForm());
    method.setConfig(DEFAULT_REQUEST_CONFIG);
    HttpClientContext context = creds!=null && !creds.isEmpty()? createHttpClientContext(root, creds): null;
    HttpResponse response = httpClient.execute(method, context);
    try (InputStream input = response.getEntity().getContent();) {
      String content = IOUtils.toString(input, "UTF-8");
      return analyzer.analyze(content);
    }
  }
}
