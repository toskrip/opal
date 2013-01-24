/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.magma;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.obiba.magma.*;
import org.obiba.magma.support.VariableEntityBean;
import org.obiba.opal.search.IndexManager;
import org.obiba.opal.search.es.ElasticSearchProvider;
import org.obiba.opal.search.service.OpalSearchService;
import org.obiba.opal.web.finder.*;
import org.obiba.opal.web.model.Magma;
import org.obiba.opal.web.search.support.EsQueryExecutor;
import org.obiba.opal.web.search.support.IndexManagerHelper;
import org.obiba.opal.web.search.support.QueryTermJsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableEntityTablesResource extends AbstractTablesResource {

  private static final Logger log = LoggerFactory.getLogger(VariableEntityTablesResource.class);

  private final OpalSearchService opalSearchService;
  private final VariableEntityBean variableEntity;
  private final IndexManager indexManager;
  private final ElasticSearchProvider esProvider;

  public VariableEntityTablesResource(VariableEntityBean variableEntity,
                                      OpalSearchService opalSearchService,
                                      IndexManager indexManager,
                                      ElasticSearchProvider esProvider) {
    this.variableEntity = variableEntity;
    this.opalSearchService = opalSearchService;
    this.indexManager = indexManager;
    this.esProvider = esProvider;
  }

  @GET
  public List<Magma.TableDto> getTables() {

    FinderResult<List<Magma.TableDto>> results = new FinderResult<List<Magma.TableDto>>(new ArrayList<Magma.TableDto>());
    VariableEntityTablesFinder finder = new VariableEntityTablesFinder();
    finder.find(new VariableEntityTablesQuery(variableEntity), results);
    List<Magma.TableDto> dtoTables = results.getValue();

    return dtoTables;
  }


  public static class VariableEntityTablesQuery extends AbstractFinderQuery {

    private final VariableEntity entity;

    public VariableEntityTablesQuery(VariableEntity entity) {
      this.entity = entity;
    }

    public VariableEntity getEntity() {
      return entity;
    }
  }

  public static class EntityTablesFinder extends
    AbstractFinder<VariableEntityTablesQuery, FinderResult<List<Magma.TableDto>>> {

    @Override
    public void find(VariableEntityTablesQuery query, FinderResult<List<Magma.TableDto>> result) {

      for (Datasource datasource : MagmaEngine.get().getDatasources()) {
        for (ValueTable valueTable : datasource.getValueTables()) {

          if (valueTable.getEntityType().equals(query.getEntity().getType())) {
            query.getTableFilter().add(valueTable);
          }
        }
      }

      next(query, result);
    }

  }

  @SuppressWarnings("ClassTooDeepInInheritanceTree")
  public static class EntityTablesElasticSearchFinder extends
    AbstractElasticSearchFinder<VariableEntityTablesQuery, FinderResult<List<Magma.TableDto>>> {

    private final IndexManager indexManager;
    private final ElasticSearchProvider esProvider;

    public EntityTablesElasticSearchFinder(OpalSearchService opalSearchService,
                                           IndexManager indexManager,
                                           ElasticSearchProvider esProvider) {
      super(opalSearchService);
      this.indexManager = indexManager;
      this.esProvider = esProvider;
    }

    @Override
    public Boolean executeQuery(VariableEntityTablesQuery query,
                             FinderResult<List<Magma.TableDto>> result,
                             String... indexes) {

      Boolean querySucceeded = false;
      Map<String, ValueTable> map = buildIndexValueTableMap(query, new IndexManagerHelper(indexManager));

      if (!map.isEmpty()) {

        try {
          JSONObject jsonResponse =  executeEsQuery(query.getEntity().getIdentifier(), new ArrayList<String>(map.keySet()));
          // parse the jsonResponse and by using the map, create the required TableDtos
          log.debug("JSON ES Response {}", jsonResponse);
          JSONObject jsonHitsInfo = jsonResponse.getJSONObject("hits");

          if (jsonHitsInfo.getInt("total")> 0) {
            JSONArray jsonHits = jsonHitsInfo.getJSONArray("hits");
            int hitCount = jsonHits.length();

            for (int i = 0; i < hitCount; ++i) {
              String indexName = jsonHits.getJSONObject(i).getString("_type");
              ValueTable valueTable = map.get(indexName);

              if (valueTable != null) {
                Magma.TableDto tableDto =
                  Magma.TableDto.newBuilder()
                    .setDatasourceName(valueTable.getDatasource().getName())
                    .setName(valueTable.getName())
                    .setEntityType(valueTable.getEntityType())
                    .build();

                result.getValue().add(tableDto);
              }
            }

            querySucceeded = true;
          }

        } catch (JSONException e) {
          // will return false
        }

      }

      return querySucceeded;
    }

    private JSONObject executeEsQuery(String identifier, List<String> tableIndexNames) throws JSONException {
      QueryTermJsonBuilder.QueryTermsFiltersBuilder filtersBuilder =
        new QueryTermJsonBuilder.QueryTermsFiltersBuilder()
          .setFieldName("_type")
          .addFilterValues(tableIndexNames);

      QueryTermJsonBuilder queryBuilder =
        new QueryTermJsonBuilder()
          .setTermFieldName("_id")
          .setTermFieldValue(identifier)
          .setTermFilters(filtersBuilder.build());

      EsQueryExecutor queryExecutor = new EsQueryExecutor(esProvider);

      return queryExecutor.execute(queryBuilder.build());
    }

    private Map<String, ValueTable> buildIndexValueTableMap(VariableEntityTablesQuery query, IndexManagerHelper indexManagerHelper) {
      Map<String,ValueTable> map = new HashMap<String, ValueTable>();

      for(ValueTable valueTable : query.getTableFilter()) {
        if (indexManager.isIndexable(valueTable) && indexManager.getIndex(valueTable).isUpToDate()) {
          String tableIndexName =
            indexManagerHelper
              .setDatasource(valueTable.getDatasource().getName())
              .setTable(valueTable.getName())
              .getIndexName();

          map.put(tableIndexName, valueTable);
        }
      }

      return map;
    }

  }

  @SuppressWarnings("ClassTooDeepInInheritanceTree")
  public static class EntityTablesMagmaFinder extends
    AbstractMagmaFinder<VariableEntityTablesQuery, FinderResult<List<Magma.TableDto>>> {
    @Override
    public Boolean executeQuery(VariableEntityTablesQuery query,
                             FinderResult<List<Magma.TableDto>> result) {

      for (ValueTable valueTable : query.getTableFilter()) {

        if (valueTable.hasValueSet(query.getEntity())) {
//          ValueSet vs = valueTable.getValueSet(query.getEntity());
//          Iterable<Variable> variables = valueTable.getVariables();
//          for (Variable variable : variables) {
//            Value value = valueTable.getValue(variable, vs);
//            log.debug("value[{}]: {}", value.getValueType(), value.getValue());
//
//          }

          Magma.TableDto tableDto =
            Magma.TableDto.newBuilder()
              .setDatasourceName(valueTable.getDatasource().getName())
              .setName(valueTable.getName())
              .setEntityType(valueTable.getEntityType())
              .build();

          result.getValue().add(tableDto);
        }
      }

      return true;
    }
  }

  public class VariableEntityTablesFinder extends
    AbstractFinder<VariableEntityTablesQuery, FinderResult<List<Magma.TableDto>>> {

    @Override
    public void find(VariableEntityTablesQuery query, FinderResult<List<Magma.TableDto>> result) {
      nextFinder(new EntityTablesFinder()) //
        .nextFinder(new EntityTablesElasticSearchFinder(opalSearchService, indexManager, esProvider)) //
        .nextFinder(new EntityTablesMagmaFinder());
      next(query, result);
    }
  }

}
