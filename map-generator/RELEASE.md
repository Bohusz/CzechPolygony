# Release procedure

Use this procedure to create and publish an independent release of the
`map-generator` tool.

## Prerequisites

* Java 21 is installed. This project uses `C:\Users\Bohusz\.jdks\corretto-21.0.9`.
* Maven is available on `PATH`.
* You have push access to [bohusz/CzechPolygony](https://github.com/bohusz/CzechPolygony).
* The GitHub CLI (`gh`) is installed and authenticated with permission to create releases, or you can create the release in the GitHub web interface.

## 1. Select the release version

Choose a version without the `-SNAPSHOT` suffix, for example `0.1.0`.

For a patch release, use the latest maintenance line. If development has moved
to a later minor version, create a release branch from the previous
`map-generator` release tag and apply only the intended backward-compatible
fixes. For example:

```powershell
git switch -c release/map-generator-0.1 map-generator-v0.1.0
```

Update only `map-generator/pom.xml` to the final version:

```xml
<version>0.1.0</version>
```

Do not change the root POM version or `shp-converter`'s version for this
release. Do not change GeoTools or Java versions unless that is intentional and
separately tested.

## 2. Build and verify the distributable JAR

From the project root in PowerShell, run:

```powershell
$env:JAVA_HOME = 'C:\Users\Bohusz\.jdks\corretto-21.0.9'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -pl map-generator clean package
java -jar map-generator\target\map-generator-0.1.0.jar --version
java -jar map-generator\target\map-generator-0.1.0.jar --help
```

The focused Maven build runs the module tests and creates the self-contained
executable JAR:

```
map-generator\target\map-generator-0.1.0.jar
```

The Maven Shade plugin replaces the normal JAR with the dependency-inclusive
executable JAR. Do not publish `*-shaded.jar`; it is not the final artifact in
this project. Confirm that `--version` prints `map-generator 0.1.0` and that
`--help` completes successfully.

## 3. Commit and tag the release

Review the pending changes, commit the version change and release notes, then
create a component-specific annotated tag:

```powershell
git status
git add map-generator/pom.xml map-generator/RELEASE.md
git commit -m "Release map-generator 0.1.0"
git tag -a map-generator-v0.1.0 -m "Release map-generator 0.1.0"
git push origin main
git push origin map-generator-v0.1.0
```

Replace `main` with the actual release branch if it differs. Only tag the
commit that was built and verified in step 2. Do not use an unprefixed tag such
as `v0.1.0`, because the repository contains independently versioned tools.

## 4. Publish the GitHub release

Create a GitHub release from the component-specific tag and attach only the
map generator JAR:

```powershell
gh release create map-generator-v0.1.0 map-generator\target\map-generator-0.1.0.jar --repo bohusz/CzechPolygony --title "map-generator 0.1.0" --generate-notes
```

Alternatively, open [the repository releases page](https://github.com/bohusz/CzechPolygony/releases), select **Draft a new release**, choose tag `map-generator-v0.1.0`, enter the title and release notes, upload `map-generator\target\map-generator-0.1.0.jar`, and publish it.

After publishing, download the attached JAR and run:

```powershell
java -jar .\map-generator-0.1.0.jar --version
```

This verifies the exact published artifact rather than only the local build.

## 5. Start the next development version

After the release is published, update only `map-generator/pom.xml` to the next
development version, for example `0.2-SNAPSHOT`, then commit and push it:

```powershell
git add map-generator/pom.xml
git commit -m "Start map-generator 0.2-SNAPSHOT development"
git push origin main
```

Apply any patch fix to the active development branch as appropriate. Do not
replace a later active development version with the patch version.
