package ir.ac.ui.ontodebugger;

import ir.ac.ui.ontodebugger.evaluation.AlignmentEvaluator;
import ir.ac.ui.ontodebugger.evaluation.DisjointnessEvaluator;
import ir.ac.ui.ontodebugger.evaluation.ErrorSetEvaluator;
import ir.ac.ui.ontodebugger.mups.BugFinderMethod;
import ir.ac.ui.ontodebugger.mups.MUPSFinderMethod;
import ir.ac.ui.ontodebugger.reasoner.ReasonerType;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Mehdi Teymourlouie <mehdi.teymourlouie@gmail.com>
 * Created on 10/27/15
 */
public class Configs {
    private static final Logger LOGGER = LogManager.getLogger(Configs.class);

    public static final String CONFIG_INI = "configs.properties";
    public static final String IRI_PREFIX = "http://dwsrg.ir/debugger";
    public static final String ARGS_DELIMITER = ",";

    private final Properties settings = new Properties();

    @Getter
    private DebuggingTaskType DEBUGGING_TASK = DebuggingTaskType.ONTOLOGY_DEBUGGING; //NOSONAR
    @Getter
    private String[] AXIOM_RANKER_METHODS = {"ProfileSupport+Shapley"}; // NOSONAR
    @Getter
    private MUPSFinderMethod MUPS_FINDER_METHOD = MUPSFinderMethod.FOURSTEPS;
    @Getter
    private BugFinderMethod BUG_FINDER_METHOD = BugFinderMethod.DF_HITSET;
    @Getter
    private boolean USE_GREEDY_ERROR_SEARCH = true; // NOSONAR
    @Getter
    private boolean PREPROCESS_ERRORS = false; // NOSONAR
    @Getter
    private boolean USE_CACHED_ONTOLOGY = true; // NOSONAR
    @Getter
    private boolean SAVE_CACHED_ONTOLOGY = true; // NOSONAR
    @Getter
    private boolean DEBUG_PROPERTIES = false; // NOSONAR
    @Getter
    private ReasonerType REASONER_TYPE = ReasonerType.HERMIT; // NOSONAR
    /* 0 means use default thread pool */
    @Getter
    private int NUMBER_OF_THREADS = 0; // NOSONAR
    @Getter
    private boolean SINGLE_THREAD_REASONING = false; // NOSONAR
    @Getter
    private boolean USE_MODULAR_ONTOLOGY_IN_BUG_FINDER = true; // NOSONAR
    @Getter
    private boolean CHECK_PROFILE_ONTOLOGY_SATISFIABILITY = true; // NOSONAR
    @Getter
    private boolean DEBUG_CLASSES = false; // NOSONAR
    @Getter
    private boolean USE_ALIGNMENTS = false; // NOSONAR
    @Getter
    private double ALIGNMENT_ACCEPTANCE_THRESHOLD = 0.9;  // NOSONAR
    @Getter
    private boolean ASSUME_PROFILE_AXIOMS_ARE_CORRECT = true; // NOSONAR
    @Getter
    private boolean ASSUME_ALIGNMENT_AXIOMS_ARE_CORRECT = false; // NOSONAR
    @Getter
    private String PROFILE_PATH = "./"; // NOSONAR
    @Getter
    private String ONTO_PATH = "./"; // NOSONAR
    @Getter
    private String ALIGN_PATH = null; //NOSONAR
    @Getter
    private List<String> ALIGN_EXCLUDED = new ArrayList<>(); // NOSONAR
    @Getter
    private File ONTO_EXTRA = null; // NOSONAR
    @Getter
    private String RESULT_PATH = ONTO_PATH; // NOSONAR
    @Getter
    private boolean EVALUATE_RESULTS = false; // NOSONAR
    @Getter
    private ErrorSetEvaluator EVALUATOR = null; // NOSONAR
    @Getter
    private boolean DEBUG_MERGED_ONTOLOGY = true; // NOSONAR
    @Getter
    private boolean DEBUG_ONTOLOGY_LONELY = true; // NOSONAR

    @Getter
    private boolean DIRECT_ERROR_DETECTION = false; // NOSONAR
    @Getter
    private boolean FIND_ROOT_ERRORS = true; // NOSONAR
    @Getter
    private boolean SYNC_RANDOM_MUPS_FIND = false; // NOSONAR

    private static Configs instance = new Configs();

