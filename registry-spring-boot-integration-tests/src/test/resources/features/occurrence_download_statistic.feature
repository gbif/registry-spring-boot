@DownloadUserStatistic
Feature: Occurrence download statistics functionality

  Scenario: get download statistic by user country
    When get download statistic using user "registry_user" with params
      | userCountry | DK   |
      | fromDate    | 2017 |
      | toDate      | 2018 |
    Then response status should be 200
    And assert statistic response
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |