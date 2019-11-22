package org.gbif.registry.ws.resources.legacy;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/legacy_organization.feature"
  },
  glue = {"org.gbif.registry.ws.resources.legacy.organization", "org.gbif.registry.utils.cucumber"},
  plugin = "pretty"
)
public class LegacyOrganizationIT {
}