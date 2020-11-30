package com.spread;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class Handler implements RequestHandler<Object, String> {

    public String handleRequest(Object input, Context context) {
        final LambdaLogger logger = context.getLogger();
        Connection connection = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            final String API_DB_HOST = Optional.ofNullable(System.getenv("DB_HOST")).orElse("localhost");
            final String API_DB_DATABASE = Optional.ofNullable(System.getenv("DB_DATABASE")).orElse("spread");
            final String API_DB_PORT = Optional.ofNullable(System.getenv("DB_PORT")).orElse("3306");
            final String API_DB_USER = Optional.ofNullable(System.getenv("DB_USER")).orElse("root");
            final String API_DB_PASSWORD = Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse("Pa55w0rd");

            logger.log("[INFO] Connecting to database " + " DB_HOST=" + DB_HOST + "\n");

            connection = DriverManager.getConnection("jdbc:mysql://" +
                                                     API_DB_HOST + ":" + API_DB_PORT + "/" + API_DB_DATABASE +
                                                     "?createDatabaseIfNotExist=true",
                                                     API_DB_USER,
                                                     API_DB_PASSWORD);
            logger.log("[INFO] Database connection established \n");

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase("liquibase/db.changelog-master.xml",
                                                new ClassLoaderResourceAccessor(),
                                                database);

            liquibase.update(new Contexts(), new LabelExpression());
            liquibase.close();
            logger.log("[INFO] Done \n");
            return "Done";
        } catch (SQLException e) {
            logger.log("[ERROR] " + Optional.ofNullable(e.getMessage()).orElse("SQL exception") + "\n");
        } catch (LiquibaseException e) {
            logger.log("[ERROR] " + Optional.ofNullable(e.getMessage()).orElse("Liquibase exception") + "\n");
        } catch (ClassNotFoundException e) {
            logger.log("[ERROR] " + Optional.ofNullable(e.getMessage()).orElse("MySQL driver is missing") + "\n");
        } catch (Exception e)  {
            logger.log("[ERROR] " + Optional.ofNullable(e.getMessage()).orElse("Unknown exception") + "\n");
        } finally {
            try {
                if (!connection.isClosed() || connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                logger.log("[ERROR] Exception when closing db connection \n");
            }
        }
        System.exit(-1);
        return null;
    }
}
