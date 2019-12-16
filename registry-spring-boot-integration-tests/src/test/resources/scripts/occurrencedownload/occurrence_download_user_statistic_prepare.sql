-- INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by, created, modified,
--                          deleted, fulltext_search, type, participation_status)
-- VALUES ('710970cf-e3f1-4e74-b09c-d8c86b9819d9', 'EUROPE', 'EUROPE', 'Org', null, 'mike', 'mike',
--         '2019-09-12 12:10:28.917000', '2019-09-12 12:10:32.189000', null, null, 'COUNTRY', 'ASSOCIATE');
--
-- INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
--                                  description, language, logo_url, city, province, country, postal_code, latitude,
--                                  longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
--                                  phone, homepage, address, challenge_code_key)
-- VALUES ('36107c15-771c-4810-a298-b7558828b8bd', '710970cf-e3f1-4e74-b09c-d8c86b9819d9', true, 'welcome', 'Org', null,
--         null, 'en', null, null, null, null, null, null, null, 'WS TEST', 'WS TEST', '2019-09-12 12:06:01.835000',
--         '2019-09-12 12:06:04.366000', null, null, null, null, null, null, null);
--
-- INSERT INTO public.installation (key, organization_key, type, title, description, created_by, modified_by, created,
--                                  modified, deleted, fulltext_search, password, disabled)
-- VALUES ('2fe63cec-9b23-4974-bab1-9f4118ef7711', '36107c15-771c-4810-a298-b7558828b8bd', 'IPT_INSTALLATION',
--         'Test IPT Registry2', 'Description of Test IPT', 'WS TEST',
--         'WS TEST', '2019-11-01 14:48:02.486307', '2019-11-01 14:48:02.486307', null,
--         null, 'welcome', false);
--
-- INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key,
--                             publishing_organization_key, external, type, sub_type, title, alias, abbreviation,
--                             description, language, homepage, logo_url, citation, citation_identifier, rights,
--                             locked_for_auto_update, created_by, modified_by, created, modified, deleted,
--                             fulltext_search, doi, license, maintenance_update_frequency, version)
-- VALUES ('d82273f6-9738-48a5-a639-2086f9c49d18', null, null, '2fe63cec-9b23-4974-bab1-9f4118ef7711',
--         '36107c15-771c-4810-a298-b7558828b8bd', false, 'OCCURRENCE', null, 'Test Dataset Registry', null, null,
--         'Description of Test Dataset', 'en', 'http://www.homepage.com', 'http://www.logo.com/1', null, null, null,
--         false, 'WS TEST', 'WS TEST',
--         '2019-11-12 08:49:53.062721', '2019-11-12 08:49:53.062721', null,
--         '''dataset'':2,8 ''descript'':5 ''occurr'':4 ''registry2'':3 ''test'':1,7 ''www.homepage.com'':9',
--         '10.21373/h9c3vc', 'UNSPECIFIED', null, null);
-- INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key,
--                             publishing_organization_key, external, type, sub_type, title, alias, abbreviation,
--                             description, language, homepage, logo_url, citation, citation_identifier, rights,
--                             locked_for_auto_update, created_by, modified_by, created, modified, deleted,
--                             fulltext_search, doi, license, maintenance_update_frequency, version)
-- VALUES ('4348adaa-d744-4241-92a0-ebf9d55eb9bb', null, null, '2fe63cec-9b23-4974-bab1-9f4118ef7711',
--         '36107c15-771c-4810-a298-b7558828b8bd', false, 'OCCURRENCE', null, 'Test Dataset Registry 2', null, null,
--         'Description of Test Dataset 2', 'en', 'http://www.homepage.com', 'http://www.logo.com/2', null, null, null,
--         false, 'WS TEST', 'WS TEST',
--         '2019-11-12 08:49:53.062721', '2019-11-12 08:49:53.062721', null,
--         '''dataset'':2,8 ''descript'':5 ''occurr'':4 ''registry2'':3 ''test'':1,7 ''www.homepage.com'':9',
--         '10.21373/h9c3vc', 'UNSPECIFIED', null, null);
--
-- INSERT INTO public.download_statistics (year_month, publishing_organization_country, dataset_key, total_records,
--                                         number_downloads)
-- VALUES ('2019-12-16 23:06:18.993000', 'DK', 'd82273f6-9738-48a5-a639-2086f9c49d18', 10, 10);
-- INSERT INTO public.download_statistics (year_month, publishing_organization_country, dataset_key, total_records,
--                                         number_downloads)
-- VALUES ('2019-12-16 23:07:58.624000', 'DK', 'd82273f6-9738-48a5-a639-2086f9c49d18', 20, 20);
-- INSERT INTO public.download_statistics (year_month, publishing_organization_country, dataset_key, total_records,
--                                         number_downloads)
-- VALUES ('2019-12-16 23:08:18.943000', 'DK', 'd82273f6-9738-48a5-a639-2086f9c49d18', 30, 30);
-- INSERT INTO public.download_statistics (year_month, publishing_organization_country, dataset_key, total_records,
--                                         number_downloads)
-- VALUES ('2019-12-16 23:08:41.188000', 'NO', '4348adaa-d744-4241-92a0-ebf9d55eb9bb', 10, 10);


INSERT INTO public.download_user_statistics (year_month, user_country, total_records,
                                             number_downloads)
VALUES ('2019-12-16 23:06:18.993000', 'DK', 10, 10);
INSERT INTO public.download_user_statistics (year_month, user_country, total_records,
                                             number_downloads)
VALUES ('2019-12-16 23:07:58.624000', 'DK', 20, 20);
INSERT INTO public.download_user_statistics (year_month, user_country, total_records,
                                             number_downloads)
VALUES ('2019-12-16 23:08:18.943000', 'DK', 30, 30);
INSERT INTO public.download_user_statistics (year_month, user_country, total_records,
                                             number_downloads)
VALUES ('2019-12-16 23:08:41.188000', 'NO', 10, 10);
