# Groovy Postbuild migration

New VectorCAST Freestyle jobs use the plugin's native
`VectorCASTPostBuildPublisher`. It provides the report-fragment summary,
description, badges, and build-result classification previously supplied by
the generated Groovy Postbuild script. New jobs therefore do not require
non-sandbox script approval or the `groovy-postbuild` plugin.

Existing jobs are not rewritten automatically. A job created by an older
plugin version can still contain a Groovy Postbuild publisher in its
`config.xml`. Keep the Groovy Postbuild plugin installed until each such job
has been recreated or deliberately migrated.

Recommended upgrade sequence:

1. Identify existing VectorCAST Freestyle jobs that use Groovy Postbuild.
2. Preserve each job's current configuration as a backup.
3. Recreate the job with the current VectorCAST job-creation form, or replace
   its legacy publisher through a reviewed configuration migration.
4. Run a representative build and confirm the build page contains the
   VectorCAST report summary, badges, description, JUnit results, and Jenkins
   Coverage report.
5. Remove the Groovy Postbuild plugin only after no remaining job configuration
   references it.

Pipeline jobs are unaffected: their reporting is performed by the generated
Pipeline and the plugin's Pipeline bridge APIs.
