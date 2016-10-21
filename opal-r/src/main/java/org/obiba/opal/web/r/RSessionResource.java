/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.r;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.obiba.opal.r.service.OpalRSession;
import org.obiba.opal.web.model.OpalR;

public interface RSessionResource {

  void setOpalRSession(OpalRSession rSession);

  @GET
  OpalR.RSessionDto getRSession();

  /**
   * Destroy the R session and optionally save the associated workspace.
   * @param saveId
   * @return
   */
  @DELETE
  Response removeRSession(@QueryParam("save") String saveId);

  @PUT
  @Path("/current")
  Response setCurrentRSession();

  @GET
  @Path("/symbols")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  Response ls();

  @POST
  @Path("/symbols")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Response assign(MultivaluedMap<String, String> symbols);

  @Path("/symbol/{name}")
  RSymbolResource getRSymbolResource(@PathParam("name") String name);

  @GET
  @Path("/commands")
  List<OpalR.RCommandDto> getRCommands();

  @GET
  @Path("/command/{rid}")
  OpalR.RCommandDto getRCommand(@PathParam("rid") String rid, @QueryParam("wait") @DefaultValue("false") boolean wait);

  @DELETE
  @Path("/command/{rid}")
  Response removeRCommand(@PathParam("rid") String rid);

  @GET
  @Path("/command/{rid}/result")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  Response getRCommandResult(@PathParam("rid") String rid, @QueryParam("rm") @DefaultValue("true") boolean remove,
      @QueryParam("wait") @DefaultValue("false") boolean wait);

  @POST
  @Path("/workspaces")
  Response saveWorkspace(@QueryParam("save") String saveId);
}
