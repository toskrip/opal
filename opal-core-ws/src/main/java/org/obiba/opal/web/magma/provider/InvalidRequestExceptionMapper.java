/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.web.magma.provider;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.obiba.opal.web.model.Ws.ClientErrorDto;
import org.obiba.opal.web.provider.ErrorDtoExceptionMapper;
import org.obiba.opal.web.support.InvalidRequestException;
import org.springframework.stereotype.Component;

import com.google.protobuf.GeneratedMessage;

@Component
@Provider
public class InvalidRequestExceptionMapper extends ErrorDtoExceptionMapper<InvalidRequestException> {

  @Override
  protected Status getStatus() {
    return Status.BAD_REQUEST;
  }

  @Override
  protected GeneratedMessage.ExtendableMessage<?> getErrorDto(InvalidRequestException exception) {
    return ClientErrorDto.newBuilder() //
        .setCode(getStatus().getStatusCode()) //
        .setStatus(exception.getMessage()) //
        .addAllArguments(exception.getMessageArgs()) //
        .build();
  }

}
