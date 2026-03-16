# Mutation Testing — kestros-osgi-service-utils

## What is mutation testing

Mutation testing is a technique for evaluating test suite quality by introducing small, deliberate faults
(mutations) into production code and checking whether the existing tests detect (kill) them. Each mutation
represents a minimal code change — for example, flipping a `>` to `>=`, negating a conditional, or
replacing a return value with `null`. If the test suite does not fail on a given mutation, that mutation
"survives", indicating a gap in test coverage or assertion quality. A high mutation score (percentage of
mutations killed) demonstrates that tests are genuinely exercising the production logic, not just executing
lines without asserting outcomes.

## Plugin version and coordinates

| Property | Value |
|---|---|
| Group ID | `org.pitest` |
| Artifact ID | `pitest-maven` |
| Version | `1.15.3` |
| Maven goal | `org.pitest:pitest-maven:mutationCoverage` |

## Run instructions

Run from the repo root:

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

This compiles test sources and runs the full mutation analysis. Expect approximately 2–5× the duration of a
normal `mvn test` run. For this module the typical runtime is under 20 seconds.

To run with parallelism (if runtime becomes an issue with more classes):

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage -Dthreads=2
```

## How to read the report

After a successful run, open the HTML report:

```
target/pit-reports/index.html
```

The report shows:

- **Overall summary** — total mutations generated, total killed, overall score
- **Per-package breakdown** — click into each package to see class-level scores
- **Per-class breakdown** — each class lists every mutant, colour-coded:
  - **Green (KILLED)** — the test suite detected this mutation and failed on it. Good.
  - **Red (SURVIVED)** — the mutation went undetected. The test suite did not assert against this code path with enough specificity to catch the change.
  - **Yellow (NO_COVERAGE)** — no test executed this line at all. The mutation was never run.

A survived mutation is an actionable finding: it points to a specific line and mutation operator where a
new or stronger assertion would improve coverage.

The XML report at `target/pit-reports/mutations.xml` contains the same data in machine-readable form.

## Baseline Scores (v0.1.11, 2026-03-16)

Run date: 2026-03-16
PIT version: 1.15.3

| Class | Mutations | Killed | Score |
|---|---|---|---|
| `BaseCachePurgeOnResourceChangeEventListener` | 6 | 4 | 66% |
| `BaseCacheService` | 22 | 20 | 90% |
| `BaseExternalConnectionService` | 5 | 4 | 80% |
| `BaseManagedServiceHealthCheck` | 7 | 2 | 28% |
| `BaseServiceResolverService` | 10 | 8 | 80% |
| `JcrFileCacheService` | 25 | 24 | 96% |
| `OsgiServiceUtils` | 17 | 9 | 52% |
| `ResourceCreationUtils` | 2 | 2 | 100% |

**Overall: 94 mutations, 73 killed — 78% mutation score**
Line coverage (mutated classes): 259/306 (85%)
Test strength (killed / covered mutations): 86%

### Notes on low-scoring classes

- **`BaseManagedServiceHealthCheck` (28%)**: This class is largely abstract with minimal testable
  branches in the concrete path. The surviving mutants are in abstract template methods that require
  concrete subclass implementations to test. The existing `BaseManagedServiceHealthCheckTest` covers the
  concrete behaviour but the abstract shell is under-covered by design.
- **`OsgiServiceUtils` (52%)**: Utility methods with conditional logic around null-checks and collection
  iteration. Several NullReturn mutations survived on methods that return `Optional` or collection types
  where tests verify the happy path but not all null-input branches.

## Threshold policy

| Threshold | Value | Status |
|---|---|---|
| Minimum (current, enforced) | **60%** | Active — build fails below this |
| Long-term target | **70%** | Aspirational — to be raised once weak test areas are addressed |

The threshold is configured via `mutationThreshold` in `pom.xml`. When the score falls below the minimum,
the build exits non-zero with:

```
[ERROR] Mutation score of XX is below minimum 60
```

Do not lower the threshold to make a failing build pass. Instead, add or strengthen tests for the
surviving mutants identified in the HTML report.

## Expanding to additional modules

Once this pilot is reviewed and merged, the same plugin block can be copied to other foundational repos
(e.g. `kestros-structured-sling-models`, `kestros-commons-projects`) with the `targetClasses` and
`targetTests` package prefix updated to match the target module. Do not expand until Danny confirms the
pilot is complete.
