![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

![CoreMedia Content Cloud Version](https://img.shields.io/static/v1?message=2406&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779)
![DeepL API](https://img.shields.io/static/v1?message=v1.3.0&label=DeepL%20Java%20Library&style=for-the-badge&color=green)

# CoreMedia DeepL Integration

This open-source extension allows to integrate DeepL for translation workflows in CoreMedia Content Cloud.


## Extension Dependencies
This extension has a dependency to the [coremedia-additional-workflows](https://github.com/CoreMedia/coremedia-additional-workflows) extension for the "create project" option.
Please make sure to also add the `coremedia-additional-workflows` extension to your workspace.


## Workflow Registration
To register the workflow, add `translation-deepl.xml` to your workflow definitions in `global/management-tools/management-tools-image/src/main/image/coremedia/import-default-workflows`.

Add `TranslationDeepl:/com/coremedia/labs/translation/deepl/workflow/translation-deepl.xml` to the variable `DEFAULT_WORKFLOWS`.

In addition, you can also upload the workflow manually using the workflow cmd-line tool `cm upload`:
```shell
./cm upload -url http://content-management-server:40180/ior -f translation-deepl.xml
```

## Configuration
To configure the DeepL integration, create a settings document named `DeepL` in `/Settings/Options/Settings/Translation Services` and link it to the _Linked Settings_ of the master site's homepage.

All settings need to be configured in a struct property named `deepl`.

The following configuration can be applied in the settings:

| Key       | Type    | Default | Required                                                              | Description                                                                                                                                |
|-----------|---------|---------|-----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `apiKey`    | String  |         | Yes                                                                   | DeepL API key. (required)                                                                                                                  |
| `url`      | String  |         | No                                                                    | Base URL for DeepL API, may be overridden for testing purposes. By default, the correct DeepL API (Free or Pro) is automatically selected. |
| `maxRetries` | Integer | `5`     | No | Maximum number of failed HTTP requests to retry, the default is 5.                                                                         |
| `timeout`   | Integer | `30`    | No | Connection timeout for each HTTP request in seconds.                  |
| `proxy`     | String  |         | No | Proxy to use for all HTTP requests to DeepL.                          |
| `headers`   |         |         | No | Additional HTTP headers to attach to all requests.                    |

