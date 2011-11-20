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
    
    resolvers += "sdb@github" at "http://sdb.github.com/maven
    
    addSbtPlugin("com.github.sdb" % "xsbt-filter" % "0.1")

Add the default filter settings to your project in `build.sbt`:

    seq(filterSettings: _*)

# Build

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

    includeFilter in filters := AllPassFilter -- HiddenFileFilter

### Filtered resource paths

Use the `include-filter` setting to change the file filter that determines which resources need to be filtered:

    includeFilter in filterResources ~= { f => f || ("*.props" | "*.conf") }

### Properties

Use the `filter-extra-props` setting to add extra properties to be used as replacement values:

    extraProps += "author" -> "Foo Bar"

Use the `filter-project-props` setting to change the name of the properties which are derived from project settings to avoid collisions with other properties:

    projectProps ~= { _ map (p => ("project." + p._1, p._2)) }

### Misc

Only include the `compile` configuration in resource filtering:

    seq((baseFilterSettings ++ inConfig(Compile)(filterConfigSettings)): _*)

## License

This project is licensed under the New BSD License.

## Contributing

Fork the repository, push your changes to a new branch and send me a merge request.
