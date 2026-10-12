# Project guidance

## Repository structure

- The root `pom.xml` is the Maven parent and reactor. Add new modules to its
  `<modules>` list.
- Each application belongs in its own Maven module directory, with its own
  `pom.xml`, README, sources, and tests.
- Keep Java packages module-specific. Do not rename an existing module's
  packages during structural refactoring unless the assignment explicitly
  requires it.
- The repository currently contains the `shp-converter` module. A
  `map-generator` module is planned and should follow the implementation plan
  in `.docs/plans/`.

## Documentation

- Put repository-wide assignments in `.docs/assignments/`.
- Put implementation plans in `.docs/plans/`. Dated plan names are preferred,
  for example `map-generator_2026-10-11.md`.
- Put reusable domain and format descriptions in `.docs/references/`.
- Keep module-specific user documentation in that module's `README.md`.
- Keep the top-level `README.md` brief and focused on the repository and its
  modules; link to module documentation for operational details.
- Do not create module-to-module references for shared requirements. Extract
  shared material into `.docs/references/` or a repository-level assignment.

## Implementation conventions

- Follow the Java version and dependency management defined by the root POM.
- Preserve existing source style and package names unless the relevant
  assignment says otherwise.
- Add tests for new parsing, validation, projection, rendering, and CLI
  behavior. Prefer deterministic fixtures and focused assertions over brittle
  full-image comparisons.
- Update shared IntelliJ run configurations when a module is moved or renamed.
- Update relevant README and release documentation when module paths or build
  artifacts change.

### Java
- Use final and @Nonnull resp. @Nullable annotations whenever possible, for parameters, fields, local variables, and return values.
- Prefer using `var` before explicit type declarations.
- Use `@Override` annotations for overridden methods.
- Use enhanced `switch` statements where applicable.
- Prefer using `switch` expressions over traditional switch statements.

## Verification

- Run the root Maven build and tests after code or Maven-structure changes.
- For executable modules, verify the produced artifact with a representative
  command-line invocation.
- For documentation-only changes, verify links, paths, and repository status.
- Do not commit generated build output such as `target/` contents.

## Commits

- Do not create a commit without explicit user approval of the commit message.
  - Prepare the commit messages in English.
  - Start with a short one-line summary of the change.
  - Follow with a more detailed description of the change no more than 100 words long.
- When committing through the Junie environment, include the required trailer:
  `Co-authored-by: Junie <junie@jetbrains.com>`.
- Commit all requested current changes together when the user explicitly asks
  for all changes, including changes already present in the working tree.

The Junie co-author trailer is an environment requirement for commits made by
the agent; it is documented here for visibility and is not a Maven or Java
project requirement.

## Dependency management

- Use Maven dependency management for all dependencies.
- Use bom files to manage versions of dependencies whenever possible.
- When adding a dependency management for a vulnerable artifact transitively included by other - explicitly required - artifacts
  mark it with a brief comment stating that it is a security workaround and which vulnerable version it replaces.