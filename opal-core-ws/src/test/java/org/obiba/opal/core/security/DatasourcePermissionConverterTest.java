/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.core.security;

import org.junit.Test;

/**
 *
 */
public class DatasourcePermissionConverterTest
    extends OpalPermissionConverterTest<DatasourcePermissionConverter.Permission> {

  @Test
  public void testDatasourceAll() {
    testConversion("/datasource/patate", DatasourcePermissionConverter.Permission.DATASOURCE_ALL, //
        "rest:/datasource/patate:*:GET/*", //
        "rest:/system/identifiers/mappings:GET", //
        "rest:/project/patate:GET:GET/*",//
        "rest:/files/projects/patate:GET:GET/*",//
        "rest:/files/projects/patate:POST:GET/*",//
        "rest:/files/projects/patate:PUT:GET/*");
  }

  @Test
  public void testCreateTable() {
    testConversion("/datasource/patate", DatasourcePermissionConverter.Permission.TABLE_ADD, //
        "rest:/datasource/patate/tables:GET:GET", //
        "rest:/datasource/patate/tables:POST:GET", //
        "rest:/datasource/patate/views:POST:GET", //
        "rest:/project/patate:GET:GET", //
        "rest:/project/patate/summary:GET:GET", //
        "rest:/project/patate/transient-datasources:POST", //
        "rest:/files/projects/patate:GET:GET/*", //
        "rest:/files/projects/patate:POST:GET/*",//
        "rest:/files/projects/patate:PUT:GET/*");
  }

  @Override
  protected SubjectPermissionConverter newConverter() {
    return new DatasourcePermissionConverter();
  }

}
