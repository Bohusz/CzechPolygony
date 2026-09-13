# Release procedure

Use this procedure to create and publish a new release of the Shapefile converter.

## Prerequisites

* Java 21 is installed. This project uses `C:\Users\Bohusz\.jdks\corretto-21.0.9`.
* Maven is available on `PATH`.
* You have push access to [bohusz/CzechPolygony](https://github.com/bohusz/CzechPolygony).
* The GitHub CLI (`gh`) is installed and authenticated with permission to create releases, or you can create the release in the GitHub web interface.

## 1. Select the release version

Choose a version without the `-SNAPSHOT` suffix, for example `1.0.0`.

### Patch release procedure

Add this procedure to this document rather than creating a separate file: a patch release uses the same build, tag, and GitHub publishing workflow as every other release.

1. Identify the latest released version in the maintenance line. For example, the patch following `1.0.0` is `1.0.1`.
2. Base the patch on the released maintenance line. If development has moved to a later minor version, create a branch from the previous release tag, for example:

```powershell
git switch -c release/1.0 v1.0.0
```

3. Apply only the intended backward-compatible fixes, review them, and run the full verification from step 2 with the patch version.
4. Update `pom.xml` to the final patch version, for example `1.0.1`, then follow steps 2 through 4 below using `1.0.1` and tag `v1.0.1`.
5. After publishing, merge or otherwise apply the patch fix to the active development branch as appropriate. Do not replace the active development version with the patch version when it is already targeting a later minor release.

For a patch while the active branch is still on the same development line, for example `1.0-SNAPSHOT`, use that branch directly and continue with the normal steps below after changing it to `1.0.1`.

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