# Jira persist issues in XML, JSON format

## How to build/test?

`./mvnw spring-boot:run`

`./mvnw test`

## Export Jira issues

Export to json call: `http://localhost:8080/export`
Export to xml call: `http://localhost:8080/export/xml`

Both the .xml and .json files are stored in the `output` directory.

## How it works?

Jira issues are persisted in chunks to a [FileOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/FileOutputStream.html) with the [SequenceWriter](https://fasterxml.github.io/jackson-databind/javadoc/2.6/com/fasterxml/jackson/databind/SequenceWriter.html). Each chunk is created from 3 batches in parallel,
the number of Issues in a batch are determined by the maxResults parameter in the rest call.

The Jira issues batch rest call is configured from the <b>application.properties</b> xml.

The export to xml with Jackson need to be patched a bit.
* Some keys need to be in cdata because contain illegal characters for an .xml file, e.g. html.
The POJOs representing those keys are patched with mixins through the ObjectMapper.
* Xml version, encoding and root tags are added directly to the file output stream.
* Some Issue key values, like description and comment's text, contain fragments wrapped
in cdata + illegal .xml chars. Add a serializer which removes all cdata in a String field
and wrap the whole string in a cdata. We must avoid nesting of cdata.
* Issue description and comment text fields contain chars not supported by a xml text.
Replace them with empty strings in the output xml.

## Testing done

1. Exported files validation

Mac os xml validator:

`brew install libxml2`

validate: 

`xmllint issues.xml`

Mac os json validator:

`brew install jsonlint`

validate:

`jsonlint issues.json`

2. In addition to the provided jql query, tested also with jql := `issuetype = Bug`
 which has more than 1m issues and maxResults := 1000 for the rest call batch.

## Performance

```
jpi.issues.jql=issuetype in (Bug, Documentation, Enhancement) and updated > startOfWeek()
jpi.issues.batchsize=300
jpi.issues.para=5
```
1.5K Issues ~ 5sec

```
jpi.issues.jql=issuetype = Bug
jpi.issues.batchsize=1000
jpi.issues.para=5
```
1.5M Issues ~ 4min
