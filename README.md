# Compose Stability Inspection Plugin

![Build](https://github.com/Starmel/compose-stats-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

<!-- Plugin description -->

## Overview

The Compose Stability Inspection Plugin is a powerful tool designed to help you ensure the stability of Compose methods. Instability in your code can lead to unnecessary recomposition and performance issues. This plugin helps you identify unstable methods and provides quick fixes to help you resolve them.

Tip:

Use IDEA: double shift press > Run Inspection by Name > "Compose Stability Inspection" to run the inspection on a specific file or whole project.

## Stability Criteria

The plugin recognizes stability in the following cases:

Stable is:

* Class with all immutable properties
* Class marked as `@Stable`, `@Immutable`. Sealed class children are stable if parent is marked with `@Stable`
  or `@Immutable`
* [kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable) library
* Interface
* Enum
* Primitive
* Tuple with stable elements
* Functional type with stable parameters

Instability is caused by:

* Mutable property
* Class from other module not marked as stable
* Java class

Not handled yet:

* Generic class parameter

<!-- Plugin description end -->

## Contributing

We welcome contributions from the community to make this plugin even better. If you have ideas, bug reports, or would
like to contribute to its development, please open an issue or submit a pull request.
