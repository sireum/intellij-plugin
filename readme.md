# Sireum IntelliJ Plugin

This IntelliJ plugin integrates [Sireum](https://github.com/sireum/kekinian) tools.

## Setup

```bash
./setup.cmd
```

Set IntelliJ preference: `Editor` -> `GUI Designer`:

* `Generate GUI into:` -> `Java source code`
* `Default accessibility for UI-bound fields:` -> `protected`
* `Build, Execution, Deployment` -> `Compiler` -> Disable `Add runtime assertion for notnull...`

## To Run/Deebug

Click on `sireum-intellij-plugin`

## To Package

Rebuild project in IntelliJ, then:

```sbt
clean; cleanFiles; packageArtifactZip
```

```bash
./post.cmd
```