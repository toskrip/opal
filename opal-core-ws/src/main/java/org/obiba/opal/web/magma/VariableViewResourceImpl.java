/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.magma;

import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.Variable;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.views.View;
import org.obiba.magma.views.ViewManager;
import org.obiba.magma.views.support.VariableOperationContext;
import org.obiba.opal.core.ValueTableUpdateListener;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.model.Magma.VariableDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Component("variableViewResource")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Transactional
public class VariableViewResourceImpl extends VariableResourceImpl implements VariableViewResource {

  @Autowired
  private ViewManager viewManager;

  @Override
  public VariableDto get(UriInfo uriInfo) {
    UriBuilder uriBuilder = UriBuilder.fromPath("/");
    List<PathSegment> pathSegments = uriInfo.getPathSegments();
    for (int i = 0; i < 4; i++) {
      uriBuilder.segment(pathSegments.get(i).getPath());
    }
    String tableUri = uriBuilder.build().toString();
    Magma.LinkDto linkDto = Magma.LinkDto.newBuilder().setLink(tableUri).setRel(getValueTable().getName()).build();

    return Dtos.asDto(linkDto, getValueTable().getVariable(getName())).build();
  }

  @Override
  public Response createOrUpdateVariable(VariableDto variableDto, @Nullable String comment) {
    return createOrUpdateVariable(Dtos.fromDto(variableDto), comment);
  }

  @Override
  public Response deleteVariable() {
    View view = getValueTableAsView();
    try (ValueTableWriter.VariableWriter variableWriter = view.getListClause().createWriter()) {

      // Remove from listClause
      for (VariableValueSource v : view.getListClause().getVariableValueSources()) {
        Variable variable = v.getVariable();
        if (variable.getName().equals(getName())) {
          for (ValueTableUpdateListener listener : getTableListeners()) {
            listener.onDelete(getValueTable(), variable);
          }
          variableWriter.removeVariable(variable);
          VariableOperationContext operationContext = new VariableOperationContext();
          operationContext.deleteVariable(view, variable);
          viewManager.addView(getDatasource().getName(), view, "Remove " + getName(), operationContext);
          break;
        }
      }
    }

    return Response.ok().build();
  }

  @Override
  public Response updateVariableAttribute(String name, String namespace, String locale, String value) {
    // The variable must exist
    ValueTable table = getValueTable();
    Variable variable = table.getVariable(getName());
    return createOrUpdateVariable(updateVariableAttribute(variable, name, namespace, locale, value), "Update attribute");
  }

  @Override
  public Response deleteVariableAttribute(String name, String namespace, String locale) {
    // The variable must exist
    ValueTable table = getValueTable();
    if (!table.hasVariable(getName())) return Response.ok().build();
    Variable variable = table.getVariable(getName());
    Variable updatedVariable = deleteVariableAttribute(variable, name, namespace, locale);
    return updatedVariable == null ? Response.ok().build() : createOrUpdateVariable(updatedVariable, "Delete attribute");
  }

  //
  // Private methods
  //

  private Response createOrUpdateVariable(Variable updatedVariable, @Nullable String comment) {
    // The variable must exist
    ValueTable table = getValueTable();
    Variable variable = table.getVariable(getName());

    if (!variable.getEntityType().equals(updatedVariable.getEntityType())) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    View view = getValueTableAsView();
    VariableOperationContext operationContext = new VariableOperationContext();

    try (ValueTableWriter.VariableWriter variableWriter = view.getListClause().createWriter()) {

      // Rename existing variable
      if (!updatedVariable.getName().equals(variable.getName())) {
        operationContext.deleteVariable(view, variable);
        renameVariable(variable, updatedVariable.getName(), table, variableWriter);
      }
      operationContext.addVariable(view, updatedVariable);
      variableWriter.writeVariable(updatedVariable);
      viewManager.addView(getDatasource().getName(), view, comment, operationContext);
    }
    return Response.ok().build();
  }

  private void renameVariable(Variable variable, String newName, ValueTable table, ValueTableWriter.VariableWriter variableWriter) {
    for (ValueTableUpdateListener listener : getTableListeners()) {
      listener.onRename(table, variable, newName);
    }
    variableWriter.removeVariable(variable);
  }

  private View getValueTableAsView() {
    return viewManager.getView(getDatasource().getName(), getValueTable().getName());
  }
}
