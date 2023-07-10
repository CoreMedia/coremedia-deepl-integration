# DeepL Test Data

This module contains test-data for the DeepL integration.

## /Settings/Options/Settings/Translation Services/DeepL

This settings content holds the configuration of the DeepL integration.

Note, that in order to activate the DeepL integration, it is required, to link this settings document to the root documents (also known as _Homepage_) of the master sites which shall use DeepL for translation.

The Settings Struct for DeepL contains the following entries:

* **`deepl`**
    * **`url`:** for example `http://yourhostname:9095/api/v3`
    * **`apiKey`:** API key
    * **`maxRetries`:** Maximum number of failed HTTP requests to retry, the default is 5
    * **`timeout`:** Connection timeout for each HTTP request
    * **`proxy`:** Proxy to use for all HTTP requests to DeepL
    * **`headers`:** Additional HTTP headers to attach to all requests
