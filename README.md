# SCAPE Connector API on Fedora 4

This is the implementation of the SCAPE Connector API as described by the spec available at 
https://github.com/openplanets/scape-apis

### What does SCAPE Connector API on Fedora 4 do?

This is the implementation of the SCAPE Connector API as described by the spec available at 
https://github.com/openplanets/scape-apis

### What are the benefits for end user?

* He can use the Connctor API

### Who is intended audience?

* SCAPE users

## Features and roadmap

### Version 0.0.1

* See https://github.com/openplanets/scape-apis 

### Roadmap

* finished

## How to install and use

* see https://github.com/openplanets/scape-fcrepo4-connector/blob/master/README_INSTALL_AND_USAGE.md


### Troubleshooting

* java.net.ConnectoException

If you're getting 
```java 
javax.jcr.RepositoryException: java.net.ConnectException

at eu.scape_project.service.ConnectorService.addFiles(ConnectorService.java:540)
```

the Servlet Container needs to know about the proxy settings. In e.g. Tomcat you can add the following to tomcat/bin/catalina.sh:

```
CATALINA_OPTS="$CATALINA_OPTS -Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=4242"
```

## More information

### Publications

### Licence

SCAPE Connector API on Fedora 4 is released under [Apache version 2.0 license](LICENSE.txt).

### Acknowledgements

Part of this work was supported by the European Union in the 7th Framework Program, IST, through the SCAPE project, Contract 270137.

### Support

This tool is supported by the [Open Planets Foundation](http://www.openplanetsfoundation.org). Commercial support is provided by company X.

## Develop

[![Build Status](https://travis-ci.org/openplanets/scape.png)](https://travis-ci.org/openplanets/scape-fcrepo4-connector)

### Build

see https://github.com/openplanets/scape-fcrepo4-connector/blob/master/README_INSTALL_AND_USAGE.md

### Deploy

see https://github.com/openplanets/scape-fcrepo4-connector/blob/master/README_INSTALL_AND_USAGE.md

### Contribute

1. [Fork the GitHub project](https://help.github.com/articles/fork-a-repo)
2. Change the code and push into the forked project
3. [Submit a pull request](https://help.github.com/articles/using-pull-requests)

To increase the changes of you code being accepted and merged into the official source here's a checklist of things to go over before submitting a contribution. For example:

* Agrees to contributor license agreement, certifying that any contributed code is original work and that the copyright is turned over to the project
