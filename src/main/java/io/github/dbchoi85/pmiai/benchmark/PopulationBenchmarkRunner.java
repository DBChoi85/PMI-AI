package io.github.dbchoi85.pmiai.benchmark;

import io.github.dbchoi85.pmiai.auth.AcPerAgentProvider;
import io.github.dbchoi85.pmiai.auth.EpgProvider;
import io.github.dbchoi85.pmiai.model.Privilege;
import io.github.dbchoi85.pmiai.pmi.MiniPmi;

import java.util.ArrayList;

public final class PopulationBenchmarkRunner {
    private PopulationBenchmarkRunner() {}

    public static PopulationResult runAcPerAgent(int agentCount, Privilege privilege) {
        var pmi = new MiniPmi();
        var provider = new AcPerAgentProvider(pmi, privilege);
        var credentials = new ArrayList<org.bouncycastle.cert.X509AttributeCertificateHolder>(agentCount);

        long issueStart = System.nanoTime();
        for (int i = 0; i < agentCount; i++) credentials.add(provider.issue());
        long issuanceNs = System.nanoTime() - issueStart;

        long credentialBytes = 0;
        int successful = 0;
        long verifyStart = System.nanoTime();
        for (var credential : credentials) {
            if (provider.verify(credential)) successful++;
            credentialBytes += provider.credentialSize(credential);
        }
        long verificationNs = System.nanoTime() - verifyStart;

        return new PopulationResult("AC_PER_AGENT", agentCount, 0, issuanceNs, verificationNs,
                issuanceNs + verificationNs, credentialBytes, provider.authorityInteractions(), successful);
    }

    public static PopulationResult runProposed(int agentCount, Privilege basePrivilege, Privilege childPrivilege) {
        var pmi = new MiniPmi();
        var provider = new EpgProvider(pmi, basePrivilege);
        var keys = new ArrayList<java.security.KeyPair>(agentCount);
        var credentials = new ArrayList<io.github.dbchoi85.pmiai.epg.EphemeralPrivilegeGrant>(agentCount);

        long keyStart = System.nanoTime();
        for (int i = 0; i < agentCount; i++) keys.add(provider.newAgentKeyPair());
        long keyGenerationNs = System.nanoTime() - keyStart;

        long issueStart = System.nanoTime();
        for (var key : keys) credentials.add(provider.issue(key.getPublic(), childPrivilege));
        long issuanceNs = System.nanoTime() - issueStart;

        long credentialBytes = 0;
        int successful = 0;
        long verifyStart = System.nanoTime();
        for (var credential : credentials) {
            if (provider.verify(credential)) successful++;
            credentialBytes += provider.credentialSize(credential);
        }
        long verificationNs = System.nanoTime() - verifyStart;

        long totalNs = keyGenerationNs + issuanceNs + verificationNs;
        return new PopulationResult("BASE_AC_EPG", agentCount, keyGenerationNs, issuanceNs, verificationNs,
                totalNs, credentialBytes, provider.authorityInteractions(), successful);
    }
}
