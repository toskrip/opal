/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.obiba.opal.core.runtime.OpalRuntime;
import org.obiba.opal.core.service.IdentifiersTableService;
import org.obiba.opal.core.service.ImportService;
import org.obiba.opal.core.unit.FunctionalUnit;
import org.obiba.opal.core.unit.FunctionalUnitService;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.model.Opal.FunctionalUnitDto;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class FunctionalUnitsResourceTest {

  private OpalRuntime opalRuntimeMock;

  private IdentifiersTableService identifiersTableResolverMock;

  private FunctionalUnitService functionalUnitServiceMock;

  private Set<FunctionalUnit> functionalUnits;

  @Before
  public void setUp() {
    opalRuntimeMock = createMock(OpalRuntime.class);
    functionalUnitServiceMock = createMock(FunctionalUnitService.class);
    identifiersTableResolverMock = createMock(IdentifiersTableService.class);

    functionalUnits = new HashSet<FunctionalUnit>();
    functionalUnits.add(new FunctionalUnit("unit1", "key1"));
    functionalUnits.add(new FunctionalUnit("unit2", "key2"));
    functionalUnits.add(new FunctionalUnit("unit3", "key3"));

  }

  @Test
  public void testGetFunctionalUnits() {

    expect(functionalUnitServiceMock.getFunctionalUnits()).andReturn(functionalUnits).once();

    replay(opalRuntimeMock, functionalUnitServiceMock);

    ImportService importService = createMock(ImportService.class);

    FunctionalUnitsResource functionalUnitsResource = new FunctionalUnitsResource();
    functionalUnitsResource.setFunctionalUnitService(functionalUnitServiceMock);
    functionalUnitsResource.setOpalRuntime(opalRuntimeMock);
    functionalUnitsResource.setImportService(importService);
    functionalUnitsResource.setIdentifiersTableService(identifiersTableResolverMock);

    List<Opal.FunctionalUnitDto> functionalUnitDtoList = functionalUnitsResource.getFunctionalUnits();
    Assert.assertTrue(functionalUnitDtoList.size() == 3);

    for(FunctionalUnitDto functionalUnitDto : functionalUnitDtoList) {
      Assert.assertTrue(functionalUnitDto.getName().startsWith("unit"));
      Assert.assertTrue(functionalUnitDto.getKeyVariableName().startsWith("key"));
    }

    verify(opalRuntimeMock, functionalUnitServiceMock);

  }
}
