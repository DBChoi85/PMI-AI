package io.github.dbchoi85.pmiai.pmi;

import io.github.dbchoi85.pmiai.model.Privilege;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.*;

import java.math.BigInteger;
import java.security.*;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class MiniPmi {
    public static final ASN1ObjectIdentifier PRIVILEGE_OID =
            new ASN1ObjectIdentifier("1.3.6.1.4.1.55555.1.1");

    private final KeyPair aaKeyPair;
    private final X500Name issuer = new X500Name("CN=PMI-AI Attribute Authority");
    private final AtomicLong serial = new AtomicLong(1);
    private final AtomicLong interactions = new AtomicLong();

    public MiniPmi() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            aaKeyPair = generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public X509AttributeCertificateHolder issue(String agentId, Privilege privilege) {
        interactions.incrementAndGet();
        try {
            long now = Instant.now().getEpochSecond();
            var holder = new AttributeCertificateHolder(new X500Name("CN=" + agentId));
            var acIssuer = new AttributeCertificateIssuer(issuer);
            var builder = new X509v2AttributeCertificateBuilder(holder, acIssuer,
                    BigInteger.valueOf(serial.getAndIncrement()),
                    Date.from(Instant.ofEpochSecond(now - 5)),
                    Date.from(Instant.ofEpochSecond(privilege.expiresAtEpochSecond())));
            builder.addAttribute(PRIVILEGE_OID, new DERUTF8String(encode(privilege)));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(aaKeyPair.getPrivate());
            return builder.build(signer);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verify(X509AttributeCertificateHolder ac) {
        try {
            ac.checkValidity(new Date());
            return ac.isSignatureValid(new JcaContentVerifierProviderBuilder().build(aaKeyPair.getPublic()));
        } catch (Exception e) {
            return false;
        }
    }

    public Privilege readPrivilege(X509AttributeCertificateHolder ac) {
        var attrs = ac.getAttributes(PRIVILEGE_OID);
        if (attrs.length != 1) throw new IllegalArgumentException("Missing privilege attribute");
        var text = DERUTF8String.getInstance(attrs[0].getAttributeValues()[0]).getString();
        String[] p = text.split("\\|", -1);
        return new Privilege(Set.of(p[0].split(",")), p[1], Long.parseLong(p[2]));
    }

    public long interactions() { return interactions.get(); }
    public void resetInteractions() { interactions.set(0); }

    private static String encode(Privilege p) {
        return String.join(",", p.operations()) + "|" + p.resource() + "|" + p.expiresAtEpochSecond();
    }
}
