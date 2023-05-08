# Sireum IntelliJ Plugin

This IntelliJ plugin integrates [Sireum](https://github.com/sireum/kekinian) tools.

## Setup

```bash
./setup.sh
```

Set IntelliJ preference: `Editor` -> `GUI Designer` -> `Generate GUI into: Java source code`

## To Run/Deebug

Click on `sireum-intellij-plugin`

## To Package

Rebuild project in IntelliJ, then:

```sbt
packageArtifactZip
```

```bash
./post.sh
```