/*
 * Copyright 2016 Esri, Inc.
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
package com.esri.geoportal.harvester.csw;

import com.esri.geoportal.commons.csw.client.IProfile;
import com.esri.geoportal.commons.csw.client.IProfiles;
import com.esri.geoportal.commons.csw.client.ObjectFactory;
import com.esri.geoportal.harvester.api.BrokerDefinition;
import com.esri.geoportal.harvester.api.support.BotsBrokerDefinitionAdaptor;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;

/**
 * CSW definition.
 */
public class CswBrokerDefinitionAdaptor extends BotsBrokerDefinitionAdaptor {

  public static final String P_HOST_URL = "csw.host.url";
  public static final String P_PROFILE_ID = "csw.profile.id";

  private URL hostUrl;
  private IProfile profile;

  /**
   * Creates instance of the adaptor.
   *
   * @param def broker definition
   */
  public CswBrokerDefinitionAdaptor(BrokerDefinition def) {
    super(def);
    if (StringUtils.trimToEmpty(def.getType()).isEmpty()) {
      def.setType(CswConnector.TYPE);
    } else if (!CswConnector.TYPE.equals(def.getType())) {
      throw new IllegalArgumentException("Broker definition doesn't match");
    } else {
      try {
        hostUrl = new URL(get(P_HOST_URL));
      } catch (MalformedURLException ex) {
        throw new IllegalArgumentException(String.format("Invalid %s: %s", P_HOST_URL, get(P_HOST_URL)), ex);
      }
      ObjectFactory of = new ObjectFactory();
      IProfiles profiles = of.newProfiles();
      profile = profiles.getProfileById(this.get(P_PROFILE_ID));
      if (profile == null) {
        throw new IllegalArgumentException(String.format("Invalid %s: %s", P_PROFILE_ID, get(P_PROFILE_ID)));
      }
    }
  }

  /**
   * Gets host url.
   *
   * @return host url
   */
  public URL getHostUrl() {
    return hostUrl;
  }

  /**
   * Sets host url.
   *
   * @param url host url
   */
  public void setHostUrl(URL url) {
    this.hostUrl = url;
    set(P_HOST_URL, url.toExternalForm());
  }

  /**
   * Gets profile.
   *
   * @return profile
   */
  public IProfile getProfile() {
    return profile;
  }

  /**
   * Sets profile.
   *
   * @param profile profile
   */
  public void setProfile(IProfile profile) {
    this.profile = profile;
    set(P_PROFILE_ID, profile.getId());
  }

}