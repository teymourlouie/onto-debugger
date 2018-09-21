package ir.ac.ui.ontodebugger;

import ir.ac.ui.ontodebugger.util.ConfigLogger;
import ir.ac.ui.ontodebugger.util.OntologyHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 */
public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    private Main() {
    }


    public static void main(String... args) throws Exception {
        LOGGER.debug("Application Started...");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.catching(e));

        OntoDebugger debugger = new OntoDebugger(Configs.getInstance().getPROFILE_PATH());

        if (args.length == 1) {
            debugFile(debugger, new File(args[0]));
        } else {
            File filePath = new File(Configs.getInstance().getONTO_PATH());
            if (!filePath.exists()) {
                LOGGER.error("ONTO_PATH is not existed!");
                return;
            }


            if (filePath.isFile()) { //debug single ontologies
                debugFile(debugger, filePath);
                //generateRandomOntology(filePath);
            } else if (filePath.isDirectory()) { // debug directory of ontologies
                final List<File> ontologyFiles = OntologyHelper.getListOfOntologyFiles(filePath.getPath());
                LOGGER.info("Debugging {} files...", ontologyFiles.size());
                ontologyFiles.stream().sorted((o1, o2) -> Long.compare(o1.length(), o2.length())).forEachOrdered(file -> debugFile(debugger, file));
            }
        }

    }

    private static void debugFile(OntoDebugger debugger, File file) {
        try {
            ConfigLogger.reconfigureResultFile(FilenameUtils.concat(Configs.getInstance().getRESULT_PATH(), FilenameUtils.getBaseName(file.getName())));
            LOGGER.info("Debugging ontology {}", file.getName());
            debugger.debug(file.getPath());
        } catch (Exception ex) {
            LOGGER.catching(ex);
        }

    }

    private static void generateRandomOntology(File file) {
        final OWLOntology ontology = OntologyHelper.loadOntology(file);
        final OWLOntologyManager m = ontology.getOWLOntologyManager();

        for (int i = 1; i < 10; i++) {
            Random random = new Random(System.currentTimeMillis());
            final double chance = 0.1 * i;
            final OWLOntology ont;
            try {
                ont = m.createOntology(ontology.getAxioms().stream().filter(owlAxiom -> random.nextDouble() <= chance).collect(Collectors.toSet()), IRI.create("http://dwsrg.ir/debugger/learned-disjointness/"));
                ont.saveOntology(new TurtleDocumentFormat(), new FileOutputStream(FilenameUtils.concat(FilenameUtils.getFullPath(file.getPath()), i + "-random-" + String.format("%.2f", chance) + ".n3")));
                m.removeOntology(ont);
            } catch (Exception ex) {
                LOGGER.catching(ex);
            }
        }

    }
}
