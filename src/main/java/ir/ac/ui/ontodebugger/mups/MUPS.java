package ir.ac.ui.ontodebugger.mups;

import ir.ac.ui.ontodebugger.KnowledgeBaseProfile;
import ir.ac.ui.ontodebugger.util.Renderer;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * created on 5/21/16.
 */
public class MUPS extends HashSet<OWLAxiom> implements Comparable {
    private static final Logger LOGGER = LogManager.getLogger(MUPS.class);

    @Getter
    private OWLEntity entity;
    @Getter
    private MUPSType type;

    public static MUPS build(@Nonnull OWLEntity entity, @Nonnull Set<OWLAxiom> axioms, @Nonnull KnowledgeBaseProfile profile, @Nonnull OWLOntology ontology) {

        MUPSType type = MUPSType.TYPE_UNKNOWN;
        if (axioms.stream().allMatch(ontology::containsAxiom))
            type = MUPSType.TYPE_1;
        else if (axioms.stream().allMatch(profile.getProfileOntology()::containsAxiom))
            type = MUPSType.TYPE_3;
        else if (axioms.stream().anyMatch(ontology::containsAxiom)
                && axioms.stream().anyMatch(profile.getProfileOntology()::containsAxiom))
            type = MUPSType.TYPE_2;

        return new MUPS(entity, type, axioms);
    }

    private MUPS(OWLEntity entity, MUPSType type, Set<OWLAxiom> axioms) {
        this.entity = entity;
        this.type = type;
        addAll(axioms);
    }

    @Override
    public String toString() {
        return "MUPS{" +
                "Entity=" + entity +
                ", type=" + type +
                ", size=" + size() +
                '}' + '\n' +
                Renderer.render(this, ", ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MUPS mups = (MUPS) o;

        if (entity != null ? !entity.equals(mups.entity) : mups.entity != null) return false;
        return type == mups.type && super.equals(mups);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Object o) {
        if (this == o)
            return 0;
        if (o == null)
            return 1;
        MUPS mups = (MUPS) o;

        if (!entity.equals(mups.getEntity()))
            return entity.compareTo(mups.getEntity());

        if (type != mups.type)
            return type.compareTo(mups.type);

        if (size() != mups.size())
            return Integer.compare(size(), mups.size());

        if (super.equals(mups))
            return 0;
        else {
            final List<OWLAxiom> list1 = this.stream().sorted().collect(Collectors.toList());
            final List<OWLAxiom> list2 = mups.stream().sorted().collect(Collectors.toList());
            for (int i = 0; i < list1.size(); i++) {
                final int cmp = list1.get(i).compareTo(list2.get(i));
                if (cmp != 0)
                    return cmp;
            }
            return 0;
        }
    }
}
