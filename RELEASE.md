# Release procedure

Use this procedure to create and publish a new release of the Shapefile converter.

## Prerequisites

* Java 21 is installed. This project uses `C:\Users\Bohusz\.jdks\corretto-21.0.9`.
* Maven is available on `PATH`.
* You have push access to [bohusz/CzechPolygony](https://github.com/bohusz/CzechPolygony).
* The GitHub CLI (`gh`) is installed and authenticated with permission to create releases, or you can create the release in the GitHub web interface.

## 1. Select the release version

Choose a version without the `-SNAPSHOT` suffix, for example `1.0.0`.

Update the project version in `pom.xml`:

```xml
<version>1.0.0</version>
```

Do not change the GeoTools or Java versions as part of a routine release unless that is intentional and separately tested.

## 2. Build and verify the distributable JAR

From the project root in PowerShell, run:

```powershell
$env:JAVA_HOME = 'C:\Users\Bohusz\.jdks\corretto-21.0.9'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean package
java -jar target\shp-converter-1.0.0.jar --version
java -jar target\shp-converter-1.0.0.jar --help
```

`mvn clean package` runs the test suite and creates the self-contained executable JAR:

```
target\shp-converter-1.0.0.jar
```

The Maven Shade plugin replaces the normal JAR with the dependency-inclusive executable JAR. Do not publish `*-shaded.jar`; it is not the final artifact in this project.

Confirm that `--version` prints `shp-converter 1.0.0` and that `--help` completes successfully.

## 3. Commit and tag the release

Review the pending changes, commit the version change and any release notes, then create an annotated tag:

```powershell
git status
git add pom.xml RELEASE.md README.md
git commit -m "Release 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin main
git push origin v1.0.0
```

Replace `main` with the actual release branch if it differs. Only tag the commit that was built and verified in step 2.

## 4. Publish the GitHub release

Create a GitHub release from the tag and attach the JAR:

```powershell
gh release create v1.0.0 target\shp-converter-1.0.0.jar --repo bohusz/CzechPolygony --title "Shapefile converter 1.0.0" --generate-notes
```

Alternatively, open [the repository releases page](https://github.com/bohusz/CzechPolygony/releases), select **Draft a new release**, choose tag `v1.0.0`, enter the title and release notes, upload `target\shp-converter-1.0.0.jar`, and publish it.

After publishing, download the attached JAR from the GitHub release page and run:

```powershell
java -jar .\shp-converter-1.0.0.jar --version
```

This verifies the exact published artifact rather than only the local build.

## 5. Start the next development version

After the release is published, update `pom.xml` to the next development version, for example `1.1-SNAPSHOT`, commit it, and push it:

```powershell
git add pom.xml
git commit -m "Start 1.1-SNAPSHOT development"
git push origin main
```