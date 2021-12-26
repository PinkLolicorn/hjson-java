# hjson-java

![License](https://img.shields.io/github/license/OkaeriPoland/okaeri-hjson)
![Total lines](https://img.shields.io/tokei/lines/github/OkaeriPoland/okaeri-hjson)
![Repo size](https://img.shields.io/github/repo-size/OkaeriPoland/okaeri-hjson)
![Contributors](https://img.shields.io/github/contributors/OkaeriPoland/okaeri-hjson)
[![Discord](https://img.shields.io/discord/589089838200913930)](https://discord.gg/hASN5eX)

Hjson, the Human JSON. A configuration file format for humans. Relaxed syntax, fewer mistakes, more comments.
Based on [PearsonTheCat/hjson-java](https://github.com/PersonTheCat/hjson-java), a modified version of [hjson/hjson-java](https://github.com/hjson/hjson-java).
Provides hjson implementation for [OkaeriPoland/okaeri-configs](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hjson), a part of the [okaeri-platform](https://github.com/OkaeriPoland/okaeri-platform).

## Installation
### Maven
Add repository to the `repositories` section:
```xml
<repository>
    <id>okaeri-repo</id>
    <url>https://storehouse.okaeri.eu/repository/maven-public/</url>
</repository>
```
Add dependency to the `dependencies` section:
```xml
<dependency>
  <groupId>eu.okaeri</groupId>
  <artifactId>okaeri-hjson</artifactId>
  <version>4.0.1</version>
</dependency>
```
### Gradle
Add repository to the `repositories` section:
```groovy
maven { url "https://storehouse.okaeri.eu/repository/maven-public/" }
```
Add dependency to the `maven` section:
```groovy
implementation 'eu.okaeri:okaeri-hjson:4.0.1'
```

## Usage

Main goal at the moment is to provide stable library for [hjson module of Okaeri's config library](https://github.com/OkaeriPoland/okaeri-configs/tree/master/hjson).
See usage in the [okaeri-configs](https://github.com/OkaeriPoland/okaeri-configs) project or browse the source projects
([PearsonTheCat/hjson-java](https://github.com/PersonTheCat/hjson-java), [hjson/hjson-java](https://github.com/hjson/hjson-java)).
