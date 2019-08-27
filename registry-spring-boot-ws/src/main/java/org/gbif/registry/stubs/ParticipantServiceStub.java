package org.gbif.registry.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Service
public class ParticipantServiceStub implements ParticipantService {

  @Override
  public Participant create(@NotNull Participant participant) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Participant get(@NotNull Integer integer) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void update(@NotNull Participant participant) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void delete(@NotNull Integer integer) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Participant> list(@Nullable String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}