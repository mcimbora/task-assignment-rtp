/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package taskassignment.business;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.instance.SolverInstance;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.SolverServicesClient;
import org.kie.server.client.impl.KieServicesClientImpl;
import org.kie.server.client.impl.KieServicesConfigurationImpl;
import org.kie.server.client.impl.SolverServicesClientImpl;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.solver.ProblemFactChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskassignment.app.CommonApp;
import taskassignment.domain.Customer;
import taskassignment.domain.Employee;
import taskassignment.domain.Skill;
import taskassignment.domain.Task;
import taskassignment.domain.TaskAssigningSolution;
import taskassignment.domain.TaskType;
import taskassignment.domain.solver.ConsumeTaskProblemFactChange;
import taskassignment.domain.solver.ProduceTaskProblemFactChange;
import taskassignment.persistence.AbstractSolutionExporter;
import taskassignment.persistence.AbstractSolutionImporter;
import taskassignment.persistence.SolutionDao;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolutionBusiness<Solution_> {

    private static final ProblemFileComparator FILE_COMPARATOR = new ProblemFileComparator();
    public static final String SERVER_URL = "http://localhost:8080/kie-server/services/rest/server";
    public static final String USERNAME = "planner";
    public static final String PASSWORD = "Planner123_";
    public static final String CONTAINER_ID = "task-assignment-rtp";
    public static final String SOLVER_ID = "rtp-solver";
    public static final String SOLVER_CONFIG_XML = "taskassignment/solver/taskAssigningSolverConfig.xml";
    public static final long CLIENT_TIMEOUT = 30_000L;

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final CommonApp app;
    private SolutionDao<Solution_> solutionDao;

    private Solution_ importedSolution;

    private KieServicesClientImpl kieServicesClient;
    private SolverServicesClient solverClient;

    private static final ReleaseId releaseId = new ReleaseId(
            "org.optaplanner",
            "task-assignment-rtp-domain",
            "1.0-SNAPSHOT");

    private AbstractSolutionImporter<Solution_>[] importers;
    private AbstractSolutionExporter<Solution_> exporter;

    private File importDataDir;
    private File unsolvedDataDir;
    private File solvedDataDir;
    private File exportDataDir;

    private String solutionFileName = null;

    public SolutionBusiness(CommonApp app) {
        this.app = app;

        KieServicesConfiguration kieServicesConfiguration = createKieServicesConfiguration();

        this.kieServicesClient = createKieServicesClient(kieServicesConfiguration);
        this.solverClient = createSolverServicesClient(kieServicesConfiguration,
                                                       kieServicesClient);

        createKieContainer();
        createSolver();
    }

    private KieServicesClientImpl createKieServicesClient(KieServicesConfiguration kieServicesConfiguration) {
        return (KieServicesClientImpl) KieServicesFactory.newKieServicesClient(kieServicesConfiguration);
    }

    private SolverServicesClient createSolverServicesClient(KieServicesConfiguration kieServicesConfiguration,
                                                            KieServicesClientImpl kieServicesClient) {
        SolverServicesClientImpl solverServicesClient = new SolverServicesClientImpl(kieServicesConfiguration);
        solverServicesClient.setOwner(kieServicesClient);
        return solverServicesClient;
    }

    private void createKieContainer() {
        KieContainerResource containerResource = new KieContainerResource(CONTAINER_ID,
                                                                          releaseId);
        ServiceResponse<KieContainerResource> containerInfo = kieServicesClient.getContainerInfo(CONTAINER_ID);

        if (containerInfo.getType() == ServiceResponse.ResponseType.FAILURE) {
            // the container might not exist, try to create one
            ServiceResponse<KieContainerResource> reply = kieServicesClient.createContainer(CONTAINER_ID,
                                                                                            containerResource);
            if (reply.getType().equals(ServiceResponse.ResponseType.FAILURE)) {
                throw new IllegalStateException("Exception while creating container '" + CONTAINER_ID + "', message: " + reply.getMsg());
            }
        }
    }

    private void createSolver() {
        try {
            SolverInstance solver = solverClient.getSolver(CONTAINER_ID,
                                                           SOLVER_ID);
            if (solver != null) {
                if (solver.getStatus() == SolverInstance.SolverStatus.SOLVING) {
                    solverClient.terminateSolverEarly(CONTAINER_ID, SOLVER_ID);
                }
                return;
            }
        } catch (KieServicesHttpException e) {
            // the solver might not exist, try to create one

        }
        solverClient.createSolver(CONTAINER_ID,
                                  SOLVER_ID,
                                  SOLVER_CONFIG_XML);
    }

    private KieServicesConfiguration createKieServicesConfiguration() {
        KieServicesConfiguration configuration = new KieServicesConfigurationImpl(SERVER_URL,
                                                                                  USERNAME,
                                                                                  PASSWORD,
                                                                                  CLIENT_TIMEOUT);
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("X-KIE-ContentType",
                       "xstream");
        headersMap.put("content-type",
                       "application/xml");
        configuration.setHeaders(headersMap);
        configuration.setMarshallingFormat(MarshallingFormat.XSTREAM);

        Set<Class<?>> extraClasses = new HashSet<>();
        extraClasses.add(Customer.class);
        extraClasses.add(Employee.class);
        extraClasses.add(Skill.class);
        extraClasses.add(Task.class);
        extraClasses.add(TaskAssigningSolution.class);
        extraClasses.add(TaskType.class);
        extraClasses.add(ConsumeTaskProblemFactChange.class);
        extraClasses.add(ProduceTaskProblemFactChange.class);
        configuration.addExtraClasses(extraClasses);

        return configuration;
    }

    public String getAppName() {
        return app.getName();
    }

    public String getAppDescription() {
        return app.getDescription();
    }

    public String getAppIconResource() {
        return app.getIconResource();
    }

    public void setSolutionDao(SolutionDao<Solution_> solutionDao) {
        this.solutionDao = solutionDao;
    }

    public AbstractSolutionImporter<Solution_>[] getImporters() {
        return importers;
    }

    public void setImporters(AbstractSolutionImporter<Solution_>[] importers) {
        this.importers = importers;
    }

    public void setExporter(AbstractSolutionExporter<Solution_> exporter) {
        this.exporter = exporter;
    }

    public String getDirName() {
        return solutionDao.getDirName();
    }

    public boolean hasImporter() {
        return importers.length > 0;
    }

    public boolean hasExporter() {
        return exporter != null;
    }

    public void updateDataDirs() {
        File dataDir = solutionDao.getDataDir();
        if (hasImporter()) {
            importDataDir = new File(dataDir,
                                     "import");
            if (!importDataDir.exists()) {
                throw new IllegalStateException("The directory importDataDir (" + importDataDir.getAbsolutePath()
                                                        + ") does not exist.");
            }
        }
        unsolvedDataDir = new File(dataDir,
                                   "unsolved");
        if (!unsolvedDataDir.exists()) {
            throw new IllegalStateException("The directory unsolvedDataDir (" + unsolvedDataDir.getAbsolutePath()
                                                    + ") does not exist.");
        }
        solvedDataDir = new File(dataDir,
                                 "solved");
        if (!solvedDataDir.exists() && !solvedDataDir.mkdir()) {
            throw new IllegalStateException("The directory solvedDataDir (" + solvedDataDir.getAbsolutePath()
                                                    + ") does not exist and could not be created.");
        }
        if (hasExporter()) {
            exportDataDir = new File(dataDir,
                                     "export");
            if (!exportDataDir.exists() && !exportDataDir.mkdir()) {
                throw new IllegalStateException("The directory exportDataDir (" + exportDataDir.getAbsolutePath()
                                                        + ") does not exist and could not be created.");
            }
        }
    }

    public File getImportDataDir() {
        return importDataDir;
    }

    public File getUnsolvedDataDir() {
        return unsolvedDataDir;
    }

    public File getSolvedDataDir() {
        return solvedDataDir;
    }

    public File getExportDataDir() {
        return exportDataDir;
    }

    public String getExportFileSuffix() {
        return exporter.getOutputFileSuffix();
    }

    public List<File> getUnsolvedFileList() {
        List<File> fileList = new ArrayList<>(
                FileUtils.listFiles(unsolvedDataDir,
                                    new String[]{solutionDao.getFileExtension()},
                                    true));
        Collections.sort(fileList,
                         FILE_COMPARATOR);
        return fileList;
    }

    public List<File> getSolvedFileList() {
        List<File> fileList = new ArrayList<>(
                FileUtils.listFiles(solvedDataDir,
                                    new String[]{solutionDao.getFileExtension()},
                                    true));
        Collections.sort(fileList,
                         FILE_COMPARATOR);
        return fileList;
    }

    public SolverInstance getSolverInstance() {
        SolverInstance solverInstance = solverClient.getSolverWithBestSolution(CONTAINER_ID, SOLVER_ID);
        return solverInstance;
    }

    public Solution_ getStartingSolution() {
        return importedSolution;
    }

    public String getSolutionFileName() {
        return solutionFileName;
    }

    public Score getScore() {
        return solverClient.getSolver(CONTAINER_ID,
                                      SOLVER_ID).getScoreWrapper().toScore();
    }

    public boolean isEveryProblemFactChangeProcessed() {
        return solverClient.isEveryProblemFactChangeProcessed(CONTAINER_ID,
                                                              SOLVER_ID);
    }

    public void importSolution(File file) {
        AbstractSolutionImporter<Solution_> importer = determineImporter(file);
        importedSolution = importer.readSolution(file);
        solutionFileName = file.getName();
    }

    private AbstractSolutionImporter<Solution_> determineImporter(File file) {
        for (AbstractSolutionImporter<Solution_> importer : importers) {
            if (importer.acceptInputFile(file)) {
                return importer;
            }
        }
        return importers[0];
    }

    public void openSolution(File file) {
        importedSolution = solutionDao.readSolution(file);
        solutionFileName = file.getName();
    }

    public void saveSolution(File file) {
        Solution_ solution = (Solution_) solverClient.getSolverWithBestSolution(CONTAINER_ID,
                                                                                SOLVER_ID).getBestSolution();
        solutionDao.writeSolution(solution,
                                  file);
    }

    public void exportSolution(File file) {
        Solution_ solution = (Solution_) solverClient.getSolverWithBestSolution(CONTAINER_ID,
                                                                                SOLVER_ID).getBestSolution();
        exporter.writeSolution(solution,
                               file);
    }

    public void doMove(Move<Solution_> move) {
        throw new UnsupportedOperationException();
    }

    public void doProblemFactChange(ProblemFactChange<Solution_> problemFactChange) {
        solverClient.addProblemFactChange(CONTAINER_ID,
                                          SOLVER_ID,
                                          problemFactChange);
    }

    public Solution_ solve(Solution_ planningProblem) {
        solverClient.solvePlanningProblem(CONTAINER_ID,
                                          SOLVER_ID,
                                          planningProblem);

        return (Solution_) solverClient.getSolverWithBestSolution(CONTAINER_ID,
                                                                  SOLVER_ID).getBestSolution();
    }

    public void terminateSolvingEarly() {
        SolverInstance solver = solverClient.getSolver(CONTAINER_ID,
                                                       SOLVER_ID);
        if (solver.getStatus() == SolverInstance.SolverStatus.SOLVING) {
            solverClient.terminateSolverEarly(CONTAINER_ID,
                                              SOLVER_ID);
        }
    }

    public void cleanup() {
        kieServicesClient.disposeContainer(CONTAINER_ID);
    }


    public void doChangeMove(Object entity,
                             String variableName,
                             Object toPlanningValue) {
        throw new UnsupportedOperationException();
    }
}
