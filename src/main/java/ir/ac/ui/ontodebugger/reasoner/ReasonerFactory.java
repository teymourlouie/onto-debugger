package ir.ac.ui.ontodebugger.reasoner;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import ir.ac.ui.ontodebugger.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.*;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/8/15
 */
public class ReasonerFactory {
    private static final Logger LOGGER = LogManager.getLogger(ReasonerFactory.class);
    private static final Map<OWLOntology, OWLReasoner> reasonerPool = new ConcurrentHashMap<>();

    private ReasonerFactory() {
    }

    /**
     * get reasoner for given ontology if it is already created in reasonerPool
     * otherwise create new reasoner and add it to reasonerPool
     *
     * @param ontology
     * @return reasoner corresponding to the given ontology
     */
    public static OWLReasoner getReasoner(OWLOntology ontology) {
        return getReasoner(ontology, false);
    }

    /**
     * get reasoner for the given ontology if it is already cached in reasonerPool
     * otherwise create new reasoner and add it to reasonerPool if cacheReasoner is set to true
     *
     * @param ontology
     * @param cacheReasoner if true, reasoner will be putted in the pool,
     * @return reasoner corresponding to the given ontology
     */
    public static OWLReasoner getReasoner(OWLOntology ontology, boolean cacheReasoner) {
        OWLReasoner reasoner = null;

        if (cacheReasoner) {
            reasoner = reasonerPool.get(ontology);
        }

        if (reasoner == null) {
            reasoner = getNewReasoner(ontology);
            if (cacheReasoner) { // if caching is required by cacheReasoner param then add it to the reasonerPool
                reasonerPool.put(ontology, reasoner);
            }
        } else {
            reasoner.flush();
        }
        return reasoner;
    }

    private static OWLReasoner getNewReasoner(OWLOntology ontology) {
        switch (Configs.getInstance().getREASONER_TYPE()) {
            case PELLET:
                return getPelletReasoner(ontology);
            case HERMIT:
                return getHermitReasoner(ontology);
            case FACTPLUSPLUS:
                return getFactPlusPlusReasoner(ontology);
        }
        return null;
    }

    private static OWLReasoner getFactPlusPlusReasoner(OWLOntology ontology) {
        return new FaCTPlusPlusReasoner(ontology, getSimpleConfiguration(), BufferingMode.BUFFERING);
    }

    private static OWLReasonerConfiguration getSimpleConfiguration() {
        return new SimpleConfiguration(getProgressMonitor());
    }

    private static OWLReasoner getHermitReasoner(OWLOntology ontology) {
        final Configuration configuration = new Configuration();
        configuration.bufferChanges = true;
        configuration.ignoreUnsupportedDatatypes = true;
        configuration.reasonerProgressMonitor = getProgressMonitor();
        return new Reasoner(configuration, ontology);
    }

    private static ReasonerProgressMonitor getProgressMonitor() {
        return new NullReasonerProgressMonitor();
    }

    private static OWLReasoner getPelletReasoner(OWLOntology ontology) {
        return new PelletReasoner(ontology, getSimpleConfiguration(), BufferingMode.BUFFERING);
    }

    public static void removeReasoner(OWLReasoner reasoner) {
        final List<OWLOntology> list = reasonerPool.keySet().stream().filter(ontology -> reasonerPool.get(ontology) == reasoner).collect(Collectors.toList());
        list.forEach(reasonerPool::remove);
    }

    public static void removeReasoner(OWLOntology ontology) {
        reasonerPool.remove(ontology);
    }
}
