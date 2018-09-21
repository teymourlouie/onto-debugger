package ir.ac.ui.ontodebugger;

import ir.ac.ui.ontodebugger.mups.MUPS;
import ir.ac.ui.ontodebugger.mups.MUPSType;
import ir.ac.ui.ontodebugger.util.Renderer;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/15/15
 */
public class BugList {
    private static final Logger LOGGER = LogManager.getLogger(BugList.class);
    public static final IRI BUGLIST_IRI = IRI.create(Configs.IRI_PREFIX, "/buglist/cached/");

    @Getter
    private final List<Bug> bugs;
    /**
     * Maps each axiom to set of AxiomSets that contain it
     */
    private final Map<OWLAxiom, Set<MUPS>> axiomMupsMap = new HashMap<>();
    @Getter
    private final Set<OWLAxiom> suspectedAxioms = new HashSet<>();
    @Getter
    private final Set<MUPS> mupsSet = new HashSet<>();

    @Getter
    private final Set<OWLEntity> unsatisfiableClasses = new HashSet<>();

    @Getter
    private Set<OWLAxiom> whiteList = new HashSet<>();

    @Getter
    private Set<OWLAxiom> blackList = new HashSet<>();

    public BugList(Collection<? extends Bug> bugs) {
        this.bugs = new ArrayList<>(bugs);
        bugs.stream().forEach(bug -> unsatisfiableClasses.add(bug.getEntity()));
        buildAxiomMap();
    }

    private void buildAxiomMap() {
        bugs.stream().map(Bug::getMupsSet).forEach(mupses -> {
            this.mupsSet.addAll(mupses);
            mupses.stream().forEach(mups -> mups.stream().forEach(owlAxiom -> {
                suspectedAxioms.add(owlAxiom);

                // build map between axiom and MUPS containing it
                Set<MUPS> containers = axiomMupsMap.getOrDefault(owlAxiom, new HashSet<>());
                containers.add(mups);
                axiomMupsMap.put(owlAxiom, containers);
            }));
        });
    }

    public Set<MUPS> getMUPSContainsAxiom(OWLAxiom axiom) {
        return axiomMupsMap.getOrDefault(axiom, new HashSet<>());
    }

    public void printInfo() {
        LOGGER.info("#Incoherent Entities: {}, #Axioms:{}, #MUPS:{}", bugs.size(), suspectedAxioms.size(), mupsSet.size());
        bugs.stream().sorted().forEach(bug -> LOGGER.info("{}", bug));
        LOGGER.info("Suspected Axioms: {}", Renderer.render(suspectedAxioms));

        LOGGER.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    }

    /**
     * get set of axioms that are contained at least in a MUPS which is not covered by given axioms
     *
     * @param removedAxioms set of axioms selected as covering set
     * @return set of uncovered axioms
     */
    public Set<OWLAxiom> getUncoveredAxioms(Set<OWLAxiom> removedAxioms) {
        Set<OWLAxiom> uncoveredAxioms = new HashSet<>();

        Stream<MUPS> mupsStream = mupsSet.stream();
        if (Configs.getInstance().isASSUME_PROFILE_AXIOMS_ARE_CORRECT()) {
            mupsStream = mupsSet.stream().filter(mups -> mups.getType() != MUPSType.TYPE_3);
        }
        mupsStream.filter(mups -> mups.stream().noneMatch(removedAxioms::contains)).forEach(uncoveredAxioms::addAll);
        return uncoveredAxioms;
    }


    public void saveAsOntology(String filename) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology;
        try {
            ontology = manager.createOntology(BUGLIST_IRI);
            manager.addAxioms(ontology, suspectedAxioms);
            ontology.saveOntology(new FileOutputStream(filename));
        } catch (OWLOntologyCreationException | FileNotFoundException | OWLOntologyStorageException e) {
            LOGGER.catching(e);
        }
    }

    public boolean isEmpty() {
        return bugs.isEmpty();
    }
}
