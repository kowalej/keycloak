package org.keycloak.testsuite.broker;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author hmlnarik,
 * <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>,
 * <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 * Jason Kowaleski
 */
public abstract class AbstractGroupMapperTest extends AbstractIdentityProviderMapperTest {

    public static final String GROUP_PATH = "/Parent/Child";

    protected abstract void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode);

    protected void updateUser() {
    }

    protected UserRepresentation loginAsUserTwiceWithMapper(
        IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin, Map<String, List<String>> userConfig) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        createUserInProviderRealm(userConfig);
        createUserGroupAndUserJoinsInProviderRealm();

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        if (!createAfterFirstLogin) {
            assertThatGroupHasBeenJoinedInConsumerRealmTo(user);
        } else {
            assertThatGroupHasNotBeenJoinedInConsumerRealmTo(user);
        }

        if (createAfterFirstLogin) {
            createMapperInIdp(idp, syncMode);
        }
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        updateUser();

        logInAsUserInIDP();
        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        return user;
    }

    protected void createUserGroupAndUserJoinsInProviderRealm() {
        GroupRepresentation userGroup = new GroupRepresentation();
        userGroup.setPath(GROUP_PATH);
        userGroup.setName("Some Nested Group");
        adminClient.realm(bc.providerRealmName()).groups().add(userGroup);
        GroupRepresentation group = adminClient.realm(bc.providerRealmName()).getGroupByPath(GROUP_PATH);
        UserResource userResource = adminClient.realm(bc.providerRealmName()).users().get(userId);
        userResource.joinGroup(group.getId());
    }

    protected void assertThatGroupHasBeenJoinedInConsumerRealmTo(UserRepresentation user) {
        assertThat(user.getGroups(), contains(GROUP_PATH));
    }

    protected void assertThatGroupHasNotBeenJoinedInConsumerRealmTo(UserRepresentation user) {
        assertThat(user.getGroups(), not(contains(GROUP_PATH)));
    }
}
