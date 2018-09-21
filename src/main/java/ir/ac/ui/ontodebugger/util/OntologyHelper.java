package ir.ac.ui.ontodebugger.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com> on 7/16/15.
 */
public class OntologyHelper {
    private static final Logger LOGGER = LogManager.getLogger(OntologyHelper.class);
    public static final String CACHED_OWL_SUFFIX = "-cached.owl";
    public static final String CACHED_ALIGNMENT_SUFFIX = "-cached-alignment.rdf";
    private static final List<String> allowedExtensions = Arrays.asList("owl", "rdf", "n3", "ttl", "xml");
    public static final String ALIGNMENT_EXTENSION = "rdf";

    private OntologyHelper() {
    }

    public static OWLOntology loadOntology(@Nonnull File file) {
        return loadOntology(file, OWLManager.createOWLOntologyManager());
    }

    public static OWLOntology loadOntology(@Nonnull File file, @Nonnull OWLOntologyManager manager) {
        if (!file.exists() || !file.isFile()) {
            LOGGER.error("{} is not existed or is not a file", file.toString());
            return null;
        }

        LOGGER.debug("loading ontology started: {}", file.getName());

        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration()
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
                .setStrict(false);
        Timer timer = Timer.start("Loading " + file.getName());
        OWLOntology ontology;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), configuration);
        } catch (Exception e) {
            LOGGER.catching(e);
            return null;
        }
        timer.stopAndPrint();

        LOGGER.info("Ontology {} loaded...", file.getName());
        LOGGER.info("Ontology: {}", getOntologyInfo(ontology));

        return ontology;
    }

    public static String getOntologyInfo(OWLOntology ontology) {
        if (ontology == null)
            return "NULL";
        else
            return String.valueOf(ontology.getOntologyID()) +
                    "\t Axioms: " + ontology.getAxiomCount() +
                    "\t LogicalAxioms: " + ontology.getLogicalAxiomCount() +
                    "\t Classes: " + ontology.getClassesInSignature().size() +
                    "\t ObjectProperties: " + ontology.getObjectPropertiesInSignature().size() +
                    "\t DataProperties: " + ontology.getDataPropertiesInSignature().size();
    }

    public static OWLOntology loadOntology(@Nonnull String fileName, @Nonnull OWLOntologyManager manager) {
        return loadOntology(new File(fileName), manager);
    }

    public static OWLOntology loadOntology(@Nonnull String fileName) {
        return loadOntology(new File(fileName));
    }

    public static List<File> getListOfOntologyFiles(@Nonnull String path) {
        File dir = new File(path);
        File[] files = dir.listFiles((dir1, name) -> !name.endsWith(CACHED_OWL_SUFFIX) && !name.endsWith(CACHED_ALIGNMENT_SUFFIX) && allowedExtensions.contains(FilenameUtils.getExtension(name)));
        Arrays.sort(files);
        return Arrays.asList(files);
    }

    public static String getCachedOntologyPath(String ontologyPath) {
        String basePath = FilenameUtils.getFullPath(ontologyPath);
        String name = FilenameUtils.getBaseName(ontologyPath);

        return FilenameUtils.concat(basePath, name + CACHED_OWL_SUFFIX);
    }

    public static String getCachedAlignmentPath(String ontologyPath, String targetNameSpace) {
        String basePath = FilenameUtils.getFullPath(ontologyPath);
        String name = FilenameUtils.getBaseName(ontologyPath);

        return FilenameUtils.concat(basePath, "alignments/" + name + "-" + targetNameSpace + CACHED_ALIGNMENT_SUFFIX);
    }

    public static Set<OWLAxiom> getClassDefinitionAxioms(@Nonnull OWLOntology ont, @Nonnull OWLEntity entity) {
        if (entity instanceof OWLClass)
            return new HashSet<>(ont.getAxioms((OWLClass) entity, Imports.INCLUDED));
        else if (entity instanceof OWLObjectProperty)
            return new HashSet<>(ont.getAxioms((OWLObjectPropertyExpression) entity, Imports.INCLUDED));
        else
            return Collections.EMPTY_SET;
    }

    public static boolean isSatisfiable(@Nonnull OWLReasoner reasoner, @Nonnull OWLEntity entity) {
        reasoner.flush();
        if (entity instanceof OWLClass)
            return !reasoner.getUnsatisfiableClasses().contains((OWLClass) entity);
        else if (entity instanceof OWLObjectProperty)
            return !reasoner.getEquivalentObjectProperties(OWLManager.getOWLDataFactory().getOWLBottomObjectProperty()).getEntitiesMinusBottom().contains(entity);
        else
            return true;
    }

    public static List<File> getListOfAlignmentFiles(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles((dir1, name) -> !name.endsWith(CACHED_ALIGNMENT_SUFFIX) && FilenameUtils.getExtension(name).equals(ALIGNMENT_EXTENSION));
        Arrays.sort(files);
        return Arrays.asList(files);
    }
}
