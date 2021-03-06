/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.login.oauth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.olat.basesecurity.AuthHelper;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.MessageWindowController;
import org.olat.core.dispatcher.Dispatcher;
import org.olat.core.dispatcher.DispatcherModule;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.UserRequestImpl;
import org.olat.core.gui.control.ChiefController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.RedirectMediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.ThreadLocalUserActivityLoggerInstaller;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.login.oauth.model.OAuthRegistration;
import org.olat.login.oauth.model.OAuthUser;
import org.olat.login.oauth.ui.OAuthAuthenticationController;
import org.olat.user.UserManager;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Callback for OAuth 2
 * 
 * 
 * Initial date: 03.11.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OAuthDispatcher implements Dispatcher {
	
	private static final OLog log = Tracing.createLoggerFor(OAuthDispatcher.class);

	
	@Autowired
	private UserManager userManager;
	@Autowired
	private BaseSecurityManager securityManager;

	@Override
	public void execute(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		
		String uri = request.getRequestURI();
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertException("UTF-8 encoding not supported!!!!");
		}
		String uriPrefix = DispatcherModule.getLegacyUriPrefix(request);
		uri = uri.substring(uriPrefix.length());

		UserRequest ureq = null;
		try{
			//upon creation URL is checked for 
			ureq = new UserRequestImpl(uriPrefix, request, response);
		} catch(NumberFormatException nfe) {
			if(log.isDebug()){
				log.debug("Bad Request "+request.getPathInfo());
			}
			DispatcherModule.sendBadRequest(request.getPathInfo(), response);
			return;
		}

		String error = request.getParameter("error"); 
		if(null != error) { 
			error(ureq, translateOauthError(ureq, error));
			return; 
		}
		String problem = request.getParameter("oauth_problem");
		if(problem != null && "token_rejected".equals(problem.trim())) {
			error(ureq, translateOauthError(ureq, error));
			return; 
		}
		
		try {
			HttpSession sess = request.getSession();
			//OAuth 2.0 hasn't any request token
			Token requestToken = (Token)sess.getAttribute(OAuthConstants.REQUEST_TOKEN);
			OAuthService service = (OAuthService)sess.getAttribute(OAuthConstants.OAUTH_SERVICE);
			OAuthSPI provider = (OAuthSPI)sess.getAttribute(OAuthConstants.OAUTH_SPI);
			String verifier = request.getParameter("oauth_verifier"); 
			if(verifier == null) {//OAuth 2.0 as a code
				verifier = request.getParameter("code"); 
			}

			Token accessToken = service.getAccessToken(requestToken, new Verifier(verifier));
			OAuthUser infos = provider.getUser(service, accessToken);
			if(infos == null || !StringHelper.containsNonWhitespace(infos.getId())) {
				error(ureq, translate(ureq, "error.no.id"));
				return;
			}

			OAuthRegistration registration = new OAuthRegistration(provider.getProviderName(), infos);
			login(infos, registration);
			
			if(registration.getIdentity() == null) {
				if(CoreSpringFactory.getImpl(OAuthLoginModule.class).isAllowUserCreation()) {
					register(request, response, registration);
				} else {
					error(ureq, translate(ureq, "error.account.creation"));
				}
			} else {
				if(ureq.getUserSession() != null) {
					//re-init the activity logger
					ThreadLocalUserActivityLoggerInstaller.initUserActivityLogger(request);
				}
			
				Identity identity = registration.getIdentity();
				int loginStatus = AuthHelper.doLogin(identity, provider.getProviderName(), ureq);
				if (loginStatus != AuthHelper.LOGIN_OK) {
					if (loginStatus == AuthHelper.LOGIN_NOTAVAILABLE) {
						DispatcherModule.redirectToServiceNotAvailable(response);
					} else {
						// error, redirect to login screen
						DispatcherModule.redirectToDefaultDispatcher(response); 
					}
				} else {
					MediaResource mr = ureq.getDispatchResult().getResultingMediaResource();
					if (mr instanceof RedirectMediaResource) {
						RedirectMediaResource rmr = (RedirectMediaResource)mr;
						rmr.prepare(response);
					} else {
						DispatcherModule.redirectToDefaultDispatcher(response); // error, redirect to login screen
					}
				}
			}
		} catch (Exception e) {
			log.error("", e);
			error(ureq, translate(ureq, "error.generic"));
		}
	}
	
	private void login(OAuthUser infos, OAuthRegistration registration) {
		String id = infos.getId();
		//has an identifier
		Authentication auth = null;
		if(StringHelper.containsNonWhitespace(id)) {
			auth = securityManager.findAuthenticationByAuthusername(id, registration.getAuthProvider());
			if(auth == null) {
				String email = infos.getEmail();
				if(StringHelper.containsNonWhitespace(email)) {
					Identity identity = userManager.findIdentityByEmail(email);
					if(identity != null) {
						auth = securityManager.createAndPersistAuthentication(identity, registration.getAuthProvider(), id, null, null);
						registration.setIdentity(identity);
					}
				}
			} else {
				registration.setIdentity(auth.getIdentity());
			}
		}
	}
	
	private String translate(UserRequest ureq, String i18nKey) {
		Translator trans = Util.createPackageTranslator(OAuthAuthenticationController.class, ureq.getLocale());
		return trans.translate(i18nKey);
	}
	
	private String translateOauthError(UserRequest ureq, String error) {
		error = error == null ? null : error.trim();
		String message;
		if("access_denied".equals(error)) {
			message = translate(ureq, "error.access.denied");
		} else if("token_rejected".equals(error)) {
			message = translate(ureq, "error.token.rejected");
		} else if("invalid_grant".equals(error)) {
			message = translate(ureq, "error.invalid.grant");
			
		} else {
			message = translate(ureq, "error.generic");
		}
		return message;
	}
	
	private void error(UserRequest ureq, String message) {
		ChiefController msgcc = new MessageWindowController(ureq, message);
		msgcc.getWindow().dispatchRequest(ureq, true);
	}
	
	private void register(HttpServletRequest request, HttpServletResponse response, OAuthRegistration registration) {
		try {
			request.getSession().setAttribute("oauthRegistration", registration);
			response.sendRedirect(WebappHelper.getServletContextPath() + DispatcherModule.getPathDefault() + OAuthConstants.OAUTH_REGISTER_PATH + "/");
		} catch (IOException e) {
			log.error("Redirect failed: url=" + WebappHelper.getServletContextPath() + DispatcherModule.getPathDefault(),e);
		}
	}
}