/*
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.core.runtime.upgrade;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import javax.sql.DataSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.Value;
import org.obiba.magma.ValueSequence;
import org.obiba.magma.type.BinaryType;
import org.obiba.magma.type.TextType;
import org.obiba.opal.core.cfg.OpalConfiguration;
import org.obiba.opal.core.runtime.jdbc.DataSourceFactory;
import org.obiba.opal.core.runtime.jdbc.JdbcDataSource;
import org.obiba.opal.core.runtime.support.OpalConfigurationProvider;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.AbstractUpgradeStep;
import org.obiba.runtime.upgrade.support.jdbc.SqlScriptUpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import static org.obiba.opal.core.runtime.jdbc.DefaultJdbcDataSourceRegistry.JdbcDataSourcesConfig;

/**
 *
 */
public class BinariesStorageUpgradeStep extends AbstractUpgradeStep {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private OpalConfigurationProvider opalConfigurationProvider;

  private DataSource opalDataSource;

  private DataSourceFactory dataSourceFactory;

  private SqlScriptUpgradeStep sqlScriptUpgradeStep;

  @Override
  public void execute(Version currentVersion) {

    Collection<DataSource> dataSources = new ArrayList<DataSource>();
    dataSources.add(opalDataSource);

    OpalConfiguration configuration = opalConfigurationProvider.readOpalConfiguration();
    JdbcDataSourcesConfig dataSourcesConfig = configuration.getExtension(JdbcDataSourcesConfig.class);
    for(JdbcDataSource jdbcDataSource : dataSourcesConfig.getDatasources()) {
      log.debug("Found datasource {}", jdbcDataSource.getUrl());
      dataSources.add(dataSourceFactory.createDataSource(jdbcDataSource));
    }

    for(DataSource dataSource : dataSources) {
      log.debug("Process dataSource {}", dataSource);
      if(hasHibernateDatasource(dataSource)) {
        log.debug("Process Hibernate Datasource for {}", dataSource);
        sqlScriptUpgradeStep.setDataSource(dataSource);
        try {
          sqlScriptUpgradeStep.initialize();
        } catch(IOException e) {
          throw new RuntimeException("Cannot upgrade schema for binaries storage", e);
        }
        sqlScriptUpgradeStep.execute(currentVersion);
        moveBinary(dataSource);
      }
    }
  }

  private boolean hasHibernateDatasource(DataSource dataSource) {
    try {
      DatabaseMetaData meta = dataSource.getConnection().getMetaData();
      ResultSet res = meta.getTables(null, null, null, new String[] {"TABLE"});
      while(res.next()) {
        if("value_set_value".equalsIgnoreCase(res.getString("TABLE_NAME"))) return true;
      }
    } catch(SQLException e) {
      log.error("Cannot check if database has an HibernateDatasource", e);
    }
    return false;
  }

  private void moveBinary(DataSource dataSource) {

    try {

      final JdbcTemplate template = new JdbcTemplate(dataSource);
      template.query("SELECT id, created, updated, is_sequence FROM value_set_value WHERE value_type = ?",
          new Object[] {"binary"}, new RowCallbackHandler() {

        @Override
        public void processRow(ResultSet rs) throws SQLException {
          int valueSetValueId = rs.getInt("id");
          String stringValue = template
              .queryForObject("SELECT value FROM value_set_value WHERE id = ?", new Object[] {valueSetValueId},
                  String.class);
          Value newValue = null;
          if(rs.getBoolean("is_sequence")) {
            ValueSequence valueSequence = BinaryType.get().sequenceOf(stringValue);
            if(valueSequence.isNull()) {
              newValue = TextType.get().nullSequence();
            } else {
              Collection<Value> newValues = new ArrayList<Value>(valueSequence.getSize());
              int occurrence = 0;
              for(Value value : valueSequence.getValue()) {
                newValues.add(writeBinaries(valueSetValueId, occurrence++, value, template));
              }
              newValue = TextType.get().sequenceOf(newValues);
            }
          } else {
            Value value = BinaryType.get().valueOf(stringValue);
            newValue = writeBinaries(valueSetValueId, 0, value, template);
          }

          template.update("UPDATE value_set_value SET value = ? WHERE id = ?", newValue.getValue(), valueSetValueId);
        }

        private Value writeBinaries(int valueSetValueId, int occurrence, Value value, JdbcTemplate template) {
          Value newValue;
          if(value.isNull()) {
            newValue = TextType.get().nullValue();
          } else {
            byte[] binary = (byte[]) value.getValue();
            int size = binary.length;
            newValue = getBinaryMetadata(size);
            template.update(
                "INSERT INTO value_set_binary_value(occurrence, size, value, value_set_value_id) VALUES (?, ?, ?, ?)",
                occurrence, size, value, valueSetValueId);
          }
          return newValue;
        }

        private Value getBinaryMetadata(int size) {
          try {
            JSONObject properties = new JSONObject();
            properties.put("size", size);
            return TextType.get().valueOf(properties.toString());
          } catch(JSONException e) {
            throw new MagmaRuntimeException(e);
          }
        }
      });

    } finally {
      MagmaEngine.get().shutdown();
    }
  }

  public void setOpalDataSource(DataSource opalDataSource) {
    this.opalDataSource = opalDataSource;
  }

  public void setOpalConfigurationProvider(OpalConfigurationProvider opalConfigurationProvider) {
    this.opalConfigurationProvider = opalConfigurationProvider;
  }

  public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
    this.dataSourceFactory = dataSourceFactory;
  }

  public void setSqlScriptUpgradeStep(SqlScriptUpgradeStep sqlScriptUpgradeStep) {
    this.sqlScriptUpgradeStep = sqlScriptUpgradeStep;
  }
}
