package io.hops.hopsworks.expat.migrations.kubernetes;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.expat.db.DbConnectionFactory;
import io.hops.hopsworks.expat.kubernetes.KubernetesClientFactory;
import io.hops.hopsworks.expat.migrations.MigrateStep;
import io.hops.hopsworks.expat.migrations.MigrationException;
import io.hops.hopsworks.expat.migrations.RollbackException;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateNS implements MigrateStep {

  private static Logger LOGGER = Logger.getLogger(CreateNS.class.getName());

  @Override
  public void migrate() throws MigrationException {
    KubernetesClient client;
    try {
      client = KubernetesClientFactory.getClient();
    } catch (ConfigurationException e) {
      throw new MigrationException("Cannot read the configuration", e);
    }

    Connection dbConn;
    Statement stmt = null;
    ResultSet resultSet = null;
    try {
      dbConn = DbConnectionFactory.getConnection();
      stmt = dbConn.createStatement();
      resultSet = stmt.executeQuery("SELECT projectname FROM project");

      while (resultSet.next()) {
        String projectName = resultSet.getString(1);
        String nsName = projectName.replace("_", "-");
        try {
          client.namespaces().createOrReplaceWithNew()
              .withNewMetadata()
              .withName(nsName)
              .endMetadata()
              .done();
          LOGGER.log(Level.INFO, "Namespace " + nsName + " created for project: " + projectName);
        } catch (KubernetesClientException e) {
          LOGGER.log(Level.SEVERE, "Could not create Namespace " + nsName + " for project: " + projectName, e);
        }

      }
    } catch (SQLException | ConfigurationException e) {
      throw new MigrationException("Cannot fetch the list of projects from the database", e);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Nothing to do here.
        }
      }

      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException e) {
          // Nothing to do here.
        }
      }
    }
  }

  @Override
  public void rollback() throws RollbackException {
    KubernetesClient client;
    try {
      client = KubernetesClientFactory.getClient();
    } catch (ConfigurationException e) {
      throw new RollbackException("Cannot read the configuration", e);
    }

    Connection dbConn;
    Statement stmt = null;
    ResultSet resultSet = null;
    try {
      dbConn = DbConnectionFactory.getConnection();
      stmt = dbConn.createStatement();
      resultSet = stmt.executeQuery("SELECT projectname FROM project");

      while (resultSet.next()) {
        String projectName = resultSet.getString(1);
        String nsName = projectName.replace("_", "-");
        try {
          Namespace ns = new NamespaceBuilder()
              .withNewMetadata()
              .withName(nsName)
              .endMetadata()
              .build();

          client.namespaces().delete(ns);
          LOGGER.log(Level.INFO, "Namespace " + nsName + " deleted for project: " + projectName);
        } catch (KubernetesClientException e) {
          LOGGER.log(Level.SEVERE, "Could not delete Namespace " + nsName + " for project: " + projectName, e);
        }

      }
    } catch (SQLException | ConfigurationException e) {
      throw new RollbackException("Cannot fetch the list of projects from the database", e);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Nothing to do here.
        }
      }

      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException e) {
          // Nothing to do here.
        }
      }
    }
  }
}