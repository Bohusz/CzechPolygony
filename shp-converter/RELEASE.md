# Release procedure

Use this procedure to create and publish an independent release of the
`shp-converter` tool.

## Prerequisites

* Java 21 is installed. This project uses `C:\Users\Bohusz\.jdks\corretto-21.0.9`.
* Maven is available on `PATH`.
* You have push access to [bohusz/CzechPolygony](https://github.com/bohusz/CzechPolygony).
* The GitHub CLI (`gh`) is installed and authenticated with permission to create releases, or you can create the release in the GitHub web interface.

## 1. Select the release version

Choose a version without the `-SNAPSHOT` suffix, for example `1.2.0`.

For a patch release, use the latest maintenance line. If development has moved
to a later minor version, create a release branch from the previous
`shp-converter` release tag and apply only the intended backward-compatible
fixes. For example:

```powershell
git switch -c release/shp-converter-1.2 shp-converter-v1.2.0
```

Update only `shp-converter/pom.xml` to the final version:

```xml
<version>1.2.0</version>
```

Do not change the root POM version or `map-generator`'s version for this
release. Do not change GeoTools or Java versions unless that is intentional and
separately tested.

## 2. Build and verify the distributable JAR

From the project root in PowerShell, run:

```powershell
$env:JAVA_HOME = 'C:\Users\Bohusz\.jdks\corretto-21.0.9'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -pl shp-converter clean package
java -jar shp-converter\target\shp-converter-1.2.0.jar --version
java -jar shp-converter\target\shp-converter-1.2.0.jar --help
```

The focused Maven build runs the module tests and creates the self-contained
executable JAR:

```
shp-converter\target\shp-converter-1.2.0.jar
```

The Maven Shade plugin replaces the normal JAR with the dependency-inclusive
executable JAR. Do not publish `*-shaded.jar`; it is not the final artifact in
this project. Confirm that `--version` prints `shp-converter 1.2.0` and that
`--help` completes successfully.

## 3. Commit and tag the release

Review the pending changes, commit the version change and release notes, then
create a component-specific annotated tag:

```powershell
git status
git add shp-converter/pom.xml shp-converter/RELEASE.md
git commit -m "Release shp-converter 1.2.0"
git tag -a shp-converter-v1.2.0 -m "Release shp-converter 1.2.0"
git push origin main
git push origin shp-converter-v1.2.0
```

Replace `main` with the actual release branch if it differs. Only tag the
commit that was built and verified in step 2. Do not use an unprefixed tag such
as `v1.2.0`, because the repository contains independently versioned tools.

## 4. Publish the GitHub release

Create a GitHub release from the component-specific tag and attach only the
converter JAR:

```powershell
gh release create shp-converter-v1.2.0 shp-converter\target\shp-converter-1.2.0.jar --repo bohusz/CzechPolygony --title "shp-converter 1.2.0" --generate-notes
```

Alternatively, open [the repository releases page](https://github.com/bohusz/CzechPolygony/releases), select **Draft a new release**, choose tag `shp-converter-v1.2.0`, enter the title and release notes, upload `shp-converter\target\shp-converter-1.2.0.jar`, and publish it.

After publishing, download the attached JAR and run:

```powershell
java -jar .\shp-converter-1.2.0.jar --version
```

This verifies the exact published artifact rather than only the local build.

## 5. Start the next development version

After the release is published, update only `shp-converter/pom.xml` to the next
development version, for example `1.3-SNAPSHOT`, then commit and push it:

```powershell
git add shp-converter/pom.xml
git commit -m "Start shp-converter 1.3-SNAPSHOT development"
git push origin main
```

Apply any patch fix to the active development branch as appropriate. Do not
replace a later active development version with the patch version.
