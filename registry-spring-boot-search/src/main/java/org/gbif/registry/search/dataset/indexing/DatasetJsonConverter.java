package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.indexing.checklistbank.ChecklistbankPersistenceService;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.search.dataset.indexing.ws.JacksonObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Slf4j
@Component
public class DatasetJsonConverter {

  private static final int MAX_FACET_LIMIT = 1200000;
  private static final String PROCESSING_NAMESPACE = "processing.gbif.org";
  private static final String INSTITUTION_TAG_NAME = "institutionCode";
  private static final String COLLECTION_TAG_NAME = "collectionCode";

  private final SAXParserFactory saxFactory = SAXParserFactory.newInstance();
  private final TimeSeriesExtractor timeSeriesExtractor = new TimeSeriesExtractor(1000, 2400, 1800, 2050);

  private final List<Consumer<ObjectNode>> consumers = new ArrayList<>();

  private ChecklistbankPersistenceService checklistbankPersistenceService;

  private final GbifWsClient gbifWsClient;

  private final Long occurrenceCount;

  private final Long nameUsagesCount;

  private final ObjectMapper mapper;

  @Autowired
  private DatasetJsonConverter(GbifWsClient gbifWsClient, ChecklistbankPersistenceService checklistbankPersistenceService,
                               @Qualifier("apiMapper") ObjectMapper mapper) {
    this.gbifWsClient = gbifWsClient;
    this.checklistbankPersistenceService = checklistbankPersistenceService;
    this.mapper = mapper;
    consumers.add(this::metadataConsumer);
    consumers.add(this::addTitles);
    consumers.add(this::enumTransforms);
    consumers.add(this::addFacetsData);
    occurrenceCount = gbifWsClient.getOccurrenceRecordCount();
    nameUsagesCount = gbifWsClient.speciesSearch(new NameUsageSearchRequest(0, 0)).getCount();
  }

  public static DatasetJsonConverter create(GbifWsClient gbifWsClient, ChecklistbankPersistenceService checklistbankPersistenceService) {
    return new DatasetJsonConverter(gbifWsClient, checklistbankPersistenceService, JacksonObjectMapper.get());
  }

  public ObjectNode convert(Dataset dataset) {
    ObjectNode datasetAsJson = mapper.valueToTree(dataset);
    consumers.forEach(c -> c.accept(datasetAsJson));
    addDecades(dataset, datasetAsJson);
    addKeyword(dataset, datasetAsJson);
    addCountryCoverage(dataset, datasetAsJson);
    addTaxonKeys(dataset, datasetAsJson);
    addMachineTags(dataset, datasetAsJson);
    return datasetAsJson;
  }

  private void metadataConsumer(ObjectNode dataset) {
    try (InputStream stream = gbifWsClient.getMetadataDocument(UUID.fromString(dataset.get("key").asText())) ) {
        if (stream != null) {
          FullTextSaxHandler handler = new FullTextSaxHandler();
          SAXParser p = saxFactory.newSAXParser();
          // parse does close the stream
          p.parse(stream, handler);
          dataset.put("metadata", handler.getFullText());
        }
      } catch (ParserConfigurationException e) {
        throw new IllegalStateException("XML Parser not working on this system", e);
      } catch (SAXException e) {
        log.warn("Cannot parse original metadata xml for dataset {}", dataset);
      } catch (Exception e) {
        log.error("Unable to index metadata document for dataset {}", dataset, e);
      }
  }

  private void addTitles(ObjectNode dataset) {
    if (dataset.has("installationKey")) {
      Installation installation = gbifWsClient.getInstallation(dataset.get("installationKey").asText());
      if(Objects.nonNull(installation)) {
        dataset.put("installationTitle", installation.getTitle());
        if (Objects.nonNull(installation.getOrganizationKey())) {
          Organization hostingOrg = gbifWsClient.getOrganization(installation.getOrganizationKey().toString());
          if (Objects.nonNull(hostingOrg)) {
            dataset.put("hostingOrganizationKey", hostingOrg.getKey().toString());
            dataset.put("hostingOrganizationTitle", hostingOrg.getTitle());
          }
        }
      }
    }
    if (dataset.has("publishingOrganizationKey")) {
      Organization publisher = gbifWsClient.getOrganization(dataset.get("publishingOrganizationKey").asText());
      if (Objects.nonNull(publisher)) {
        dataset.put("publishingOrganizationTitle", publisher.getTitle());
        if(Objects.nonNull(publisher.getCountry())) {
          dataset.put("publishingCountry", publisher.getCountry().getIso2LetterCode());
        }
      } else {
        dataset.put("publishingCountry", Country.UNKNOWN.getIso2LetterCode());
      }
    }
  }

  private void addRecordCounts(ObjectNode dataset, Long datasetOccurrenceCount) {
    String datasetKey  = dataset.get("key").textValue();
    dataset.put("occurrenceCount", datasetOccurrenceCount);

    double occurrencePercentage = (double)datasetOccurrenceCount / occurrenceCount;
    double nameUsagesPercentage = 0D;

    //Contribution of occurrence records
    dataset.put("occurrencePercentage", occurrencePercentage);
    DatasetMetrics datasetMetrics = gbifWsClient.getDatasetSpeciesMetrics(datasetKey);

    if (Objects.nonNull(datasetMetrics)) {
      nameUsagesPercentage = (double)datasetMetrics.getUsagesCount() / nameUsagesCount;
      dataset.put("nameUsagesCount", datasetMetrics.getUsagesCount());
    } else {
      dataset.put("nameUsagesCount", 0);
    }

    //Contribution of NameUsages
    dataset.put("nameUsagesPercentage", nameUsagesPercentage);

    //How much a dataset contributes in terms of records to GBIF data
    dataset.put("dataScore", occurrencePercentage + nameUsagesPercentage);
  }

