package io.github.dbchoi85.pmiai.epg;

import io.github.dbchoi85.pmiai.model.Privilege;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EpgSubjectBindingTest {
 @Test void bindsGrantToSubjectNameAndPublicKey() {
  EpgService service=new EpgService(); var issuer=service.newAgentKeyPair(); var subject=service.newAgentKeyPair(); var other=service.newAgentKeyPair();
  long now=System.currentTimeMillis()/1000; var p=new Privilege(Set.of("mail.read"),"/project-alpha/mail/**",now+60);
  var g=service.issue("orchestrator","mail-agent","base-ac","task",p,"pmi-aa",1,subject.getPublic(),issuer.getPrivate());
  assertTrue(EpgSubjectBinding.matches(g,"mail-agent",subject.getPublic()));
  assertFalse(EpgSubjectBinding.matches(g,"calendar-agent",subject.getPublic()));
  assertFalse(EpgSubjectBinding.matches(g,"mail-agent",other.getPublic()));
 }
}
