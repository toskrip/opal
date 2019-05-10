/*
 * Copyright (c) 2019 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.obiba.opal.server.httpd;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.obiba.oidc.OIDCConfigurationProvider;
import org.obiba.oidc.OIDCCredentials;
import org.obiba.oidc.OIDCStateManager;
import org.obiba.oidc.shiro.authc.OIDCAuthenticationToken;
import org.obiba.oidc.web.filter.OIDCCallbackFilter;
import org.obiba.shiro.web.filter.AuthenticationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Component("opalCallbackFilter")
public class OpalCallbackFilter extends OIDCCallbackFilter {

  private static final Logger log = LoggerFactory.getLogger(OpalCallbackFilter.class);

  @Autowired
  private OIDCConfigurationProvider oidcConfigurationProvider;

  @Autowired
  private OIDCStateManager oidcStateManager;

  @Autowired
  private AuthenticationExecutor authenticationExecutor;

  @Value("${org.obiba.opal.public.url}")
  private String opalPublicUrl;

  @PostConstruct
  public void init() {
    setOIDCConfigurationProvider(oidcConfigurationProvider);
    setOIDCStateManager(oidcStateManager);
    setDefaultRedirectURL(opalPublicUrl);
    String callbackUrl = opalPublicUrl + (opalPublicUrl.endsWith("/") ? "" : "/") + "auth/callback/";
    setCallbackURL(callbackUrl);
  }

  @Override
  protected void onAuthenticationSuccess(OIDCCredentials credentials, HttpServletResponse response) {
    Subject subject = authenticationExecutor.login(new OIDCAuthenticationToken(credentials));
    if (subject != null) {
      Session session = subject.getSession();
      log.trace("Binding subject {} session {} to executing thread {}", subject.getPrincipal(), session.getId(), Thread.currentThread().getId());
      ThreadContext.bind(subject);
      session.touch();
      int timeout = (int) (session.getTimeout() / 1000);
      Cookie cookie = new Cookie("opalsid", session.getId().toString());
      cookie.setPath("/");
      response.addCookie(cookie);
      log.debug("Successfully authenticated subject {}", SecurityUtils.getSubject().getPrincipal());
    }
  }

  public static class Wrapper extends DelegatingFilterProxy {
    public Wrapper() {
      super("opalCallbackFilter");
    }
  }

}