  private void enumTransforms(ObjectNode dataset) {
    Optional.ofNullable(dataset.get("license"))
      .ifPresent(licenseUrl -> License.fromLicenseUrl(licenseUrl.asText())
        .ifPresent(license -> dataset.put("license", license.name())));
  }

  private void addDecades(Dataset dataset, ObjectNode datasetJsonNode) {
    // decade series
    List<Integer> decades = timeSeriesExtractor.extractDecades(dataset.getTemporalCoverages());
    datasetJsonNode.putArray("decade").addAll(decades.stream().map(IntNode::new).collect(Collectors.toList()));
  }

  private void addKeyword(Dataset dataset, ObjectNode datasetJsonNode) {

    Collection<JsonNode> keywords =  Stream.concat(dataset.getTags().stream().map(Tag::getValue).map(TextNode::valueOf),
                                                   dataset.getKeywordCollections().stream().map(KeywordCollection::getKeywords).flatMap(Set::stream).map(TextNode::valueOf))
                                      .collect(Collectors.toList());
    datasetJsonNode.putArray("keyword").addAll(keywords);
  }


  private void addCountryCoverage(Dataset dataset, ObjectNode datasetJsonNode) {
    if(Objects.nonNull(dataset.getCountryCoverage())) {
      datasetJsonNode.putArray("countryCoverage")
        .addAll(dataset.getCountryCoverage().stream().map(Country::getIso2LetterCode).map(TextNode::valueOf).collect(Collectors.toList()));
    }
  }

  private void addFacetsData(ObjectNode datasetJsonNode) {
    String datasetKey = datasetJsonNode.get("key").textValue();
    Set<OccurrenceSearchParameter> facets = EnumSet.of(OccurrenceSearchParameter.COUNTRY, OccurrenceSearchParameter.CONTINENT,
                                                       OccurrenceSearchParameter.TAXON_KEY, OccurrenceSearchParameter.YEAR);
    OccurrenceSearchRequest occurrenceSearchRequest = new OccurrenceSearchRequest();
    occurrenceSearchRequest.setLimit(0);
    occurrenceSearchRequest.setOffset(0);
    occurrenceSearchRequest.setMultiSelectFacets(false);
    occurrenceSearchRequest.setFacetLimit(MAX_FACET_LIMIT);
    occurrenceSearchRequest.setFacetMinCount(1);
    occurrenceSearchRequest.setFacets(facets);
    occurrenceSearchRequest.addParameter(OccurrenceSearchParameter.DATASET_KEY, datasetKey);
    SearchResponse<Occurrence, OccurrenceSearchParameter> response = gbifWsClient.occurrenceSearch(occurrenceSearchRequest);
    addRecordCounts(datasetJsonNode, response.getCount());
    ArrayNode countryNode = datasetJsonNode.putArray("country");
    ArrayNode continentNode = datasetJsonNode.putArray("continent");
    ArrayNode taxonKeyNode = datasetJsonNode.putArray("taxonKey");
    ArrayNode yearNode = datasetJsonNode.putArray("year");
    response.getFacets().forEach(facet -> {
      if (OccurrenceSearchParameter.COUNTRY == facet.getField()) {
        facet.getCounts().forEach(count -> countryNode.add(count.getName()));
      } else if (OccurrenceSearchParameter.CONTINENT == facet.getField()) {
        facet.getCounts().forEach(count -> continentNode.add(count.getName()));
      } else if (OccurrenceSearchParameter.TAXON_KEY == facet.getField()) {
        facet.getCounts().forEach(count -> taxonKeyNode.add(count.getName()));
      } else if (OccurrenceSearchParameter.YEAR == facet.getField()) {
        facet.getCounts().forEach(count -> yearNode.add(count.getName()));
      }
    });
  }

  private void addTaxonKeys(Dataset dataset, ObjectNode datasetObjectNode) {
    if (DatasetType.CHECKLIST == dataset.getType()) {
      ArrayNode taxonKeyNode = datasetObjectNode.has("taxonKey")? (ArrayNode)datasetObjectNode.get("taxonKey") : datasetObjectNode.putArray("taxonKey");
      for(Integer taxonKey : checklistbankPersistenceService.getTaxonKeys(dataset.getKey().toString())) {
        taxonKeyNode.add(new IntNode(taxonKey));
      }
    }
  }

  private void addMachineTags(Dataset dataset, ObjectNode datasetObjectNode) {
    datasetObjectNode.putArray("institutionKey")
      .addAll(dataset.getMachineTags().stream()
              .filter(mt -> PROCESSING_NAMESPACE.equals(mt.getNamespace()) && INSTITUTION_TAG_NAME.equals(mt.getName()))
              .map(v -> new TextNode(v.getValue()))
              .collect(Collectors.toList()));
    datasetObjectNode.putArray("collectionKey")
      .addAll(dataset.getMachineTags().stream()
                .filter(mt -> PROCESSING_NAMESPACE.equals(mt.getNamespace()) && COLLECTION_TAG_NAME.equals(mt.getName()))
                .map(v -> new TextNode(v.getValue()))
                .collect(Collectors.toList()));
  }
}