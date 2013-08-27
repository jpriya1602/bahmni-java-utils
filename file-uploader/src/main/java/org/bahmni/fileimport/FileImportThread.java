package org.bahmni.fileimport;

import org.apache.log4j.Logger;
import org.bahmni.csv.*;
import org.bahmni.fileimport.dao.ImportStatusDao;
import org.bahmni.fileimport.dao.JDBCConnectionProvider;

import java.io.File;
import java.sql.SQLException;

// Does the actual import. This includes basic validation, import and updating status tables..
class FileImportThread<T extends CSVEntity> implements Runnable {
    private String originalFileName;
    private File csvFile;
    private final EntityPersister<T> persister;
    private final Class csvEntityClass;
    private JDBCConnectionProvider jdbcConnectionProvider;
    private String uploadedBy;

    private static Logger logger = Logger.getLogger(FileImportThread.class);
    private Migrator migrator;

    public FileImportThread(String originalFileName, File csvFile, EntityPersister<T> persister, Class csvEntityClass, JDBCConnectionProvider jdbcConnectionProvider, String uploadedBy) {
        this.originalFileName = originalFileName;
        this.csvFile = csvFile;
        this.persister = persister;
        this.csvEntityClass = csvEntityClass;
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.uploadedBy = uploadedBy;
    }

    public void run() {
        try {
            getNewImportStatusDao().saveInProgress(originalFileName, csvFile, csvEntityClass.getSimpleName(), uploadedBy);

            migrator = new MigratorBuilder(csvEntityClass)
                    .readFrom(csvFile.getParent(), csvFile.getName())
                    .persistWith(persister)
                    .withMultipleValidators(5)
                    .withMultipleMigrators(5)
                    .build();
            MigrateResult migrateResult = migrator.migrate();
            getNewImportStatusDao().saveFinished(csvFile, csvEntityClass.getSimpleName(), migrateResult);

            logger.info("Migration was " + (migrateResult.hasFailed() ? "unsuccessful" : "successful"));
            logger.info("Stage : " + migrateResult.getStageName() + ". Success count : " + migrateResult.numberOfSuccessfulRecords() +
                    ". Fail count : " + migrateResult.numberOfFailedRecords());

        } catch (Exception e) {
            logger.error("There was an error during migration. " + e.getMessage(), e);
            try {
                getNewImportStatusDao().saveFatalError(csvFile, csvEntityClass.getSimpleName(), e);
            } catch (SQLException e1) {
                logger.error(e1);
            }
        } finally {
            ImportRegistry.unregister(csvFile);
        }
    }

    public void shutdown() {
        migrator.shutdown();
    }

    private ImportStatusDao getNewImportStatusDao() {
        return new ImportStatusDao(jdbcConnectionProvider);
    }
}