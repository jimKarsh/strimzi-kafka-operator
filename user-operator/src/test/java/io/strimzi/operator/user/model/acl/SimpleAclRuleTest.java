/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.user.model.acl;

import io.strimzi.api.kafka.model.AclOperation;
import io.strimzi.api.kafka.model.AclResourcePatternType;
import io.strimzi.api.kafka.model.AclRule;
import io.strimzi.api.kafka.model.AclRuleBuilder;
import io.strimzi.api.kafka.model.AclRuleResource;
import io.strimzi.api.kafka.model.AclRuleTopicResourceBuilder;
import io.strimzi.api.kafka.model.AclRuleType;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class SimpleAclRuleTest {
    private static AclRuleResource aclRuleTopicResource;
    private static SimpleAclRuleResource resource = new SimpleAclRuleResource("my-topic", SimpleAclRuleResourceType.TOPIC, AclResourcePatternType.LITERAL);
    private static ResourcePattern kafkaResourcePattern = new ResourcePattern(ResourceType.TOPIC, "my-topic", PatternType.LITERAL);
    private static KafkaPrincipal kafkaPrincipal = new KafkaPrincipal("User", "my-user");

    static {
        aclRuleTopicResource = new AclRuleTopicResourceBuilder()
            .withName("my-topic")
            .withPatternType(AclResourcePatternType.LITERAL)
            .build();
    }

    @Test
    public void testFromCrd()   {
        AclRule rule = new AclRuleBuilder()
            .withType(AclRuleType.ALLOW)
            .withResource(aclRuleTopicResource)
            .withHost("127.0.0.1")
            .withOperation(AclOperation.READ)
            .build();

        List<SimpleAclRule> simple = SimpleAclRule.fromCrd(rule);
        assertThat(simple.size(), is(1));

        SimpleAclRule singleMappedSimpleRule = simple.get(0);
        assertThat(singleMappedSimpleRule.getOperation(), is(AclOperation.READ));
        assertThat(singleMappedSimpleRule.getType(), is(AclRuleType.ALLOW));
        assertThat(singleMappedSimpleRule.getHost(), is("127.0.0.1"));
        assertThat(singleMappedSimpleRule.getResource(), is(resource));
    }

    @Test
    public void testToKafkaAclBindingForSpecifiedKafkaPrincipalReturnsKafkaAclBindingForKafkaPrincipal() {
        SimpleAclRule kafkaTopicSimpleAclRule = new SimpleAclRule(AclRuleType.ALLOW, resource, "127.0.0.1", AclOperation.READ);
        AclBinding expectedAclBinding = new AclBinding(
                kafkaResourcePattern,
                new AccessControlEntry(kafkaPrincipal.toString(), "127.0.0.1",
                        org.apache.kafka.common.acl.AclOperation.READ, AclPermissionType.ALLOW)
        );
        assertThat(kafkaTopicSimpleAclRule.toKafkaAclBinding(kafkaPrincipal), is(expectedAclBinding));
    }

    @Test
    public void testFromAclBindingReturnsSimpleAclRule() {
        AclBinding aclBinding = new AclBinding(
                kafkaResourcePattern,
                new AccessControlEntry(kafkaPrincipal.toString(), "127.0.0.1",
                        org.apache.kafka.common.acl.AclOperation.READ, AclPermissionType.ALLOW)
        );
        SimpleAclRule expectedSimpleAclRule = new SimpleAclRule(AclRuleType.ALLOW, resource, "127.0.0.1", AclOperation.READ);
        assertThat(SimpleAclRule.fromAclBinding(aclBinding), is(expectedSimpleAclRule));
    }

    @Test
    public void testFromKafkaAclBindingToKafkaAclBindingRoundtrip()   {
        AclBinding kafkaAclBinding = new AclBinding(
                kafkaResourcePattern,
                new AccessControlEntry(kafkaPrincipal.toString(), "127.0.0.1",
                        org.apache.kafka.common.acl.AclOperation.READ, AclPermissionType.ALLOW)
        );
        assertThat(SimpleAclRule.fromAclBinding(kafkaAclBinding).toKafkaAclBinding(kafkaPrincipal), is(kafkaAclBinding));
    }

    @Test
    public void testFromCrdToKafkaAclBinding()   {
        AclRule rule = new AclRuleBuilder()
            .withType(AclRuleType.ALLOW)
            .withResource(aclRuleTopicResource)
            .withHost("127.0.0.1")
            .withOperation(AclOperation.READ)
            .build();

        AclBinding expectedKafkaAclBinding = new AclBinding(
                kafkaResourcePattern,
                new AccessControlEntry(kafkaPrincipal.toString(), "127.0.0.1",
                        org.apache.kafka.common.acl.AclOperation.READ, AclPermissionType.ALLOW)
        );

        assertThat(SimpleAclRule.fromCrd(rule).get(0).toKafkaAclBinding(kafkaPrincipal), is(expectedKafkaAclBinding));
    }

    @Test
    public void testFromCrdMultipleOperations() {
        AclRule rule = new AclRuleBuilder()
                .withType(AclRuleType.ALLOW)
                .withResource(aclRuleTopicResource)
                .withHost("127.0.0.1")
                .withOperations(AclOperation.READ, AclOperation.WRITE)
                .build();

        List<SimpleAclRule> simpleRules = SimpleAclRule.fromCrd(rule);

        assertThat(simpleRules.size(), is(2));
        assertThat(simpleRules.get(0).getOperation(), is(AclOperation.READ));
        assertThat(simpleRules.get(1).getOperation(), is(AclOperation.WRITE));

        simpleRules.forEach((testRule) -> {
            assertThat(testRule.getType(), is(AclRuleType.ALLOW));
            assertThat(testRule.getHost(), is("127.0.0.1"));
            assertThat(testRule.getResource(), is(resource));
        });
    }
}
