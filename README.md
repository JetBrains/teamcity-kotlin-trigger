## TeamCity Kotlin Triggers plugin
[![JetBrains incubator project](https://jb.gg/badges/incubator-plastic.svg)](https://github.com/JetBrains#jetbrains-on-github)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 

**Please note that this plugin is not finished and might not work as expected.**

**Currently this plugin is not under development, but any contributions from interested external contributors are welcome**

This plugin allows users to write custom trigger using Kotlin DSL without creating new plugins.

### Implement
Put your implementing classes to "<artifactId>-server" module. Do not forget to update spring context file in 'main/resources/META-INF'. See TeamCity documentation for details.

### Build
Issue 'mvn package' command from the root project to build your plugin. Resulting package <artifactId>.zip will be placed in 'target' directory. 

### Install
To install the plugin, put zip archive to 'plugins' dir under TeamCity data directory and restart the server.


