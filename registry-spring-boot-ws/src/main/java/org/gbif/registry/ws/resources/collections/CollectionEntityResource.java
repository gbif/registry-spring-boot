/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.security.EditorAuthorizationService;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@RestController
@RequestMapping(value = "grscicoll/collection", produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionEntityResource extends ExtendedCollectionEntityResource<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;

  public CollectionEntityResource(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      EventManager eventManager,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(
        collectionMapper,
        addressMapper,
        tagMapper,
        identifierMapper,
        collectionMapper,
        machineTagMapper,
        eventManager,
        Collection.class,
        userAuthService,
        withMyBatis);
    this.collectionMapper = collectionMapper;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/collection/{key}")
  @Override
  public Collection get(@PathVariable @NotNull UUID key) {
    return super.get(key);
  }

  @GetMapping
  @Override
  public PagingResponse<Collection> list(
      @Nullable @RequestParam(value = "q", required = false) String query,
      @Nullable @RequestParam(value = "institution", required = false) UUID institutionKey,
      @Nullable @RequestParam(value = "contact", required = false) UUID contactKey,
      @Nullable @RequestParam(value = "code", required = false) String code,
      @Nullable @RequestParam(value = "name", required = false) String name,
      Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = collectionMapper.count(institutionKey, contactKey, query, code, name);
    return new PagingResponse<>(
        page, total, collectionMapper.list(institutionKey, contactKey, query, code, name, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Collection> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(
        page, collectionMapper.countDeleted(), collectionMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return collectionMapper.suggest(q);
  }
}
