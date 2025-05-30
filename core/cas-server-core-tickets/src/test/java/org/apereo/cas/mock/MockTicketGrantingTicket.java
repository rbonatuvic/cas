package org.apereo.cas.mock;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.DefaultAuthenticationBuilder;
import org.apereo.cas.authentication.DefaultAuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.handler.support.SimpleTestUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.UniqueTicketIdGenerator;
import org.apereo.cas.ticket.expiration.TicketGrantingTicketExpirationPolicy;
import org.apereo.cas.ticket.tracking.TicketTrackingPolicy;
import org.apereo.cas.util.DefaultUniqueTicketIdGenerator;
import org.apereo.cas.util.function.FunctionUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import java.io.Serial;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mock ticket-granting ticket.
 *
 * @author Marvin S. Addison
 * @since 3.0.0
 */
@Getter
@EqualsAndHashCode(of = "id")
@SuppressWarnings("JdkObsolete")
public class MockTicketGrantingTicket implements TicketGrantingTicket {

    public static final UniqueTicketIdGenerator ID_GENERATOR = new DefaultUniqueTicketIdGenerator();

    @Serial
    private static final long serialVersionUID = 6546995681334670659L;

    @Setter
    private String id;

    @Setter
    private String tenantId;

    private final Authentication authentication;

    private final Map<String, Service> services = new HashMap<>();

    private final Map<String, Service> proxyGrantingTickets = new HashMap<>();

    private final Set<String> descendantTickets = new LinkedHashSet<>();

    @Setter
    private Service proxiedBy;

    @Setter
    private ZonedDateTime created;

    private int usageCount;

    private boolean expired;

    @Setter
    private ExpirationPolicy expirationPolicy = new TicketGrantingTicketExpirationPolicy(100, 100);

    public MockTicketGrantingTicket(final String principalId, final Credential credential,
                                    final Map<String, List<Object>> principalAttributes) {
        this(principalId, credential, principalAttributes, Map.of());
    }

    public MockTicketGrantingTicket(final String principalId, final Map<String, List<Object>> principalAttributes,
                                    final Map<String, List<Object>> authnAttributes) {
        this(principalId,
            CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("uid", "password"),
            principalAttributes, authnAttributes);
    }

    public MockTicketGrantingTicket(final String principalId, final Credential credential,
                                    final Map<String, List<Object>> principalAttributes,
                                    final Map<String, List<Object>> authnAttributes) {
        this(new DefaultAuthenticationBuilder(getPrincipal(principalId, principalAttributes))
            .addCredential(credential)
            .setAttributes(authnAttributes)
            .addAttribute(AuthenticationHandler.SUCCESSFUL_AUTHENTICATION_HANDLERS,
                List.of(SimpleTestUsernamePasswordAuthenticationHandler.class.getSimpleName()))
            .addSuccess(SimpleTestUsernamePasswordAuthenticationHandler.class.getName(),
                new DefaultAuthenticationHandlerExecutionResult(new SimpleTestUsernamePasswordAuthenticationHandler(), credential))
            .build());
    }

    public MockTicketGrantingTicket(final Authentication authentication) {
        id = FunctionUtils.doUnchecked(() -> ID_GENERATOR.getNewTicketId(TicketGrantingTicket.PREFIX));
        created = ZonedDateTime.now(ZoneOffset.UTC);
        this.authentication = authentication;
    }

    public MockTicketGrantingTicket(final String principal) {
        this(principal, new HashMap<>());
    }

    public MockTicketGrantingTicket(final String principal, final Map principalAttributes) {
        this(principal,
            CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("uid", "password"),
            principalAttributes);
    }

    private static Principal getPrincipal(final String principalId, final Map<String, List<Object>> principalAttributes) {
        return FunctionUtils.doUnchecked(() -> PrincipalFactoryUtils.newPrincipalFactory().createPrincipal(principalId, principalAttributes));
    }

    public ServiceTicket grantServiceTicket(final Service service,
                                            final TicketTrackingPolicy trackingPolicy) throws Throwable {
        return grantServiceTicket(ID_GENERATOR.getNewTicketId("ST"), service, null,
            false, trackingPolicy);
    }

    @Override
    public ServiceTicket grantServiceTicket(final String id, final Service service, final ExpirationPolicy expirationPolicy,
                                            final boolean credentialProvided,
                                            final TicketTrackingPolicy trackingPolicy) {
        val st = new MockServiceTicket(id, service, this, expirationPolicy);
        trackingPolicy.trackTicket(this, st);
        return st;
    }

    @Override
    public void removeAllServices() {
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    @CanIgnoreReturnValue
    public TicketGrantingTicket getRoot() {
        return this;
    }

    @Override
    public List<Authentication> getChainedAuthentications() {
        return new ArrayList<>();
    }

    @Override
    public Collection<String> getDescendantTickets() {
        return this.descendantTickets;
    }

    @Override
    public TicketGrantingTicket getTicketGrantingTicket() {
        return this;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return created;
    }

    @Override
    public int getCountOfUses() {
        return usageCount;
    }

    @Override
    public String getPrefix() {
        return TicketGrantingTicket.PREFIX;
    }

    @Override
    public void markTicketExpired() {
        expired = true;
    }

    @Override
    public ZonedDateTime getLastTimeUsed() {
        return created;
    }

    @Override
    public ZonedDateTime getPreviousTimeUsed() {
        return created;
    }

    @Override
    public Ticket update() {
        usageCount++;
        return this;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int compareTo(final Ticket o) {
        return this.id.compareTo(o.getId());
    }


}
