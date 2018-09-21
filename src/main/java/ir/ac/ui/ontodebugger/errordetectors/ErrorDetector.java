package ir.ac.ui.ontodebugger.errordetectors;

import ir.ac.ui.ontodebugger.BugList;
import ir.ac.ui.ontodebugger.Configs;
import ir.ac.ui.ontodebugger.mups.MUPS;
import ir.ac.ui.ontodebugger.util.Renderer;
import ir.ac.ui.ontodebugger.util.Timer;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/21/15
 */
public class ErrorDetector {
    private static final Logger LOGGER = LogManager.getLogger(ErrorDetector.class);

    /**
     * Map each axiom to a cost value (Double)
     */
    private final Map<OWLAxiom, Double> axiomCostMap = new HashMap<>();

    protected BugList bugs;
    @Getter
    protected Set<OWLAxiom> errors = new HashSet<>();
    private Set<OWLAxiom> whiteList = new HashSet<>();
    private final AxiomRanker axiomRanker;

    public ErrorDetector(AxiomRanker axiomRanker) {
        this.axiomRanker = axiomRanker;
    }

    /**
     * find a hit set for all of bugs that has minimum cost
     *
     * @param ont
     * @param bugs
     * @return set of axioms that are responsible for errors in ontology
     */
    public Set<OWLAxiom> findErrors(OWLOntology ont, BugList bugs) {
        axiomRanker.init(ont, bugs);
        init(bugs);

        final Set<OWLAxiom> undecidedAxioms = bugs.getSuspectedAxioms().stream().filter(axiom -> !errors.contains(axiom))
                .collect(Collectors.toSet());
        calculateCosts(undecidedAxioms);
        if (Configs.getInstance().isUSE_GREEDY_ERROR_SEARCH()) {
            errors = greedyErrorSearch(bugs, errors);
        } else {
            errors = hitSetErrorSearch(bugs, errors);
        }
        axiomRanker.fini();
        return errors;
    }

    private boolean isUndecided(OWLAxiom axiom) {
        return !whiteList.contains(axiom) && !errors.contains(axiom);
    }

    protected void init(BugList bugs) {
        this.bugs = bugs;

        this.whiteList.clear();
        this.whiteList.addAll(bugs.getWhiteList());

        errors.clear();
        errors.addAll(bugs.getBlackList());
    }

    /**
     * Select error set based on greedy algorithm:
     * sort axioms based on cost asc, remove the axiom with minimum cost
     * then select next axiom among axioms which their mups is not covered by selected path
     *
     * @return Set of erroneous axioms
     */
    private Set<OWLAxiom> greedyErrorSearch(BugList bugs, Set<OWLAxiom> initialErrors) {
        Set<OWLAxiom> path = new HashSet<>(initialErrors);
        List<OWLAxiom> axioms = new ArrayList<>(bugs.getUncoveredAxioms(path));
        while (!axioms.isEmpty()) {
            path.add(getAxiomWithMinCost(axioms));
            axioms = new ArrayList<>(bugs.getUncoveredAxioms(path));
        }
        return path;
    }

    private OWLAxiom getAxiomWithMinCost(List<OWLAxiom> axiomList) {
        List<OWLAxiom> axioms;
        axioms = axiomList.stream().filter(this::isUndecided).collect(Collectors.toList());
        if (axioms.isEmpty())
            axioms = axiomList;
        return orderAxioms(axioms).get(0);
    }


    /**
     * Select minimum cost error set using general cost based search tree algorithm
     * it's sound and complete but has exponential time
     *
     * @return
     */
    private Set<OWLAxiom> hitSetErrorSearch(BugList bugs, Set<OWLAxiom> initialErrorSet) {
        final Timer timer = Timer.start("Finding Errors using hitSetErrorSearch");
        Set<OWLAxiom> optimalPath = new HashSet<>(initialErrorSet);
        Set<OWLAxiom> path = new HashSet<>(initialErrorSet);
        final Set<MUPS> mupsSet = bugs.getMupsSet();

        double optimum = Double.MAX_VALUE;
        Stack<OWLAxiom> stack = new Stack<>();

        // select any random mups as root of HST
        Set<OWLAxiom> root = checkPathHitSet(path, mupsSet);
        stack.addAll(root);

        while (!stack.isEmpty()) {
            final OWLAxiom axiom = stack.pop();

            if (path.contains(axiom)) { //backtrack and try another paths
                path.remove(axiom);
                continue;
            }

            //add axiom to path
            path.add(axiom);
            //evaluate if it is increasing path cost from optimum or not
            double pathCost = getPathCost(path);
            if (pathCost >= optimum) {
                path.remove(axiom);
                continue;
            }

            Set<OWLAxiom> left = checkPathHitSet(path, mupsSet);

            if (left.isEmpty()) { // path is a hitting set
                LOGGER.debug("New optimum path found:{}", pathCost);
                optimum = pathCost;
                optimalPath = new HashSet<>(path);
                path.remove(axiom);
            } else {
                stack.add(axiom); //add it back to stack to enable backtracking in tree
                stack.addAll(orderAxioms(left));
            }
        }
        timer.stopAndPrint();
        return optimalPath;
    }

    /**
     * initialize cost map
     *
     * @param allAxioms axioms to be evaluated
     */
    private void calculateCosts(Set<OWLAxiom> allAxioms) {
        axiomCostMap.clear();
        allAxioms.stream().forEach(axiom -> axiomCostMap.put(axiom, axiomRanker.getAxiomCost(axiom)));
        LOGGER.debug("calculating cost for axioms finished!");

        // print sorted list of suspected axioms
        List<OWLAxiom> sortedAxioms = orderAxioms(axiomCostMap.keySet());

        StringBuilder builder = new StringBuilder("Axiom Costs:");
        sortedAxioms.stream().forEachOrdered(owlAxiom -> builder.append("\n" + String.format("%.2f", getAxiomCost(owlAxiom)) + "\t" + bugs.getMUPSContainsAxiom(owlAxiom).size() + "\t" + Renderer.render(owlAxiom)));
        LOGGER.info("{} - {}", axiomRanker, builder.toString());
    }


    /**
     * Sort axioms in collection according to cost value asc.
     *
     * @param axioms to be sorted
     * @return List of sorted axioms
     */
    private List<OWLAxiom> orderAxioms(Collection<OWLAxiom> axioms) {
        List<OWLAxiom> list = new ArrayList<>(axioms);
        // sort asc, first one has low cost
        Collections.sort(list, Comparator.comparing(this::getAxiomCost));
        return list;
    }

    private Double getAxiomCost(OWLAxiom axiom) {
        return axiomCostMap.getOrDefault(axiom, Double.MAX_VALUE);
    }

    /**
     * Checks if the given path is a hitset for MUPS sets
     *
     * @param path   Set of Axioms building the path
     * @param mupses Set of MUPSes
     * @return the set that has none of it's members are contained in the path
     */
    private Set<OWLAxiom> checkPathHitSet(Set<OWLAxiom> path, Set<MUPS> mupses) {
        // find a MUPS which none of path nodes contained in it, so path is not hit set and return that MUPS
        final Optional<MUPS> any = mupses.stream().filter(m -> path.stream().noneMatch(m::contains)).findFirst();
        if (any.isPresent()) {
            return any.get();
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Get cost of the path, which is sum of costs of path nodes
     *
     * @param path consist of some axiom nodes
     * @return path cost
     */
    private double getPathCost(Set<OWLAxiom> path) {
        return path.stream().mapToDouble(this::getAxiomCost).sum();
    }
}
