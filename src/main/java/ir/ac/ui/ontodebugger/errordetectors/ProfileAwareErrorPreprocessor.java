package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.util.Renderer;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 11/5/15
 */
public class ProfileAwareErrorPreprocessor {
    private static final Logger LOGGER = LogManager.getLogger(ProfileAwareErrorPreprocessor.class);

    @Getter
    private Set<OWLAxiom> errors = new HashSet<>();
    @Getter
    private Set<OWLAxiom> whiteList = new HashSet<>();

    public ProfileAwareErrorPreprocessor() {

    }

    public void init(KnowledgeBaseProfile profile, OWLOntology alignment, Set<OWLAxiom> axioms) {
        errors.clear();
        whiteList.clear();

        ProfileSupportChecker checker = new ProfileSupportChecker();
        checker.init(profile, alignment);

        axioms.stream().forEach(axiom -> checkAxiomInitialStatus(checker, axiom));
        checker.fini();

        LOGGER.info("Preprocess Results: Errors:{}, WhiteList:{}", errors.size(), whiteList.size());
        LOGGER.debug("White List:{}\n{}", whiteList.size(), Renderer.render(whiteList));
        LOGGER.debug("Initial Errors:{}\n{}", errors.size(), Renderer.render(errors));
    }

    private void checkAxiomInitialStatus(ProfileSupportChecker checker, OWLAxiom axiom) {
        final List<SupportStatus> statusList = checker.getAxiomSupportStatus(axiom);

        long positiveVotes = statusList.stream().filter(supportStatus -> supportStatus == SupportStatus.AXIOM_ENTAILED).count();
        long negatives = statusList.stream().filter(supportStatus -> supportStatus == SupportStatus.NEGATION_ENTAILED).count();

        if (!statusList.isEmpty()) {
            if (positiveVotes > 0 && negatives == 0) {
                // negation of axiom is not supported in any profile of ontologies
                whiteList.add(axiom);
            } else if (negatives > 0 && positiveVotes == 0) {
                // axiom entailment is not supported in any of profile ontologies
                errors.add(axiom);
            }
        }
    }
}
