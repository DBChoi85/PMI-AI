# Project Meeting Privilege-Confinement Workflow

This benchmark implements Issue #40 as a deterministic, real-world-derived cross-application workflow. It models mail, calendar, and shared-drive permission boundaries without live SaaS accounts or an LLM.

The user asks an orchestrator to prepare a Project Alpha meeting. Short-lived Mail, Calendar, Availability, and Drive agents receive attenuated EPGs. Availability Agent is a child of Calendar Agent and demonstrates multi-hop attenuation and a shorter lifetime.

The workflow evaluates legitimate actions, operation escalation, resource escape, transitive escalation, parent-lifetime violation, expired-grant reuse, and cross-agent EPG misuse.

Run `./gradlew test projectMeetingWorkflow`. The default is 100 iterations and output is `results/project-meeting-workflow.csv`. Override with `-PworkflowIterations=1000`.

This is an authorization benchmark, not an LLM or prompt-injection benchmark. Cross-agent misuse requires both the logical subject name and caller public key to match the values signed into the EPG.
