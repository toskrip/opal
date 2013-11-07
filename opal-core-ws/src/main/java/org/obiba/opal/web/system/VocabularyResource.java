/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.system;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;

import org.obiba.opal.core.cfg.NoSuchTaxonomyException;
import org.obiba.opal.core.cfg.NoSuchVocabularyException;
import org.obiba.opal.core.cfg.TaxonomyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;

import au.com.bytecode.opencsv.CSVReader;

public class VocabularyResource {

  private final String taxonomyName;

  private final String vocabularyName;

  private final TaxonomyService taxonomyService;

  public VocabularyResource(TaxonomyService taxonomyService, String taxonomyName, String vocabularyName) {
    this.taxonomyService = taxonomyService;
    this.taxonomyName = taxonomyName;
    this.vocabularyName = vocabularyName;
  }

  @GET
  public Response getVocabulary() {
    Taxonomy taxonomy = taxonomyService.getTaxonomy(taxonomyName);

    if(taxonomy == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    Vocabulary vocabulary = taxonomyService.getVocabulary(taxonomyName, vocabularyName);
    if(vocabulary == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.ok().entity(Dtos.asDto(vocabulary)).build();
  }

  @POST
  @Consumes(value = "text/plain")
  public Response addVocabularyTerms(String csv) {
    Taxonomy taxonomy = taxonomyService.getTaxonomy(taxonomyName);

    if(taxonomy == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    Vocabulary vocabulary = taxonomyService.getVocabulary(taxonomyName, vocabularyName);
    if(vocabulary == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    vocabulary.getTerms().clear();

    try {
      parseStringAsCsv(csv, vocabulary);
    } catch(IOException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    taxonomyService.saveVocabulary(null, vocabulary);

    return Response.ok().build();
  }

  private void parseStringAsCsv(String csv, Vocabulary vocabulary) throws IOException {// Parse csv and add terms
    CSVReader reader = new CSVReader(new StringReader(csv));
    List<String[]> lines = reader.readAll();

    Term t = null;
    for(String[] terms : lines) {

      int level = terms.length - 1;
      if(level == 0) {
        if(t != null) {
          vocabulary.getTerms().add(t);
        }

        t = new Term(terms[0]);
      } else {
        Term parent = t;

        for(int i = 1; i < level; i++) {
          // find parent
          if(parent != null) {
            parent = parent.getTerms().get(parent.getTerms().size() - 1);
          }
        }

        // Add new term
        if(parent != null) {
          parent.getTerms().add(new Term(terms[terms.length - 1]));
        }
      }
    }
  }

  @PUT
  public Response saveVocabulary(Opal.VocabularyDto dto) {

    try {

      Vocabulary vocabulary = taxonomyService.getVocabulary(taxonomyName, vocabularyName);
      taxonomyService.saveVocabulary(vocabulary, Dtos.fromDto(dto));
    } catch(NoSuchTaxonomyException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch(NoSuchVocabularyException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.ok().build();
  }
}

