package ir.ac.ui.ontodebugger.mups;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ir.ac.ui.ontodebugger.util.OntologyHelper.getClassDefinitionAxioms;
import static ir.ac.ui.ontodebugger.util.OntologyHelper.isSatisfiable;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 5/24/16.
 */
public class SwoopMUPSFinder extends ExpandShrinkMUPSFinder {
    private static final Logger LOGGER = LogManager.getLogger(SwoopMUPSFinder.class);

    @Override
    protected boolean expand(OWLOntology ont, OWLEntity entity, OWLOntology temp, OWLReasoner reasoner, Set<MUPS> allMUPSs) {
        OWLOntologyManager m = temp.getOWLOntologyManager();

        // step 1: add class definition axioms of the clazz
        final Set<OWLAxiom> classAxioms = getClassDefinitionAxioms(ont, entity);
        m.addAxioms(temp, classAxioms);
        boolean found = !isSatisfiable(reasoner, entity);

        int axiomLimit = 40;
        boolean someAdded = true;
        while (!found && someAdded) {
            // step 2: add axioms related to clazz
            Set<OWLAxiom> related = new HashSet<>();
            temp.getClassesInSignature().stream().forEach(owlClass -> related.addAll(getClassDefinitionAxioms(ont, owlClass)));
            related.removeAll(temp.getAxioms());

            if (related.isEmpty())
                someAdded = false;
            else if (related.size() > axiomLimit) {
                List<OWLAxiom> axioms = new ArrayList<>(related);
                m.addAxioms(temp, new HashSet<>(axioms.subList(0, axiomLimit)));
                axiomLimit *= 1.25;
            } else {
                m.addAxioms(temp, related);
            }
            found = !isSatisfiable(reasoner, entity);
        }
        return found;
    }

    @Override
    protected Set<OWLAxiom> shrink(OWLEntity entity, OWLOntology temp, OWLReasoner reasoner, Set<MUPS> allMUPSs) {
        OWLOntologyManager m = temp.getOWLOntologyManager();
        Set<OWLAxiom> mups = new HashSet<>();
        final List<OWLAxiom> axioms = new ArrayList<>(temp.getAxioms());

        int pruneWindowSize = 10;
        // fast pruning
        if (axioms.size() >= pruneWindowSize) {
            int index = 0;
            while (index < axioms.size()) {
                Set<OWLAxiom> windowAxioms =
                        new HashSet<>(axioms.subList(index, Math.min(index + pruneWindowSize, axioms.size())));

                m.removeAxioms(temp, windowAxioms);
                if (isSatisfiable(reasoner, entity)) { // there is some part of mups in this part of the list, so go to prune next parts of the list
                    index += pruneWindowSize;
                    m.addAxioms(temp, windowAxioms);
                } else {
                    LOGGER.trace("{} axioms removed from pruning ontology.", windowAxioms.size());
                    axioms.removeAll(windowAxioms);
                }
            }
        }

        // slow pruning
        LOGGER.trace("Slow Pruning started with {} axioms.", axioms.size());
        List<OWLAxiom> toBeRemoved = new ArrayList<>();
        for (OWLAxiom axiom : axioms) {
            m.removeAxiom(temp, axiom);
            if (isSatisfiable(reasoner, entity)) { // there is some part of mups in this part of the list, so go to prune next parts of the list
                m.addAxiom(temp, axiom);
            } else {
                toBeRemoved.add(axiom);
            }
        }
        axioms.removeAll(toBeRemoved);
        //remained axioms are a min MUPS
        mups.addAll(axioms);
        return mups;
    }
}
