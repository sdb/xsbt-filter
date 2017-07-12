The development of this library has been discontinued.

[![Build Status](https://secure.travis-ci.org/sdb/xsbt-filter.png)](http://travis-ci.org/sdb/xsbt-filter)

The xsbt-filter project provides the necessary functionality for filtering resources in an [sbt](https://github.com/harrah/xsbt) project. The plugin's functionality is similar to Maven's [resource filtering](http://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html) mechanism and is useful when migrating Maven projects that depend heavily on resource filtering.

## Requirements

* sbt 0.11.0 or later

## Overview 

This plugin scans your resources (e.g `src/main/resources`, `src/test/resources`) for variables (surrounded by `${` and `}`) and replaces them with values which can come from the system properties, your project properties and filter resources.

For example, if we have a resource `src/main/resources/hello.txt` containing:

    Hello, ${name}

When `copy-resources` is executed, this will create a resource output `hello.txt` in the project's classes output directory. The variable (or property reference) `${name}` will be replaced with the name of your project.

Filter resources are one way to add extra properties which can be used to substitute variables. By default the plug-in looks for filter resources in a `filters` directory under the source directories for the `main` (`compile`) and `test` configurations (`src/main/filters` and `src/test/filters`).

## Setup

Add the following to your plugin configuration (in `project/plugins/build.sbt`):
    
    addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.4")

If you want to use the latest snapshot version then you need to add the Sonatype OSS Maven repository:

    resolvers += "Sonatype OSS snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
    
    addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.5-SNAPSHOT")

Add the default filter settings to your project in `build.sbt`:

    seq(filterSettings: _*)

## Build

Build and install the plugin to use the latest SNAPSHOT version:

    git clone git://github.com/sdb/xsbt-filter.git
    cd xsbt-filter
    sbt publish-local

## Default Configuration

The plugin comes with a set of default settings. This is what you get with the default configuration:

* only `.properties` and `.xml` resources are filtered
* filter resources (only `.properties` and `.xml`) should be placed under a directory `filters`
* the following values are available as replacement values:
  * name, version, scalaVersion and a couple of other build settings (the setting name is used as key)
  * system properties
  * environment variables
  * the user-defined properties defined in filter resources
* resources filtering is triggered when `copy-resources` is executed

## Settings

Take a look at the source code for [FilterPlugin](https://github.com/sdb/xsbt-filter/blob/master/src/FilterPlugin.scala) for all settings.

Add the following to your `build.sbt` if you want to change any of the settings provided by this plugin:

    import FilterKeys._

### Filter resource paths

Use the `filter-directory-name` setting to configure the name of the directory containing the filters:

    filterDirectoryName := "my-filters"

Use the `include-filter` setting to change the file filter that determines which filters to include:

    includeFilter in (Compile, filters) ~= { f => f || ("*.props" | "*.conf") }

### Filtered resource paths

Use the `include-filter` setting to change the file filter that determines which resources need to be filtered:

    includeFilter in (Compile, filterResources) ~= { f => f || ("*.props" | "*.conf") }

### Properties

Use the `filter-extra-props` setting to add extra properties to be used as replacement values:

    extraProps += "author" -> "Foo Bar"

Use the `filter-project-props` setting to change the name of the properties which are derived from project settings to avoid collisions with other properties:

    projectProps ~= { _ map (p => ("project." + p._1, p._2)) }

## License

This project is licensed under the New BSD License.

## Contributing

Fork the repository, push your changes to a new branch and send me a merge request.

## Build, test & publish

### Tests

    sbt scripted

### Publish

See [Deploying to Sonatype](http://www.scala-sbt.org/release/docs/Community/Using-Sonatype.html) and [Sonatype OSS Maven Repository Usage Guide](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-8.ReleaseIt) for more information.

    sbt publish