    private Configs() {
        File configFile = new File(CONFIG_INI);
        if (!configFile.exists()) {
            LOGGER.info("{} is not found, continue with default settings...", CONFIG_INI);
        } else {
            try {
                settings.load(new FileReader(configFile));
                readConfigs();
            } catch (Exception e) {
                LOGGER.catching(e);
            }
        }
    }

    public static Configs getInstance() {
        return instance;
    }

    private String readString(String key, String defaultValue) {
        String value = settings.getProperty(key);
        return value != null ? value : defaultValue;
    }

    private int readInt(String key, int defaultValue) {
        String temp = settings.getProperty(key);
        int value = defaultValue;
        try {
            value = Integer.valueOf(temp);
        } catch (Exception ex) {
            LOGGER.catching(ex);
        }
        return value;
    }

    private double readDouble(String key, double defaultValue) {
        String temp = settings.getProperty(key);
        double value = defaultValue;
        if (temp != null) {
            try {
                value = Double.valueOf(temp);
            } catch (Exception ex) {
                LOGGER.catching(ex);
            }
        }
        return value;
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        String value = settings.getProperty(key);
        return value != null ? Boolean.valueOf(value) : defaultValue;
    }

    private void readConfigs() {

        String temp;

        temp = readString("DEBUG_TASK", "Ontology");
        if ("ontology".equalsIgnoreCase(temp)) {
            DEBUGGING_TASK = DebuggingTaskType.ONTOLOGY_DEBUGGING;
        } else if ("alignment".equalsIgnoreCase(temp)) {
            DEBUGGING_TASK = DebuggingTaskType.ALIGNMENT_DEBUGGING;
        } else {
            LOGGER.error("Debugging task {} is not supported!!!", temp);
        }

        temp = readString("AXIOM_RANKER_METHODS", "");
        AXIOM_RANKER_METHODS = temp.split(ARGS_DELIMITER);

        PROFILE_PATH = readString("PROFILE_PATH", PROFILE_PATH);

        ONTO_PATH = readString("ONTO_PATH", ONTO_PATH);

        ALIGN_PATH = readString("ALIGN_PATH", ALIGN_PATH);

        temp = readString("ALIGN_EXCLUDED", null);
        if (temp != null) {
            final String[] excludedFiles = temp.split(ARGS_DELIMITER);
            ALIGN_EXCLUDED.clear();
            for (String excludedFile : excludedFiles) {
                ALIGN_EXCLUDED.add(excludedFile.trim());
            }
        }

        temp = readString("ONTO_EXTRA", null);
        if (temp != null) {
            ONTO_EXTRA = new File(temp);
        }

        RESULT_PATH = readString("RESULT_PATH", ONTO_PATH);

        EVALUATE_RESULTS = readBoolean("EVALUATE_RESULTS", EVALUATE_RESULTS);

        temp = readString("EVALUATOR", null);
        String args = readString("EVALUATOR_ARGS", "");
        if (EVALUATE_RESULTS && temp != null) {
            if ("disjointness".equalsIgnoreCase(temp)) {
                EVALUATOR = new DisjointnessEvaluator(args);
            } else if ("alignment".equalsIgnoreCase(temp)) {
                EVALUATOR = new AlignmentEvaluator(args);
            } else {
                LOGGER.error("Evaluator type \"{}\" is not supported", temp);
                EVALUATOR = null;
            }
        }

        USE_GREEDY_ERROR_SEARCH = readBoolean("USE_GREEDY_ERROR_SEARCH", USE_GREEDY_ERROR_SEARCH);

        PREPROCESS_ERRORS = readBoolean("PREPROCESS_ERRORS", PREPROCESS_ERRORS);

        USE_CACHED_ONTOLOGY = readBoolean("USE_CACHED_ONTOLOGY", USE_CACHED_ONTOLOGY);

        SAVE_CACHED_ONTOLOGY = readBoolean("SAVE_CACHED_ONTOLOGY", SAVE_CACHED_ONTOLOGY);

        DEBUG_PROPERTIES = readBoolean("DEBUG_PROPERTIES", DEBUG_PROPERTIES);

        temp = readString("REASONER", "Pellet");
        if (temp != null) {
            temp = temp.trim();
            if ("Pellet".equalsIgnoreCase(temp)) {
                REASONER_TYPE = ReasonerType.PELLET;
            } else if ("Hermit".equalsIgnoreCase(temp)) {
                REASONER_TYPE = ReasonerType.HERMIT;
            } else if ("Fact++".equalsIgnoreCase(temp)) {
                REASONER_TYPE = ReasonerType.FACTPLUSPLUS;
            } else {
                LOGGER.error("Reasoner type \"{}\" is not supported", temp);
            }
        }

        temp = readString("MUPS_FINDER_METHOD", "FourStep");
        if (temp != null) {
            temp = temp.trim();
            if ("FiveStep".equalsIgnoreCase(temp)) {
                MUPS_FINDER_METHOD = MUPSFinderMethod.FIVESTEPS;
            } else if ("FourStep".equalsIgnoreCase(temp)) {
                MUPS_FINDER_METHOD = MUPSFinderMethod.FOURSTEPS;
            } else if ("SWOOP".equalsIgnoreCase(temp)) {
                MUPS_FINDER_METHOD = MUPSFinderMethod.SWOOP;
            } else {
                LOGGER.error("MUPS finder method \"{}\" is not supported", temp);
            }
        }

        temp = readString("BUG_FINDER_METHOD", "HitSet");
        if (temp != null) {
            temp = temp.trim();
            if ("DFHitSet".equalsIgnoreCase(temp)) {
                BUG_FINDER_METHOD = BugFinderMethod.DF_HITSET;
            } else if ("PDFHitSet".equalsIgnoreCase(temp)) {
                BUG_FINDER_METHOD = BugFinderMethod.PARALLEL_DF_HITSET;
            } else if ("HitSet".equalsIgnoreCase(temp)) {
                BUG_FINDER_METHOD = BugFinderMethod.HITSET;
            } else if ("PHitSet".equalsIgnoreCase(temp)) {
                BUG_FINDER_METHOD = BugFinderMethod.PARALLEL_HITSET;
            } else if ("HitSetPlus".equalsIgnoreCase(temp)) {
                BUG_FINDER_METHOD = BugFinderMethod.HITSET_PLUS;
            } else if ("PHitSetPlus".equalsIgnoreCase(temp)) {
                BUG_FINDER_METHOD = BugFinderMethod.PARALLEL_HITSET_PLUS;
            } else {
                LOGGER.error("Bug finder method \"{}\" is not supported", temp);
            }
        }

        NUMBER_OF_THREADS = readInt("NUMBER_OF_THREADS", NUMBER_OF_THREADS);

        SINGLE_THREAD_REASONING = readBoolean("SINGLE_THREAD_REASONING", SINGLE_THREAD_REASONING);

        USE_MODULAR_ONTOLOGY_IN_BUG_FINDER = readBoolean("USE_MODULAR_ONTOLOGY_IN_BUG_FINDER", USE_MODULAR_ONTOLOGY_IN_BUG_FINDER);

        CHECK_PROFILE_ONTOLOGY_SATISFIABILITY = readBoolean("CHECK_PROFILE_ONTOLOGY_SATISFIABILITY", CHECK_PROFILE_ONTOLOGY_SATISFIABILITY);

        DEBUG_CLASSES = readBoolean("DEBUG_CLASSES", DEBUG_CLASSES);

        USE_ALIGNMENTS = readBoolean("USE_ALIGNMENTS", USE_ALIGNMENTS);
        DEBUG_MERGED_ONTOLOGY = readBoolean("DEBUG_MERGED_ONTOLOGY", DEBUG_MERGED_ONTOLOGY);
        DEBUG_ONTOLOGY_LONELY = readBoolean("DEBUG_ONTOLOGY_LONELY", DEBUG_ONTOLOGY_LONELY);

        ALIGNMENT_ACCEPTANCE_THRESHOLD = readDouble("ALIGNMENT_ACCEPTANCE_THRESHOLD", ALIGNMENT_ACCEPTANCE_THRESHOLD);

        ASSUME_PROFILE_AXIOMS_ARE_CORRECT = readBoolean("ASSUME_PROFILE_AXIOMS_ARE_CORRECT", ASSUME_PROFILE_AXIOMS_ARE_CORRECT);

        ASSUME_ALIGNMENT_AXIOMS_ARE_CORRECT = readBoolean("ASSUME_ALIGNMENT_AXIOMS_ARE_CORRECT", ASSUME_ALIGNMENT_AXIOMS_ARE_CORRECT);

        DIRECT_ERROR_DETECTION = readBoolean("DIRECT_ERROR_DETECTION", DIRECT_ERROR_DETECTION);

        FIND_ROOT_ERRORS = readBoolean("FIND_ROOT_ERRORS", FIND_ROOT_ERRORS);

        SYNC_RANDOM_MUPS_FIND = readBoolean("SYNC_RANDOM_MUPS_FIND", SYNC_RANDOM_MUPS_FIND);
    }

}
