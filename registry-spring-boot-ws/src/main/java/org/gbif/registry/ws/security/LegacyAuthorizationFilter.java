package org.gbif.registry.ws.security;

import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.ws.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.gbif.ws.util.CommonWsUtils.getFirst;

/**
 * A filter that will intercept legacy web service requests to /registry/* and perform authentication setting
 * a security context on the request.
 */
@Component
public class LegacyAuthorizationFilter extends GenericFilterBean {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyAuthorizationFilter.class);

  private final LegacyAuthorizationService legacyAuthorizationService;

  public LegacyAuthorizationFilter(LegacyAuthorizationService legacyAuthorizationService) {
    this.legacyAuthorizationService = legacyAuthorizationService;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getRequestURI().toLowerCase();

    // is it a legacy web service request?
    if (path.contains("registry/")) {
      // is it a GET request requiring authorization?
      if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {

        // E.g. validate organization request, identified by param op=login
        if (getFirst(httpRequest.getParameterMap(), "op") != null) {
          if (getFirst(httpRequest.getParameterMap(), "op").equalsIgnoreCase("login")) {
            UUID organizationKey = retrieveKeyFromRequestPath(httpRequest);
            authorizeOrganizationChange(httpRequest, organizationKey);
          }
        }
      }
      // is it a POST, PUT, DELETE request requiring authorization?
      else if ("POST".equalsIgnoreCase(httpRequest.getMethod())
          || "PUT".equalsIgnoreCase(httpRequest.getMethod())
          || "DELETE".equalsIgnoreCase(httpRequest.getMethod())) {
        // legacy installation request
        if (path.contains("/ipt")) {
          // register installation?
          if (path.endsWith("/register")) {
            authorizeOrganizationChange(httpRequest);
          }
          // update installation?
          else if (path.contains("/update/")) {
            UUID installationKey = retrieveKeyFromRequestPath(httpRequest);
            authorizeInstallationChange(httpRequest, installationKey);
          }
          // register dataset?
          else if (path.endsWith("/resource")) {
            authorizeOrganizationChange(httpRequest);
          }
          // update dataset, delete dataset?
          else if (path.contains("/resource/")) {
            UUID datasetKey = retrieveKeyFromRequestPath(httpRequest);
            authorizeOrganizationDatasetChange(httpRequest, datasetKey);
          }
        }
        // legacy dataset request
        else if (path.contains("/resource")) {
          // register dataset?
          if (path.endsWith("/resource")) {
            authorizeOrganizationChange(httpRequest);
          }
          // update dataset, delete dataset?
          else if (path.contains("/resource/")) {
            UUID datasetKey = retrieveKeyFromRequestPath(httpRequest);
            authorizeOrganizationDatasetChange(httpRequest, datasetKey);
          }
        }
        // legacy endpoint request
        else if (path.endsWith("/service")) {
          // add endpoint?
          if (request.getParameterMap().isEmpty() || httpRequest.getRequestURI().contains("?resourceKey=")) {
            UUID datasetKey = retrieveDatasetKeyFromFormOrQueryParameters(httpRequest);
            authorizeOrganizationDatasetChange(httpRequest, datasetKey);
          }
        }
      }
    }
    // otherwise just do nothing (request unchanged)
  }

  /**
   * Authorize request can make a change to an organization, setting the request security context specifying the
   * principal provider. Called for example, when adding a new dataset.
   *
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeOrganizationChange(HttpServletRequest request) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyOrganization(authorization)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to register not authorized!");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Authorize request can make a change to an organization, first extracting the organization key from the
   * request path. If authorization is successful, the method sets the request security context specifying the
   * principal provider. Called for example, when verifying the credentials are correct for an organization.
   *
   * @param organizationKey organization key
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeOrganizationChange(HttpServletRequest request, UUID organizationKey) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyOrganization(authorization, organizationKey)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to register not authorized!");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Authorize request can make a change to an organization's dataset, setting the request security context specifying
   * the principal provider. Called for example, when updating or deleting a dataset.
   *
   * @param datasetKey dataset key
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeOrganizationDatasetChange(HttpServletRequest request, UUID datasetKey) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyOrganizationsDataset(authorization, datasetKey)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to update Dataset not authorized!");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Authorize request can make a change to an installation, setting the request security context specifying the
   * principal provider. Called for example, when adding a new dataset.
   *
   * @param installationKey installation key
   * @throws WebApplicationException if request isn't authorized
   */
  private void authorizeInstallationChange(HttpServletRequest request, UUID installationKey) {
    LegacyRequestAuthorization authorization = legacyAuthorizationService.authenticate(request);
    if (legacyAuthorizationService.isAuthorizedToModifyInstallation(authorization, installationKey)) {
      SecurityContextHolder.getContext().setAuthentication(authorization);
    } else {
      LOG.error("Request to update IPT not authorized!");
      throw new WebApplicationException(HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Retrieve key from request path, where the key is the last path segment, e.g. /registry/resource/{key}
   * Ensure any trailing .json for example is removed.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveKeyFromRequestPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String key = path.substring(path.lastIndexOf("/") + 1);
    if (key.contains(".")) {
      key = key.substring(0, key.lastIndexOf("."));
    }
    try {
      return UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Retrieve dataset key from form or query parameters.
   *
   * @param request request
   * @return dataset key
   * @throws WebApplicationException if incoming string key isn't a valid UUID
   */
  private UUID retrieveDatasetKeyFromFormOrQueryParameters(HttpServletRequest request) {
    Map<String, String[]> params = request.getParameterMap();
    String key = getFirst(params, LegacyResourceConstants.RESOURCE_KEY_PARAM);
    try {
      return UUID.fromString(key);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(HttpStatus.BAD_REQUEST);
    }
  }
}