/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke, Benjamin Weimer, Jason Kowaleski</a>
 * @version $Revision: 1 $
 */
public class ClaimToGroupMapper extends AbstractClaimMapper {
    public static final String[] COMPATIBLE_PROVIDERS = { KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(
            Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM);
        property1.setLabel("Claim");
        property1.setHelpText(
                "Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM_VALUE);
        property1.setLabel("Claim Value");
        property1.setHelpText(
                "Value the claim must have.  If the claim is an array, then the value must be contained in the array.");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        property = new ProviderConfigProperty();
        property.setName(ConfigConstants.GROUP);
        property.setLabel("Group Path");
        property.setHelpText(
                "Group to join if claim is present. Use the full group path. Ex. '/ParentGroup/ChildGroup' or '/TopLevelGroup'.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-group-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Group Importer";
    }

    @Override
    public String getDisplayType() {
        return "Claim to Group";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String groupPath = mapperModel.getConfig().get(ConfigConstants.GROUP);
        if (hasClaimValue(mapperModel, context)) {
            GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
            if (group == null) {
                throw new IdentityBrokerException("Unable to find group: " + groupPath);
            }
            user.joinGroup(group);
        }
    }

    @Override
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String groupPath = mapperModel.getConfig().get(ConfigConstants.GROUP);
        if (!hasClaimValue(mapperModel, context)) {
            GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
            if (group == null) {
                throw new IdentityBrokerException("Unable to find group: " + groupPath);
            }
            user.leaveGroup(group);
        }

    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String groupPath = mapperModel.getConfig().get(ConfigConstants.GROUP);
        GroupModel group = KeycloakModelUtils.findGroupByPath(realm, groupPath);
        if (group == null) {
            throw new IdentityBrokerException("Unable to find group: " + groupPath);
        }
        if (!hasClaimValue(mapperModel, context)) {
            user.leaveGroup(group);
        } else {
            user.joinGroup(group);
        }
    }

    @Override
    public String getHelpText() {
        return "If the claim exists and has the required value, the user will become a member of the specified group.";
    }
}
