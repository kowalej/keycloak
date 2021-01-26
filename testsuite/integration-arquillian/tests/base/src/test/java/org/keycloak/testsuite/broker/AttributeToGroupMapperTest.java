package org.keycloak.testsuite.broker;

import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;

import java.util.List;

import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class AttributeToGroupMapperTest extends AbstractGroupMapperTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration();
    }

    @Test
    public void mapperJoinsGroupOnFirstLogin() {
        UserRepresentation user = createMapperThenLoginAsUserTwiceWithAttributeToRoleMapper(FORCE);

        assertThatGroupHasBeenJoinedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserJoinsGroupInLegacyMode() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithAttributeToRoleMapper(LEGACY);

        assertThatGroupHasBeenJoinedInConsumerRealmTo(user);
    }

    @Test
    public void updateBrokeredUserJoinsGroupInForceMode() {
        UserRepresentation user = loginAsUserThenCreateMapperAndLoginAgainWithAttributeToRoleMapper(FORCE);

        assertThatGroupHasBeenJoinedInConsumerRealmTo(user);
    }

    private UserRepresentation createMapperThenLoginAsUserTwiceWithAttributeToRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, false,
            ImmutableMap.<String, List<String>>builder()
                .put("Group", ImmutableList.<String>builder().add(CLIENT_ROLE_MAPPER_REPRESENTATION).build())
                .build());
    }

    private UserRepresentation loginAsUserThenCreateMapperAndLoginAgainWithAttributeToRoleMapper(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, true,
            ImmutableMap.<String, List<String>>builder()
                .put("Group", ImmutableList.<String>builder().add(CLIENT_ROLE_MAPPER_REPRESENTATION).build())
                .build());
    }

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation samlAttributeToRoleMapper = new IdentityProviderMapperRepresentation();
        samlAttributeToRoleMapper.setName("user-group-mapper");
        samlAttributeToRoleMapper.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
        samlAttributeToRoleMapper.setConfig(ImmutableMap.<String,String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
            .put(UserAttributeMapper.ATTRIBUTE_NAME, "Group")
            .put(ATTRIBUTE_VALUE, GROUP_USER)
            .put("group", GROUP_PATH)
            .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        samlAttributeToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(samlAttributeToRoleMapper).close();
    }
}
