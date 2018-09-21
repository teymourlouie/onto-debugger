package ir.ac.ui.ontodebugger;

import ir.ac.ui.ontodebugger.mups.MUPS;
import ir.ac.ui.ontodebugger.mups.MUPSType;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 9/14/15
 */

public class Bug implements Comparable {
    private static final Logger LOGGER = LogManager.getLogger(Bug.class);
    @Getter
    private final OWLEntity entity;
    @Getter
    private final Set<MUPS> mupsSet;
    @Getter
    private final Set<Set<OWLAxiom>> diagnoses;

    public static Bug emptyBug(OWLEntity entity) {
        return new Bug(entity, Collections.emptySet(), Collections.emptySet());
    }

    public Bug(OWLEntity entity, Set<MUPS> mupsSet, Set<Set<OWLAxiom>> diagnoses) {
        this.entity = entity;
        this.mupsSet = mupsSet;
        this.diagnoses = diagnoses;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Bug{");
        String entityName = entity.getClass().getSimpleName();
        if (entity instanceof OWLClass) {
            entityName = "Class";
        } else if (entity instanceof OWLObjectProperty) {
            entityName = "OProp";
        }
        sb.append(entityName).append(": ")
                .append(String.format("%-60s", entity));
        sb.append(", # MUPSs: ").append(mupsSet.size());

        sb.append(", TYPE1: ").append(mupsSet.stream().filter(mups -> mups.getType() == MUPSType.TYPE_1).count());
        sb.append(" TYPE2: ").append(mupsSet.stream().filter(mups -> mups.getType() == MUPSType.TYPE_2).count());
        sb.append(" TYPE3: ").append(mupsSet.stream().filter(mups -> mups.getType() == MUPSType.TYPE_3).count());

        sb.append(", # diagnoses: ").append(diagnoses.size());
        sb.append('}');
        return sb.toString();
    }

    /**
     * Replace axiom a with b in all of MUPS
     *
     * @param a axiom to be replaced
     * @param b axiom to be replaced with
     */
    public void replaceAxiom(OWLAxiom a, OWLAxiom b) {
        mupsSet.stream().filter(mups -> mups.contains(a)).forEach(mups -> {
            mups.remove(a);
            mups.add(b);
        });
    }

    /**
     * replace key axiom with value axiom for all of entry set of given map in MUPS
     *
     * @param axiomPairs map shows which axiom (key) should be replaced by which one (value)
     */
    public void replaceAxioms(Map<OWLAxiom, OWLAxiom> axiomPairs) {
        axiomPairs.entrySet().stream().forEach(entry -> replaceAxiom(entry.getKey(), entry.getValue()));
    }

    public Set<OWLAxiom> getAxioms() {
        Set<OWLAxiom> axioms = new HashSet<>();
        mupsSet.stream().forEach(axioms::addAll);
        return axioms;
    }

    @Override
    public int compareTo(Object o) {
        if (this == o)
            return 0;
        if (o != null)
            return getEntity().compareTo(((Bug) o).getEntity());
        else
            return 1;
    }
}
